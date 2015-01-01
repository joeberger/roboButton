package com.ndipatri.arduinoButton.events;

import android.bluetooth.BluetoothDevice;

import com.ndipatri.arduinoButton.models.Button;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ArduinoButtonLostEvent {

    public Button button;

    public ArduinoButtonLostEvent(final Button button) {
        this.button = button;
    }
}
