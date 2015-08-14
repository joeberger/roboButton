package com.ndipatri.roboButton.dagger.bluetooth.communication.stubs;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.ButtonCommunicator;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.models.Button;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

public class GenericButtonCommunicatorStub extends ButtonCommunicator {

    private static final String TAG = GenericButtonCommunicatorStub.class.getCanonicalName();

    protected long STUB_DELAY_MILLIS = 1000;

    // This is our fake remote button's state.. This needs to persist across various instances
    // of communicator (as we go in and out of region)
    //
    // We maintain a separate notion of the remote button state as synchronizing this and 'localButtonState'
    // is the main job of a ButtonCommunicator.
    private static HashMap<String, ButtonState> remoteButtonStates = new HashMap<>();

    private ButtonState getRemoteButtonState() {
        ButtonState localButtonState = remoteButtonStates.get(buttonId);
        if (localButtonState == null) {
            localButtonState = ButtonState.OFF;
            remoteButtonStates.put(buttonId, localButtonState);
        }

        return localButtonState;
    }

    private void setRemoteButtonState(ButtonState buttonState) {
        remoteButtonStates.put(buttonId, buttonState);
    }

    public GenericButtonCommunicatorStub(final Context context, final BluetoothDevice device, final String buttonId) {
        super(context, device, buttonId);

        startAssumingAlreadyConnected();
    }

    @Override
    public void startCommunicating(final boolean assumeAlreadyConnected) {
        // Here we assume we've communicated with remote button and determined its current state, without changing it.
        setLocalButtonState(getRemoteButtonState());
    }

    // We will assume this is always successful with a slight delay..
    protected void setRemoteState(final ButtonState requestedButtonState) {

        setRemoteButtonState(requestedButtonState);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (state == STATE.RUNNING) {
                    if (requestedButtonState.value) {
                        setLocalButtonState(ButtonState.ON);
                    } else {
                        setLocalButtonState(ButtonState.OFF);
                    }
                }
            }
        }, STUB_DELAY_MILLIS);
    }
}

