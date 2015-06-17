package com.ndipatri.roboButton.dagger.bluetooth.discovery.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.daos.ButtonDao;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.events.ButtonDiscoveryEvent;
import com.ndipatri.roboButton.utils.BusProvider;

import javax.inject.Inject;

/**
 * This class will perform a Bluetooth Classic 'Discovery' operation.  After a defined timeout period, the scan will
 * be stopped.  At that time a 'success' or 'failure' event will be emitted based on whether a device matching the
 * defined 'discoveryPattern' was found.
 */
public class PurpleButtonDiscoveryProviderImpl implements ButtonDiscoveryProvider {

    private static final String TAG = PurpleButtonDiscoveryProviderImpl.class.getCanonicalName();

    private Context context;

    BluetoothAdapter bluetoothAdapter = null;

    protected int buttonDiscoveryDurationMillis;

    String discoverableButtonPatternString;

    protected boolean discovering = false;

    protected BluetoothDevice discoveredButton;

    @Inject
    BusProvider bus;

    @Inject
    ButtonDao buttonDao;

    public PurpleButtonDiscoveryProviderImpl(Context context) {
        this.context = context;

        RBApplication.getInstance().getGraph().inject(this);

        discoverableButtonPatternString = context.getString(R.string.button_discovery_pattern);
        buttonDiscoveryDurationMillis = context.getResources().getInteger(R.integer.button_discovery_duration_millis);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        RBApplication.getInstance().registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    public synchronized void startButtonDiscovery() {

        Log.d(TAG, "Beginning Purple Button Monitoring Process...");
        if (discovering) {
            // make this request idempotent
            return;
        }

        //Check to see if the device supports Bluetooth and that it's turned on
        if (!discovering && bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {

            discovering = true;

            // This is an idempotent operation
            bluetoothAdapter.startDiscovery();

            // We should not let scan run indefinitely as it consumes POWER!
            new Handler().postDelayed(discoveryTimeoutRunnable, buttonDiscoveryDurationMillis);
        } else {
            discovering = false;
        }
    }

    private Runnable discoveryTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (discovering) {

                stopButtonDiscovery();

                if (discoveredButton == null) {
                    postButtonDiscoveredEvent(false, null);
                }
            }
        }
    };

    @Override
    public synchronized void stopButtonDiscovery() {
        Log.d(TAG, "Stopping Button Discovery...");

        discovering = false;

        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    // These are always sent on UI thread from BluetoothAdapter.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getName() != null && device.getName().matches(discoverableButtonPatternString)) {
                    Log.d(TAG, "We have a nearby ArduinoButton device! + '" + device + "'.");

                    discoveredButton = device;
                    postButtonDiscoveredEvent(true, device);
                }
            }
        }
    };

    protected void postButtonDiscoveredEvent(final boolean success, final BluetoothDevice device) {
        bus.post(new ButtonDiscoveryEvent(success, ButtonType.PURPLE_BUTTON, device == null ? null : device.getAddress(), device));
    }
}
