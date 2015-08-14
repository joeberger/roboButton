package com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.ButtonDiscoveryListener;
import com.ndipatri.roboButton.events.ButtonDiscoveryFinished;
import com.ndipatri.roboButton.utils.BusProvider;

import javax.inject.Inject;

public abstract class ButtonDiscoveryProvider {

    protected boolean discovering = false;

    @Inject
    protected BluetoothProvider bluetoothProvider;

    private ButtonDiscoveryListener listener = null;

    protected Context context;

    @Inject
    protected BusProvider bus;

    public ButtonDiscoveryProvider(Context context) {
        this.context = context;

        RBApplication.getInstance().getGraph().inject(this);
    }

    public void startButtonDiscovery(ButtonDiscoveryListener listener) {

        this.listener = listener;

        if (discovering) {
            // make this request idempotent
            return;
        }
        this.discovering = true;

        if (bluetoothProvider.isBluetoothSupported() && bluetoothProvider.isBluetoothEnabled()) {
            _startButtonDiscovery();
        } else {
            discovering = false;
        }
    }

    public void stopButtonDiscovery() {
        this.listener = null;
        this.discovering = false;

        if (bluetoothProvider.isBluetoothSupported() && bluetoothProvider.isBluetoothEnabled()) {
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


    protected abstract void _startButtonDiscovery();

    protected abstract void _stopButtonDiscovery();

    protected abstract void startButtonCommunicator(BluetoothDevice bluetoothDevice);
}
