package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;
import android.os.RemoteException;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.ndipatri.roboButton.RegionDiscoveryListener;

public class RegionDiscoveryProviderTestImpl implements RegionDiscoveryProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    private BeaconManager beaconManager;

    private RegionDiscoveryListener regionDiscoveryListener;

    private boolean isDiscoveryCancelled = false;

    public RegionDiscoveryProviderTestImpl(Context context) {
        this.context = context;

        beaconManager = new BeaconManager(context);
    }

    @Override
    public Region getMonitoredRegion() {
        return new Region("regionId", null, 1234, null);
    }

    @Override
    public void startRegionDiscovery(RegionDiscoveryListener regionDiscoveryListener) {
        this.regionDiscoveryListener = regionDiscoveryListener;
    }

    @Override
    public void stopRegionDiscovery() throws RemoteException {
        // NJD TODO - noop for now.. eventually, should track which regions are being ranged...
    }

    public RegionDiscoveryListener getRegionDiscoveryListener() {
        return regionDiscoveryListener;
    }

    public boolean isDiscoveryCancelled() {
        return isDiscoveryCancelled;
    }
}
