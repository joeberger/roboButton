package com.ndipatri.arduinoButton.activity;


Next step.. move the query/set handler stuff from maincontrolleractiivty into here... duplicate it so the mainController still uses a handler for
        discover only...



import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.events.ArduinoButtonBluetoothDisabledEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonInformationEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonStateChangeEvent;
import com.ndipatri.arduinoButton.utils.BusProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import butterknife.InjectView;
import butterknife.Views;

/**
 * Created by ndipatri on 12/31/13.
 */
public class ArduinoButtonFragment extends Fragment {

    private static final String TAG = ArduinoButtonFragment.class.getCanonicalName();

    private static final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private boolean enabled = true;
    private BluetoothSocket socket = null;
    private boolean state = false;

    public static ArduinoButtonFragment newInstance(String description, String buttonId, BluetoothDevice device) {

        ArduinoButtonFragment arduinoButtonFragment = new ArduinoButtonFragment();
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

        imageView.setImageResource(R.drawable.yellow_button);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Button Pressed!");

                if (enabled) {
                    enabled = false;
                    imageView.setImageResource(R.drawable.yellow_button);
                    state = !state;

                    BusProvider.getInstance().post(new ArduinoButtonStateChangeEvent(getButtonId()));
                }
            }
        });

        return rootView;
    }

    public void setState(final boolean state) {

        this.enabled = true;
        this.state = state;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state) {
                    Log.d(TAG, "State is ON");
                    imageView.setImageResource(R.drawable.green_button);
                } else {
                    Log.d(TAG, "State is OFF");
                    imageView.setImageResource(R.drawable.red_button);
                }
            }
        });
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
            Log.d(TAG, "Shutting down Bluetooth Socket for Button('" + getDescription() + "').");
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    public boolean isConnected() {
        return (socket != null) && socket.isConnected();
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
                setState(Integer.valueOf(responseChar) > 0);
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

                ByteBuffer desiredState = getState();

                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(new byte[]{0x58, 0x58, 0x58}); // 'XXX' - StateChangeRequest
                outputStream.write(desiredState.array());
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
