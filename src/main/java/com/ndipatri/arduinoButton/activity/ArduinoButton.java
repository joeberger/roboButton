package com.ndipatri.arduinoButton.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by ndipatri on 12/31/13.
 */
public class ArduinoButton {

    private static final String TAG = ArduinoButton.class.getCanonicalName();

    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private boolean enabled = true;
    private ImageView imageView = null;
    private ViewGroup rootViewGroup = null;
    private BluetoothDevice device = null;
    private BluetoothSocket socket = null;
    private boolean state = false;
    private String description = null;
    private String buttonId;

    public ArduinoButton(String description, String buttonId, BluetoothDevice device) {
        this.device = device;
        this.description = description;
        this.buttonId = buttonId;

        rootViewGroup = (ViewGroup) LayoutInflater.from(MainControllerActivity.this).inflate(R.layout.button_layout, null);
        imageView = (ImageView) rootViewGroup.findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.yellow_button);

        rootViewGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Pressed!");

                if (enabled) {
                    enabled = false;
                    imageView.setImageResource(R.drawable.yellow_button);
                    state = !state;
                    bluetoothMessageHandler.queueSetStateRequest();
                }
            }
        });
    }

    public void setState(boolean state) {

        this.enabled = true;
        this.state = state;

        if (state) {
            Log.d(TAG, "State is ON");
            imageView.setImageResource(R.drawable.green_button);
        } else {
            Log.d(TAG, "State is OFF");
            imageView.setImageResource(R.drawable.red_button);
        }
    }

    public ByteBuffer getState() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(1);

        if (state) {
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
            Log.d(TAG, "Shutting down Bluetooth Socket for Button('" + description + "').");
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
                socket = createConnectionToBluetoothDevice(BluetoothAdapter.getDefaultAdapter(), device);
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
                state = Integer.valueOf(responseChar) > 0;
            } catch (NumberFormatException nex) {
                Log.d(TAG, "Invalid response from bluetooth device: '" + this + "'.");
                disconnect();
            }
        }
    }

    public void setRemoteState() {

        try {
            if (socket == null || !socket.isConnected()) {
                publishProgress(getString(R.string.opening_bluetooth_socket));
                socket = createConnectionToBluetoothDevice(BluetoothAdapter.getDefaultAdapter(), device);
            }

            if (socket != null) {

                ByteBuffer desiredState = getState();

                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(new byte[]{0x58, 0x58, 0x58}); // 'XXX' - StateChangeRequest
                outputStream.write(desiredState.array());
            }
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            publishProgress(getString(R.string.transmission_failure));
            Log.d(TAG, "Socket connect exception!", connectException);
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    public ViewGroup getRootViewGroup() {
        return rootViewGroup;
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
                onResume();
            }

            bluetoothSocket = null;
        }

        return bluetoothSocket;
    }
}
