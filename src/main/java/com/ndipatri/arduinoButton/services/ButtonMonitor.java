package com.ndipatri.arduinoButton.services;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.ndipatri.arduinoButton.ABApplication;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProvider;
import com.ndipatri.arduinoButton.enums.ButtonState;
import com.ndipatri.arduinoButton.events.ABStateChangeReport;
import com.ndipatri.arduinoButton.events.ABStateChangeRequest;
import com.ndipatri.arduinoButton.events.BluetoothDisabledEvent;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.BusProvider;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import javax.inject.Inject;

/**
 * Communicates with each individual Button
 */
public class ButtonMonitor {

    private static final String TAG = ButtonMonitor.class.getCanonicalName();

    @Inject protected BluetoothProvider bluetoothProvider;

    protected long communicationsGracePeriodMillis = -1;

    // region localArgs
    private BluetoothSocket socket = null;

    protected static final int QUERY_STATE_MESSAGE = 0;
    protected static final int SET_STATE_MESSAGE = 1;
    protected static final int AUTO_SHUTDOWN = 2;

    // Handler which uses background thread to handle BT communications
    private MessageHandler bluetoothMessageHandler;

    protected long queryStateIntervalMillis = -1;

    protected int timeMultiplier = 1;

    private boolean shouldRun = false;

    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    // This value will always be set by what is received from Button itself
    private ButtonState buttonState = ButtonState.NEVER_CONNECTED;

    private Button button;

    private Context context;

    // The '0' means the last time we spoke to this button was in 1970.. which essentially means too long ago.
    private long lastButtonStateUpdateTimeMillis = 0;
    private long lastButtonStateChangeTimeMillis = 0;

    // endregion

    public ButtonMonitor(final Context context, final Button button) {

        Log.d(TAG, "Starting new monitor for button '" + button.getId() + "'.");

        this.context = context;
        this.button = button;

        communicationsGracePeriodMillis = context.getResources().getInteger(R.integer.communications_grace_period_millis);

        ((ABApplication)context).inject(this);

        // Create thread for handling communication with Bluetooth
        // This thread only runs if it's passed a message.. so no need worrying about if it's running or not after this point.
        HandlerThread messageProcessingThread = new HandlerThread("GetSet_BluetoothCommunicationThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        messageProcessingThread.start();

        // Connect up above background thread's looper with our message processing handler.
        bluetoothMessageHandler = new MessageHandler(messageProcessingThread.getLooper());

        // Periodically query remote state...
        queryStateIntervalMillis = context.getResources().getInteger(R.integer.remote_state_check_interval_millis);

        start();
    }

    public void start() {
        shouldRun = true;

        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                BusProvider.getInstance().register(ButtonMonitor.this);
            }
        });

        scheduleImmediateQueryStateMessage();
    }

    private void scheduleImmediateQueryStateMessage() {
        bluetoothMessageHandler.queueQueryStateRequest(0);
    }

    public void shutdown() {
        if (isCommunicating() &&
            button.isAutoModeEnabled()) {

            bluetoothMessageHandler.queueAutoShutdownRequest();
        } else {
            stop();
        }
    }

    protected void stop() {
        shouldRun = false;

        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                BusProvider.getInstance().unregister(ButtonMonitor.this);
            }
        });

        bluetoothMessageHandler.removeMessages(QUERY_STATE_MESSAGE);
        bluetoothMessageHandler.removeMessages(SET_STATE_MESSAGE);

        disconnect();
    }

    // This only sets local state, it does not result in a request to set remote state... this
    // would presumably be called after we retrieve remote state (or during startup of this fragment)
    protected void setLocalButtonState(final ButtonState buttonState) {

        this.lastButtonStateUpdateTimeMillis = SystemClock.uptimeMillis();
        Log.d(TAG, "Button state updated @'" + lastButtonStateChangeTimeMillis + ".'");

        if (this.buttonState != buttonState) {
            this.lastButtonStateChangeTimeMillis = lastButtonStateUpdateTimeMillis;
            Log.d(TAG, "Button state changed @'" + lastButtonStateChangeTimeMillis + ".'");
            this.buttonState = buttonState;

            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "State is '" + buttonState + "'");

                    BusProvider.getInstance().post(new ABStateChangeReport(new ABStateChangeReport.ABStateChangeReportValue(buttonState, button.getId())));
                }
            });
        }
    }

    private void scheduleQueryStateMessage() {
        bluetoothMessageHandler.queueQueryStateRequest(getQueryStateIntervalMillis());
    }

    // Hands outgoing bluetooth messages to background thread.
    protected final class MessageHandler extends Handler {

        private ButtonState candidateButtonState;

        public MessageHandler(Looper looper) {
            super(looper);
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

        public void queueAutoShutdownRequest() {

            // If a set request is already pending, do nothing.
            removeMessages(SET_STATE_MESSAGE);
            removeMessages(QUERY_STATE_MESSAGE);

            Message rawMessage = obtainMessage();
            rawMessage.what = AUTO_SHUTDOWN;

            // To be handled by separate thread.
            sendMessage(rawMessage);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case QUERY_STATE_MESSAGE:

                    if (shouldRun) {

                        Log.d(TAG, "queryRemoteState()");
                        ButtonState newRemoteState = getRemoteState();

                        if (newRemoteState != null) {

                            boolean isBeaconFilteringOn = ABApplication.getInstance().getBooleanPreference(ABApplication.BEACON_FILTER_ON_PREF, false);

                            if (buttonState == ButtonState.NEVER_CONNECTED &&
                                isBeaconFilteringOn &&
                                button.isAutoModeEnabled() &&
                                newRemoteState != ButtonState.ON) {

                                // Now that we've established we can communicate with newly discovered
                                // button, let's set its auto-state....
                                setRemoteState(ButtonState.ON);
                            } else {
                                setLocalButtonState(newRemoteState);
                            }
                        }

                        scheduleQueryStateMessage();
                    }

                    break;

                case SET_STATE_MESSAGE:

                    if (shouldRun) {
                        Log.d(TAG, "setRemoteState()");
                        setRemoteState(candidateButtonState);
                    }

                    break;


                case AUTO_SHUTDOWN:

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

    public void disconnect() {
        if (socket != null) {
            Log.d(TAG, "Shutting down Bluetooth Socket for Button('" + button.getId() + "').");
            try {
                socket.close();
            } catch (IOException ignored) {
            }

            setLocalButtonState(ButtonState.DISCONNECTED);
        }
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
                disconnect();
            }
        }

        return newButtonState;
    }

    public void setRemoteState(ButtonState buttonState) {

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
            bluetoothSocket = button.getBluetoothDevice().createInsecureRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));

            // Cancel discovery because it will slow down the connection
            bluetoothProvider.cancelDiscovery();

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
                BusProvider.getInstance().post(new BluetoothDisabledEvent());
            }

            bluetoothSocket = null;
        }

        return bluetoothSocket;
    }

    public ButtonState getButtonState() {
        return buttonState;
    }

    public Button getButton() {
        return button;
    }

    @Subscribe
    public void onArduinoButtonStateChangeRequestEvent(final ABStateChangeRequest event) {
        queueSetStateRequest(event.requestedButtonState);
    }

    // Presumaby, this is called from the UI thread...
    protected void queueSetStateRequest(final ButtonState candidateButtonState) {
        bluetoothMessageHandler.queueSetStateRequest(candidateButtonState);
    }

    public synchronized long getQueryStateIntervalMillis() {
        return queryStateIntervalMillis * timeMultiplier;
    }

    public synchronized void setTimeMultiplier(int timeMultiplier) {
        this.timeMultiplier = timeMultiplier;
    }

    public MessageHandler getBluetoothMessageHandler() {
        return bluetoothMessageHandler;
    }

    public boolean isRunning() {
        return shouldRun;
    }

    public long getLastButtonStateChangeTimeMillis() {
        return lastButtonStateChangeTimeMillis;
    }

    public boolean isCommunicating() {
        long timeSinceLastUpdate = SystemClock.uptimeMillis() - lastButtonStateUpdateTimeMillis;
        return timeSinceLastUpdate <= communicationsGracePeriodMillis;
    }
}



