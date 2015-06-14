package com.ndipatri.roboButton.dagger.bluetooth.discovery.stubs;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public class BluetoothProviderStub implements com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.BluetoothProvider {

    private static final String TAG = BluetoothProviderStub.class.getCanonicalName();

    @Override
    public boolean isBluetoothSupported() {
        return true;
    }

    @Override
    public boolean isBluetoothEnabled() {
        return true;
    }
}
