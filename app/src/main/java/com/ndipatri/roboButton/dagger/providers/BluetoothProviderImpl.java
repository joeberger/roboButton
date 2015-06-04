package com.ndipatri.roboButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public class BluetoothProviderImpl implements BluetoothProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    public BluetoothProviderImpl(Context context) {
        this.context = context;
    }

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