package com.ndipatri.roboButton.dagger.providers.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.providers.interfaces.RegionDiscoveryProvider;
import com.ndipatri.roboButton.events.RegionFoundEvent;
import com.ndipatri.roboButton.events.RegionLostEvent;
import com.ndipatri.roboButton.utils.BusProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * When 'startRegionDiscovery()' is called, this class will perform a scan for a defined period of time looking for Regions.
 * A Region is considered 'Found' only if its RSSI is above a threshold.  If a previously found Region's RSSI goes below that
 * threshold, it will be declared lost even though it is still reported back in scan results.
 * 
 * After a scan in finished, this class will stop scanning for a third of its scan period, then it will scan again.
 * 
 * Before going to sleep, it will check for any Regions that were not detected in the last scan.. If a Region fails to report
 * after a defined number of scans, it will be also be declared lost.
 *
 * This class can look for more than one type of Region.  A 'type' is identified by the UUID found in the transmitted iBeacon
 * advertisement packet.
 *
 * This advertisement information is returned in a 'scanRecord' after a 'startLeScan()'.  Parsing this scanRecord for
 * various Regions can require a different 'offset' from the beginning to find the UUID.  THis offset must also be passed
 * in when constructing this provider.
 */
public class GenericRegionDiscoveryProviderImpl implements RegionDiscoveryProvider {

    private static final String TAG = GenericRegionDiscoveryProviderImpl.class.getCanonicalName();

    final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    // List of desired Regions.
    protected List<String> regionUUIDPatternList;

    // List of parse offsets associated with each Region UUID.
    protected List<Integer> regionUUIDOffsetList;

    private int beaconDetectionThresholdDbms = -1;
    
    // If we've heard from a Region and then stop hearing from the Region (which isn't the same as INFERIOR RSSI), 
    // this is the number of scan periods we wait before declaring the Region 'lost'.
    private int beaconLostScanIntervals = 0;
    
    // This is how long we allow BLE to scan before resting.
    private int beaconScanIntervalMillis;
    
    // This is how many consecutive 'INFERIOR' RSSI measurements we need before declaring the region lost.
    private int beaconInferiorRSSICountThreshold;
    
    private boolean scanning = false;
    
    private boolean inBackground = false;
    
    private Context context;

    private BluetoothAdapter.LeScanCallback scanRunnable;

    @Inject
    BusProvider bus;

    /**
     * Key: Found Region
     * Object:  Number of scans for which this region was not found.
     * 
     * The idea is, when a Region is first put in this map, it is 'Found'.  If a Region's RSSI too low,
     * it is removed from this map..  If, however, the Region just disappears (e.g. doesn't come back in a scan),
     * then we will eventually assume it's 'Lost', but only after it hasn't been found for a number of scan periods.
     */
    Map<com.ndipatri.roboButton.models.Region, MutableInteger> nearbyRegions = new HashMap<com.ndipatri.roboButton.models.Region, MutableInteger>();

    public GenericRegionDiscoveryProviderImpl(final Context context, final String[] regionUUIDPatternArray, final Integer[] regionUUIDOffsetArray) {
        
        this.context = context;
        this.regionUUIDPatternList = Arrays.asList(regionUUIDPatternArray);
        this.regionUUIDOffsetList= Arrays.asList(regionUUIDOffsetArray);

        RBApplication.getInstance().getGraph().inject(this);

        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }

        beaconDetectionThresholdDbms = context.getResources().getInteger(R.integer.gele_beacon_detection_threshold);
        beaconLostScanIntervals = context.getResources().getInteger(R.integer.beacon_lost_scan_intervals);
        beaconInferiorRSSICountThreshold = context.getResources().getInteger(R.integer.beacon_inferior_rssi_count_threshold);
        beaconScanIntervalMillis = context.getResources().getInteger(R.integer.beacon_scan_interval_millis);

        bus.register(this);
    }

    // This is a non-blocking call.
    @Override
    public void startRegionDiscovery(final boolean inBackground) {
        this.inBackground = inBackground;
        
        startRegionDiscovery();
    }

    protected void startRegionDiscovery() {
        
        Log.d(TAG, "Beginning Beacon Monitoring Process...");
        
        if (scanning) {
            // make this request idempotent
            return;
        }
        
        //Check to see if the device supports Bluetooth and that it's turned on
        if (!scanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            
            scanning = true;

            scanRunnable = getScanRunnable();
            
            // This is an idempotent operation
            mBluetoothAdapter.startLeScan(scanRunnable);

            // We should not let scan run indefinitely as it consumes POWER!
            new Handler().postDelayed(scanTimeoutRunnable, beaconScanIntervalMillis);
        } else {
            scanning = false;
        }
    }

    @Override
    public void stopRegionDiscovery() throws RemoteException {
        Log.d(TAG, "Stopping region discovery ...");
        
        scanning = false;
        nearbyRegions = new HashMap<com.ndipatri.roboButton.models.Region, MutableInteger>();

        //Check to see if the device supports Bluetooth and that it's turned on
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.stopLeScan(scanRunnable);
        }
    }

    private Runnable scanTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (scanning) {

                try {
                    stopRegionDiscovery();
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception while stopping discovery.", e);
                }

                // All nearby Regions that still exist at the end of a scan get a 'LostMetric' value of '1'.  Presumably,
                // if the Region is still present, it will get reset to '0' at the next scan.  If not, it will be
                // incremented to '2' at the end of the next scan.  This is how we detect Regions that are just lost
                // and do no necessarily degrade w.r.t RSSI over time.
                Iterator<com.ndipatri.roboButton.models.Region> lostRegionIterator = nearbyRegions.keySet().iterator();
                while (lostRegionIterator.hasNext()) {
                    com.ndipatri.roboButton.models.Region lostRegion = lostRegionIterator.next();
                    MutableInteger lostMetric = nearbyRegions.get(lostRegion);

                    if (lostMetric.value >= beaconLostScanIntervals) {
                        postRegionLostEvent(lostRegion);
                        lostRegionIterator.remove();
                    } else {
                        lostMetric.value += 1;
                    }
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startRegionDiscovery();
                    }
                }, getBeaconScanRestPeriod());
            }
        }
    };

    private BluetoothAdapter.LeScanCallback getScanRunnable() {
        return new BluetoothAdapter.LeScanCallback() {

            Map<BluetoothDevice, MutableInteger> successiveInferiorRSSICountMap = new HashMap<BluetoothDevice, MutableInteger>();

            // This call is always made on UI thread from BluetoothAdapter.
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                for (String regionUUIDPattern : regionUUIDPatternList) {
                    int index = 0;
                    com.ndipatri.roboButton.models.Region
                            discoveredRegion = checkForRegion(rssi, scanRecord, regionUUIDPattern, regionUUIDOffsetList.get(index++));
                    if (discoveredRegion != null) {

                        if (rssi > beaconDetectionThresholdDbms) {
                            Log.d(TAG, "Region with ACCEPTABLE RSSI '" + rssi + "' (" + discoveredRegion + "'!");
                            postRegionFoundEvent(discoveredRegion);

                            successiveInferiorRSSICountMap.put(device, new MutableInteger(0)); // reset low pass filter for this device
                            nearbyRegions.put(discoveredRegion, new MutableInteger(0)); // reset the 'LostMetric' value back to 0.
                        } else {
                            Log.d(TAG, "Region with INFERIOR RSSI '" + rssi + "' (" + discoveredRegion + "'!");

                            MutableInteger inferiorRSSIInteger = successiveInferiorRSSICountMap.get(device);
                            if (inferiorRSSIInteger == null) {
                                inferiorRSSIInteger = new MutableInteger(0);
                                successiveInferiorRSSICountMap.put(device, inferiorRSSIInteger);
                            }
                            inferiorRSSIInteger.value += 1;

                            // This is an attempt to 'low pass filter' the RSSI measurements as at times they can be
                            // spurious.
                            if (inferiorRSSIInteger.value >= beaconInferiorRSSICountThreshold) {
                                successiveInferiorRSSICountMap.remove(device);
                                postRegionLostEvent(discoveredRegion);
                                nearbyRegions.remove(nearbyRegions);
                            }
                        }
                    }
                }
            }
        };
    }

    protected com.ndipatri.roboButton.models.Region checkForRegion(int rssi, byte[] scanRecord, String regionUUIDPattern, int regionUUIDOffset) {

        // TODO - should persist RSSI as this should effect what we consider 'close' (proximity measurement)

        com.ndipatri.roboButton.models.Region region = null;

        String uuidHex = convertBytesToHex(Arrays.copyOfRange(scanRecord, 9-regionUUIDOffset, 25-regionUUIDOffset)).toLowerCase(); // LightBlue

        if (scanning && regionUUIDPattern.equals(uuidHex.toLowerCase())) {

            int major = ((scanRecord[25-regionUUIDOffset] & 0xFF) << 8)
                    | (scanRecord[26-regionUUIDOffset] & 0xFF);

            int minor = ((scanRecord[27-regionUUIDOffset] & 0xFF) << 8)
                    | (scanRecord[28-regionUUIDOffset] & 0xFF);

            region = new com.ndipatri.roboButton.models.Region(minor, major, uuidHex);
        }

        return region;
    }

    private static String convertBytesToHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hex[i * 2] = HEX_ARRAY[v >>> 4];
            hex[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hex);
    }
    
    private class MutableInteger {
        Integer value;
        
        public MutableInteger(Integer value) {
            this.value = value;
        }
        
    }

    protected int getBeaconScanRestPeriod() {
        return RBApplication.getInstance().getAutoModeEnabledFlag() ? (inBackground ? beaconScanIntervalMillis/2  : 0) :
                                                                      (inBackground ? beaconScanIntervalMillis : beaconScanIntervalMillis/2);
    }
    
    protected void postRegionFoundEvent(final com.ndipatri.roboButton.models.Region region) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.post(new RegionFoundEvent(region));
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
