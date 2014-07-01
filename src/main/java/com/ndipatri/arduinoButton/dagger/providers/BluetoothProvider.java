package com.ndipatri.arduinoButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.estimote.sdk.BeaconManager;

public class BluetoothProvider {

    private static final String TAG = BluetoothProvider.class.getCanonicalName();

    private Context context;

    private BeaconManager beaconManager;

    public BluetoothProvider(Context context) {
        this.context = context;

        beaconManager = new BeaconManager(context);
    }

    public BluetoothAdapter getAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    public BeaconManager getBeaconManager() {
        return beaconManager;
    }
}
