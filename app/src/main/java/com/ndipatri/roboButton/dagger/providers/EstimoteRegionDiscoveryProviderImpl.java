package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.events.RegionFoundEvent;
import com.ndipatri.roboButton.events.RegionLostEvent;
import com.ndipatri.roboButton.utils.BusProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Estimote provides two interfaces: Monitoring and Ranging.  You're meant to first use Monitoring to detect the presence of a region occupied by beacons.  This is a very rough
 * measure and a beacon is 'present within a region' when you detect its signal... This can be over 100 feet.  Once you are within a region of beacons, you can then start 'ranging'.
 * Ranging gives you a much more precise understanding of how close you are to a beacon.
 */
public class EstimoteRegionDiscoveryProviderImpl implements RegionDiscoveryProvider, BeaconManager.MonitoringListener, BeaconManager.RangingListener {

    private static final String TAG = EstimoteRegionDiscoveryProviderImpl.class.getCanonicalName();

    private static final int AB_ESTIMOTE_MAJOR_VALUE = 2112;
    private static final Region AB_ESTIMOTE_REGION = new Region("regionId", null, AB_ESTIMOTE_MAJOR_VALUE, null);

    private Context context;

    private int beaconDetectionThresholdMeters = -1;

    Set<com.ndipatri.roboButton.models.Region> nearbyRegions = new HashSet<com.ndipatri.roboButton.models.Region>();

    private BeaconManager beaconManager;

    @Inject
    BusProvider bus;

    public EstimoteRegionDiscoveryProviderImpl(Context context) {

        this.context = context;

        RBApplication.getInstance().getGraph().inject(this);

        beaconDetectionThresholdMeters = context.getResources().getInteger(R.integer.estimote_beacon_detection_threshold);

        beaconManager = new BeaconManager(context);
        
        bus.register(this);
    }

    @Override
    public void startRegionDiscovery(final boolean inBackground) {

        Log.d(TAG, "Beginning Beacon Monitoring Process...");

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
                com.ndipatri.roboButton.models.Region beaconRegion = new com.ndipatri.roboButton.models.Region(region.getMinor(), region.getMajor(), region.getProximityUUID());

                if (distance < beaconDetectionThresholdMeters) {
                    if (!nearbyRegions.contains(beaconRegion)) {
                        postRegionFoundEvent(beaconRegion);
                        nearbyRegions.add(beaconRegion);
                        Log.d(TAG, "Region Found! ('" + beaconRegion + "'.)");
                    }
                } else {
                    if (nearbyRegions.contains(beaconRegion)) {
                        postRegionLostEvent(beaconRegion);
                        nearbyRegions.remove(beaconRegion);
                        Log.d(TAG, "Region Lost! ('" + beaconRegion + "'.)");
                    }
                }
            }
        }
    }

    protected void postRegionFoundEvent(final com.ndipatri.roboButton.models.Region region) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.post(new RegionFoundEvent(region, region.getButton().getBluetoothDevice()));
            }
        });
    }

    protected void postRegionLostEvent(final com.ndipatri.roboButton.models.Region region) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.post(new RegionLostEvent(region));
            }
        });
    }
}
