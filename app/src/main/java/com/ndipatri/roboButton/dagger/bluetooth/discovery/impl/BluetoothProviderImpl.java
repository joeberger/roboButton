package com.ndipatri.roboButton.dagger.bluetooth.discovery.impl;

import android.bluetooth.BluetoothAdapter;

public class BluetoothProviderImpl implements com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.BluetoothProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private BluetoothAdapter bluetoothAdapter;

    public BluetoothProviderImpl() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public boolean isBluetoothSupported() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    @Override
    public boolean isBluetoothEnabled() {
        boolean isEnabled = false;

        if (bluetoothAdapter != null) {
            isEnabled = bluetoothAdapter.isEnabled();
        }

        return isEnabled;
    }

    @Override
    public void startDiscovery() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.startDiscovery();
        }
    }

    @Override
    public void cancelDiscovery() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }
}
