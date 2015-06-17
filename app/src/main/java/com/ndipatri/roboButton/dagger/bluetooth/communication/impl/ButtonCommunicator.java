package com.ndipatri.roboButton.dagger.bluetooth.communication.impl;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
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

/**
 * Communicates with each individual Button
 */
public abstract class ButtonCommunicator {

    private static final String TAG = ButtonCommunicator.class.getCanonicalName();

    protected boolean shouldRun = false;

    @Inject
    BusProvider bus;

    protected boolean inBackground = false;

    // This value will always be set by what is received from Button itself
    protected ButtonState localButtonState = ButtonState.NEVER_CONNECTED;

    protected long communicationsGracePeriodMillis = -1;

    protected Button button;

    protected Context context;

    private long lastButtonStateUpdateTimeMillis;

    private BusProxy busProxy = new BusProxy();

    public ButtonCommunicator(final Context context, final Button button) {

        Log.d(TAG, "Starting button communicator for '" + button.getId() + "'.");

        this.context = context;
        this.button = button;

        communicationsGracePeriodMillis = context.getResources().getInteger(R.integer.communications_grace_period_millis);

        ((RBApplication)context).getGraph().inject(this);
    }

    protected abstract void setRemoteState(ButtonState buttonState);
    protected abstract void startCommunicating();

    public void start() {
        shouldRun = true;

        // The '0' means the last time we spoke to this button was in 1970.. which essentially means too long ago.
        lastButtonStateUpdateTimeMillis = 0;

        bus.register(busProxy);

        startCommunicating();
    }

    public void shutdown() {
        if (isAutoModeEnabled() && button.isAutoModeEnabled() && isCommunicating()) {

            if (shouldRun) {
                Log.d(TAG, "Auto Shutdown!");
                if (isCommunicating() && localButtonState != ButtonState.OFF) {
                    setRemoteState(ButtonState.OFF);

                    // Need to give above command time to reach button... I know this is lame.
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                stop();
            }
        } else {
            stop();
        }
    }

    protected void stop() {
        Log.d(TAG, "stop()");
        shouldRun = false;

        postButtonLostEvent(button.getId());

        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.unregister(busProxy);
            }
        });
    }

    public boolean isCommunicating() {
        long timeSinceLastUpdate = SystemClock.uptimeMillis() - lastButtonStateUpdateTimeMillis;
        boolean isCommunicating = timeSinceLastUpdate <= communicationsGracePeriodMillis;

        Log.d(TAG, "isCommunicating(): '" + isCommunicating + "'");

        return isCommunicating;
    }

    protected boolean isAutoModeEnabled() {
        return RBApplication.getInstance().getAutoModeEnabledFlag();
    }

    protected void setLocalButtonState(final ButtonState buttonState) {

        this.lastButtonStateUpdateTimeMillis = SystemClock.uptimeMillis();
        Log.d(TAG, "Button state updated @'" + lastButtonStateUpdateTimeMillis + ".'");

        if (this.localButtonState == ButtonState.NEVER_CONNECTED || this.localButtonState != buttonState) {
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

    protected void setRemoteAutoStateIfApplicable(final ButtonState remoteState) {
        if (localButtonState == ButtonState.NEVER_CONNECTED) {

            if (isAutoModeEnabled() && button.isAutoModeEnabled() && remoteState != ButtonState.ON) {

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

        @Produce
        public ButtonStateChangeReport produceStateChangeReport() {
            return new ButtonStateChangeReport(getButton().getId(), localButtonState);
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
    }
}
