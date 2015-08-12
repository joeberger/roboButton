package com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.ButtonDiscoveryListener;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.events.ButtonDiscoveryFinished;
import com.ndipatri.roboButton.utils.BusProvider;

import javax.inject.Inject;

public abstract class ButtonDiscoveryProvider {

    protected boolean discovering = false;

    protected BluetoothAdapter bluetoothAdapter = null;

    private ButtonDiscoveryListener listener = null;

    protected Context context;

    @Inject
    BusProvider bus;

    public ButtonDiscoveryProvider(Context context) {
        this.context = context;

        RBApplication.getInstance().getGraph().inject(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void startButtonDiscovery() {
        startButtonDiscovery(null);
    }

    public void startButtonDiscovery(ButtonDiscoveryListener listener) {

        this.listener = listener;

        if (discovering) {
            // make this request idempotent
            return;
        }
        this.discovering = true;

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            _startButtonDiscovery();
        } else {
            discovering = false;
        }
    }

    public void stopButtonDiscovery() {
        this.listener = null;
        this.discovering = false;

        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            _stopButtonDiscovery();
        }
    }

    protected void buttonDiscoveryFinished() {
        if (listener == null) {
            bus.post(new ButtonDiscoveryFinished());
        } else {
            listener.buttonDiscoveryFinished();
        }

        stopButtonDiscovery();
    }

    // Listener is optional.  It is expected that if a listener is NOT passed in, an
    // Otto even will be emitted instead by this Provider.
    protected abstract void _startButtonDiscovery();

    protected abstract void _stopButtonDiscovery();

    protected abstract ButtonType getButtonType();

    protected abstract void startButtonCommunicator(BluetoothDevice bluetoothDevice);
}
