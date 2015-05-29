package com.ndipatri.roboButton.events;

import android.bluetooth.BluetoothDevice;

import com.ndipatri.roboButton.enums.ButtonType;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ButtonDiscoveryEvent {

    public boolean success = false;
    
    // Will be null if success if false.
    public BluetoothDevice buttonDevice;

    public ButtonType buttonType = ButtonType.UNKNOWN;

    public ButtonDiscoveryEvent(final boolean success, final BluetoothDevice buttonDevice, final ButtonType buttonType) {
        this.success = success;
        this.buttonDevice = buttonDevice;
        this.buttonType =  buttonType;
    }

    public boolean isSuccess() {
        return success;
    }

    public BluetoothDevice getButtonDevice() {
        return buttonDevice;
    }

    public ButtonType getButtonType() {
        return buttonType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ButtonDiscoveryEvent that = (ButtonDiscoveryEvent) o;

        if (success != that.success) return false;
        if (buttonDevice != null ? !buttonDevice.equals(that.buttonDevice) : that.buttonDevice != null)
            return false;
        return buttonType == that.buttonType;

    }

    @Override
    public int hashCode() {
        int result = (success ? 1 : 0);
        result = 31 * result + (buttonDevice != null ? buttonDevice.hashCode() : 0);
        result = 31 * result + (buttonType != null ? buttonType.hashCode() : 0);
        return result;
    }
}
