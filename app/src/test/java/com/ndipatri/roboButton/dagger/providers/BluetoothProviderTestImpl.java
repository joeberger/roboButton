package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;

import com.ndipatri.roboButton.dagger.bluetooth.BluetoothProviderImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.BluetoothProvider;

public class BluetoothProviderTestImpl implements BluetoothProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    private boolean isBluetoothSupported = false;
    private boolean isBluetoothEnabled = false;

    public BluetoothProviderTestImpl(Context context) {
        this.context = context;
    }

    @Override
    public boolean isBluetoothSupported() {
        return isBluetoothSupported;
    }

    public void setIsBluetoothSupported(boolean isBluetoothSupported) {
        this.isBluetoothSupported = isBluetoothSupported;
    }

    @Override
    public boolean isBluetoothEnabled() {
        return isBluetoothEnabled;
    }

    public void setIsBluetoothEnabled(boolean isBluetoothEnabled) {
        this.isBluetoothEnabled = isBluetoothEnabled;
    }
}
