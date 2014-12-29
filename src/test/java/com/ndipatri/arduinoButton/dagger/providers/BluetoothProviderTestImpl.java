package com.ndipatri.arduinoButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.estimote.sdk.BeaconManager;

public class BluetoothProviderTestImpl implements BluetoothProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    private BeaconManager beaconManager;

    public BluetoothProviderTestImpl(Context context) {
        this.context = context;

        // NJD TODO - Need to provide a facade for this so we can mock out all Bluetooth behavior.
        beaconManager = new BeaconManager(context);
    }

    public BluetoothAdapter getAdapter() {
        // NJD TODO - Need to provide a facade for this so we can mock out all Bluetooth behavior.
        return BluetoothAdapter.getDefaultAdapter();
    }

    public BeaconManager getBeaconManager() {
        return beaconManager;
    }
}
