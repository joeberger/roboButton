package com.ndipatri.arduinoButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.ndipatri.arduinoButton.ABApplication;
import com.ndipatri.arduinoButton.BeaconDistanceListener;
import com.ndipatri.arduinoButton.ButtonDiscoveryListener;
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
    private ButtonDiscoveryListener buttonDiscoveryListener;

    String discoverableButtonPatternString;

    public BluetoothProviderImpl(Context context) {
        this.context = context;

        beaconManager = new BeaconManager(context);

        ABApplication.getInstance().registerForDependencyInjection(this);

        discoverableButtonPatternString = context.getString(R.string.button_discovery_pattern);

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        ABApplication.getInstance().registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    public void startButtonDiscovery(ButtonDiscoveryListener buttonDiscoveryListener) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isDiscovering()) {
            this.buttonDiscoveryListener = buttonDiscoveryListener;
            bluetoothAdapter.startDiscovery();
        }
    }

    @Override
    public void stopButtonDiscovery() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            buttonDiscoveryListener = null;
        }
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
    public void startBeaconDiscovery(BeaconDistanceListener beaconDistanceListener) {

        Log.d(TAG, "Beginning Beacon Monitoring Process...");

        this.beaconDistanceListener = beaconDistanceListener;

        com.estimote.sdk.utils.L.enableDebugLogging(true);

        // Default values are 5s of scanning and 25s of waiting time to save CPU cycles.
        // In order for this demo to be more responsive and immediate we lower down those values.
        beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(5));
        beaconManager.setForegroundScanPeriod(TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(1));

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
                Log.d(TAG, "Paired beacons detected!");

                // We've entered one of our known regions, so we begin to range.. Even if no button is currently
                // associated with the beacons.. this assocation might happen at any time within the app while we are
                // in this region.
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
                double distance = Math.min(Utils.computeAccuracy(beacon), 10.0);
                Log.d(TAG, "Beacon distance update! ('" + distance + "'m)");

                this.beaconDistanceListener.beaconDistanceUpdate(beacon, distance);
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getName() != null && device.getName().matches(discoverableButtonPatternString)) {
                    Log.d(TAG, "We have a nearby ArduinoButton device! + '" + device + "'.");

                    Button discoveredButton = null;

                    Button persistedButton = buttonProvider.getButton(device.getAddress());
                    if (persistedButton != null) {
                        discoveredButton = persistedButton;
                    } else {
                        discoveredButton = new Button(device.getAddress(), device.getAddress(), true);
                    }
                    discoveredButton.setBluetoothDevice(device);

                    buttonProvider.createOrUpdateButton(discoveredButton);

                    buttonDiscoveryListener.buttonDiscovered(discoveredButton);
                }
            }
        }
    };
}
