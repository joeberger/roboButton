package com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces;

import android.content.Context;

import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.ButtonCommunicator;
import com.ndipatri.roboButton.models.Button;

public interface ButtonCommunicatorFactory {
    ButtonCommunicator getButtonCommunicator(final Context context, final Button button);
}
