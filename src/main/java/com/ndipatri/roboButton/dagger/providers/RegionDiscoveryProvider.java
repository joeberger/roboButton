package com.ndipatri.roboButton.dagger.providers;

import android.os.RemoteException;

import com.estimote.sdk.Region;

public interface RegionDiscoveryProvider {
    
    // This should be a non-blocking call.
    public void startRegionDiscovery();
    
    public void stopRegionDiscovery() throws RemoteException;
}
