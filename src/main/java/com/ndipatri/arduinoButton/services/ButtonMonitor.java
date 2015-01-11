package com.ndipatri.arduinoButton.services;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.ndipatri.arduinoButton.ArduinoButtonApplication;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProvider;
import com.ndipatri.arduinoButton.enums.ButtonState;
import com.ndipatri.arduinoButton.events.ArduinoButtonBluetoothDisabledEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonInformationEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonStateChangeReportEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonStateChangeRequestEvent;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.BusProvider;
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

    // region localArgs
    private BluetoothSocket socket = null;

    protected static final int QUERY_STATE_MESSAGE = 0;
    protected static final int SET_STATE_MESSAGE = 1;

    // Handler which uses background thread to handle BT communications
    private MessageHandler bluetoothMessageHandler;

    protected long queryStateIntervalMillis = -1;

    protected int timeMultiplier = 1;

    private boolean shouldRun = false;

    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private ButtonState buttonState = null;

    private Button button;

    private Context context;

    // endregion

    public ButtonMonitor(final Context context, final Button button) {

        this.context = context;
        this.button = button;

        ((ArduinoButtonApplication)context).inject(this);

        // Create thread for handling communication with Bluetooth
        // This thread only runs if it's passed a message.. so no need worrying about if it's running or not after this point.
        HandlerThread messageProcessingThread = new HandlerThread("GetSet_BluetoothCommunicationThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        messageProcessingThread.start();

        // Connect up above background thread's looper with our message processing handler.
        bluetoothMessageHandler = new MessageHandler(messageProcessingThread.getLooper());

        // Periodically query remote state...
        queryStateIntervalMillis = context.getResources().getInteger(R.integer.remote_state_check_interval_millis);

        setLocalButtonState(ButtonState.NEVER_CONNECTED);

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

    public void stop() {
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

        this.buttonState = buttonState;

        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "State is '" + buttonState + "'");
                BusProvider.getInstance().post(new ArduinoButtonStateChangeReportEvent(button.getId(), buttonState));
            }
        });
    }

    private void scheduleImmediateQueryStateMessage() {
        bluetoothMessageHandler.queueQueryStateRequest(0);
    }

    private void scheduleQueryStateMessage() {
        bluetoothMessageHandler.queueQueryStateRequest(getQueryStateIntervalMillis());
    }

    // Hands outgoing bluetooth messages to background thread.
    protected final class MessageHandler extends Handler {

        public MessageHandler(Looper looper) {
            super(looper);
        }

        public void queueQueryStateRequest(final long offsetMillis) {

            // We only queue a query request if NO requests are already pending...
            if (!hasMessages(SET_STATE_MESSAGE) &&
                    !hasMessages(QUERY_STATE_MESSAGE)) {

                // Queue a QueryState Message
                Message rawMessage = obtainMessage();
                rawMessage.what = QUERY_STATE_MESSAGE;

                // To be handled by separate thread.
                sendMessageDelayed(rawMessage, offsetMillis);
            }
        }

        public void queueSetStateRequest() {

            // If a set request is already pending, do nothing.
            if (!hasMessages(SET_STATE_MESSAGE)) {

                // A set request pre-empts any pending query request
                removeMessages(QUERY_STATE_MESSAGE);

                Message rawMessage = obtainMessage();
                rawMessage.what = SET_STATE_MESSAGE;

                // To be handled by separate thread.
                sendMessage(rawMessage);
            }
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case QUERY_STATE_MESSAGE:

                    if (shouldRun) {

                        Log.d(TAG, "queryRemoteState()");
                        getRemoteState();
                    }

                    break;

                case SET_STATE_MESSAGE:

                    if (shouldRun) {
                        Log.d(TAG, "setRemoteState()");
                        try {
                            setRemoteState();
                        } catch (Exception ex) {
                            BusProvider.getInstance().post(new ArduinoButtonInformationEvent(context.getString(R.string.transmission_failure), button.getId()));
                        }
                    }

                    break;
            }

            scheduleQueryStateMessage();
        }

    }

    public ByteBuffer encodeCurrentButtonState() {

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

    public void getRemoteState() {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(1);

        try {
            if (socket == null || !socket.isConnected()) {
                Log.d(TAG, "Trying to create bluetooth connection...");
                socket = createConnectionToBluetoothDevice();
            }

            if (socket != null) {
                Log.d(TAG, "Bluetooth connect. Getting output stream ...");

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

                    return;
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
                final ButtonState newButtonState = Integer.valueOf(responseChar) > 0 ? ButtonState.ON : ButtonState.OFF;
                setLocalButtonState(newButtonState);
            } catch (NumberFormatException nex) {
                Log.d(TAG, "Invalid response from bluetooth device: '" + this + "'.");
                disconnect();
            }
        }

    }

    public void setRemoteState() {

        try {
            if (socket == null || !socket.isConnected()) {
                BusProvider.getInstance().post(new ArduinoButtonInformationEvent(context.getString(R.string.opening_bluetooth_socket), button.getId()));
                socket = createConnectionToBluetoothDevice();
            }

            if (socket != null) {

                ByteBuffer desiredState = encodeCurrentButtonState();

                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(new byte[]{0x58, 0x58, 0x58}); // 'XXX' - StateChangeRequest
                outputStream.write(desiredState.array());
            }
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            BusProvider.getInstance().post(new ArduinoButtonInformationEvent(context.getString(R.string.transmission_failure), button.getId()));
            Log.d(TAG, "Socket connect exception!", connectException);
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
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
                BusProvider.getInstance().post(new ArduinoButtonBluetoothDisabledEvent());
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
    public void onArduinoButtonStateChangeRequestEvent(final ArduinoButtonStateChangeRequestEvent event) {
        this.buttonState = event.requestedButtonState;
        bluetoothMessageHandler.queueSetStateRequest();
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
}

