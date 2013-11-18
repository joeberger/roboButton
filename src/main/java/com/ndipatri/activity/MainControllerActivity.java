package com.ndipatri.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
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
import sun.jvm.hotspot.interpreter.BytecodeGetField;

public class MainControllerActivity extends Activity {

    public static final String EXTERNAL_BLUETOOTH_DEVICE_MAC = "macAddress";

    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private static final String TAG = MainControllerActivity.class.getCanonicalName();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice selectedBluetoothDevice;

    protected @InjectView(R.id.textView) TextView textView;
    protected @InjectView(R.id.progressBar) android.widget.ProgressBar progressBar;
    protected @InjectView(R.id.toggleButton) ToggleButton toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

        Views.inject(this);

        // NJD TODO - Safe to assume since our prevous Activity hooked us up? We should have BroadcastReceiver monitoring this maybe?
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        selectedBluetoothDevice = getIntent().getParcelableExtra(EXTERNAL_BLUETOOTH_DEVICE_MAC);

        Toast.makeText(this, "Bluetooth Device Selected: '" + selectedBluetoothDevice + "'.", Toast.LENGTH_SHORT).show();

        setupViews();

        new RetrieveRemoteStateTask().execute();
    }

    private void setupViews() {
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new UdpateRemoteStateTask().execute();
            }
        });
    }

    private class RetrieveRemoteStateTask extends AsyncTask<Void, String, ByteBuffer> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            toggleButton.setVisibility(View.GONE);
        }

        @Override
        protected ByteBuffer doInBackground(Void... Void) {

            ByteBuffer byteBuffer = null;

            try {
                if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                    publishProgress(getString(R.string.opening_bluetooth_socket));
                    bluetoothSocket = createConnectionToBluetoothDevice(bluetoothAdapter, selectedBluetoothDevice);
                }

                // Tell Arduino to send us StateReport
                OutputStream outputStream = bluetoothSocket.getOutputStream();
                outputStream.write(new byte[] {0x40, 0x40, 0x40}); // '@@@'

                // For now, assume one byte state response...
                final byte[] remoteStateBytes = new byte[1];
                final int bytesRead = bluetoothSocket.getInputStream().read(remoteStateBytes);

                if (bytesRead != 1) {
                    publishProgress(getString(R.string.transmission_failure));
                    return null;
                }

                publishProgress(getString(R.string.communication_established));

                byteBuffer = ByteBuffer.allocate(1);
                byteBuffer.put(remoteStateBytes);
                byteBuffer.flip();  // prepare for reading.

            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                publishProgress(getString(R.string.transmission_failure));
                Log.d(TAG, "Socket connect exception!", connectException);
                try { bluetoothSocket.close(); } catch (IOException closeException) {}
            }

            return byteBuffer;
        }

        protected void onProgressUpdate(String... progress) {
            textView.setText(progress[0]);
        }

        @Override
        protected void onPostExecute(ByteBuffer byteBuffer) {
            super.onPostExecute(byteBuffer);

            renderLocalViewsFromRemoteState(byteBuffer);
        }
    }

    /**
     * This renders local views from remote state provided.
     */
    private void renderLocalViewsFromRemoteState(ByteBuffer byteBuffer) {

        // For now, assume single byte state

        int state = Integer.valueOf(String.valueOf(new char[] {(char)byteBuffer.get()}));
        if (state > 0) {
            Log.d(TAG, "State is ON");
            toggleButton.setChecked(true);
        } else {
            Log.d(TAG, "State is OFF");
            toggleButton.setChecked(false);
        }

        toggleButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        textView.setText(getString(R.string.main_controller));
    }

    private class UdpateRemoteStateTask extends AsyncTask<Void, String, Void> {

        @Override
        protected void onPreExecute() {
            Toast.makeText(MainControllerActivity.this, getString(R.string.transmitting_data), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Void... Void) {

            try {
                if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                    publishProgress(getString(R.string.opening_bluetooth_socket));
                    bluetoothSocket = createConnectionToBluetoothDevice(bluetoothAdapter, selectedBluetoothDevice);
                }

                ByteBuffer desiredState = renderRemoteStateFromLocalViews();

                OutputStream outputStream = bluetoothSocket.getOutputStream();
                outputStream.write(new byte[] {0x58, 0x58, 0x58}); // 'XXX'
                outputStream.write(desiredState.array());

                publishProgress(getString(R.string.transmission_success));

            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                publishProgress(getString(R.string.transmission_failure));
                Log.d(TAG, "Socket connect exception!", connectException);
                try { bluetoothSocket.close(); } catch (IOException closeException) {}
            }

            return null;
        }

        protected void onProgressUpdate(String... progress) {
            Toast.makeText(MainControllerActivity.this, progress[0], Toast.LENGTH_SHORT).show();
        }
    }

    private ByteBuffer renderRemoteStateFromLocalViews() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(1);

        if (toggleButton.isChecked()) {
            byteBuffer.put((byte)'1'); // a char is the ascii representation of '1'
        } else {
            byteBuffer.put((byte)'0');
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
            try { bluetoothSocket.close(); } catch (IOException closeException) {}
            bluetoothSocket = null;
        }

        return bluetoothSocket;
    }
}
