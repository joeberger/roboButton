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

    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private static final int REQUEST_ENABLE_BT = 1;

    private static final int QUERY_STATE_MESSAGE = 0;
    private static final int SET_STATE_MESSAGE = 1;
    private static final int DISCOVER_BUTTON_DEVICES = 2;

    private static final String TAG = MainControllerActivity.class.getCanonicalName();

    // Handler which passes outgoing bluetooth messages background thread to be processed.
    private MessageHandler bluetoothMessageHandler;

    // This ensures that we are periodically querying the remote state and discovery of bluetooth buttons.
    private Handler buttonQueryHandler = null;
    private Runnable buttonQueryRunnable = null;
    long queryStateIntervalMillis = -1;
    long buttonDiscoveryIntervalMillis = -1;

    private boolean stateBoolean = false;

    private int failedToConnectCount = 0;

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
            if (!bluetoothMessageHandler.hasMessages(SET_STATE_MESSAGE) &&
                !bluetoothMessageHandler.hasMessages(QUERY_STATE_MESSAGE)) {

                // Queue a QueryState Message
                Message rawMessage = bluetoothMessageHandler.obtainMessage();
                rawMessage.what = QUERY_STATE_MESSAGE;

                // To be handled by separate thread.
                bluetoothMessageHandler.sendMessage(rawMessage);
            }
        }

        public void queueDiscoverButtonRequest() {

            Message rawMessage = bluetoothMessageHandler.obtainMessage();
            rawMessage.what = DISCOVER_BUTTON_DEVICES;

            // To be handled by separate thread.
            bluetoothMessageHandler.sendMessage(rawMessage);
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

            BluetoothSocket bluetoothSocket = arduinoButton.socket;
            Log.d(TAG, "queryRemoteState()");
            final ByteBuffer byteBuffer = ByteBuffer.allocate(1);

            try {
                if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                    Log.d(TAG, "Trying to create bluetooth connection...");
                    publishProgress(getString(R.string.opening_bluetooth_socket));
                    bluetoothSocket = createConnectionToBluetoothDevice(BluetoothAdapter.getDefaultAdapter(), arduinoButton.device);
                }

                if (bluetoothSocket != null) {
                    Log.d(TAG, "Bluetooth connect. Getting output stream ...");

                    failedToConnectCount = 0;

                    // Tell Arduino to send us StateReport
                    OutputStream outputStream = bluetoothSocket.getOutputStream();
                    outputStream.write(new byte[]{0x51, 0x51, 0x51}); // 'QQQ'
                    Log.d(TAG, "StateRequestUpdate sent! Waiting for reply...");

                    // For now, assume one byte state response...
                    final byte[] remoteStateBytes = new byte[1];

                    // NJD TODO - Need watchdog thread on these blocking calls (so we can call socket.close() if need be)
                    final int bytesRead = bluetoothSocket.getInputStream().read(remoteStateBytes);

                    if (bytesRead != 1) {
                        publishProgress(getString(R.string.transmission_failure));
                        forgetArduinoButton(arduinoButton);
                        return;
                    } else {
                        Log.d(TAG, "Reply received.");
                    }
                    byteBuffer.put(remoteStateBytes);
                    byteBuffer.flip();  // prepare for reading.
                } else {
                    Log.d(TAG, "Cannot create bluetooth socket!");

                    failedToConnectCount++;

                    if (failedToConnectCount > getResources().getInteger(R.integer.max_reconnect_attempts)) {
                        failedToConnectCount = 0;

                        forgetArduinoButton(arduinoButton);
                    }
                }

            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                publishProgress(getString(R.string.transmission_failure));
                Log.d(TAG, "Socket connect exception!", connectException);
                try {
                    bluetoothSocket.close();
                } catch (IOException ignored) {
                }
            }

            if (byteBuffer.hasRemaining()) {
                // queues up this update runnable on UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // Currently, the response is a single character: an ASCII representation of either a '0' or '1'
                        String responseChar = String.valueOf(new char[]{(char) byteBuffer.get()});
                        Log.d(TAG, "Response from bluetooth device '" + arduinoButton + " ', '" + responseChar + "'.");
                        try {
                            arduinoButton.setState(Integer.valueOf(responseChar) > 0);
                        } catch (NumberFormatException nex) {
                            Log.d(TAG, "Invalid response from bluetooth device: '" + arduinoButton + "'.");

                            publishProgress(getString(R.string.transmission_failure));
                            forgetArduinoButton(arduinoButton);
                        }
                    }
                });
            }
        }

        scheduleQueryStateMessage();
    }

    private synchronized void forgetAllArduinoButtons() {
        for (ArduinoButton button : arduinoButtonMap.values()) {
            forgetArduinoButton(button);
        }
    }

    private synchronized void forgetArduinoButton(final ArduinoButton arduinoButton) {

        if (arduinoButton.socket != null) {
            try {
                arduinoButton.socket.close();
            } catch (IOException ignored) {
            }
        }

        arduinoButtonMap.remove(arduinoButton.buttonId);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainViewGroup.removeView(arduinoButton.getRootViewGroup());
            }
        });
    }

    private synchronized void setRemoteState() {

        for (ArduinoButton arduinoButton : arduinoButtonMap.values()) {

            BluetoothSocket bluetoothSocket = arduinoButton.socket;
            Log.d(TAG, "setRemoteState()");
            try {
                if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                    publishProgress(getString(R.string.opening_bluetooth_socket));
                    bluetoothSocket = createConnectionToBluetoothDevice(BluetoothAdapter.getDefaultAdapter(), arduinoButton.device);
                }

                if (bluetoothSocket != null) {
                    ByteBuffer desiredState = renderRemoteStateFromLocalViews();

                    OutputStream outputStream = bluetoothSocket.getOutputStream();
                    outputStream.write(new byte[]{0x58, 0x58, 0x58}); // 'XXX' - StateChangeRequest
                    outputStream.write(desiredState.array());
                }
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                publishProgress(getString(R.string.transmission_failure));
                Log.d(TAG, "Socket connect exception!", connectException);
                if (bluetoothSocket != null) {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException ignored) {
                    }
                }
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
                    Log.d(TAG, "Discovered '" + discoverableButtonPatternString + "' device!");

                    foundButtonIdToDeviceMap.put(device.getAddress(), device);
                }
            }
        }

        Set<String> lostButtonSet = new HashSet<String>(arduinoButtonMap.keySet());

        Map<String, ArduinoButton> combinedButtonMap = new HashMap<String, ArduinoButton>();

        for (String foundButtonId : foundButtonIdToDeviceMap.keySet()) {
            if (arduinoButtonMap.containsKey(foundButtonId)) {
                // Need to keep track of these for cleanup
                lostButtonSet.remove(foundButtonId);

                // Preserve existing buttons
                combinedButtonMap.put(foundButtonId, arduinoButtonMap.get(foundButtonId));
            } else {
                final ArduinoButton newArduinoButton = new ArduinoButton(foundButtonId, foundButtonId, foundButtonIdToDeviceMap.get(foundButtonId)); // NJD TODO - need to prompt user to name each
                combinedButtonMap.put(foundButtonId, newArduinoButton);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainViewGroup.addView(newArduinoButton.getRootViewGroup());
                    }
                });
            }
        }

        // remove and disconnect all lost buttons
        for (String lostButtonId : lostButtonSet) {
            forgetArduinoButton(arduinoButtonMap.get(lostButtonId));
        }

        arduinoButtonMap = combinedButtonMap;

        scheduleButtonDiscoveryMessage();
    }

    private ByteBuffer renderRemoteStateFromLocalViews() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(1);

        if (stateBoolean) {
            byteBuffer.put((byte) '1'); // a char is the ascii representation of '1'
        } else {
            byteBuffer.put((byte) '0');
        }

        // prepare for reading
        byteBuffer.flip();

        return byteBuffer;
    }

    // Should not be run in UI thread.
    private BluetoothSocket createConnectionToBluetoothDevice(BluetoothAdapter bluetoothAdapter, BluetoothDevice bluetoothDevice) {

        BluetoothSocket bluetoothSocket = null;

        try {
            Log.d(TAG, "Creating Bluetooth Socket ...");
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));

            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            // NJD TODO - really need timeout mechanism here..
            bluetoothSocket.connect();

            Log.d(TAG, "Success!");

        } catch (IOException connectException) {

            Log.e(TAG, "Failed with Exception!", connectException);
            if (bluetoothSocket != null) {
                try { bluetoothSocket.close(); } catch (IOException ignored) { }
            }

            if (connectException.getMessage().contains("Bluetooth is off")) {
                onResume();
            }

            bluetoothSocket = null;
        }

        return bluetoothSocket;
    }

    public class ArduinoButton {

        private boolean enabled = true;
        private ImageView imageView = null;
        private ViewGroup rootViewGroup = null;
        private BluetoothDevice device = null;
        private BluetoothSocket socket = null;
        private boolean state = false;
        private String description = null;
        private String buttonId;

        public ArduinoButton(String description, String buttonId, BluetoothDevice device) {
            this.device = device;
            this.description = description;
            this.buttonId = buttonId;

            rootViewGroup = (ViewGroup) LayoutInflater.from(MainControllerActivity.this).inflate(R.layout.button_layout, null);
            imageView = (ImageView) rootViewGroup.findViewById(R.id.imageView);
            imageView.setImageResource(R.drawable.yellow_button);

            rootViewGroup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Button Pressed!");

                    if (enabled) {
                        enabled = false;
                        imageView.setImageResource(R.drawable.yellow_button);
                        state = !state;
                        bluetoothMessageHandler.queueSetStateRequest();
                    }
                }
            });
        }

        public void setState(boolean state) {

            this.enabled = true;
            this.state = state;

            if (state) {
                Log.d(TAG, "State is ON");
                imageView.setImageResource(R.drawable.green_button);
            } else {
                Log.d(TAG, "State is OFF");
                imageView.setImageResource(R.drawable.red_button);
            }
        }

        public void disconnect() {
            if (socket != null) {
                Log.d(TAG, "Shutting down Bluetooth Socket for Button('" + description + "').");
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        public ViewGroup getRootViewGroup() {
            return rootViewGroup;
        }
    }
}
