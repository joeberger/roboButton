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
import java.util.UUID;

import butterknife.InjectView;
import butterknife.Views;

public class MainControllerActivity extends Activity {

    public static final String EXTERNAL_BLUETOOTH_DEVICE_MAC = "macAddress";

    private static final String TAG = MainControllerActivity.class.getCanonicalName();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;

    protected @InjectView(R.id.textView) TextView textView;
    protected @InjectView(R.id.progressBar) android.widget.ProgressBar progressBar;
    protected @InjectView(R.id.toggleButton) ToggleButton toggleButton;
    protected @InjectView(R.id.toggleButton2) ToggleButton toggleButton2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

        Views.inject(this);

        // NJD TODO - Safe to assume since our prevous Activity hooked us up? We should have BroadcastReceiver monitoring this maybe?
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothDevice selectedBluetoothDevice = getIntent().getParcelableExtra(EXTERNAL_BLUETOOTH_DEVICE_MAC);
        Toast.makeText(this, "Bluetooth Device Selected: '" + selectedBluetoothDevice + "'.", Toast.LENGTH_SHORT).show();

        setupViews();

        // Forge socket with bluetooth device..
        new CreateClientSocketTask().execute(selectedBluetoothDevice);
    }

    private void setupViews() {
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "check changed: " + isChecked);

                // NJD TODO - send data on socket!
            }
        });
    }

    private class CreateClientSocketTask extends AsyncTask<BluetoothDevice, Void, BluetoothSocket> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            textView.setText(getString(R.string.opening_bluetooth_socket));
            toggleButton.setVisibility(View.GONE);
            toggleButton2.setVisibility(View.GONE);
        }

        @Override
        protected BluetoothSocket doInBackground(BluetoothDevice... params) {
            final BluetoothDevice bluetoothDevice = params[0];
            BluetoothSocket bluetoothSocket = null;

            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.nameUUIDFromBytes(new byte[] {(byte)1, (byte)1, (byte)0, (byte)1}));

                // Cancel discovery because it will slow down the connection
                bluetoothAdapter.cancelDiscovery();

                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                bluetoothSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try { bluetoothSocket.close(); } catch (IOException closeException) {}
                bluetoothSocket = null;
            }

            return bluetoothSocket;
        }

        @Override
        protected void onPostExecute(BluetoothSocket bluetoothSocket) {
            super.onPostExecute(bluetoothSocket);

            MainControllerActivity.this.bluetoothSocket = bluetoothSocket;

            progressBar.setVisibility(View.INVISIBLE);
            textView.setText(getString(R.string.main_controller));
            toggleButton.setVisibility(View.VISIBLE);
            toggleButton2.setVisibility(View.VISIBLE);
        }
    }
}
