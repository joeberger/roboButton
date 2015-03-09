package com.ndipatri.roboButton.dagger.providers;

import android.os.RemoteException;

import com.estimote.sdk.Region;
import com.ndipatri.roboButton.BeaconDiscoveryListener;

public interface BeaconDiscoveryProvider {
    public Region getMonitoredRegion();
    public void startBeaconDiscovery(BeaconDiscoveryListener listener);
    public void stopBeaconDiscovery() throws RemoteException;
}
