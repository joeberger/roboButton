package com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces;

import android.os.RemoteException;

public interface RegionDiscoveryProvider {
    
    // This should be a non-blocking call.
    public void startRegionDiscovery(boolean inBackground);
    
    public void stopRegionDiscovery() throws RemoteException;
}
