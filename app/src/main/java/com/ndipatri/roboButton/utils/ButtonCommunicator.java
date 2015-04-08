package com.ndipatri.roboButton.utils;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ApplicationFocusChangeEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonStateChangeReport;
import com.ndipatri.roboButton.events.ButtonStateChangeRequest;
import com.ndipatri.roboButton.events.BluetoothDisabledEvent;
import com.ndipatri.roboButton.models.Button;
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
public class ButtonCommunicator {

    private static final String TAG = ButtonCommunicator.class.getCanonicalName();

    @Inject protected ButtonDiscoveryProvider buttonDiscoveryProvider;

    protected long communicationsGracePeriodMillis = -1;

    // region localArgs
    private BluetoothSocket socket = null;

    protected static final int QUERY_STATE_MESSAGE = 0;
    protected static final int SET_STATE_MESSAGE = 1;
    protected static final int AUTO_SHUTDOWN = 2;
    protected static final int CONNECTIVITY_CHECK_MESSAGE = 3;

    // Handler which uses background thread to handle BT communications
    private MessageHandler bluetoothMessageHandler;

    protected long queryStateIntervalMillis = -1;

    protected boolean inBackground = false;

    private boolean shouldRun = false;

    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    // This value will always be set by what is received from Button itself
    private ButtonState buttonState = ButtonState.NEVER_CONNECTED;

    private Button button;

    private Context context;

    private long lastButtonStateUpdateTimeMillis;

    // endregion

    public ButtonCommunicator(final Context context, final Button button) {

        Log.d(TAG, "Starting new monitor for button '" + button.getId() + "'.");

        this.context = context;
        this.button = button;

        communicationsGracePeriodMillis = context.getResources().getInteger(R.integer.communications_grace_period_millis);

        ((RBApplication)context).getGraph().inject(this);

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

        // The '0' means the last time we spoke to this button was in 1970.. which essentially means too long ago.
        lastButtonStateUpdateTimeMillis = 0;

        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                BusProvider.getInstance().register(ButtonCommunicator.this);
            }
        });

        scheduleImmediateQueryStateMessage();
        scheduleConnectivityCheck();
    }

    public boolean isCommunicating() {
        long timeSinceLastUpdate = SystemClock.uptimeMillis() - lastButtonStateUpdateTimeMillis;
        boolean isCommunicating = timeSinceLastUpdate <= communicationsGracePeriodMillis;
        
        Log.d(TAG, "isCommunicating(): '" + isCommunicating + "'");
        
        return isCommunicating;
    }

    private void scheduleImmediateQueryStateMessage() {
        bluetoothMessageHandler.queueQueryStateRequest(0);
    }

    private void scheduleConnectivityCheck() {
        bluetoothMessageHandler.queueConnectivityCheck(communicationsGracePeriodMillis);
    }

    public void shutdown() {
        if (isAutoModeEnabled() && button.isAutoModeEnabled() && isCommunicating()) {
            bluetoothMessageHandler.queueAutoShutdownRequest();
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
                BusProvider.getInstance().unregister(ButtonCommunicator.this);
            }
        });

        bluetoothMessageHandler.removeMessages(QUERY_STATE_MESSAGE);
        bluetoothMessageHandler.removeMessages(SET_STATE_MESSAGE);
        bluetoothMessageHandler.removeMessages(AUTO_SHUTDOWN);
        bluetoothMessageHandler.removeMessages(CONNECTIVITY_CHECK_MESSAGE);

        disconnect();
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

                BusProvider.getInstance().post(new ButtonStateChangeReport(getButton().getId(), buttonState));
            }
        });
    }

    @Produce
    public ButtonStateChangeReport produceStateChangeReport() {
        return new ButtonStateChangeReport(getButton().getId(), buttonState);
    }

    private void scheduleQueryStateMessage() {
        bluetoothMessageHandler.queueQueryStateRequest(getQueryStateIntervalMillis());
    }

    @Override
    public String toString() {
        return "ButtonMonitor{" +
                "button=" + button +
                ", buttonState=" + buttonState +
                ", socket=" + socket +
                '}';
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

                            if (buttonState == ButtonState.NEVER_CONNECTED) {
                                
                                if (isAutoModeEnabled() && button.isAutoModeEnabled() && newRemoteState != ButtonState.ON) {

                                    // Now that we've established we can communicate with newly discovered
                                    // button, let's set its auto-state....
                                    setRemoteState(ButtonState.ON);
                                }
                            }
                            
                            setLocalButtonState(newRemoteState);
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

                case CONNECTIVITY_CHECK_MESSAGE:

                    Log.d(TAG, "checkingConnectivity...");
                    if (shouldRun) {

                        if (isCommunicating()) {
                            postButtonStateChangeReport(buttonState);
                            scheduleConnectivityCheck();
                        } else {
                            stop();
                            postButtonStateChangeReport(ButtonState.DISCONNECTED);
                        }
                    }

                    break;
            }
        }
    }
    
    public void postButtonLostEvent(final String buttonId) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                BusProvider.getInstance().post(new ButtonLostEvent(buttonId));
            }
        });
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
                // NJD TODO - one theory is to reconnect and see if that helps...
                // disconnect();
                
                // another is to just continue to listen until we are declare no longer communicating and are killed
                // by the monitoring service.
                newButtonState = null;
            }
        }

        return newButtonState;
    }

    private void setRemoteState(ButtonState buttonState) {

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
            buttonDiscoveryProvider.stopButtonDiscovery();

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
    public void onArduinoButtonStateChangeRequestEvent(final ButtonStateChangeRequest event) {

        ButtonState requestedButtonState = event.requestedButtonState;
        if (requestedButtonState == null)  {
            if (buttonState.value) {
                requestedButtonState = ButtonState.OFF_PENDING;
            } else {
                requestedButtonState = ButtonState.ON_PENDING;
            }
        }

        queueSetStateRequest(requestedButtonState);
    }

    @Subscribe
    public void onApplicationFocusChangeEvent(final ApplicationFocusChangeEvent event) {
        this.inBackground = event.inBackground;
    }

    // Presumaby, this is called from the UI thread...
    protected void queueSetStateRequest(final ButtonState candidateButtonState) {
        bluetoothMessageHandler.queueSetStateRequest(candidateButtonState);
    }

    public synchronized long getQueryStateIntervalMillis() {
        return queryStateIntervalMillis * (inBackground ? 10 : 1);
    }

    public void setInBackground(boolean inBackground) {
        this.inBackground = inBackground;
    }

    public MessageHandler getBluetoothMessageHandler() {
        return bluetoothMessageHandler;
    }

    public boolean isRunning() {
        return shouldRun;
    }

    protected boolean isAutoModeEnabled() {
        return RBApplication.getInstance().getAutoModeEnabledFlag();
    }

    protected void setAutoModeEnabled(boolean enabled) {
        RBApplication.getInstance().setAutoModeEnabledFlag(enabled);
    }
}



