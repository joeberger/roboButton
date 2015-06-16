package com.ndipatri.roboButton.dagger.bluetooth.communication.impl;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.models.Button;

import java.io.UnsupportedEncodingException;

import nl.littlerobots.bean.Bean;
import nl.littlerobots.bean.BeanDiscoveryListener;
import nl.littlerobots.bean.BeanListener;
import nl.littlerobots.bean.BeanManager;

/**
 * Communicates with each individual LightBlue Bean Button
 */
public class LightBlueButtonCommunicatorImpl extends ButtonCommunicator {

    private static final String TAG = LightBlueButtonCommunicatorImpl.class.getCanonicalName();

    private Bean discoveredBean;

    BluetoothAdapter bluetoothAdapter = null;

    public LightBlueButtonCommunicatorImpl(final Context context, final Button button) {
        super(context, button);

        Log.d(TAG, "Starting LightBlue button communicator for '" + button.getId() + "'.");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        start();
    }

    public void startCommunicating() {
        startButtonConnect();
    }

    public synchronized void startButtonConnect() {

        if (shouldRun && bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            getBeanManager().startDiscovery(getButtonDiscoveryListener());
        } else {
            shouldRun = false;
        }
    }

    protected BeanDiscoveryListener getButtonDiscoveryListener() {
        return new BeanDiscoveryListener() {
            @Override
            public void onBeanDiscovered(Bean discoveredBean) {
                if (shouldRun && discoveredBean.getDevice().getAddress().equals(button.getId())) {
                    LightBlueButtonCommunicatorImpl.this.discoveredBean = discoveredBean;
                    discoveredBean.connect(context, getBeanConnectionListener());
                }
            }

            @Override
            public void onDiscoveryComplete() {
                if (shouldRun && LightBlueButtonCommunicatorImpl.this.discoveredBean == null) {
                    // try indefinitely until this communicator is explicitly stopped
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

                // The LightBlue Button doesn't send its state upon BT connect, so we query
                // it once here.. after that, changes in state are pushed down to us.
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

                        setRemoteAutoStateIfApplicable(newButtonState);

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

            if (this.localButtonState != buttonState) {
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

