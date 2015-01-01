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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArduinoButtonLostEvent that = (ArduinoButtonLostEvent) o;

        if (button != null ? !button.equals(that.button) : that.button != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return button != null ? button.hashCode() : 0;
    }
}
