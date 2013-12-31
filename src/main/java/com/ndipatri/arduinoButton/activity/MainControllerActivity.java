package com.ndipatri.arduinoButton.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.ndipatri.arduinoButton.R;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import butterknife.InjectView;
import butterknife.Views;

public class MainControllerActivity extends Activity {

    public static final String EXTERNAL_BLUETOOTH_DEVICE_MAC = "macAddress";

    private static final int REQUEST_ENABLE_BT = 1;

    private static final int QUERY_STATE_MESSAGE = 0;
    private static final int SET_STATE_MESSAGE = 1;
    private static final int DISCOVER_BUTTON_DEVICES = 2;

    private static final String TAG = MainControllerActivity.class.getCanonicalName();

    // This ensures that we are periodically querying the remote state and discovery of bluetooth buttons.
    private Handler buttonQueryHandler = null;
    private Runnable buttonQueryRunnable = null;
    long queryStateIntervalMillis = -1;
    long buttonDiscoveryIntervalMillis = -1;

    // Handler which passes outgoing bluetooth messages background thread to be processed.
    private MessageHandler bluetoothMessageHandler;

    private boolean shouldRun = false;

    private Map<String, ArduinoButton> arduinoButtonMap = new HashMap<String, ArduinoButton>();

    protected @InjectView(R.id.mainViewGroup) ViewGroup mainViewGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

        Views.inject(this);

        // Create thread for handling communication with Bluetooth
        // This thread only runs if it's passed a message.. so no need worrying about if it's running or not after this point.
        HandlerThread messageProcessingThread = new HandlerThread("BluetoothCommunicationThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        messageProcessingThread.start();

        // Connect up above background thread's looper with our message processing handler.
        bluetoothMessageHandler = new MessageHandler(messageProcessingThread.getLooper());

        // Periodically query remote state...
        queryStateIntervalMillis = getResources().getInteger(R.integer.remote_state_check_interval_millis);
        buttonQueryHandler = new Handler(getMainLooper());

        buttonDiscoveryIntervalMillis = getResources().getInteger(R.integer.button_discovery_interval_millis);
    }

    @Override
    public void onResume() {
        super.onResume();

        shouldRun = true;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                _onResume();
            }
        }
    }

    private void _onResume() {

        // These both will reschedule themselves as long as this activity is resumed...
        scheduleButtonDiscoveryMessage();
        scheduleQueryStateMessage();
    }

    @Override
    public void onPause() {
        super.onPause();

        shouldRun = false;

        buttonQueryHandler.removeCallbacks(buttonQueryRunnable);
        bluetoothMessageHandler.removeMessages(QUERY_STATE_MESSAGE);
        bluetoothMessageHandler.removeMessages(SET_STATE_MESSAGE);

        forgetAllArduinoButtons();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                _onResume();
            } else {
                Toast.makeText(this, "This application cannot run without Bluetooth enabled!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void scheduleQueryStateMessage() {
        buttonQueryHandler.postDelayed(new QueueQueryStateRunnable(), queryStateIntervalMillis);
    }

    private void scheduleButtonDiscoveryMessage() {
        buttonQueryHandler.postDelayed(new ButtonDiscoveryRunnable(), queryStateIntervalMillis);
    }

    private class QueueQueryStateRunnable implements Runnable {
        @Override
        public void run() {
            if (shouldRun) {
                bluetoothMessageHandler.queueQueryStateRequest();
            }
        }
    }

    private class ButtonDiscoveryRunnable implements Runnable {
        @Override
        public void run() {
            if (shouldRun) {
                bluetoothMessageHandler.queueDiscoverButtonRequest();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_controller_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.forget_all_buttons:

                forgetAllArduinoButtons();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Hands outgoing bluetooth messages to background thread.
    private final class MessageHandler extends Handler {

        public MessageHandler(Looper looper) {
            super(looper);
        }

        public void queueQueryStateRequest() {

            // We only queue a query request if NO requests are already pending...
            if (!hasMessages(SET_STATE_MESSAGE) &&
                !hasMessages(QUERY_STATE_MESSAGE)) {

                // Queue a QueryState Message
                Message rawMessage = obtainMessage();
                rawMessage.what = QUERY_STATE_MESSAGE;

                // To be handled by separate thread.
                sendMessage(rawMessage);
            }
        }

        public void queueDiscoverButtonRequest() {

            Message rawMessage = obtainMessage();
            rawMessage.what = DISCOVER_BUTTON_DEVICES;

            // To be handled by separate thread.
            sendMessage(rawMessage);
        }

        public void queueSetStateRequest() {

            // If a set request is already pending, do nothing.
            if (!bluetoothMessageHandler.hasMessages(SET_STATE_MESSAGE)) {

                // A set request pre-empts any pending query request
                bluetoothMessageHandler.removeMessages(QUERY_STATE_MESSAGE);

                Message rawMessage = bluetoothMessageHandler.obtainMessage();
                rawMessage.what = SET_STATE_MESSAGE;

                // To be handled by separate thread.
                bluetoothMessageHandler.sendMessage(rawMessage);
            }
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case QUERY_STATE_MESSAGE:

                    if (shouldRun) {
                        queryRemoteState();
                    }

                    break;

                case SET_STATE_MESSAGE:

                    setRemoteState();

                    break;

                case DISCOVER_BUTTON_DEVICES:

                    discoverButtonDevices();

                    break;
            }
        }
    }

    private void publishProgress(final String progressString)  {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainControllerActivity.this, progressString, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private synchronized void queryRemoteState() {

        for (final ArduinoButton arduinoButton : arduinoButtonMap.values()) {

            Log.d(TAG, "queryRemoteState()");
            arduinoButton.readRemoteState();
        }

        scheduleQueryStateMessage();
    }

    private synchronized void setRemoteState() {

        for (ArduinoButton arduinoButton : arduinoButtonMap.values()) {

            Log.d(TAG, "setRemoteState()");
            try {
                arduinoButton.setRemoteState();
            } catch (Exception ex) {
                publishProgress(getString(R.string.transmission_failure));
            }
        }
    }

    private synchronized void discoverButtonDevices() {

        Map<String, BluetoothDevice> foundButtonIdToDeviceMap = new HashMap<String, BluetoothDevice>();

        String discoverableButtonPatternString = getString(R.string.button_discovery_pattern);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains(discoverableButtonPatternString)) {
                    Log.d(TAG, "We have a paired ArduinoButton device!");

                    foundButtonIdToDeviceMap.put(device.getAddress(), device);
                }
            }
        }

        // To keep track of buttons that have gone missing.
        Set<String> lostButtonSet = new HashSet<String>(arduinoButtonMap.keySet());

        Map<String, ArduinoButton> combinedButtonMap = new HashMap<String, ArduinoButton>();

        for (String foundButtonId : foundButtonIdToDeviceMap.keySet()) {
            if (arduinoButtonMap.containsKey(foundButtonId)) {

                // Existing button!
                final ArduinoButton existingButton = arduinoButtonMap.get(foundButtonId);
                if ((existingButton.socket != null) && (existingButton.socket.isConnected())) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mainViewGroup.addView(existingButton.getRootViewGroup());
                        }
                    });

                    // this is not a lost button
                    lostButtonSet.remove(foundButtonId);

                    // Preserve existing buttons
                    combinedButtonMap.put(foundButtonId, arduinoButtonMap.get(foundButtonId));
                }
            } else {
                final ArduinoButton newArduinoButton = new ArduinoButton(foundButtonId, foundButtonId, foundButtonIdToDeviceMap.get(foundButtonId)); // NJD TODO - need to prompt user to name each
                combinedButtonMap.put(foundButtonId, newArduinoButton);
            }
        }

        // remove and disconnect all lost buttons
        for (String lostButtonId : lostButtonSet) {
            Log.d(TAG, "Forgetting lost button '" + lostButtonId + "'.");
            forgetArduinoButton(arduinoButtonMap.get(lostButtonId));
        }

        arduinoButtonMap = combinedButtonMap;

        scheduleButtonDiscoveryMessage();
    }

    private synchronized void forgetAllArduinoButtons() {
        for (ArduinoButton button : arduinoButtonMap.values()) {
            forgetArduinoButton(button);
        }
    }

    private synchronized void forgetArduinoButton(final ArduinoButton arduinoButton) {

        arduinoButton.disconnect();
        arduinoButtonMap.remove(arduinoButton.buttonId);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainViewGroup.removeView(arduinoButton.getRootViewGroup());
            }
        });
    }
}
