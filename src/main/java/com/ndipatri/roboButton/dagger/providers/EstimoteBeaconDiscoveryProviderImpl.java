package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.ndipatri.roboButton.BeaconDiscoveryListener;
import com.ndipatri.roboButton.RBApplication;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class EstimoteBeaconDiscoveryProviderImpl implements BeaconDiscoveryProvider, BeaconManager.MonitoringListener, BeaconManager.RangingListener {

    private static final String TAG = EstimoteBeaconDiscoveryProviderImpl.class.getCanonicalName();

    @Inject
    BeaconProvider beaconProvider;

    private static final int AB_ESTIMOTE_MAJOR_VALUE = 2112;
    private static final Region AB_ESTIMOTE_REGION = new Region("regionId", null, AB_ESTIMOTE_MAJOR_VALUE, null);

    private Context context;

    private BeaconManager beaconManager;

    private BeaconDiscoveryListener beaconDiscoveryListener;

    public EstimoteBeaconDiscoveryProviderImpl(Context context) {
        this.context = context;

        beaconManager = new BeaconManager(context);

        RBApplication.getInstance().registerForDependencyInjection(this);
    }

    @Override
    public void startBeaconDiscovery(BeaconDiscoveryListener beaconDiscoveryListener) {

        Log.d(TAG, "Beginning Beacon Monitoring Process...");

        this.beaconDiscoveryListener = beaconDiscoveryListener;

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
    public void stopBeaconDiscovery() throws RemoteException {
        Log.d(TAG, "Stop Beacon Monitoring Process...");

        beaconManager.stopMonitoring(AB_ESTIMOTE_REGION);
        beaconManager.stopRanging(AB_ESTIMOTE_REGION);

        this.beaconDiscoveryListener = null;

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

        this.beaconDiscoveryListener.leftRegion(region);

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

                this.beaconDiscoveryListener.beaconDistanceUpdate(beacon, distance);
            }
        }
    }
}
