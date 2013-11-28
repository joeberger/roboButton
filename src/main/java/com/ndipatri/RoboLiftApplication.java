package com.ndipatri;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

public class RoboLiftApplication extends Application {

    public static final String APPLICATION_PREFS = "RoboLift.prefs";
    public static final String APPLICATION_PREFS_BLUETOOTH_DEVICE_MAC = "RoboLift.prefs.bluetoothDeviceMac";

    private SharedPreferences preferences;

    private static RoboLiftApplication instance = null;

    public static RoboLiftApplication getInstance() {
        return instance;
    }

    public RoboLiftApplication() {
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = getSharedPreferences(RoboLiftApplication.APPLICATION_PREFS, Context.MODE_PRIVATE);
    }

    public void selectBluetoothDevice(String macAddress) {
        preferences.edit().putString(RoboLiftApplication.APPLICATION_PREFS_BLUETOOTH_DEVICE_MAC, macAddress).commit();
    }

    public BluetoothDevice getSelectedBluetoothDevice() {

        BluetoothDevice bluetoothDevice = null;

        String selectedBluetoothDeviceAddress = preferences.getString(RoboLiftApplication.APPLICATION_PREFS_BLUETOOTH_DEVICE_MAC, null);

        if (selectedBluetoothDeviceAddress != null) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getAddress().equals(selectedBluetoothDeviceAddress)) {
                        bluetoothDevice = device;
                        break;
                    }
                }
            }
        }

        return bluetoothDevice;
    }
}
