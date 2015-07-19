package com.ndipatri.roboButton.dagger.bluetooth.communication.stubs;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.ButtonCommunicator;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.models.Button;

public class GenericButtonCommunicatorStub extends ButtonCommunicator {

    private static final String TAG = GenericButtonCommunicatorStub.class.getCanonicalName();

    protected long STUB_DELAY_MILLIS = 2000;

    // This is our fake remote button's state.. This needs to persist across various instances
    // of communicator (as we go in and out of region)
    //
    // We maintain a separate notion of the remote button state as synchronizing this and 'localButtonState'
    // is the main job of a ButtonCommunicator.
    private static ButtonState remoteButtonState = ButtonState.OFF;

    public GenericButtonCommunicatorStub(final Context context, final BluetoothDevice device, final String buttonId) {
        super(context, device, buttonId);

        start();
    }

    @Override
    public void startCommunicating() {
        // Here we assume we've communicated with remote button and determined its current state, without changing it.
        setLocalButtonState(remoteButtonState);
    }

    @Override
    protected boolean isCommunicating() {
        return true;
    }

    // We will assume this is always successful with a slight delay..
    protected void setRemoteState(final ButtonState requestedButtonState) {

        Toast.makeText(context, "Sending '" + (requestedButtonState.value ? "ON" : "OFF") + "' request to Button.", Toast.LENGTH_SHORT).show();
        GenericButtonCommunicatorStub.remoteButtonState = requestedButtonState;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Received '" + (requestedButtonState.value ? "ON" : "OFF") + "' response from Button.", Toast.LENGTH_SHORT).show();
                if (requestedButtonState.value) {
                    setLocalButtonState(ButtonState.ON);
                } else {
                    setLocalButtonState(ButtonState.OFF);
                }
            }
        }, STUB_DELAY_MILLIS);
    }
}

