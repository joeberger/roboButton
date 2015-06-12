package com.ndipatri.roboButton.dagger.bluetooth.communication.impl;

import android.content.Context;

import com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces.ButtonCommunicator;
import com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces.ButtonCommunicatorFactory;
import com.ndipatri.roboButton.models.Button;

public class LightBlueButtonCommunicatorFactoryImpl implements ButtonCommunicatorFactory{

    private static final String TAG = LightBlueButtonCommunicatorFactoryImpl.class.getCanonicalName();

    public ButtonCommunicator getButtonCommunicator(final Context context, final Button button) {
        return new LightBlueButtonCommunicatorImpl(context, button);
    }
}
