package com.ndipatri.arduinoButton.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.events.ArduinoButtonBluetoothDisabledEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonInformationEvent;
import com.ndipatri.arduinoButton.events.ButtonImageRequestEvent;
import com.ndipatri.arduinoButton.events.ButtonImageResponseEvent;
import com.ndipatri.arduinoButton.fragments.ArduinoButtonFragment;
import com.ndipatri.arduinoButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.InjectView;
import butterknife.Views;

// This class coordinates all BT communications from its various ArduinoButtonFragments.  This class
// schedules and provides thread resources for the querying and setting of button state.
public class MainControllerActivity extends Activity {

    private AtomicInteger imageRequestIdGenerator = new AtomicInteger();

    protected HashMap<Integer, String> pendingImageRequestMap = new HashMap<Integer, String>();

    private static final int REQUEST_ENABLE_BT = -101;
    private static final int DISCOVER_BUTTON_DEVICES = -102;

    private static final String TAG = MainControllerActivity.class.getCanonicalName();

    long buttonDiscoveryIntervalMillis = -1;

    // Handler which uses background thread to handle BT communications
    private MessageHandler bluetoothMessageHandler;

    private boolean shouldRun = false;

    // Set of all ArduinoButtons that are currently rendered into ArduinoButtonFragments
    private Set<String> arduinoButtonSet = new HashSet<String>();

    // The main ViewGroup to which all ArduinoButtonFragments are added.
    protected @InjectView(R.id.mainViewGroup) ViewGroup mainViewGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

        Views.inject(this);

        // Create thread for handling communication with Bluetooth
        // This thread only runs if it's passed a message.. so no need worrying about if it's running or not after this point.
        HandlerThread messageProcessingThread = new HandlerThread("Discovery_BluetoothCommunicationThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        messageProcessingThread.start();

        // Connect up above background thread's looper with our message processing handler.
        bluetoothMessageHandler = new MessageHandler(messageProcessingThread.getLooper());

        buttonDiscoveryIntervalMillis = getResources().getInteger(R.integer.button_discovery_interval_millis);
    }

    @Override
    public void onResume() {
        super.onResume();

        shouldRun = true;

        BusProvider.getInstance().register(this);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                scheduleImmediateButtonDiscoveryMessage();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        shouldRun = false;

        BusProvider.getInstance().unregister(this);

        forgetAllArduinoButtons();
    }

    public void chooseImage(String requestingButtonId) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");

        startActivityForResult(photoPickerIntent, getNextImageRequestId(requestingButtonId));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                scheduleButtonDiscoveryMessage();
            } else {
                Toast.makeText(this, "This application cannot run without Bluetooth enabled!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            String requestingButtonId = getButtonForRequestId(new Integer(requestCode));
            if (requestingButtonId != null && resultCode == RESULT_OK) {

                Uri selectedImage = returnedIntent.getData();
                BusProvider.getInstance().post(new ButtonImageResponseEvent(requestingButtonId, selectedImage));
            }
        }
    }

    private void scheduleImmediateButtonDiscoveryMessage() {
        bluetoothMessageHandler.queueDiscoverButtonRequest(0);
    }

    private void scheduleButtonDiscoveryMessage() {
        bluetoothMessageHandler.queueDiscoverButtonRequest(buttonDiscoveryIntervalMillis);
    }

    // Hands outgoing bluetooth messages to background thread.
    private final class MessageHandler extends Handler {

        public MessageHandler(Looper looper) {
            super(looper);
        }

        public void queueDiscoverButtonRequest(final long offsetMillis) {

            Message rawMessage = obtainMessage();
            rawMessage.what = DISCOVER_BUTTON_DEVICES;

            // To be handled by separate thread.
            sendMessageDelayed(rawMessage, offsetMillis);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case DISCOVER_BUTTON_DEVICES:

                    if (shouldRun) {
                        discoverButtonDevices();
                    }

                    break;
            }
        }
    }

    // This deals with adding/removing buttons based on which bluetooth devices are 'bonded' (paired).  No BT
    // communications is done here.
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
        Set<String> lostButtonSet = new HashSet<String>(arduinoButtonSet);

        Set<String> newAndExistingButtonIdSet = new HashSet<String>();

        for (final String foundButtonId : foundButtonIdToDeviceMap.keySet()) {

            if (arduinoButtonSet.contains(foundButtonId)) {

                // Existing button!
                final ArduinoButtonFragment existingButtonFragment = lookupButtonFragment(foundButtonId);
                if (existingButtonFragment != null && (existingButtonFragment.isNeverConnected() || existingButtonFragment.isConnected())) {

                    // We only preserve fragments that are either still connected OR have not yet successfully connected.

                    // this is NOT a lost button
                    lostButtonSet.remove(foundButtonId);
                    newAndExistingButtonIdSet.add(foundButtonId);
                }
            } else {
                // new button!
                final ArduinoButtonFragment newArduinoButtonFragment = ArduinoButtonFragment.newInstance(foundButtonId, foundButtonId, foundButtonIdToDeviceMap.get(foundButtonId)); // NJD TODO - need to prompt user to name each
                newAndExistingButtonIdSet.add(foundButtonId);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getFragmentManager().beginTransaction().add(R.id.mainViewGroup, newArduinoButtonFragment, getButtonFragmentTag(foundButtonId)).commitAllowingStateLoss();
                    }
                });
            }
        }

        // remove and disconnect all lost buttons
        for (String lostButtonId : lostButtonSet) {
            Log.d(TAG, "Forgetting lost button '" + lostButtonId + "'.");
            forgetArduinoButton(lostButtonId);
        }

        arduinoButtonSet = newAndExistingButtonIdSet;

        scheduleButtonDiscoveryMessage();
    }

    private void publishProgress(final String progressString)  {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainControllerActivity.this, progressString, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ArduinoButtonFragment lookupButtonFragment(String buttonId) {
        return (ArduinoButtonFragment) getFragmentManager().findFragmentByTag(getButtonFragmentTag(buttonId));
    }

    private String getButtonFragmentTag(String buttonId) {
        return "button_" + buttonId;
    }

    private synchronized void forgetAllArduinoButtons() {
        for (String buttonId : arduinoButtonSet) {
            forgetArduinoButton(buttonId);
        }
    }

    private synchronized void forgetArduinoButton(final String lostButtonId) {

        final ArduinoButtonFragment arduinoButtonFragment = lookupButtonFragment(lostButtonId);
        if (arduinoButtonFragment != null) {

            arduinoButtonFragment.disconnect();
            arduinoButtonSet.remove(lostButtonId);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getFragmentManager().beginTransaction().remove(arduinoButtonFragment).commitAllowingStateLoss();
                }
            });
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

    @Subscribe
    public void onArduinoButtonInformation(ArduinoButtonInformationEvent arduinoButtonInformationEvent) {
        Log.d(TAG, "Information received detected for button '" + arduinoButtonInformationEvent.buttonId + "(" + arduinoButtonInformationEvent.buttonDescription + ")'.");

        publishProgress(arduinoButtonInformationEvent.message);
    }

    @Subscribe
    public void onArduinoButtonBluetoothDisabled(ArduinoButtonBluetoothDisabledEvent arduinoButtonBluetoothDisabledEvent) {
        Log.d(TAG, "Bluetooth disabled!");

        onResume();
    }

    @Subscribe
    public void onButtonImageRequestEvent(ButtonImageRequestEvent request) {
        chooseImage(request.buttonId);
    }

    protected synchronized Integer getNextImageRequestId(String buttonId) {
        Integer nextImageRequestId = imageRequestIdGenerator.incrementAndGet();
        pendingImageRequestMap.put(nextImageRequestId, buttonId);

        return nextImageRequestId;
    }

    protected synchronized String getButtonForRequestId(Integer imageRequestId) {
        return pendingImageRequestMap.get(imageRequestId);
    }

    protected synchronized  void finishedRequest(Integer imageRequestId) {
        pendingImageRequestMap.remove(imageRequestId);
    }
}
