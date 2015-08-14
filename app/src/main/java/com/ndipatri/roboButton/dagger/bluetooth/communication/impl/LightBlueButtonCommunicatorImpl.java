package com.ndipatri.roboButton.dagger.bluetooth.communication.impl;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.ndipatri.roboButton.enums.ButtonState;
import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.ScratchBank;

import java.io.UnsupportedEncodingException;


/**
 * Communicates with each individual LightBlue Bean Button
 */
public class LightBlueButtonCommunicatorImpl extends ButtonCommunicator {

    private static final String TAG = LightBlueButtonCommunicatorImpl.class.getCanonicalName();

    private Bean discoveredBean;

    public LightBlueButtonCommunicatorImpl(final Context context, final BluetoothDevice device, final String buttonId) {
        super(context, device, buttonId);

        Log.d(TAG, "Starting LightBlue button communicator for '" + buttonId + "'.");

        start();
    }

    public void startCommunicating() {
        startButtonConnect();
    }

    public synchronized void startButtonConnect() {
        getBeanManager().startDiscovery(getButtonDiscoveryListener());
    }

    protected BeanDiscoveryListener getButtonDiscoveryListener() {
        return new BeanDiscoveryListener() {
            @Override
            public void onBeanDiscovered(Bean discoveredBean, int receivedRSSI) {
                if (discoveredBean != null && state == STATE.RUNNING && discoveredBean.getDevice().getAddress().equals(buttonId)) {

                    LightBlueButtonCommunicatorImpl.this.discoveredBean = discoveredBean;
                    getBeanManager().cancelDiscovery();
                    discoveredBean.connect(context, getBeanConnectionListener());
                }
            }

            @Override
            public void onDiscoveryComplete() {
                if (state == STATE.RUNNING && LightBlueButtonCommunicatorImpl.this.discoveredBean == null) {
                    // try indefinitely until this communicator is explicitly running
                    startButtonConnect();
                }
            }
        };
    }

    protected BeanListener getBeanConnectionListener() {
        return new BeanListener() {

            @Override
            public void onConnected() {
                Log.d(TAG, "onConnected()");

                // TODO - The following is done purely due to a limitation of the LightBlueButton. Specifically, for the first 10
                // seconds of a connection, this button cannot send back data.  Normally, upon connection we would immediately
                // send a query message (sendRemoteStateQuery()), but the button won't respond to this.  So for now, we will
                // send an 'unlock' command, assume it works, and post that as an immediate fake response so we can update
                // our local state immediately... Once the button is fixed, we should just do a 'seendRemoteStateQuery()' call
                // here and not do this set nonsense.
                //
                // The LightBlue Button doesn't send its state upon BT connect, so we query
                // it once here.. after that, changes in state are pushed down to us.
                //sendRemoteStateQuery();

                //setRemoteState(ButtonState.ON); // 'ON' is unlocked
                //setLocalButtonState(ButtonState.ON);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendRemoteStateQuery();
                    }
                }, 10000);
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

    protected void sendRemoteStateQuery() {
        if (state == STATE.RUNNING && discoveredBean != null & discoveredBean.isConnected()) {
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

    protected BeanManager getBeanManager() {
        return BeanManager.getInstance();
    }
}

