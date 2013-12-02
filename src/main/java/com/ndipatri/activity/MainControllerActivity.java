package com.ndipatri.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ndipatri.R;
import com.ndipatri.RoboLiftApplication;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import butterknife.InjectView;
import butterknife.Views;

public class MainControllerActivity extends Activity {

    public static final String EXTERNAL_BLUETOOTH_DEVICE_MAC = "macAddress";

    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private static final int QUERY_STATE_MESSAGE = 0;
    private static final int SET_STATE_MESSAGE = 1;

    private static final String TAG = MainControllerActivity.class.getCanonicalName();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice selectedBluetoothDevice;

    // Handler which passes outgoing bluetooth messages background thread to be processed.
    private MessageHandler bluetoothMessageHandler;

    // This ensures that we are periodically querying the remote state of bluetooth device.
    private Handler queueQueryStateHandler = null;
    private QueueQueryStateRunnable queueQueryStateRunnable = new QueueQueryStateRunnable();
    long queryStateIntervalMillis = -1;

    private boolean firstChange = true;

    private int failedToConnectCount = 0;

    private boolean shouldQueryState = false;

    protected @InjectView(R.id.progressBar) android.widget.ProgressBar progressBar;
    protected @InjectView(R.id.toggleButton) ToggleButton toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

        Views.inject(this);

        // Create thread for handling communication with Bluetooth
        // This thread only runs if it's passed a message.. so no need worrying about if it's running or not after this point.
        HandlerThread messageProcessingThread = new HandlerThread("BluetoothCommunicationThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        messageProcessingThread.start();

        // Connect up above background thread's looper with our message processing handler.
        bluetoothMessageHandler = new MessageHandler(messageProcessingThread.getLooper());

        // Periodically query remote state...
        queryStateIntervalMillis = getResources().getInteger(R.integer.remote_state_check_interval_millis);
        queueQueryStateHandler = new Handler(getMainLooper());
    }

    @Override
    public void onResume() {
        super.onResume();

        shouldQueryState = true;

        // NJD TODO - Safe to assume since our prevous Activity hooked us up? We should have BroadcastReceiver monitoring this maybe?
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        selectedBluetoothDevice = RoboLiftApplication.getInstance().getSelectedBluetoothDevice();
        if (selectedBluetoothDevice == null) {
            // presumably, the only way this would happen is if we are destroyed by OS and recreated..or due to orientation change..
            // alternatively, we could implement onSaveInstancestate() and remember selected bluetooth, but if we've been
            // recreated, might as well make them re-select desired bluetooth

            returnToBluetoothSetupActivity();
        } else {

            Toast.makeText(this, "Bluetooth Device Selected: '" + selectedBluetoothDevice + "'.", Toast.LENGTH_SHORT).show();

            setupViews();

            scheduleNextQueryStateMessage();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        shouldQueryState = false;

        queueQueryStateHandler.removeCallbacks(queueQueryStateRunnable);
        bluetoothMessageHandler.removeMessages(QUERY_STATE_MESSAGE);
        bluetoothMessageHandler.removeMessages(SET_STATE_MESSAGE);

        if (bluetoothSocket != null) {
            Log.d(TAG, "Shutting down Bluetooth Socket..");
            try {
                bluetoothSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private void returnToBluetoothSetupActivity() {
        Intent controllerIntent = new Intent(MainControllerActivity.this, BluetoothSetupActivity.class);
        startActivity(controllerIntent);
        finish();
    }

    private void scheduleNextQueryStateMessage() {
        queueQueryStateHandler.postDelayed(new QueueQueryStateRunnable(),  queryStateIntervalMillis);
    }

    private class QueueQueryStateRunnable implements Runnable {
        @Override
        public void run() {
            if (shouldQueryState) {
                bluetoothMessageHandler.queueQueryStateRequest();
                scheduleNextQueryStateMessage();
            }
        }
    }

    private void setupViews() {
        progressBar.setVisibility(View.GONE);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Log.d(TAG, "Button Pressed!");

                if (!firstChange) {
                    bluetoothMessageHandler.queueSetStateRequest();
                } else {
                    firstChange = false;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_controller_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_setup_bluetooth:

                returnToBluetoothSetupActivity();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Hands outgoing bluetooth messages to background thread.
    private final class MessageHandler extends Handler {

        public MessageHandler(Looper looper) {
            super(looper);
        }

        public void queueQueryStateRequest() {

            // We only queue a query request if NO requests are already pending...
            if (!bluetoothMessageHandler.hasMessages(SET_STATE_MESSAGE) &&
                !bluetoothMessageHandler.hasMessages(QUERY_STATE_MESSAGE)) {

                // Queue a QueryState Message
                Message rawMessage = bluetoothMessageHandler.obtainMessage();
                rawMessage.what = QUERY_STATE_MESSAGE;

                // To be handled by separate thread.
                bluetoothMessageHandler.sendMessage(rawMessage);
            }
        }

        public void queueSetStateRequest() {

            // If a set request is already pending, do nothing.
            if (!bluetoothMessageHandler.hasMessages(SET_STATE_MESSAGE)) {

                // A set request pre-empts any pending query request
                bluetoothMessageHandler.removeMessages(QUERY_STATE_MESSAGE);

                Message rawMessage = bluetoothMessageHandler.obtainMessage();
                rawMessage.what = SET_STATE_MESSAGE;

                // To be handled by separate thread.
                bluetoothMessageHandler.sendMessage(rawMessage);
            }
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case QUERY_STATE_MESSAGE:

                    if (shouldQueryState) {
                        queryRemoteState();
                    }

                    break;

                case SET_STATE_MESSAGE:

                    setRemoteState();

                    break;
            }
        }
    }

    private void publishProgress(final String progressString)  {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainControllerActivity.this, progressString, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void queryRemoteState() {

        Log.d(TAG, "queryRemoteState()");
        final ByteBuffer byteBuffer = ByteBuffer.allocate(1);

        try {
            if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                Log.d(TAG, "Trying to create bluetooth connection...");
                publishProgress(getString(R.string.opening_bluetooth_socket));
                bluetoothSocket = createConnectionToBluetoothDevice(bluetoothAdapter, selectedBluetoothDevice);
            }

            if (bluetoothSocket != null) {
                Log.d(TAG, "Bluetooth connect. Getting output stream ...");

                failedToConnectCount = 0;

                // Tell Arduino to send us StateReport
                OutputStream outputStream = bluetoothSocket.getOutputStream();
                outputStream.write(new byte[]{0x51, 0x51, 0x51}); // 'QQQ'
                Log.d(TAG, "StateRequestUpdate sent! Waiting for reply...");

                // For now, assume one byte state response...
                final byte[] remoteStateBytes = new byte[1];

                // NJD TODO - Need watchdog thread on these blocking calls (so we can call socket.close() if need be)
                final int bytesRead = bluetoothSocket.getInputStream().read(remoteStateBytes);

                if (bytesRead != 1) {
                    publishProgress(getString(R.string.transmission_failure));
                    if (bluetoothSocket != null) {
                        try { bluetoothSocket.close(); } catch (IOException ignored) { }
                    }
                    return;
                } else {
                    Log.d(TAG, "Reply received.");
                }
                byteBuffer.put(remoteStateBytes);
                byteBuffer.flip();  // prepare for reading.
            } else {
                Log.d(TAG, "Cannot create bluetooth socket!");

                failedToConnectCount++;

                if (failedToConnectCount > getResources().getInteger(R.integer.max_reconnect_attempts_before_redirect_to_step)) {
                    failedToConnectCount = 0;
                    returnToBluetoothSetupActivity();
                }
            }

        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            publishProgress(getString(R.string.transmission_failure));
            Log.d(TAG, "Socket connect exception!", connectException);
            try {
                bluetoothSocket.close();
            } catch (IOException ignored) {}
        }

        if (byteBuffer.hasRemaining()) {
            // queues up this update runnable on UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    renderLocalViewsFromRemoteState(byteBuffer);
                }
            });
        }
    }

    /**
     * This renders local views from remote state provided.
     */
    private void renderLocalViewsFromRemoteState(ByteBuffer byteBuffer) {

        // For now, assume single byte state

        toggleButton.setActivated(false);

        // Currently, the response is a single character: an ASCII representation of either a '0' or '1'
        String responseChar = String.valueOf(new char[]{(char) byteBuffer.get()});
        Log.d(TAG, "Response from bluetooth device: '" + responseChar + "'.");
        try {
            int state = Integer.valueOf(responseChar);
            if (state > 0) {
                Log.d(TAG, "State is ON");
                toggleButton.setChecked(true);
            } else {
                Log.d(TAG, "State is OFF");
                toggleButton.setChecked(false);
            }
        } catch (NumberFormatException nex) {
            Log.d(TAG, "Invalid response from bluetooth device: '");

            publishProgress(getString(R.string.transmission_failure));
            if (bluetoothSocket != null) {
                try { bluetoothSocket.close(); } catch (IOException ignored) { }
            }
        }

        progressBar.setVisibility(View.GONE);
        toggleButton.setActivated(true);
        toggleButton.setVisibility(View.VISIBLE);
    }

    private void setRemoteState() {

        Log.d(TAG, "setRemoteState()");
        try {
            if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                publishProgress(getString(R.string.opening_bluetooth_socket));
                bluetoothSocket = createConnectionToBluetoothDevice(bluetoothAdapter, selectedBluetoothDevice);
            }

            if (bluetoothSocket != null) {
                ByteBuffer desiredState = renderRemoteStateFromLocalViews();

                OutputStream outputStream = bluetoothSocket.getOutputStream();
                outputStream.write(new byte[]{0x58, 0x58, 0x58}); // 'XXX' - StateChangeRequest
                outputStream.write(desiredState.array());
            }
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            publishProgress(getString(R.string.transmission_failure));
            Log.d(TAG, "Socket connect exception!", connectException);
            if (bluetoothSocket != null) {
                try {
                    bluetoothSocket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private ByteBuffer renderRemoteStateFromLocalViews() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(1);

        if (toggleButton.isChecked()) {
            byteBuffer.put((byte) '1'); // a char is the ascii representation of '1'
        } else {
            byteBuffer.put((byte) '0');
        }

        // prepare for reading
        byteBuffer.flip();

        return byteBuffer;
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
                returnToBluetoothSetupActivity();
            }

            bluetoothSocket = null;
        }

        return bluetoothSocket;
    }
}
