package com.ndipatri.arduinoButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.models.Button;

import java.util.HashSet;
import java.util.Set;

public class BluetoothProviderImpl implements BluetoothProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    private BeaconManager beaconManager;

    public BluetoothProviderImpl(Context context) {
        this.context = context;

        beaconManager = new BeaconManager(context);
    }

    @Override
    public Set<Button> getAvailableButtons() {

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
    public void setBTLEListener(BeaconManager.MonitoringListener listener) {
        beaconManager.setMonitoringListener(listener);
    }

    @Override
    public void disconnectFromBTLEServiceAndStopRanging(Region region) throws RemoteException {
        beaconManager.stopRanging(region);
        beaconManager.disconnect();
    }

}
