package com.ndipatri.roboButton.dagger.bluetooth.communication.stubs;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces.ButtonCommunicator;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ApplicationFocusChangeEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonStateChangeReport;
import com.ndipatri.roboButton.events.ButtonStateChangeRequest;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.BusProvider;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

public class GenericButtonCommunicatorStub implements ButtonCommunicator {

    private static final String TAG = GenericButtonCommunicatorStub.class.getCanonicalName();

    @Inject
    BusProvider bus;

    protected long communicationsGracePeriodMillis = -1;

    protected long STUB_DELAY_MILLIS = 3000;

    protected boolean inBackground = false;

    private boolean shouldRun = false;

    // This value will always be set by what is received from Button itself
    private ButtonState localButtonState = ButtonState.NEVER_CONNECTED;

    // This is our fake remote button's state.. This needs to persist across various instances
    // of communicator (as we go in and out of region)
    //
    // We maintain a separate notion of the remote button state as synchronizing this and 'localButtonState'
    // is the main job of a ButtonCommunicator.
    private static ButtonState remoteButtonState = ButtonState.OFF;

    private Button button;

    private Context context;

    private long lastButtonStateUpdateTimeMillis;

    public GenericButtonCommunicatorStub(final Context context, final Button button) {

        Log.d(TAG, "Starting Stub button communicator for '" + button.getId() + "'.");

        this.context = context;
        this.button = button;

        communicationsGracePeriodMillis = context.getResources().getInteger(R.integer.communications_grace_period_millis);

        ((RBApplication)context).getGraph().inject(this);

        start();
    }

    public void start() {
        shouldRun = true;

        // The '0' means the last time we spoke to this button was in 1970.. which essentially means too long ago.
        lastButtonStateUpdateTimeMillis = 0;

        bus.register(this);

        if (isAutoModeEnabled() && button.isAutoModeEnabled()) {
            setRemoteState(ButtonState.ON);
        }

        // Here we assume we've communicated with remote button and determined its current state, without changing it.
        setLocalButtonState(remoteButtonState);
    }

    public boolean isCommunicating() {
        long timeSinceLastUpdate = SystemClock.uptimeMillis() - lastButtonStateUpdateTimeMillis;
        boolean isCommunicating = timeSinceLastUpdate <= communicationsGracePeriodMillis;

        Log.d(TAG, "isCommunicating(): '" + isCommunicating + "'");

        return isCommunicating;
    }

    public void shutdown() {
        if (isAutoModeEnabled() && button.isAutoModeEnabled() && isCommunicating()) {

            if (shouldRun) {
                Log.d(TAG, "Auto Shutdown!");
                if (isCommunicating() && localButtonState != ButtonState.OFF) {
                    setRemoteState(ButtonState.OFF);
                }
                stop();
            }
        } else {
            stop();
        }
    }

    protected void stop() {
        shouldRun = false;

        postButtonLostEvent(button.getId());

        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.unregister(GenericButtonCommunicatorStub.this);
            }
        });
    }

    protected void setLocalButtonState(final ButtonState buttonState) {

        this.lastButtonStateUpdateTimeMillis = SystemClock.uptimeMillis();
        Log.d(TAG, "Button state updated @'" + lastButtonStateUpdateTimeMillis + ".'");

        if (this.localButtonState != buttonState) {
            Log.d(TAG, "Button state changed @'" + lastButtonStateUpdateTimeMillis + ".'");
            this.localButtonState = buttonState;

            postButtonStateChangeReport(buttonState);
        }
    }

    protected void postButtonStateChangeReport(final ButtonState buttonState) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "State is '" + buttonState + "'");

                bus.post(new ButtonStateChangeReport(getButton().getId(), buttonState));
            }
        });
    }

    @Produce
    public ButtonStateChangeReport produceStateChangeReport() {
        return new ButtonStateChangeReport(getButton().getId(), localButtonState);
    }

    protected void postButtonLostEvent(final String buttonId) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.post(new ButtonLostEvent(buttonId));
            }
        });
    }

    public ButtonState getLocalButtonState() {
        return localButtonState;
    }

    public Button getButton() {
        return button;
    }

    @Subscribe
    public void onArduinoButtonStateChangeRequestEvent(final ButtonStateChangeRequest event) {

        boolean toggle = false;
        ButtonState requestedButtonState = event.requestedButtonState;
        if (requestedButtonState == null) {
            toggle = true;
            requestedButtonState = localButtonState;
        }

        if (requestedButtonState.value) {
            requestedButtonState = toggle ? ButtonState.OFF : ButtonState.ON;
        } else {
            requestedButtonState = toggle ? ButtonState.ON : ButtonState.OFF;
        }

        setRemoteState(requestedButtonState);
    }

    // We will assume this is always successful with a slight delay..
    protected void setRemoteState(final ButtonState requestedButtonState) {

        GenericButtonCommunicatorStub.remoteButtonState = requestedButtonState;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (requestedButtonState.value) {
                    setLocalButtonState(ButtonState.ON);
                } else {
                    setLocalButtonState(ButtonState.OFF);
                }
            }
        }, STUB_DELAY_MILLIS);
    }

    @Subscribe
    public void onApplicationFocusChangeEvent(final ApplicationFocusChangeEvent event) {
        // TODO - currently not using this w.r.t. our comms with LightBlue Bean
        this.inBackground = event.inBackground;
    }

    protected boolean isAutoModeEnabled() {
        return RBApplication.getInstance().getAutoModeEnabledFlag();
    }
}

