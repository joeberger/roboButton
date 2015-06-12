package com.ndipatri.roboButton.events;

import android.bluetooth.BluetoothDevice;

import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.models.Button;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ButtonDiscoveryEvent {

    protected boolean success = false;

    protected BluetoothDevice device;
    protected ButtonType buttonType;
    protected String deviceAddress;

    public ButtonDiscoveryEvent(final boolean success, ButtonType buttonType, String deviceAddress, BluetoothDevice device) {
        this.success = success;
        this.buttonType = buttonType;
        this.device = device;
        this.deviceAddress = deviceAddress;
    }

    public boolean isSuccess() {
        return success;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public ButtonType getButtonType() {
        return buttonType;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ButtonDiscoveryEvent that = (ButtonDiscoveryEvent) o;

        if (success != that.success) return false;
        if (device != null ? !device.equals(that.device) : that.device != null) return false;
        if (buttonType != that.buttonType) return false;
        return !(deviceAddress != null ? !deviceAddress.equals(that.deviceAddress) : that.deviceAddress != null);

    }

    @Override
    public int hashCode() {
        int result = (success ? 1 : 0);
        result = 31 * result + (device != null ? device.hashCode() : 0);
        result = 31 * result + (buttonType != null ? buttonType.hashCode() : 0);
        result = 31 * result + (deviceAddress != null ? deviceAddress.hashCode() : 0);
        return result;
    }
}
