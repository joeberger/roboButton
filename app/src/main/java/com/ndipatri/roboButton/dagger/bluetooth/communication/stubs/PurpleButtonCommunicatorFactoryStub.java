package com.ndipatri.roboButton.dagger.bluetooth.communication.stubs;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.ButtonCommunicator;
import com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces.ButtonCommunicatorFactory;
import com.ndipatri.roboButton.models.Button;

public class PurpleButtonCommunicatorFactoryStub implements ButtonCommunicatorFactory {

    private static final String TAG = PurpleButtonCommunicatorFactoryStub.class.getCanonicalName();

    public ButtonCommunicator getButtonCommunicator(final Context context, final BluetoothDevice device, final String buttonId) {
        return new GenericButtonCommunicatorStub(context, device, buttonId);
    }
}
