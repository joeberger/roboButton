package com.ndipatri.roboButton.dagger.bluetooth.discovery.impl;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public class BluetoothProviderImpl implements com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.BluetoothProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    @Override
    public boolean isBluetoothSupported() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    @Override
    public boolean isBluetoothEnabled() {
        boolean isEnabled = false;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            isEnabled = bluetoothAdapter.isEnabled();
        }

        return isEnabled;
    }
}
