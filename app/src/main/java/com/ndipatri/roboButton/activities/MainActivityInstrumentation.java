package com.ndipatri.roboButton.activities;

import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.BluetoothProvider;
import com.ndipatri.roboButton.dagger.daos.ButtonDao;
import com.ndipatri.roboButton.utils.BusProvider;

import javax.inject.Inject;

import static org.mockito.Mockito.when;

public abstract class MainActivityInstrumentation extends InjectableActivityInstrumentation<MainActivity> {

    @Inject protected BluetoothProvider bluetoothProvider;

    @Inject protected BusProvider bus;

    @Inject protected ButtonDao buttonDao;

    public MainActivityInstrumentation() {
        super(MainActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();

        targetApplication.getGraph().inject(this);

        when(bluetoothProvider.isBluetoothEnabled()).thenReturn(true);
        when(bluetoothProvider.isBluetoothSupported()).thenReturn(true);

    }

    @Override
    protected void tearDown() throws Exception {
        targetApplication.clearGraph();
    }
}
