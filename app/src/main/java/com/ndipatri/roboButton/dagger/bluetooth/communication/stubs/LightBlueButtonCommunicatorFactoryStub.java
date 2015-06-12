package com.ndipatri.roboButton.dagger.bluetooth.communication.stubs;

import android.content.Context;

import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.LightBlueButtonCommunicatorImpl;
import com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces.ButtonCommunicator;
import com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces.ButtonCommunicatorFactory;
import com.ndipatri.roboButton.models.Button;

public class LightBlueButtonCommunicatorFactoryStub implements ButtonCommunicatorFactory {

    private static final String TAG = LightBlueButtonCommunicatorFactoryStub.class.getCanonicalName();

    public ButtonCommunicator getButtonCommunicator(final Context context, final Button button) {
        return new GenericButtonCommunicatorStub(context, button);
    }
}
