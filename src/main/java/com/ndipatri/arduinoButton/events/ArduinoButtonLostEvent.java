package com.ndipatri.arduinoButton.events;

import android.bluetooth.BluetoothDevice;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ArduinoButtonLostEvent {

    public BluetoothDevice bluetoothDevice;

    public ArduinoButtonLostEvent(final BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }
}
