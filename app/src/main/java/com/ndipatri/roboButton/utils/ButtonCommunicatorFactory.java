package com.ndipatri.roboButton.utils;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.events.ApplicationFocusChangeEvent;
import com.ndipatri.roboButton.events.BluetoothDisabledEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonStateChangeReport;
import com.ndipatri.roboButton.events.ButtonStateChangeRequest;
import com.ndipatri.roboButton.models.Button;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import javax.inject.Inject;

/**
 * Communicates with each individual Button
 */
public class ButtonCommunicatorFactory {

    private static final String TAG = ButtonCommunicatorFactory.class.getCanonicalName();

    public static ButtonCommunicator getButtonCommunicator(final Context context, final Button button) {

        switch(ButtonType.getByType(button.getType())) {
            case PURPLE_BUTTON:
                return new PurpleButtonCommunicator(context, button);
            case LIGHTBLUE_BUTTON:
                return new LightBlueButtonCommunicator(context, button);
        }

        return null;
    }
}
