package com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces;

/**
 * Created by ndipatri on 5/29/14.
 */
public interface BluetoothProvider {
    boolean isBluetoothSupported();
    boolean isBluetoothEnabled();
    void startDiscovery();
    void cancelDiscovery();
}
