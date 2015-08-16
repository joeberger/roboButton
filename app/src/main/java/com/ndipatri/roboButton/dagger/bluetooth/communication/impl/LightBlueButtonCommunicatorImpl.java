package com.ndipatri.roboButton.dagger.bluetooth.communication.impl;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.ndipatri.roboButton.enums.ButtonState;
import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;

import java.io.UnsupportedEncodingException;


/**
 * Communicates with each individual LightBlue Bean Button
 */
public class LightBlueButtonCommunicatorImpl extends ButtonCommunicator {

    private static final String TAG = LightBlueButtonCommunicatorImpl.class.getCanonicalName();

    private Bean discoveredBean;

    private static final int BEAN_STARTUP_DELAY = 10000;

    public LightBlueButtonCommunicatorImpl(final Context context, Bean discoveredBean) {
        super(context, discoveredBean.getDevice(), discoveredBean.getDevice().getAddress());

        if (discoveredBean == null) {
            throw new NullPointerException();
        }

        Log.d(TAG, "Starting LightBlue button communicator for '" + buttonId + "' (already connected).");

        this.discoveredBean = discoveredBean;

        start();
    }

    public void startCommunicating() {
        startButtonConnect();
        sendDelayedRemoteStateQuery();
    }

    public synchronized void startButtonConnect() {
        discoveredBean.connect(context, getBeanConnectionListener());
    }

    protected BeanListener getBeanConnectionListener() {
        return new BeanListener() {

            @Override
            public void onConnected() {
                Log.d(TAG, "onConnected()");

                sendDelayedRemoteStateQuery();
            }

            @Override
            public void onConnectionFailed() {
                Log.d(TAG, "onConnectionFailed()");

                // try indefinitely until this communicator is explicitly running
                startButtonConnect();
            }

            @Override
            public void onError(BeanError beanError) {
                Log.d(TAG, "onError()");

                // try indefinitely until this communicator is explicitly running
                startButtonConnect();
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "onDisconnected()");

                stop();
                setLocalButtonState(ButtonState.OFFLINE);
            }

            @Override
            public void onSerialMessageReceived(byte[] bytes) {

                Log.d(TAG, "onSerialMessageReceived()");

                if (state == STATE.RUNNING) {
                    ButtonState newButtonState;

                    String lightBlueButtonValue = null;
                    try {
                        lightBlueButtonValue = new String(bytes, "US-ASCII");
                    } catch (UnsupportedEncodingException e) {
                        lightBlueButtonValue = null;
                    }

                    if (lightBlueButtonValue != null) {

                        // I'm making unlocked equivalent to 'ON' so 'green' means the door is open :-)
                        int buttonValue = lightBlueButtonValue.equals("unlocked") ? 1 : 0;

                        Log.d(TAG, "Serial data from LightBlue Bean: '" + this + " ', '" + buttonValue + "'.");
                        try {
                            newButtonState = buttonValue > 0 ? ButtonState.ON : ButtonState.OFF;
                        } catch (NumberFormatException nex) {
                            newButtonState = null;
                        }

                        if (newButtonState != null) {
                            setRemoteAutoStateIfApplicable(newButtonState);

                            setLocalButtonState(newButtonState);
                        }
                    }
                }
            }

            @Override
            public void onScratchValueChanged(ScratchBank scratchBank, byte[] bytes) {
                Log.d(TAG, "onScratchValueChanged()");
            }
        };
    }

    protected void setRemoteState(ButtonState buttonState) {
        if ((state == STATE.RUNNING || state == STATE.SHUTTING_DOWN) && discoveredBean != null && discoveredBean.isConnected()) {
            byte[] encodedButtonState = null;

            if (getButton().getState() != buttonState) {
                if (buttonState.value) {
                    // For the LightBlue, 'ON' means unlocked
                    encodedButtonState = new byte[]{'U', '1', '2', '3', '4'};
                    Log.d(TAG, "Sending 'Unlock' Command");
                } else {
                    encodedButtonState = new byte[]{'L', '1', '2', '3', '4'};
                    Log.d(TAG, "Sending 'Lock' Command");
                }
            }

            if (encodedButtonState != null) {
                discoveredBean.sendSerialMessage(encodedButtonState);
            }
        }
    }

    protected void sendDelayedRemoteStateQuery() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                sendRemoteStateQuery();
            }
        }, BEAN_STARTUP_DELAY);
    }

    protected void sendRemoteStateQuery() {
        if (state == STATE.RUNNING && discoveredBean != null && discoveredBean.isConnected()) {
            Log.d(TAG, "Sending remote state query...");
            discoveredBean.sendSerialMessage(new byte[]{'Q', '1', '2', '3', '4'});
            // The LightBlue will respond with a serial message..
        }
    }

    protected void stop() {
        super.stop();

        disconnect();
    }

    protected void disconnect() {
        if (discoveredBean != null) {
            discoveredBean.disconnect();
        }
    }
}

