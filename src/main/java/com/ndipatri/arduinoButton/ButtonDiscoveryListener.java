package com.ndipatri.arduinoButton;

import android.bluetooth.BluetoothDevice;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Region;
import com.ndipatri.arduinoButton.models.Button;

/**
 * Created by ndipatri on 1/19/15.
 */
public interface ButtonDiscoveryListener {
    public void buttonDiscovered(Button button);
}
