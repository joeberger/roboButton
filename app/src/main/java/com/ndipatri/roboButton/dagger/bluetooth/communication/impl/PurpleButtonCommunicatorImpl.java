package com.ndipatri.roboButton.dagger.bluetooth.communication.impl;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.RBModule;
import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.BluetoothDisabledEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import javax.inject.Inject;

/**
 * Communicates with each individual Purple Button.
 *
 * This is a 'classic' Bluetooth Device and we use RFCOMM (serial) directly to communicate to we
 * need a background worker thread (the bluetoothMessageHandler below).
 *
 */
public class PurpleButtonCommunicatorImpl extends ButtonCommunicator {

    private static final String TAG = PurpleButtonCommunicatorImpl.class.getCanonicalName();

    protected long communicationsGracePeriodMillis = -1;

    private long lastButtonStateUpdateTimeMillis;

    @Inject
    @Named(RBModule.PURPLE_BUTTON)
    protected ButtonDiscoveryProvider purpleButtonDiscoveryProvider;

    // region localArgs
    private BluetoothSocket socket = null;

    protected static final int QUERY_STATE_MESSAGE = 0;
    protected static final int SET_STATE_MESSAGE = 1;
    protected static final int CONNECTIVITY_CHECK_MESSAGE = 3;

    // Handler which uses background thread to handle BT communications
    private MessageHandler bluetoothMessageHandler;

    protected long queryStateIntervalMillis = -1;

    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    // endregion

    public PurpleButtonCommunicatorImpl(final Context context, final BluetoothDevice device, final String buttonId) {
        super(context, device, buttonId);

        Log.d(TAG, "Starting new monitor for button '" + buttonId + "'.");

        RBApplication.getInstance().getGraph().inject(this);

        communicationsGracePeriodMillis = context.getResources().getInteger(R.integer.purple_button_communications_grace_period_millis);

        // Create thread for handling communication with Bluetooth
        // This thread only runs if it's passed a message.. so no need worrying about if it's running or not after this point.
        HandlerThread messageProcessingThread = new HandlerThread("GetSet_BluetoothCommunicationThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        messageProcessingThread.start();

        // Connect up above background thread's looper with our message processing handler.
        bluetoothMessageHandler = new MessageHandler(messageProcessingThread.getLooper());

        // Periodically query remote state...
        queryStateIntervalMillis = context.getResources().getInteger(R.integer.purple_button_remote_state_check_interval_millis);

        start();
    }

    @Override
    public void startCommunicating() {

        // The '0' means the last time we spoke to this button was in 1970.. which essentially means too long ago.
        lastButtonStateUpdateTimeMillis = 0;

        scheduleImmediateQueryStateMessage();
        scheduleConnectivityCheck();
    }

    public boolean isCommunicating() {
        long timeSinceLastUpdate = SystemClock.uptimeMillis() - lastButtonStateUpdateTimeMillis;
        boolean isCommunicating = timeSinceLastUpdate <= communicationsGracePeriodMillis;

        Log.d(TAG, "isCommunicating(): '" + isCommunicating + "'");

        return isCommunicating;
    }

    protected void stop() {
        super.stop();

        bluetoothMessageHandler.removeMessages(QUERY_STATE_MESSAGE);
        bluetoothMessageHandler.removeMessages(SET_STATE_MESSAGE);
        bluetoothMessageHandler.removeMessages(CONNECTIVITY_CHECK_MESSAGE);

        disconnect();
    }

    protected void disconnect() {
        if (socket != null) {
            Log.d(TAG, "Shutting down Bluetooth Socket for Button('" + buttonId + "').");
            try {
                socket.close();
            } catch (IOException ignored) {
            }

            setLocalButtonState(ButtonState.OFFLINE);
        }
    }

    @Override
    protected void setLocalButtonState(final ButtonState buttonState) {

        this.lastButtonStateUpdateTimeMillis = SystemClock.uptimeMillis();
        Log.d(TAG, "Purple button state updated @'" + lastButtonStateUpdateTimeMillis + ".'");

        super.setLocalButtonState(buttonState);

    }

    private void scheduleImmediateQueryStateMessage() {
        bluetoothMessageHandler.queueQueryStateRequest(0);
    }

    private void scheduleQueryStateMessage() {
        bluetoothMessageHandler.queueQueryStateRequest(getQueryStateIntervalMillis());
    }

    private void scheduleConnectivityCheck() {
        Log.d(TAG, "scheduleConnectivityCheck()");
        bluetoothMessageHandler.queueConnectivityCheck(communicationsGracePeriodMillis);
    }

    // Hands outgoing bluetooth messages to background thread.
    protected final class MessageHandler extends Handler {

        private ButtonState candidateButtonState;

        public MessageHandler(Looper looper) {
            super(looper);
        }

        public void queueConnectivityCheck(final long offsetMillis) {

            if (hasMessages(CONNECTIVITY_CHECK_MESSAGE)) {
                removeMessages(CONNECTIVITY_CHECK_MESSAGE);
            }
            
            // Queue a ConnectivityCheck Message
            Message rawMessage = obtainMessage();
            rawMessage.what = CONNECTIVITY_CHECK_MESSAGE;

            // To be handled by separate thread.
            sendMessageDelayed(rawMessage, offsetMillis);
        }

        public void queueQueryStateRequest(final long offsetMillis) {

            // query has lowest priority
            if (!hasMessages(SET_STATE_MESSAGE) &&
                !hasMessages(QUERY_STATE_MESSAGE)) {

                // Queue a QueryState Message
                Message rawMessage = obtainMessage();
                rawMessage.what = QUERY_STATE_MESSAGE;

                // To be handled by separate thread.
                sendMessageDelayed(rawMessage, offsetMillis);
            }
        }

        public void queueSetStateRequest(final ButtonState candidateButtonState) {

            // If a set request is already pending, do nothing.
            if (!hasMessages(SET_STATE_MESSAGE)) {

                // A set request pre-empts any pending query request
                removeMessages(QUERY_STATE_MESSAGE);

                Message rawMessage = obtainMessage();
                rawMessage.what = SET_STATE_MESSAGE;

                // To be handled by separate thread.
                this.candidateButtonState = candidateButtonState;

                sendMessage(rawMessage);
            }
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case QUERY_STATE_MESSAGE:

                    if (shouldRun) {

                        Log.d(TAG, "queryRemoteState()");
                        ButtonState newRemoteState = getRemoteState();

                        if (newRemoteState != null) {

                            setRemoteAutoStateIfApplicable(newRemoteState);

                            setLocalButtonState(newRemoteState);
                        }

                        scheduleQueryStateMessage();
                    }

                    break;

                case SET_STATE_MESSAGE:

                    if (shouldRun) {
                        Log.d(TAG, "sendSerialDataToButton()");
                        sendSerialDataToButton(candidateButtonState);
                    }

                    break;

                case CONNECTIVITY_CHECK_MESSAGE:

                    Log.d(TAG, "checkingConnectivity...");
                    if (shouldRun) {

                        if (isCommunicating()) {
                            scheduleConnectivityCheck();
                        } else {
                            stop();
                            setButtonPersistedStateAndNotify(ButtonState.OFFLINE);
                        }
                    }

                    break;
            }
        }
    }
    
    public ByteBuffer encodeCurrentButtonState(final ButtonState buttonState) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(1);

        if (buttonState.value) {
            byteBuffer.put((byte) '1'); // a char is the ascii representation of '1'
        } else {
            byteBuffer.put((byte) '0');
        }

        // prepare for reading
        byteBuffer.flip();

        return byteBuffer;
    }

    // Will be null if remote state could not be determined.
    protected ButtonState getRemoteState() {

        ButtonState newButtonState = null;

        final ByteBuffer byteBuffer = ByteBuffer.allocate(1);

        try {
            if (socket == null || !socket.isConnected()) {
                Log.d(TAG, "Trying to create bluetooth connection...");
                socket = createConnectionToBluetoothDevice();
            } else {
                Log.d(TAG, "Bluetooth already connected...");
            }

            if (socket != null) {

                Log.d(TAG, "Getting output stream ...");

                // Tell Arduino to send us StateReport
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(new byte[]{0x51, 0x51, 0x51}); // 'QQQ'

                Log.d(TAG, "StateRequestUpdate sent! Waiting for reply...");

                // For now, assume one byte state response...
                final byte[] remoteStateBytes = new byte[1];

                final int bytesRead = socket.getInputStream().read(remoteStateBytes);

                if (bytesRead != 1) {
                    Log.d(TAG, "Reply received.. but not right length!");
                    disconnect();

                    return null;
                } else {
                    Log.d(TAG, "Reply received.");
                }
                byteBuffer.put(remoteStateBytes);
                byteBuffer.flip();  // prepare for reading.
            } else {
                Log.d(TAG, "Cannot create bluetooth socket!");
            }

        } catch (IOException connectException)
        {
            // Unable to connect; close the socket and get out
            Log.d(TAG, "Socket connect exception!", connectException);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        if (byteBuffer.hasRemaining())
        {
            String responseChar = String.valueOf(new char[]{(char) byteBuffer.get()});
            Log.d(TAG, "Response from bluetooth device '" + this + " ', '" + responseChar + "'.");
            try {
                newButtonState = Integer.valueOf(responseChar) > 0 ? ButtonState.ON : ButtonState.OFF;
            } catch (NumberFormatException nex) {
                Log.d(TAG, "Invalid response from bluetooth device: '" + this + "'.");
                // NJD TODO - one theory is to reconnect and see if that helps...
                // disconnect();
                
                // another is to just continue to listen until we are declare no longer communicating and are killed
                // by the monitoring service.
                newButtonState = null;
            }
        }

        return newButtonState;
    }

    private void sendSerialDataToButton(ButtonState buttonState) {

        try {
            if (socket == null || !socket.isConnected()) {
                Log.d(TAG, "Trying to create bluetooth connection...");
                socket = createConnectionToBluetoothDevice();
            } else {
                Log.d(TAG, "Bluetooth already connected...");
            }

            if (socket != null) {

                ByteBuffer desiredState = encodeCurrentButtonState(buttonState);

                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(new byte[]{0x58, 0x58, 0x58}); // 'XXX' - StateChangeRequest
                outputStream.write(desiredState.array());
            }
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            Log.d(TAG, "Socket connect exception!", connectException);
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        } finally {
            scheduleQueryStateMessage();
        }
    }

    // Should not be run in UI thread.
    private BluetoothSocket createConnectionToBluetoothDevice() {

        BluetoothSocket bluetoothSocket = null;

        try {
            Log.d(TAG, "Creating Bluetooth Socket ...");

            //bluetoothSocket = button.getBluetoothDevice().createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));

            bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));

            // Cancel discovery because it will slow down the connection
            purpleButtonDiscoveryProvider.stopButtonDiscovery();

            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            // NJD TODO - really need timeout mechanism here..
            bluetoothSocket.connect();

            Log.d(TAG, "Success!");

        } catch (IOException connectException) {

            Log.e(TAG, "Failed with Exception!", connectException);
            if (bluetoothSocket != null) {
                try {
                    bluetoothSocket.close();
                } catch (IOException ignored) {
                }
            }

            if (connectException.getMessage().contains("Bluetooth is off")) {
                bus.post(new BluetoothDisabledEvent());
            }

            bluetoothSocket = null;
        }

        return bluetoothSocket;
    }

    @Override
    protected void setRemoteState(final ButtonState candidateButtonState) {
        bluetoothMessageHandler.queueSetStateRequest(candidateButtonState);
    }

    public synchronized long getQueryStateIntervalMillis() {
        return queryStateIntervalMillis * (inBackground ? 10 : 1);
    }
}



