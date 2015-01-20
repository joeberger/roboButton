package com.ndipatri.arduinoButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.RemoteException;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.ndipatri.arduinoButton.BeaconDistanceListener;
import com.ndipatri.arduinoButton.models.Button;

import java.util.Set;

public interface BluetoothProvider {

    public Region getMonitoredRegion();

    public Set<Button> getAllBondedButtons();

    public Button getBondedButton(String buttinId);

    public boolean isBluetoothSupported();

    public boolean isBluetoothEnabled();

    public void cancelDiscovery();

    // Low Power Bluetooth (BTLE) interface...
    public void startBTMonitoring(BeaconDistanceListener listener);

    public void stopBTMonitoring() throws RemoteException;
}
