package com.ndipatri.arduinoButton.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.enums.ButtonState;
import com.ndipatri.arduinoButton.events.ArduinoButtonFoundEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonLostEvent;
import com.ndipatri.arduinoButton.utils.BusProvider;
import com.ndipatri.arduinoButton.utils.ButtonMonitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ndipatri on 5/29/14.
 */
public class ButtonDiscoveryService extends Service {

    public static final String TAG = ButtonDiscoveryService.class.getCanonicalName();

    private static final int DISCOVER_BUTTON_DEVICES = -102;

    long buttonDiscoveryIntervalMillis = -1;

    // Handler which uses background thread to handle BT communications
    private MessageHandler bluetoothMessageHandler;

    private boolean running = false;

    HashMap<String, ButtonMonitor> currentButtonMap = new HashMap<String, ButtonMonitor>();

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BusProvider.getInstance().unregister(this);
        running = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!running) {

            BusProvider.getInstance().register(this);

            // Create thread for handling communication with Bluetooth
            // This thread only runs if it's passed a message.. so no need worrying about if it's running or not after this point.
            HandlerThread messageProcessingThread = new HandlerThread("Discovery_BluetoothCommunicationThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
            messageProcessingThread.start();

            // Connect up above background thread's looper with our message processing handler.
            bluetoothMessageHandler = new MessageHandler(messageProcessingThread.getLooper());

            buttonDiscoveryIntervalMillis = getResources().getInteger(R.integer.button_discovery_interval_millis);

            scheduleImmediateButtonDiscoveryMessage();

            running = true;
        }

        return Service.START_FLAG_REDELIVERY; // this ensure the service is restarted
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

                    if (running) {
                        discoverButtonDevices();
                    }

                    break;
            }
        }
    }

    // This deals with adding/removing buttons based on which bluetooth devices are 'bonded' (paired).  No BT
    // communications is done here.
    private synchronized void discoverButtonDevices() {

        final Set<BluetoothDevice> foundBluetoothDevices = new HashSet<BluetoothDevice>();

        String discoverableButtonPatternString = getString(R.string.button_discovery_pattern);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains(discoverableButtonPatternString)) {
                    Log.d(TAG, "We have a paired ArduinoButton device! + '" + device + "'.");

                    foundBluetoothDevices.add(device);
                }
            }
        }

        // To keep track of buttons that have gone missing.
        final Set<String> lostButtonSet = new HashSet<String>(currentButtonMap.keySet());

        final HashMap<String, ButtonMonitor> newAndExistingButtonMap = new HashMap<String, ButtonMonitor>();

        for (final BluetoothDevice foundBluetoothDevice : foundBluetoothDevices) {

            String buttonId = getButtonId(foundBluetoothDevice);

            ButtonMonitor buttonMonitor = currentButtonMap.get(buttonId);

            if (buttonMonitor != null &&
                    ((buttonMonitor.getButtonState() == ButtonState.NEVER_CONNECTED) ||
                     (buttonMonitor.getButtonState() != ButtonState.DISCONNECTED))) {

                // Existing button found!
                // This is either an existing button that has yet to connect OR it's a connected button
                lostButtonSet.remove(foundBluetoothDevice);
                newAndExistingButtonMap.put(buttonId, buttonMonitor);
            } else {
                // new button!

                newAndExistingButtonMap.put(buttonId, new ButtonMonitor(getApplicationContext(), foundBluetoothDevice));
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {

                        // Note, just because we can 'see' a bluetooth button doesn't mean we can
                        // communicate with it...
                        BusProvider.getInstance().post(new ArduinoButtonFoundEvent(foundBluetoothDevice));
                    }
                });
            }
        }

        // remove and disconnect all lost buttons
        for (final String lostButtonId : lostButtonSet) {
            Log.d(TAG, "Forgetting lost button '" + lostButtonId + "'.");

            final ButtonMonitor lostButtonMonitor = currentButtonMap.get(lostButtonId);
            lostButtonMonitor.stop();

            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    BusProvider.getInstance().post(new ArduinoButtonLostEvent(lostButtonMonitor.getBluetoothDevice()));
                }
            });
        }

        currentButtonMap = newAndExistingButtonMap;

        scheduleButtonDiscoveryMessage();
    }

    protected String getButtonId(final BluetoothDevice bluetoothDevice) {
        return bluetoothDevice.getAddress();
    }
}
