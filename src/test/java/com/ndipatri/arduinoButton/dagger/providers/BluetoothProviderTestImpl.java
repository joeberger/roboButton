package com.ndipatri.arduinoButton.dagger.providers;

import android.content.Context;
import android.os.RemoteException;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.ndipatri.arduinoButton.BeaconDistanceListener;
import com.ndipatri.arduinoButton.models.Button;

import java.util.HashSet;
import java.util.Set;

public class BluetoothProviderTestImpl implements BluetoothProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    private BeaconManager beaconManager;

    private BeaconDistanceListener beaconDistanceListener;

    private Set<Button> availableButtons = new HashSet<Button>();

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
    public Set<Button> getAllBondedButtons() {
        return availableButtons;
    }

    @Override
    public Button getDiscoveredButton(String buttonId) {
        final Set<Button> pairedButtons = getAllBondedButtons();
        for (Button pairedDevice : pairedButtons) {
            if (pairedDevice.getId().equals(buttonId)) {
                return pairedDevice;
            }
        }

        return null;
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
    public void startBTMonitoring(BeaconDistanceListener beaconDistanceListener) {
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

    public void setAvailableButtons(Set<Button> availableButtons) {
        this.availableButtons = availableButtons;
    }
}
