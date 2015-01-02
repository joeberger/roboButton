package com.ndipatri.arduinoButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.models.Button;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class BluetoothProviderImpl implements BluetoothProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private static final int ROBOBUTTON_ESTIMOTE_MAJOR_VALUE = 23524123;
    private static final Region ALL_ROBOBUTTON_BEACONS = new Region("regionId", null, ROBOBUTTON_ESTIMOTE_MAJOR_VALUE, null);

    private Context context;

    private BeaconManager beaconManager;

    public BluetoothProviderImpl(Context context) {
        this.context = context;

        beaconManager = new BeaconManager(context);
    }

    @Override
    public Set<Button> getAllNearbyButtons() {

        final Set<Button> pairedButtons = new HashSet<Button>();

        // we monitor all paired devices...
        String discoverableButtonPatternString = context.getString(R.string.button_discovery_pattern);
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, "Checking BT device: + '" + device.getName() + ":" + device.getAddress() + "'.");
                if (device.getName().matches(discoverableButtonPatternString)) {
                    Log.d(TAG, "We have a paired ArduinoButton device! + '" + device + "'.");

                    Button pairedButton = new Button(device.getAddress(), device.getAddress(), false, null);
                    pairedButton.setBluetoothDevice(device);
                    pairedButtons.add(pairedButton);
                }
            }
        }

        return pairedButtons;
    }

    @Override
    public boolean isBluetoothSupported() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    @Override
    public boolean isBluetoothEnabled() {
        boolean isEnabled = false;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            isEnabled = bluetoothAdapter.isEnabled();
        }

        return isEnabled;
    }

    @Override
    public void cancelDiscovery() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    @Override
    public void startBTMonitoring(BeaconManager.MonitoringListener listener) {

        // Default values are 5s of scanning and 25s of waiting time to save CPU cycles.
        // In order for this demo to be more responsive and immediate we lower down those values.
        beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);

        beaconManager.setMonitoringListener(listener);

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startMonitoring(ALL_ROBOBUTTON_BEACONS);
                } catch (RemoteException e) {
                    Log.d(TAG, "Error while starting monitoring");
                }
            }
        });
    }

    @Override
    public void stopBTMonitoring() throws RemoteException {
        beaconManager.stopMonitoring(ALL_ROBOBUTTON_BEACONS);
        beaconManager.disconnect();
    }
}
