package com.ndipatri.arduinoButton.dagger.providers;

import android.content.Context;
import android.os.RemoteException;

import com.estimote.sdk.BeaconManager;
import com.ndipatri.arduinoButton.models.Button;

import java.util.HashSet;
import java.util.Set;

public class BluetoothProviderTestImpl implements BluetoothProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    private BeaconManager beaconManager;

    private BeaconManager.MonitoringListener btleListener;

    private Set<Button> availableButtons = new HashSet<Button>();

    private boolean isBluetoothSupported = false;
    private boolean isBluetoothEnabled = false;
    private boolean isDiscoveryCancelled = false;

    public BluetoothProviderTestImpl(Context context) {
        this.context = context;

        // NJD TODO - Need to provide a facade for this so we can mock out all Bluetooth behavior.
        beaconManager = new BeaconManager(context);
    }

    @Override
    public Set<Button> getAllNearbyButtons() {
        return availableButtons;
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
    public void cancelDiscovery() {
        this.isDiscoveryCancelled = true;
    }

    @Override
    public void startBTMonitoring(BeaconManager.MonitoringListener btleListener) {
        this.btleListener = btleListener;
    }

    @Override
    public void stopBTMonitoring() throws RemoteException {
        // NJD TODO - noop for now.. eventually, should track which regions are being ranged...
    }

    public BeaconManager.MonitoringListener getBtleListener() {
        return btleListener;
    }

    public boolean isDiscoveryCancelled() {
        return isDiscoveryCancelled;
    }

    public void setIsBluetoothEnabled(boolean isBluetoothEnabled) {
        this.isBluetoothEnabled = isBluetoothEnabled;
    }

    public void setAvailableButtons(Set<Button> availableButtons) {
        this.availableButtons = availableButtons;
    }
}
