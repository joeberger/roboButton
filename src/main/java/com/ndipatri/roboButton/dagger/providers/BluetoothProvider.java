package com.ndipatri.roboButton.dagger.providers;

import android.os.RemoteException;

import com.estimote.sdk.Region;
import com.ndipatri.roboButton.BeaconDistanceListener;
import com.ndipatri.roboButton.ButtonDiscoveryListener;

public interface BluetoothProvider {

    public Region getMonitoredRegion();

    public void startButtonDiscovery(ButtonDiscoveryListener listener);

    public void stopButtonDiscovery();

    public boolean isBluetoothSupported();

    public boolean isBluetoothEnabled();

    // Low Power Bluetooth (BTLE) interface...
    public void startBeaconDiscovery(BeaconDistanceListener listener);

    public void stopBTMonitoring() throws RemoteException;
}
