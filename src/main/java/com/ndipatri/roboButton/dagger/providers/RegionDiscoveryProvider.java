package com.ndipatri.roboButton.dagger.providers;

import android.os.RemoteException;

import com.estimote.sdk.Region;
import com.ndipatri.roboButton.RegionDiscoveryListener;

public interface RegionDiscoveryProvider {
    public void startRegionDiscovery(RegionDiscoveryListener listener);
    public void stopRegionDiscovery() throws RemoteException;
}
