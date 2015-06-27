package com.ndipatri.roboButton.dagger.bluetooth.communication.impl;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces.ButtonCommunicatorFactory;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.models.Button;

public class LightBlueButtonCommunicatorFactoryImpl implements ButtonCommunicatorFactory {

    private static final String TAG = LightBlueButtonCommunicatorFactoryImpl.class.getCanonicalName();

    public ButtonCommunicator getButtonCommunicator(final Context context, final BluetoothDevice device, final String buttonId) {
        return new LightBlueButtonCommunicatorImpl(context, device, buttonId);
    }
}
