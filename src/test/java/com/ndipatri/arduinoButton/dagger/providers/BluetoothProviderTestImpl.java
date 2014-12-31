package com.ndipatri.arduinoButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.RemoteException;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.HashSet;
import java.util.Set;

public class BluetoothProviderTestImpl implements BluetoothProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    private BeaconManager beaconManager;

    private BeaconManager.MonitoringListener btleListener;

    private Set<BluetoothDevice> pairedDevices = new HashSet<BluetoothDevice>();

    private boolean isBluetoothSupported = false;
    private boolean isBluetoothEnabled = false;
    private boolean isDiscoveryCancelled = false;

    public BluetoothProviderTestImpl(Context context) {
        this.context = context;

        // NJD TODO - Need to provide a facade for this so we can mock out all Bluetooth behavior.
        beaconManager = new BeaconManager(context);
    }

    public Set<BluetoothDevice> getPairedDevices() {
        return pairedDevices;
    }

    @Override
    public boolean isBluetoothSupported() {
        return false;
    }

    public void setIsBluetoothSupported(boolean isBluetoothSupported) {
        this.isBluetoothSupported = isBluetoothSupported;
    }

    @Override
    public boolean isBluetoothEnabled() {
        return false;
    }

    @Override
    public void cancelDiscovery() {
        this.isDiscoveryCancelled = true;
    }

    @Override
    public void setBTLEListener(BeaconManager.MonitoringListener btleListener) {
        this.btleListener = btleListener;
    }

    @Override
    public void disconnectFromBTLEServiceAndStopRanging(Region region) throws RemoteException {
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

    public void setPairedDevices(Set<BluetoothDevice> pairedDevices) {
        this.pairedDevices = pairedDevices;
    }


    public BeaconManager getBeaconManager() {
        return beaconManager;
    }
}
