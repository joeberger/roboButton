package com.ndipatri.roboButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.Region;
import com.ndipatri.roboButton.RegionDiscoveryListener;

import java.util.Arrays;

public class GeloRegionDiscoveryProviderImpl implements RegionDiscoveryProvider {

    private static final String TAG = GeloRegionDiscoveryProviderImpl.class.getCanonicalName();

    final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    final static String GELO_UUID = "11E44F094EC4407E9203CF57A50FBCE0";
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private Integer highestRssi = null;

    private static final int AB_GELO_MAJOR_VALUE = 0; // NJD TODO - once we program GELO beacons, this can change.
    private static final Region AB_GELO_REGION = new Region("regionId", null, AB_GELO_MAJOR_VALUE, null);

    private RegionDiscoveryListener regionDiscoveryListener;

    public GeloRegionDiscoveryProviderImpl(Context context) {

        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        highestRssi = null;
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
    }

    @Override
    public void startRegionDiscovery(RegionDiscoveryListener regionDiscoveryListener) {

        Log.d(TAG, "Beginning Beacon Monitoring Process...");

        //Check to see if the device supports Bluetooth and that it's turned on
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            this.regionDiscoveryListener = regionDiscoveryListener;
        }
    }

    @Override
    public void stopRegionDiscovery() throws RemoteException {
        Log.d(TAG, "Stop Beacon Monitoring Process...");

        this.regionDiscoveryListener = null;
    }

    public Region getMonitoredRegion() {
        return AB_GELO_REGION;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            //For readability we convert the bytes of the UUID into hex
            String UUIDHex = convertBytesToHex(Arrays.copyOfRange(scanRecord, 9, 25));

            if (UUIDHex.equals(GELO_UUID)) {
                //Bytes 25 and 26 of the advertisement packet represent the major value
                int major = (scanRecord[25] << 8)
                        | (scanRecord[26] << 0);

                //Bytes 27 and 28 of the advertisement packet represent the minor value
                int minor = ((scanRecord[27] & 0xFF) << 8)
                        | (scanRecord[28] & 0xFF);

                //RSSI values increase towards zero as the source gets closer to the reciever
                if (highestRssi == null || rssi > highestRssi) {
                    regionDiscoveryListener.beaconDistanceUpdate(device.getAddress(), device.getName(), rssi);
                }

                    //If the beacon we found  is the current nearest, update the RSSI. You may have
                    //gotten closer or further away and you don't want to remember an old RSSI
                highestRssi = rssi;
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
}
