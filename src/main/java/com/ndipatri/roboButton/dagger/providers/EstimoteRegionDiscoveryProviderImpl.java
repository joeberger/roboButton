package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.ndipatri.roboButton.RegionDiscoveryListener;
import com.ndipatri.roboButton.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class EstimoteRegionDiscoveryProviderImpl implements RegionDiscoveryProvider, BeaconManager.MonitoringListener, BeaconManager.RangingListener {

    private static final String TAG = EstimoteRegionDiscoveryProviderImpl.class.getCanonicalName();

    private static final int AB_ESTIMOTE_MAJOR_VALUE = 2112;
    private static final Region AB_ESTIMOTE_REGION = new Region("regionId", null, AB_ESTIMOTE_MAJOR_VALUE, null);

    private Context context;

    private int beaconDetectionThresholdMeters = -1;

    Set<String> foundBeacons = new HashSet<String>();

    private BeaconManager beaconManager;

    private RegionDiscoveryListener regionDiscoveryListener;

    public EstimoteRegionDiscoveryProviderImpl(Context context) {
        this.context = context;

        beaconDetectionThresholdMeters = context.getResources().getInteger(R.integer.estimote_beacon_detection_threshold);

        beaconManager = new BeaconManager(context);
    }

    @Override
    public void startRegionDiscovery(RegionDiscoveryListener regionDiscoveryListener) {

        Log.d(TAG, "Beginning Beacon Monitoring Process...");

        this.regionDiscoveryListener = regionDiscoveryListener;

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
    public void stopRegionDiscovery() throws RemoteException {
        Log.d(TAG, "Stop Beacon Monitoring Process...");

        beaconManager.stopMonitoring(AB_ESTIMOTE_REGION);
        beaconManager.stopRanging(AB_ESTIMOTE_REGION);

        this.regionDiscoveryListener = null;

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
                if (distance < beaconDetectionThresholdMeters) {

                } else {

                }

                this.regionDiscoveryListener.beaconDistanceUpdate(beacon.getMacAddress(), beacon.getName(), distance);
            }
        }
    }
}
