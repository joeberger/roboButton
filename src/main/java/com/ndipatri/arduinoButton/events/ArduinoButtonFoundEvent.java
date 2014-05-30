package com.ndipatri.arduinoButton.events;

import android.bluetooth.BluetoothDevice;

import com.ndipatri.arduinoButton.enums.ButtonState;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ArduinoButtonFoundEvent {

    public BluetoothDevice bluetoothDevice;

    public ArduinoButtonFoundEvent(final BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }
}
