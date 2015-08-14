package com.ndipatri.roboButton.dagger.bluetooth.communication.impl;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.BluetoothProvider;
import com.ndipatri.roboButton.dagger.daos.ButtonDao;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ApplicationFocusChangeEvent;
import com.ndipatri.roboButton.events.BluetoothDisabledEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonStateChangeRequest;
import com.ndipatri.roboButton.events.ButtonUpdatedEvent;
import com.ndipatri.roboButton.events.MonitoringServiceDestroyedEvent;
import com.ndipatri.roboButton.events.RegionLostEvent;
import com.ndipatri.roboButton.events.ToggleAllButtonsRequest;
import com.ndipatri.roboButton.events.ToggleButtonStateRequest;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.BusProvider;
import com.ndipatri.roboButton.utils.NotificationHelper;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

/**
 * Communicates with each individual Button.
 * <p/>
 * Implementations are expected to call 'setLocalButtonState(ButtonState)' when a new button state has been detected.
 */
public abstract class ButtonCommunicator {

    private static final String TAG = ButtonCommunicator.class.getCanonicalName();

    protected enum STATE {
        RUNNING,
        SHUTTING_DOWN,
        STOPPED,;
    }

    protected STATE state = STATE.STOPPED;

    protected ButtonState lastNotifiedState;

    @Inject
    BusProvider bus;

    @Inject
    BluetoothProvider bluetoothProvider;

    @Inject
    ButtonDao buttonDao;

    @Inject
    NotificationHelper notificationHelper;

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

    protected abstract void startCommunicating(boolean assumeAlreadyConnected);

    protected Button persistButton(final String buttonAddress) {

        Button discoveredButton;

        Button persistedButton = buttonDao.getButton(buttonAddress);

        if (persistedButton != null) {
            discoveredButton = persistedButton;
        } else {
            discoveredButton = new Button(buttonAddress, buttonAddress, false);
        }
        discoveredButton.setState(ButtonState.OFFLINE);

        buttonDao.createOrUpdateButton(discoveredButton);

        return discoveredButton;
    }

    public void startAssumingAlreadyConnected() {
        start(true);
    }

    public void startAssumingNotAlreadyConnected() {
        start(false);
    }

    private void start(final boolean assumeAlreadyConnected) {

        bus.register(busProxy);

        if (bluetoothProvider.isBluetoothSupported() && bluetoothProvider.isBluetoothEnabled()) {
            state = STATE.RUNNING;

            setButtonPersistedState(ButtonState.OFFLINE);
            sendButtonStateNotificationIfChanged();

            startCommunicating(assumeAlreadyConnected);
        } else {
            bus.post(new BluetoothDisabledEvent());
            state = STATE.STOPPED;
        }

        registerForScreenWakeBroadcast();
    }

    public void shutdown() {
        if (isAutoModeEnabled() && getButton().isAutoModeEnabled()) {

            if (state == STATE.RUNNING) {
                state = STATE.SHUTTING_DOWN;
                if (getButton().getState() != ButtonState.OFF) {
                    Log.d(TAG, "Auto Shutdown!");
                    setRemoteState(ButtonState.OFF);
                }
            }
        }

        stop();
    }

    protected void stop() {
        Log.d(TAG, "stop()");
        state = STATE.STOPPED;

        postButtonLostEvent(buttonId);

        clearNotification();

        setLocalButtonState(ButtonState.OFFLINE);

        unRegisterForScreenWakeBroadcast();

        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.unregister(busProxy);
            }
        });
    }

    protected void setLocalButtonState(final ButtonState buttonState) {

        Log.d(TAG, "setLocalButtonState(): " + buttonState);

        setButtonPersistedState(buttonState);
        sendButtonStateNotificationIfChanged();
    }

    protected void setButtonPersistedState(ButtonState buttonState) {
        Button button = getButton();
        button.setState(buttonState);
        buttonDao.createOrUpdateButton(button);

        if (state == STATE.RUNNING) {
            bus.post(new ButtonUpdatedEvent(buttonId));
        }
    }

    protected boolean isAutoModeEnabled() {
        return RBApplication.getInstance().getAutoModeEnabledFlag();
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
        if (getButton().getState() == ButtonState.CONNECTING) {

            if (isAutoModeEnabled() && getButton().isAutoModeEnabled() && remoteState != ButtonState.ON) {

                // Now that we've established we can communicate with newly discovered
                // button, let's set its auto-state....
                setRemoteState(ButtonState.ON);
            }
        }
    }

    private class BusProxy {

        @Subscribe
        public void onRegionLost(final RegionLostEvent event) {
            shutdown();
            clearNotification();
        }

        @Subscribe
        public void onMonitoringServiceDestroyedEvent(final MonitoringServiceDestroyedEvent event) {
            shutdown();
        }

        @Subscribe
        public void onApplicationFocusChangeEvent(final ApplicationFocusChangeEvent event) {
            // TODO - currently not using this w.r.t. our comms with LightBlue Bean
            inBackground = event.inBackground;
        }

        @Subscribe
        public void onToggleButtonStateRequest(final ToggleButtonStateRequest event) {
            if (event.buttonId.equals(ButtonCommunicator.this.buttonId)) {
                toggleButtonState();
            }
        }

        @Subscribe
        public void onToggleAllButtonsRequest(final ToggleAllButtonsRequest event) {
            toggleButtonState();
        }

        @Subscribe
        public void onButtonStateChangeRequest(final ButtonStateChangeRequest event) {
            Log.d(TAG, "onButtonStateChangeRequest()");
            if (event.buttonId.equals(ButtonCommunicator.this.buttonId)) {
                setRemoteState(event.requestedButtonState);
            }
        }

        @Subscribe
        public void onApplicationFocusChanged(final ApplicationFocusChangeEvent event) {
            if (event.inBackground) {
                sendButtonStateNotificationIfChanged(true);
            } else {
                clearNotification();
            }
        }
    }

    protected void toggleButtonState() {
        ButtonState requestedButtonState = getButton().getState();

        if (requestedButtonState.value) {
            requestedButtonState = ButtonState.OFF;
        } else {
            requestedButtonState = ButtonState.ON;
        }

        setRemoteState(requestedButtonState);
    }

    private void registerForScreenWakeBroadcast() {
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(screenWakeReceiver, screenStateFilter);
    }

    private void unRegisterForScreenWakeBroadcast() {
        context.unregisterReceiver(screenWakeReceiver);
    }

    private BroadcastReceiver screenWakeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive()");
            sendButtonStateNotificationIfChanged();
        }
    };

    protected void sendButtonStateNotificationIfChanged() {
        sendButtonStateNotificationIfChanged(false);
    }

    protected void sendButtonStateNotificationIfChanged(boolean force) {
        if (RBApplication.getInstance().isBackgrounded() &&
                (force || lastNotifiedState == null || getButton().getState() != lastNotifiedState)) {
            lastNotifiedState = getButton().getState();
            notificationHelper.sendNotification(getButton().getId(), getButton().getName(), getButton().getState());
        }
    }

    protected void clearNotification() {
        notificationHelper.clearNotification(getButton().getId());
    }
}
