package com.ndipatri.arduinoButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.estimote.sdk.BeaconManager;

public interface BluetoothProvider {

    public BluetoothAdapter getAdapter();

    public BeaconManager getBeaconManager();
}
