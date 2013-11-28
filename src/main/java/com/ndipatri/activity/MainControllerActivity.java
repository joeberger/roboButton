package com.ndipatri.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ndipatri.R;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import butterknife.InjectView;
import butterknife.Views;

public class MainControllerActivity extends Activity {

    public static final String EXTERNAL_BLUETOOTH_DEVICE_MAC = "macAddress";

    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private static final String TAG = MainControllerActivity.class.getCanonicalName();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice selectedBluetoothDevice;

    private UpdateRemoteStateTask updateRemoteStateTask;
    private UpdateLocalStateTask updateLocalStateTask;

    private Handler remoteStateRequestHandler = null;

    private boolean firstChange = true;

    protected
    @InjectView(R.id.progressBar)
    android.widget.ProgressBar progressBar;
    protected
    @InjectView(R.id.toggleButton)
    ToggleButton toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

        Views.inject(this);

        remoteStateRequestHandler = new Handler(getMainLooper());

        // NJD TODO - Safe to assume since our prevous Activity hooked us up? We should have BroadcastReceiver monitoring this maybe?
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        selectedBluetoothDevice = getIntent().getParcelableExtra(EXTERNAL_BLUETOOTH_DEVICE_MAC);

        if (selectedBluetoothDevice == null) {
            // presumably, the only way this would happen is if we are destroyed by OS and recreated..or due to orientation change..
            // alternatively, we could implement onSaveInstancestate() and remember selected bluetooth, but if we've been
            // recreated, might as well make them re-select desired bluetooth

            Intent controllerIntent = new Intent(MainControllerActivity.this, RoboLiftStartupActivity.class);
            startActivity(controllerIntent);
            finish();
        }

        Toast.makeText(this, "Bluetooth Device Selected: '" + selectedBluetoothDevice + "'.", Toast.LENGTH_SHORT).show();

        setupViews();

        new UpdateLocalStateTask().execute();
    }

    private void setupViews() {
        progressBar.setVisibility(View.GONE);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Log.d(TAG, "Button Pressed!");

                if (!firstChange) {

                    // We have priority over remote.. So if we want to change remote, we go ahead and do it
                    // even if we are in the middle of retrieving local state (UpdateLocalStateTask)
                    updateRemoteStateTask = new UpdateRemoteStateTask();
                    updateRemoteStateTask.execute();
                } else {
                    firstChange = false;
                }
            }
        });
    }

    private void scheduleNextLocalStateUpdate() {
        remoteStateRequestHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "UpdateLocalStateTask() awake...");

                updateLocalStateTask = new UpdateLocalStateTask();
                updateLocalStateTask.execute();
            }
        }, getResources().getInteger(R.integer.remote_state_check_interval_millis));
    }

    private class UpdateLocalStateTask extends AsyncTask<Void, String, ByteBuffer> {

        @Override
        protected ByteBuffer doInBackground(Void... Void) {

            ByteBuffer byteBuffer = null;

            if (updateRemoteStateTask == null || updateRemoteStateTask.getStatus() == AsyncTask.Status.FINISHED) {

                try {
                    if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                        Log.d(TAG, "Trying to create bluetooth connection...");
                        publishProgress(getString(R.string.opening_bluetooth_socket));
                        bluetoothSocket = createConnectionToBluetoothDevice(bluetoothAdapter, selectedBluetoothDevice);
                    }

                    if (bluetoothSocket != null) {
                        Log.d(TAG, "Bluetooth connect. Getting output stream ...");
                        // Tell Arduino to send us StateReport
                        OutputStream outputStream = bluetoothSocket.getOutputStream();
                        outputStream.write(new byte[]{0x40, 0x40, 0x40}); // '@@@'
                        Log.d(TAG, "StateRequestUpdate sent! Waiting for reply...");
                        seen cases where it just blocks here indefinitely.. need a watchdog that can call bluetoohSocket.close()...

                        // For now, assume one byte state response...
                        final byte[] remoteStateBytes = new byte[1];
                        final int bytesRead = bluetoothSocket.getInputStream().read(remoteStateBytes);

                        if (bytesRead != 1) {
                            publishProgress(getString(R.string.transmission_failure));
                            return null;
                        }
                        byteBuffer = ByteBuffer.allocate(1);
                        byteBuffer.put(remoteStateBytes);
                        byteBuffer.flip();  // prepare for reading.
                    } else {
                        Log.d(TAG, "Cannot create bluetooth socket!");
                        byteBuffer = null; // for clarify
                    }

                } catch (IOException connectException) {
                    // Unable to connect; close the socket and get out
                    publishProgress(getString(R.string.transmission_failure));
                    Log.d(TAG, "Socket connect exception!", connectException);
                    try {
                        bluetoothSocket.close();
                    } catch (IOException closeException) {
                    }
                }
            }

            return byteBuffer;
        }

        protected void onProgressUpdate(String... progress) {
            Toast.makeText(MainControllerActivity.this, progress[0], Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(ByteBuffer byteBuffer) {
            super.onPostExecute(byteBuffer);

            // It's possible a local change was made while retrieving remote state, so make sure
            // we aren't in teh middle of local change...
            if (updateRemoteStateTask == null || updateRemoteStateTask.getStatus() == AsyncTask.Status.FINISHED) {
                // We only want to update state if we're NOT pending a transmission of new state

                if (byteBuffer != null) {
                    renderLocalViewsFromRemoteState(byteBuffer);
                }
            }

            scheduleNextLocalStateUpdate();
        }
    }

    /**
     * This renders local views from remote state provided.
     */
    private void renderLocalViewsFromRemoteState(ByteBuffer byteBuffer) {

        // For now, assume single byte state

        toggleButton.setActivated(false);

        int state = Integer.valueOf(String.valueOf(new char[]{(char) byteBuffer.get()}));
        if (state > 0) {
            Log.d(TAG, "State is ON");
            toggleButton.setChecked(true);
        } else {
            Log.d(TAG, "State is OFF");
            toggleButton.setChecked(false);
        }

        toggleButton.setActivated(true);
        toggleButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private class UpdateRemoteStateTask extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... Void) {

            try {
                if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                    publishProgress(getString(R.string.opening_bluetooth_socket));
                    bluetoothSocket = createConnectionToBluetoothDevice(bluetoothAdapter, selectedBluetoothDevice);
                }

                ByteBuffer desiredState = renderRemoteStateFromLocalViews();

                OutputStream outputStream = bluetoothSocket.getOutputStream();
                outputStream.write(new byte[]{0x58, 0x58, 0x58}); // 'XXX' - StateChangeRequest
                outputStream.write(desiredState.array());
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                publishProgress(getString(R.string.transmission_failure));
                Log.d(TAG, "Socket connect exception!", connectException);
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                }
            }

            updateRemoteStateTask = null;

            return null;
        }

        protected void onProgressUpdate(String... progress) {
            Toast.makeText(MainControllerActivity.this, progress[0], Toast.LENGTH_SHORT).show();
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
    private static BluetoothSocket createConnectionToBluetoothDevice(BluetoothAdapter bluetoothAdapter, BluetoothDevice bluetoothDevice) {

        BluetoothSocket bluetoothSocket = null;

        try {
            Log.d(TAG, "Creating Bluetooth Socket ...");
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));

            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            bluetoothSocket.connect();

            Log.d(TAG, "Success!");

        } catch (IOException connectException) {

            Log.e(TAG, "Failed with Exception!", connectException);
            try {
                bluetoothSocket.close();
            } catch (IOException closeException) {
            }
            bluetoothSocket = null;
        }

        return bluetoothSocket;
    }
}
