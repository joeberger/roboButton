package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;
import android.os.RemoteException;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.ndipatri.roboButton.BeaconDiscoveryListener;

public class BeaconDiscoveryProviderTestImpl implements BeaconDiscoveryProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    private BeaconManager beaconManager;

    private BeaconDiscoveryListener beaconDiscoveryListener;

    private boolean isDiscoveryCancelled = false;

    public BeaconDiscoveryProviderTestImpl(Context context) {
        this.context = context;

        beaconManager = new BeaconManager(context);
    }

    @Override
    public Region getMonitoredRegion() {
        return new Region("regionId", null, 1234, null);
    }

    @Override
    public void startBeaconDiscovery(BeaconDiscoveryListener beaconDiscoveryListener) {
        this.beaconDiscoveryListener = beaconDiscoveryListener;
    }

    @Override
    public void stopBeaconDiscovery() throws RemoteException {
        // NJD TODO - noop for now.. eventually, should track which regions are being ranged...
    }

    public BeaconDiscoveryListener getBeaconDiscoveryListener() {
        return beaconDiscoveryListener;
    }

    public boolean isDiscoveryCancelled() {
        return isDiscoveryCancelled;
    }
}
