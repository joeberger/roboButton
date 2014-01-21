package com.ndipatri.arduinoButton.fragments;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.events.ArduinoButtonBluetoothDisabledEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonInformationEvent;
import com.ndipatri.arduinoButton.utils.BusProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import butterknife.InjectView;
import butterknife.Views;

/**
 * Created by ndipatri on 12/31/13.
 *
 *  This fragment presents the user with a button which can be pressed to toggle on/off state.  When
 *  the button is pressed, the state is 'pending' and the button is disabled. A single attempt is then
 *  made to change remote arduino button based on this state.
 *
 *  Periodically, this fragment overwrites the current button state with the remote arduino button state.
 *
 **/
public class ArduinoButtonFragment extends Fragment {

    private static final String TAG = ArduinoButtonFragment.class.getCanonicalName();

    private BluetoothSocket socket = null;

    private static final int QUERY_STATE_MESSAGE = 0;
    private static final int SET_STATE_MESSAGE = 1;

    // Handler which uses background thread to handle BT communications
    private MessageHandler bluetoothMessageHandler;

    long queryStateIntervalMillis = -1;

    private boolean shouldRun = false;

    private boolean neverConnected = true;

    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private BUTTON_STATE buttonState = null;

    private static enum BUTTON_STATE {
        ON (true, true, R.drawable.green_button),
        OFF (false, true, R.drawable.red_button),
        ON_PENDING(true, false, R.drawable.yellow_button),
        OFF_PENDING(false, false, R.drawable.yellow_button),
        ;

        private boolean value;

        // Is button enabled in this state
        private boolean enabled;

        private int drawableResourceId;

        private BUTTON_STATE (final boolean value, final boolean enabled, final int drawableResourceId) {
            this.value = value;
            this.enabled = enabled;
            this.drawableResourceId = drawableResourceId;
        }
    }

    // This only sets local state, it does not result in a request to set remote state... this
    // would presumably be called after we retrieve remote state (or during startup of this fragment)
    public void setButtonState(final BUTTON_STATE buttonState) {

        this.buttonState = buttonState;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "State is '" + buttonState + "'");
                imageView.setImageResource(buttonState.drawableResourceId);
            }
        });
    }

    // This sets a pending local state then requests a remote state change...
    public void toggleButtonState() {
        if (buttonState.value) {
            setButtonState(BUTTON_STATE.OFF_PENDING);
        } else {
            setButtonState(BUTTON_STATE.ON_PENDING);
        }

        bluetoothMessageHandler.queueSetStateRequest();
    }

    public static ArduinoButtonFragment newInstance(String description, String buttonId, BluetoothDevice device) {

        ArduinoButtonFragment arduinoButtonFragment = new ArduinoButtonFragment();
        Bundle args = new Bundle();
        arduinoButtonFragment.setArguments(args);

        arduinoButtonFragment.setButtonId(buttonId);
        arduinoButtonFragment.setDescription(description);
        arduinoButtonFragment.setBluetoothDevice(device);

        return arduinoButtonFragment;
    }

    // ButterKnife Injected Views
    protected @InjectView(R.id.imageView) ImageView imageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.button_layout, container, false);

        // Use ButterKnife for view injection (http://jakewharton.github.io/butterknife/)
        Views.inject(this, rootView);

        // Create thread for handling communication with Bluetooth
        // This thread only runs if it's passed a message.. so no need worrying about if it's running or not after this point.
        HandlerThread messageProcessingThread = new HandlerThread("GetSet_BluetoothCommunicationThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        messageProcessingThread.start();

        // Connect up above background thread's looper with our message processing handler.
        bluetoothMessageHandler = new MessageHandler(messageProcessingThread.getLooper());

        // Periodically query remote state...
        queryStateIntervalMillis = getResources().getInteger(R.integer.remote_state_check_interval_millis);

        setButtonState(BUTTON_STATE.OFF_PENDING);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonState.enabled) {
                    Log.d(TAG, "Button Pressed!");
                    toggleButtonState();
                }
            }
        });

        rootView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                ButtonDetailsDialogFragment dialog = ButtonDetailsDialogFragment.newInstance(getButtonId());
                dialog.show(getFragmentManager().beginTransaction(), "button details dialog");

                return false;
            }
        });

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();

        shouldRun = false;

        bluetoothMessageHandler.removeMessages(QUERY_STATE_MESSAGE);
        bluetoothMessageHandler.removeMessages(SET_STATE_MESSAGE);
    }


    public void onResume() {
        super.onResume();

        shouldRun = true;

        scheduleQueryStateMessage();
    }

    private void scheduleQueryStateMessage() {
        bluetoothMessageHandler.queueQueryStateRequest();
    }

    // Hands outgoing bluetooth messages to background thread.
    private final class MessageHandler extends Handler {

        public MessageHandler(Looper looper) {
            super(looper);
        }

        public void queueQueryStateRequest() {

            // We only queue a query request if NO requests are already pending...
            if (!hasMessages(SET_STATE_MESSAGE) &&
                    !hasMessages(QUERY_STATE_MESSAGE)) {

                // Queue a QueryState Message
                Message rawMessage = obtainMessage();
                rawMessage.what = QUERY_STATE_MESSAGE;

                // To be handled by separate thread.
                sendMessageDelayed(rawMessage, queryStateIntervalMillis);
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
                        readRemoteState();

                        scheduleQueryStateMessage();
                    }

                    break;

                case SET_STATE_MESSAGE:

                    if (shouldRun) {
                        Log.d(TAG, "setRemoteState()");
                        try {
                            setRemoteState();
                        } catch (Exception ex) {
                            BusProvider.getInstance().post(new ArduinoButtonInformationEvent(getActivity().getString(R.string.transmission_failure), getDescription(), getButtonId()));
                        }

                        scheduleQueryStateMessage();
                    }

                    break;
            }
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
            Log.d(TAG, "Shutting down Bluetooth Socket for Button('" + getDescription() + "').");
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    public void readRemoteState() {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(1);

        try {
            if (socket == null || !socket.isConnected()) {
                Log.d(TAG, "Trying to create bluetooth connection...");
                socket = createConnectionToBluetoothDevice(BluetoothAdapter.getDefaultAdapter(), getBluetoothDevice());
            }

            if (socket != null) {
                Log.d(TAG, "Bluetooth connect. Getting output stream ...");

                // Tell Arduino to send us StateReport
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(new byte[]{0x51, 0x51, 0x51}); // 'QQQ'
                Log.d(TAG, "StateRequestUpdate sent! Waiting for reply...");

                // For now, assume one byte state response...
                final byte[] remoteStateBytes = new byte[1];

                // NJD TODO - Need watchdog thread on these blocking calls (so we can call socket.close() if need be)
                final int bytesRead = socket.getInputStream().read(remoteStateBytes);

                if (bytesRead != 1) {
                    disconnect();

                    return;
                } else {
                    Log.d(TAG, "Reply received.");
                }
                byteBuffer.put(remoteStateBytes);
                byteBuffer.flip();  // prepare for reading.

                neverConnected = false;
            } else {
                Log.d(TAG, "Cannot create bluetooth socket!");
            }

        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            Log.d(TAG, "Socket connect exception!", connectException);
            try { socket.close(); } catch (IOException ignored) {}
        }

        if (byteBuffer.hasRemaining()) {

            String responseChar = String.valueOf(new char[]{(char) byteBuffer.get()});
            Log.d(TAG, "Response from bluetooth device '" + this + " ', '" + responseChar + "'.");
            try {
                setButtonState(Integer.valueOf(responseChar) > 0 ? BUTTON_STATE.ON : BUTTON_STATE.OFF);
            } catch (NumberFormatException nex) {
                Log.d(TAG, "Invalid response from bluetooth device: '" + this + "'.");
                disconnect();
            }
        }
    }

    public void setRemoteState() {

        try {
            if (socket == null || !socket.isConnected()) {
                BusProvider.getInstance().post(new ArduinoButtonInformationEvent(getActivity().getString(R.string.opening_bluetooth_socket), getDescription(), getButtonId()));
                socket = createConnectionToBluetoothDevice(BluetoothAdapter.getDefaultAdapter(), getBluetoothDevice());
            }

            if (socket != null) {

                ByteBuffer desiredState = encodeCurrentButtonState();

                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(new byte[]{0x58, 0x58, 0x58}); // 'XXX' - StateChangeRequest
                outputStream.write(desiredState.array());

                neverConnected = false;
            }
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            BusProvider.getInstance().post(new ArduinoButtonInformationEvent(getActivity().getString(R.string.transmission_failure), getDescription(), getButtonId()));
            Log.d(TAG, "Socket connect exception!", connectException);
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    // Should not be run in UI thread.
    private BluetoothSocket createConnectionToBluetoothDevice(BluetoothAdapter bluetoothAdapter, BluetoothDevice bluetoothDevice) {

        BluetoothSocket bluetoothSocket = null;

        try {
            Log.d(TAG, "Creating Bluetooth Socket ...");
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));

            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            // NJD TODO - really need timeout mechanism here..
            bluetoothSocket.connect();

            Log.d(TAG, "Success!");

        } catch (IOException connectException) {

            Log.e(TAG, "Failed with Exception!", connectException);
            if (bluetoothSocket != null) {
                try { bluetoothSocket.close(); } catch (IOException ignored) { }
            }

            if (connectException.getMessage().contains("Bluetooth is off")) {
                BusProvider.getInstance().post(new ArduinoButtonBluetoothDisabledEvent());
            }

            bluetoothSocket = null;
        }

        return bluetoothSocket;
    }

    public boolean isConnected() {
        return (socket != null)  && socket.isConnected();
    }


    public boolean isNeverConnected() {
        return neverConnected;
    }

    private synchronized String getDescription() {
        return getArguments().getString("description");
    }

    private synchronized void setDescription(String description) {
        getArguments().putString("description", description);
    }

    private synchronized String getButtonId() {
        return getArguments().getString("buttonId");
    }

    private synchronized void setButtonId(String buttonId) {
        getArguments().putString("buttonId", buttonId);
    }

    private synchronized BluetoothDevice getBluetoothDevice() {
        return getArguments().getParcelable("bluetoothDevice");
    }

    private synchronized void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        getArguments().putParcelable("bluetoothDevice", bluetoothDevice);
    }
}
