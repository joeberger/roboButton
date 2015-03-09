package com.ndipatri.roboButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.ndipatri.roboButton.ButtonDiscoveryListener;
import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.models.Button;

import javax.inject.Inject;

public class ButtonDiscoveryProviderImpl implements ButtonDiscoveryProvider {

    private static final String TAG = ButtonDiscoveryProviderImpl.class.getCanonicalName();

    @Inject
    ButtonProvider buttonProvider;

    private Context context;

    private ButtonDiscoveryListener buttonDiscoveryListener;

    String discoverableButtonPatternString;

    public ButtonDiscoveryProviderImpl(Context context) {
        this.context = context;

        RBApplication.getInstance().registerForDependencyInjection(this);

        discoverableButtonPatternString = context.getString(R.string.button_discovery_pattern);

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        RBApplication.getInstance().registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    public void startButtonDiscovery(ButtonDiscoveryListener buttonDiscoveryListener) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isDiscovering()) {
            this.buttonDiscoveryListener = buttonDiscoveryListener;
            bluetoothAdapter.startDiscovery();
        }
    }

    @Override
    public void stopButtonDiscovery() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            buttonDiscoveryListener = null;
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (buttonDiscoveryListener != null && device.getName() != null && device.getName().matches(discoverableButtonPatternString)) {
                    Log.d(TAG, "We have a nearby ArduinoButton device! + '" + device + "'.");

                    Button discoveredButton = null;

                    Button persistedButton = buttonProvider.getButton(device.getAddress());
                    if (persistedButton != null) {
                        discoveredButton = persistedButton;
                    } else {
                        discoveredButton = new Button(device.getAddress(), device.getAddress(), true);
                    }
                    discoveredButton.setBluetoothDevice(device);

                    buttonProvider.createOrUpdateButton(discoveredButton);

                    buttonDiscoveryListener.buttonDiscovered(discoveredButton);
                }
            }
        }
    };
}
