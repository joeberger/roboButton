package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;
import android.os.RemoteException;

import com.estimote.sdk.BeaconManager;
import com.ndipatri.roboButton.dagger.bluetooth.BluetoothProviderImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.RegionDiscoveryProvider;

public class RegionDiscoveryProviderTestImpl implements RegionDiscoveryProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    private BeaconManager beaconManager;

    private boolean isDiscoveryCancelled = false;

    public RegionDiscoveryProviderTestImpl(Context context) {
        this.context = context;

        beaconManager = new BeaconManager(context);
    }

    @Override
    public void startRegionDiscovery(boolean inBackground) {}

    @Override
    public void stopRegionDiscovery() throws RemoteException {
        // NJD TODO - noop for now.. eventually, should track which regions are being ranged...
    }

    public boolean isDiscoveryCancelled() {
        return isDiscoveryCancelled;
    }
}
