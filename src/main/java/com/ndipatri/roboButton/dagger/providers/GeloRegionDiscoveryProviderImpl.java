package com.ndipatri.roboButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.Region;
import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.events.RegionFoundEvent;
import com.ndipatri.roboButton.events.RegionLostEvent;
import com.ndipatri.roboButton.utils.BusProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * When 'startRegionDiscovery()' is called, this class will perform a scan for a defined period of time looking for Regions.
 * A Region is considered 'Found' only if its RSSI is above a threshold.  If a previously found Region's RSSI goes below that
 * threshold, it will be declared lost even though it is still reported back in scan results.
 * 
 * After a scan in finished, this class will stop scanning for a third of its scan period, then it will scan again.
 * 
 * Before going to sleep, it will check for any Regions that were not detected in the last scan.. If a Region fails to report
 * after a defined number of scans, it will be also be declared lost.
 */
public class GeloRegionDiscoveryProviderImpl implements RegionDiscoveryProvider {

    private static final String TAG = GeloRegionDiscoveryProviderImpl.class.getCanonicalName();

    final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    final static String GELO_UUID = "11E44F094EC4407E9203CF57A50FBCE0";
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private static final int AB_GELO_MAJOR_VALUE = 0; // NJD TODO - once we program GELO beacons, this can change.
    private static final Region AB_GELO_REGION = new Region("regionId", null, AB_GELO_MAJOR_VALUE, null);

    private int beaconDetectionThresholdDbms = -1;
    private int beaconLostScanIntervals = 0;
    private int beaconScanIntervalMillis;
    
    private boolean scanning = false;
    
    private Context context;

    /**
     * Key: Found Region
     * Object:  Number of scans for which this region was not found.
     * 
     * The idea is, when a Region is first put in this map, it is 'Found'.  If a Region's RSSI too low,
     * it is removed from this map..  If, however, the Region just disappears (e.g. doesn't come back in a scan),
     * then we will eventually assume it's 'Lost', but only after it hasn't been found for a number of scan periods.
     */
    Map<com.ndipatri.roboButton.models.Region, MutableInteger> nearbyRegions = new HashMap<com.ndipatri.roboButton.models.Region, MutableInteger>();

    public GeloRegionDiscoveryProviderImpl(Context context) {
        
        this.context = context;

        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }

        beaconDetectionThresholdDbms = context.getResources().getInteger(R.integer.gele_beacon_detection_threshold);
        beaconLostScanIntervals = context.getResources().getInteger(R.integer.beacon_lost_scan_intervals);
        beaconScanIntervalMillis = context.getResources().getInteger(R.integer.beacon_scan_interval_millis);

        BusProvider.getInstance().register(this);
    }

    // This is a non-blocking call.
    @Override
    public synchronized void startRegionDiscovery() {

        Log.d(TAG, "Beginning Beacon Monitoring Process...");
        if (scanning) {
            // make this request idempotent
            return;
        }
        
        //Check to see if the device supports Bluetooth and that it's turned on
        if (!scanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            
            scanning = true;
            
            // This is an idempotent operation
            mBluetoothAdapter.startLeScan(scanRunnable);

            // We should not let scan run indefinitely as it consumes POWER!
            new Handler().postDelayed(scanTimeoutRunnable, getBeaconScanIntervalMillis());
        } else {
            scanning = false;
        }
    }

    @Override
    public synchronized void stopRegionDiscovery() throws RemoteException {
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
                        lostMetric.value += lostMetric.value;
                    }
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startRegionDiscovery();
                    }
                }, getBeaconScanIntervalMillis() > 0 ? getBeaconScanIntervalMillis() / 3 : 0); // we don't sleep for the entire scan interval
            }
        }
    };

    private BluetoothAdapter.LeScanCallback scanRunnable = new BluetoothAdapter.LeScanCallback() {
        
        // This call is always made on UI thread from BluetoothAdapter.
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            //For readability we convert the bytes of the UUID into hex
            String UUIDHex = convertBytesToHex(Arrays.copyOfRange(scanRecord, 9, 25));

            if (scanning && UUIDHex.equals(GELO_UUID)) {
                //Bytes 25 and 26 of the advertisement packet represent the major value
                int major = (scanRecord[25] << 8)
                        | (scanRecord[26] << 0);

                //Bytes 27 and 28 of the advertisement packet represent the minor value
                int minor = ((scanRecord[27] & 0xFF) << 8)
                        | (scanRecord[28] & 0xFF);

                //RSSI values increase towards zero as the source gets closer to the reciever

                com.ndipatri.roboButton.models.Region beaconRegion = new com.ndipatri.roboButton.models.Region(minor, major, UUIDHex);
                
                if (rssi > beaconDetectionThresholdDbms) {
                    Log.d(TAG, "Region with ACCEPTABLE RSSI '" + rssi + "' (" + beaconRegion + "'!");
                    postRegionFoundEvent(beaconRegion);
                    
                    nearbyRegions.put(beaconRegion, new MutableInteger(0)); // reset the 'LostMetric' value back to 0.
                } else {
                    if (nearbyRegions.containsKey(beaconRegion)) {
                        // First time inferior RSSI found for previously Found region..
                        Log.d(TAG, "Existing Region with INFERIOR RSSI '" + rssi + "' (" + beaconRegion + "'!");
                        postRegionLostEvent(beaconRegion);
                        nearbyRegions.remove(nearbyRegions);
                    } else {
                        Log.d(TAG, "New Region with INFERIOR RSSI '" + rssi + "' (" + beaconRegion + "'!");
                    }
                }
            }
        }
    };

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

    protected int getBeaconScanIntervalMillis() {
        if (RBApplication.getInstance().getAutoModeEnabledFlag()) {
            return 0;
        } else {
            return beaconScanIntervalMillis;
        }
    }
    
    protected void postRegionFoundEvent(final com.ndipatri.roboButton.models.Region region) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                BusProvider.getInstance().post(new RegionFoundEvent(region));
            }
        });
    }

    protected void postRegionLostEvent(final com.ndipatri.roboButton.models.Region region) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                BusProvider.getInstance().post(new RegionLostEvent(region));
            }
        });
    }
}
