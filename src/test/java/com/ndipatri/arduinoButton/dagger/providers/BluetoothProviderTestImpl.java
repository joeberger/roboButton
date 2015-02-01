package com.ndipatri.arduinoButton.dagger.providers;

import android.content.Context;
import android.os.RemoteException;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.ndipatri.arduinoButton.BeaconDistanceListener;
import com.ndipatri.arduinoButton.ButtonDiscoveryListener;
import com.ndipatri.arduinoButton.models.Button;

import java.util.HashSet;
import java.util.Set;

public class BluetoothProviderTestImpl implements BluetoothProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    private BeaconManager beaconManager;

    private BeaconDistanceListener beaconDistanceListener;

    private Set<Button> discoveredButtons = new HashSet<Button>();

    private boolean isBluetoothSupported = false;
    private boolean isBluetoothEnabled = false;
    private boolean isDiscoveryCancelled = false;

    public BluetoothProviderTestImpl(Context context) {
        this.context = context;

        beaconManager = new BeaconManager(context);
    }

    @Override
    public Region getMonitoredRegion() {
        return new Region("regionId", null, 1234, null);
    }

    @Override
    public void startButtonDiscovery(ButtonDiscoveryListener listener) {

    }

    @Override
    public void stopButtonDiscovery() {
        this.isDiscoveryCancelled = true;
    }

    @Override
    public boolean isBluetoothSupported() {
        return isBluetoothSupported;
    }

    public void setIsBluetoothSupported(boolean isBluetoothSupported) {
        this.isBluetoothSupported = isBluetoothSupported;
    }

    @Override
    public boolean isBluetoothEnabled() {
        return isBluetoothEnabled;
    }

    @Override
    public void startBeaconDiscovery(BeaconDistanceListener beaconDistanceListener) {
        this.beaconDistanceListener = beaconDistanceListener;
    }

    @Override
    public void stopBTMonitoring() throws RemoteException {
        // NJD TODO - noop for now.. eventually, should track which regions are being ranged...
    }

    public BeaconDistanceListener getBeaconDistanceListener() {
        return beaconDistanceListener;
    }

    public boolean isDiscoveryCancelled() {
        return isDiscoveryCancelled;
    }

    public void setIsBluetoothEnabled(boolean isBluetoothEnabled) {
        this.isBluetoothEnabled = isBluetoothEnabled;
    }

    public void setDiscoveredButtons(Set<Button> discoveredButtons) {
        this.discoveredButtons = discoveredButtons;
    }
}
