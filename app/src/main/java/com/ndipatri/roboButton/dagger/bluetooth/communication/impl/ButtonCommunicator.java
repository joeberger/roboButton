package com.ndipatri.roboButton.dagger.bluetooth.communication.impl;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.BluetoothProvider;
import com.ndipatri.roboButton.dagger.daos.ButtonDao;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.events.ApplicationFocusChangeEvent;
import com.ndipatri.roboButton.events.BluetoothDisabledEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonStateChangeRequest;
import com.ndipatri.roboButton.events.ButtonUpdatedEvent;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.BusProvider;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

/**
 * Communicates with each individual Button.
 *
 * Implementations are expected to call 'setLocalButtonState(ButtonState)' when a new button state has been detected.
 *
 */
public abstract class ButtonCommunicator {

    private static final String TAG = ButtonCommunicator.class.getCanonicalName();

    protected boolean shouldRun = false;

    @Inject
    BusProvider bus;

    @Inject BluetoothProvider bluetoothProvider;

    @Inject ButtonDao buttonDao;

    protected boolean inBackground = false;

    protected String buttonId;

    protected BluetoothDevice bluetoothDevice;

    protected Context context;

    private BusProxy busProxy = new BusProxy();

    public ButtonCommunicator(final Context context,
                              final BluetoothDevice bluetoothDevice,
                              final String buttonId) {

        Log.d(TAG, "Starting button communicator for '" + buttonId + "'.");

        this.context = context;
        this.buttonId = buttonId;
        this.bluetoothDevice = bluetoothDevice;

        RBApplication.getInstance().getGraph().inject(this);

        persistButton(buttonId);
    }

    protected abstract void setRemoteState(ButtonState buttonState);
    protected abstract void startCommunicating();
    protected abstract boolean isCommunicating();

    protected Button persistButton(final String buttonAddress) {

        Button discoveredButton;

        Button persistedButton = buttonDao.getButton(buttonAddress);

        if (persistedButton != null) {
            discoveredButton = persistedButton;

            persistedButton.setState(ButtonState.NEVER_CONNECTED);
        } else {
            discoveredButton = new Button(buttonAddress, buttonAddress, true);
        }

        buttonDao.createOrUpdateButton(discoveredButton);

        return discoveredButton;
    }


    public void start() {

        bus.register(busProxy);

        if (bluetoothProvider.isBluetoothSupported() && bluetoothProvider.isBluetoothEnabled()) {
            shouldRun = true;

            setButtonPersistedState(ButtonState.NEVER_CONNECTED);

            startCommunicating();
        } else {
            bus.post(new BluetoothDisabledEvent());
            shouldRun = false;
        }
    }

    public void shutdown() {
        if (isAutoModeEnabled() && getButton().isAutoModeEnabled() && isCommunicating()) {

            if (shouldRun) {
                if (getButton().getState() != ButtonState.OFF) {
                    Log.d(TAG, "Auto Shutdown!");
                    setRemoteState(ButtonState.OFF);
                }
            }
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stop();
            }
        }, 3000);
    }

    protected void stop() {
        Log.d(TAG, "stop()");
        shouldRun = false;

        postButtonLostEvent(buttonId);

        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.unregister(busProxy);
            }
        });
    }

    protected void setButtonPersistedState(ButtonState buttonState) {
        Button button = getButton();
        button.setState(buttonState);
        buttonDao.createOrUpdateButton(button);
    }

    protected boolean isAutoModeEnabled() {
        return RBApplication.getInstance().getAutoModeEnabledFlag();
    }

    protected void setLocalButtonState(final ButtonState buttonState) {
        setLocalButtonState(buttonState, false);
    }

    protected void setLocalButtonState(final ButtonState newButtonState, boolean force) {

        Log.d(TAG, "setLocalButtonState()");

        ButtonState currentButtonState = getButton().getState();

        if (force || (currentButtonState == ButtonState.NEVER_CONNECTED || currentButtonState != newButtonState)) {
            setButtonPersistedState(newButtonState);
        }
    }

    protected void postButtonLostEvent(final String buttonId) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.post(new ButtonLostEvent(buttonId));
            }
        });
    }

    public Button getButton() {
        return buttonDao.getButton(buttonId);
    }

    protected void setRemoteAutoStateIfApplicable(final ButtonState remoteState) {
        if (getButton().getState() == ButtonState.NEVER_CONNECTED) {

            if (isAutoModeEnabled() && getButton().isAutoModeEnabled() && remoteState != ButtonState.ON) {

                // Now that we've established we can communicate with newly discovered
                // button, let's set its auto-state....
                setRemoteState(ButtonState.ON);
            }
        }
    }

    private class BusProxy {

        @Subscribe
        public void onApplicationFocusChangeEvent(final ApplicationFocusChangeEvent event) {
            // TODO - currently not using this w.r.t. our comms with LightBlue Bean
            inBackground = event.inBackground;
        }

        @Subscribe
        public void onArduinoButtonStateChangeRequestEvent(final ButtonStateChangeRequest event) {

            boolean toggle = false;
            ButtonState requestedButtonState = event.requestedButtonState;
            if (requestedButtonState == null) {
                toggle = true;
                requestedButtonState = getButton().getState();
            }

            if (requestedButtonState.value) {
                requestedButtonState = toggle ? ButtonState.OFF : ButtonState.ON;
            } else {
                requestedButtonState = toggle ? ButtonState.ON : ButtonState.OFF;
            }


            setRemoteState(requestedButtonState);
        }
    }
}
