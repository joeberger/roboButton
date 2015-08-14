package com.ndipatri.roboButton.dagger.bluetooth.discovery.impl;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.PurpleButtonCommunicatorImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.ButtonDiscoveryProvider;

/**
 * This class will perform a Bluetooth Classic 'Discovery' operation.  After a defined timeout period,
 * a 'success' or 'failure' event will be emitted based on whether a device matching the
 * defined 'discoveryPattern' was found.
 */
public class PurpleButtonDiscoveryProviderImpl extends ButtonDiscoveryProvider {

    private static final String TAG = PurpleButtonDiscoveryProviderImpl.class.getCanonicalName();

    protected int buttonDiscoveryDurationMillis;

    String discoverableButtonPatternString;

    public PurpleButtonDiscoveryProviderImpl(Context context) {
        super(context);

        RBApplication.getInstance().getGraph().inject(this);

        discoverableButtonPatternString = context.getString(R.string.button_discovery_pattern);
        buttonDiscoveryDurationMillis = context.getResources().getInteger(R.integer.purple_button_discovery_duration_millis);

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        RBApplication.getInstance().registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    public synchronized void _startButtonDiscovery() {

        Log.d(TAG, "Beginning Purple Button Monitoring Process...");

        // This is an idempotent operation
        bluetoothProvider.startDiscovery();

        // We should not let scan run indefinitely as it consumes POWER!
        new Handler().postDelayed(discoveryTimeoutRunnable, buttonDiscoveryDurationMillis);
    }

    private Runnable discoveryTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (discovering) {
                buttonDiscoveryFinished();
            }
        }
    };

    @Override
    public synchronized void _stopButtonDiscovery() {
        Log.d(TAG, "Stopping Button Discovery...");

        bluetoothProvider.cancelDiscovery();
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    // These are always sent on UI thread from BluetoothAdapter.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getName() != null && device.getName().matches(discoverableButtonPatternString)) {
                    Log.d(TAG, "We have a nearby Purple RoboButton! + '" + device + "'.");

                    startButtonCommunicator(device);
                }
            }
        }
    };

    protected void startButtonCommunicator(BluetoothDevice discoveredDevice) {
        new PurpleButtonCommunicatorImpl(context, discoveredDevice, discoveredDevice.getAddress());
    }
}
