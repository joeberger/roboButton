package com.ndipatri.arduinoButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.RemoteException;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.Set;

public interface BluetoothProvider {

    public Set<BluetoothDevice> getPairedDevices();

    public boolean isBluetoothSupported();

    public boolean isBluetoothEnabled();

    public void cancelDiscovery();

    // Low Power Bluetooth (BTLE) interface...
    public void setBTLEListener(BeaconManager.MonitoringListener listener);

    public void disconnectFromBTLEServiceAndStopRanging(Region region) throws RemoteException;
}
