package com.ndipatri.arduinoButton.events;

import android.bluetooth.BluetoothDevice;

import com.ndipatri.arduinoButton.enums.ButtonState;
import com.ndipatri.arduinoButton.models.Button;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ArduinoButtonFoundEvent {

    public Button button;

    public ArduinoButtonFoundEvent(final Button button) {
        this.button = button;
    }
}
