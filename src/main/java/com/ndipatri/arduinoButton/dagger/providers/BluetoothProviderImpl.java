package com.ndipatri.arduinoButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.ndipatri.arduinoButton.ABApplication;
import com.ndipatri.arduinoButton.BeaconDistanceListener;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.models.Button;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class BluetoothProviderImpl implements BluetoothProvider, BeaconManager.MonitoringListener, BeaconManager.RangingListener {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    @Inject
    ButtonProvider buttonProvider;

    @Inject
    BeaconProvider beaconProvider;

    // NJD TODO - Need to figure out how to maek this value different (currently, can't change value using andorid estimote app. so i'm using the default value
    // i foudn on the estimote in my office)
    private static final int AB_ESTIMOTE_MAJOR_VALUE = 2112;
    private static final Region AB_ESTIMOTE_REGION = new Region("regionId", null, AB_ESTIMOTE_MAJOR_VALUE, null);
    //private static final Region ALL_ROBOBUTTON_BEACONS = new Region("regionId", null, null, null);

    private Context context;

    private BeaconManager beaconManager;

    private BeaconDistanceListener beaconDistanceListener;

    public BluetoothProviderImpl(Context context) {
        this.context = context;

        beaconManager = new BeaconManager(context);

        ABApplication.getInstance().inject(this);
    }

    @Override
    public Set<Button> getAllBondedButtons() {

        final Set<Button> pairedButtons = new HashSet<Button>();

        // we monitor all paired devices...
        String discoverableButtonPatternString = context.getString(R.string.button_discovery_pattern);
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG, "Checking BT device: + '" + device.getName() + ":" + device.getAddress() + "'.");
                if (device.getName().matches(discoverableButtonPatternString)) {
                    Log.d(TAG, "We have a paired ArduinoButton device! + '" + device + "'.");

                    Button pairedButton = null;

                    Button persistedButton = buttonProvider.getButton(device.getAddress());
                    if (persistedButton != null) {
                        pairedButton = persistedButton;
                    } else {
                        pairedButton = new Button(device.getAddress(), device.getAddress(), false, null);
                    }
                    pairedButton.setBluetoothDevice(device);

                    pairedButtons.add(pairedButton);
                }
            }
        }

        return pairedButtons;
    }

    @Override
    public Button getBondedButton(String buttonId) {
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
    public void startBTMonitoring(BeaconDistanceListener beaconDistanceListener) {

        Log.d(TAG, "Beginning Beacon Monitoring Process...");

        this.beaconDistanceListener = beaconDistanceListener;

        com.estimote.sdk.utils.L.enableDebugLogging(true);

        // Default values are 5s of scanning and 25s of waiting time to save CPU cycles.
        // In order for this demo to be more responsive and immediate we lower down those values.
        beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);

        beaconManager.setMonitoringListener(this);
        beaconManager.setRangingListener(this);

        // Configure verbose debug logging.

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    Log.d(TAG, "Connected to BeaconManager.  Starting Monitoring...");
                    beaconManager.startMonitoring(AB_ESTIMOTE_REGION);
                } catch (RemoteException e) {
                    Log.d(TAG, "Error while starting monitoring");
                }
            }
        });
    }

    @Override
    public void stopBTMonitoring() throws RemoteException {
        Log.d(TAG, "Stop Beacon Monitoring Process...");

        beaconManager.stopMonitoring(AB_ESTIMOTE_REGION);
        beaconManager.stopRanging(AB_ESTIMOTE_REGION);

        this.beaconDistanceListener = null;

        beaconManager.disconnect();
    }

    public Region getMonitoredRegion() {
        return AB_ESTIMOTE_REGION;
    }

    @Override
    public void onEnteredRegion(Region region, List<Beacon> beacons) {
        String message = "Entering beacon region!";
        Log.d(TAG, message + "(" + region + "').");

        if (region == getMonitoredRegion()) {
            for (Beacon beacon : beacons) {
                com.ndipatri.arduinoButton.models.Beacon pairedBeacon = beaconProvider.getBeacon(beacon.getMacAddress(), true);

                if (pairedBeacon != null) {
                    Log.d(TAG, "Paired beacons detected!");

                    // We've detected at least one, start ranging...
                    try {
                        // we're assuming a connection already to BeaconManager...
                        beaconManager.startRanging(getMonitoredRegion());
                    } catch (RemoteException e) {
                        Log.d(TAG, "Error while starting ranging");
                    }

                    break;
                }
            }
        }
    }

    @Override
    public void onExitedRegion(Region region) {
        String message = "Leaving beacon region!";
        Log.d(TAG, message + "(" + region + "').");

        this.beaconDistanceListener.leftRegion(region);

        try {
            beaconManager.stopRanging(getMonitoredRegion());
        } catch (RemoteException e) {
            Log.d(TAG, "Error while stopping ranging");
        }
    }

    @Override
    public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
        if (region == getMonitoredRegion()) {
            for (Beacon beacon : beacons) {
                com.ndipatri.arduinoButton.models.Beacon pairedBeacon = beaconProvider.getBeacon(beacon.getMacAddress(), true);

                if (pairedBeacon != null) {
                    double distance = Math.min(Utils.computeAccuracy(beacon), 6.0);
                    Log.d(TAG, "Paired beacon distance update! ('" + distance + "'m)");

                    this.beaconDistanceListener.beaconDistanceUpdate(pairedBeacon, distance);
                }
            }
        }
    }
}
