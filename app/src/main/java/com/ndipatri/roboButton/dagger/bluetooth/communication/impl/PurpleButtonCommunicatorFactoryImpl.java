package com.ndipatri.roboButton.dagger.bluetooth.communication.impl;

import android.content.Context;

import com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces.ButtonCommunicatorFactory;
import com.ndipatri.roboButton.models.Button;

public class PurpleButtonCommunicatorFactoryImpl implements ButtonCommunicatorFactory {

    private static final String TAG = PurpleButtonCommunicatorFactoryImpl.class.getCanonicalName();

    public ButtonCommunicator getButtonCommunicator(final Context context, final Button button) {
        return new PurpleButtonCommunicatorImpl(context, button);
    }
}
