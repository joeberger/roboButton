package com.ndipatri.roboButton.dagger.bluetooth.communication.stubs;

import android.bluetooth.BluetoothAdapter;
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

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;

import nl.littlerobots.bean.Bean;
import nl.littlerobots.bean.BeanDiscoveryListener;
import nl.littlerobots.bean.BeanListener;
import nl.littlerobots.bean.BeanManager;

/**
 * Communicates with each individual LightBlue Bean Button
 */
public class GenericButtonCommunicatorStub implements ButtonCommunicator {

    private static final String TAG = GenericButtonCommunicatorStub.class.getCanonicalName();

    @Inject
    BusProvider bus;

    protected Button button;

    protected boolean inBackground = false;

    private boolean shouldRun = false;

    // This value will always be set by what is received from Button itself
    private ButtonState buttonState = ButtonState.NEVER_CONNECTED;

    private Context context;

    public GenericButtonCommunicatorStub(final Context context, final Button button) {

        Log.d(TAG, "Starting GenericStub button communicator for '" + button.getId() + "'.");

        this.context = context;
        this.button = button;

        ((RBApplication)context).getGraph().inject(this);

        start();
    }

    public void start() {
        shouldRun = true;

        bus.register(this);
    }

need to impleement this communicator stub.

    protected BeanListener getBeanConnectionListener() {

        return new BeanListener() {

            @Override
            public void onConnected() {
                Log.d(TAG, "onConnected()");

                sendRemoteStateQuery();
            }

            @Override
            public void onConnectionFailed() {
                Log.d(TAG, "onConnectionFailed()");
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "onDisconnected()");
            }

            @Override
            public void onSerialMessageReceived(byte[] bytes) {

                Log.d(TAG, "onSerialMessageReceived()");

                if (shouldRun) {
                    ButtonState newButtonState;

                    String lightBlueButtonValue = null;
                    try {
                        lightBlueButtonValue = new String(bytes, "US-ASCII");
                    } catch (UnsupportedEncodingException e) {
                        lightBlueButtonValue = null;
                    }

                    if (lightBlueButtonValue != null) {

                        int buttonValue = lightBlueButtonValue.equals("locked") ? 1 : 0;

                        Log.d(TAG, "Serial data from LightBlue Bean: '" + this + " ', '" + buttonValue + "'.");
                        try {
                            newButtonState = buttonValue > 0 ? ButtonState.ON : ButtonState.OFF;
                        } catch (NumberFormatException nex) {
                            Log.d(TAG, "Invalid response from bluetooth device: '" + this + "'.");
                            // NJD TODO - one theory is to reconnect and see if that helps...
                            // disconnect();

                            // another is to just continue to listen until we are declare no longer communicating and are killed
                            // by the monitoring service.
                            newButtonState = null;
                        }

                        if (buttonState == ButtonState.NEVER_CONNECTED) {

                            if (isAutoModeEnabled() && button.isAutoModeEnabled() && newButtonState != ButtonState.ON) {

                                // Now that we've established we can communicate with newly discovered
                                // button, let's set its auto-state....
                                setRemoteState(ButtonState.ON);
                            }
                        }

                        setLocalButtonState(newButtonState);
                    }
                }
            }

            @Override
            public void onScratchValueChanged(int i, byte[] bytes) {

            }
        };
    }

    protected void setRemoteState(ButtonState buttonState) {
        if (shouldRun && discoveredBean != null & discoveredBean.isConnected()) {
            byte[] encodedButtonState = null;

            if (this.buttonState != buttonState) {
                // The LightBlueButton only can be toggled.. If you send the PIN code, it toggles.. so
                // we only send if we are toggling...
                encodedButtonState = new byte[] {'X', '1', '2', '3', '4'};
            }

            if (encodedButtonState != null) {
                discoveredBean.sendSerialMessage(encodedButtonState);
            }
        }
    }

    protected void sendRemoteStateQuery() {

        if (shouldRun && discoveredBean != null & discoveredBean.isConnected()) {
            discoveredBean.sendSerialMessage(new byte[] {'Q', '1', '2', '3', '4'});
        }

        // The LightBlue will respond with a serial message..
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
                if (isCommunicating() && buttonState != ButtonState.OFF) {
                    setRemoteState(ButtonState.OFF);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // who cares
                }

                stop();
            }
        } else {
            stop();
        }
    }

    public void stop() {
        shouldRun = false;

        postButtonLostEvent(button.getId());

        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.unregister(GenericButtonCommunicatorStub.this);
            }
        });

        disconnect();
    }

    protected void disconnect() {
        if (discoveredBean != null) {
            discoveredBean.disconnect();
        }
    }

    // This only sets local state, it does not result in a request to set remote state... this
    // would presumably be called after we retrieve remote state (or during startup of this fragment)
    protected void setLocalButtonState(final ButtonState buttonState) {

        this.lastButtonStateUpdateTimeMillis = SystemClock.uptimeMillis();
        Log.d(TAG, "Button state updated @'" + lastButtonStateUpdateTimeMillis + ".'");

        if (this.buttonState != buttonState) {
            Log.d(TAG, "Button state changed @'" + lastButtonStateUpdateTimeMillis + ".'");
            this.buttonState = buttonState;

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
        return new ButtonStateChangeReport(getButton().getId(), buttonState);
    }

    protected void postButtonLostEvent(final String buttonId) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.post(new ButtonLostEvent(buttonId));
            }
        });
    }

    public ButtonState getButtonState() {
        return buttonState;
    }

    public Button getButton() {
        return button;
    }

    @Subscribe
    public void onArduinoButtonStateChangeRequestEvent(final ButtonStateChangeRequest event) {

        ButtonState requestedButtonState = event.requestedButtonState;
        if (requestedButtonState == null)  {
            if (buttonState.value) {
                requestedButtonState = ButtonState.OFF_PENDING;
            } else {
                requestedButtonState = ButtonState.ON_PENDING;
            }
        }

        setRemoteState(requestedButtonState);
    }

    @Subscribe
    public void onApplicationFocusChangeEvent(final ApplicationFocusChangeEvent event) {
        // TODO - currently not using this w.r.t. our comms with LightBlue Bean
        this.inBackground = event.inBackground;
    }

    protected boolean isAutoModeEnabled() {
        return RBApplication.getInstance().getAutoModeEnabledFlag();
    }

    protected BeanManager getBeanManager() {
        return BeanManager.getInstance();
    }
}

