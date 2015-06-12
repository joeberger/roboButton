package com.ndipatri.roboButton.activities;

import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.BluetoothProvider;
import com.ndipatri.roboButton.utils.BusProvider;

import javax.inject.Inject;

import static org.mockito.Mockito.when;

public abstract class MainControllerActivityInstrumentation extends InjectableActivityInstrumentation<MainControllerActivity> {

    @Inject protected BluetoothProvider bluetoothProvider;

    @Inject protected BusProvider bus;

    public MainControllerActivityInstrumentation() {
        super(MainControllerActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();

        // By creating the graph here BEFORE it's used by the activity, we can inject our mock collaborator.
        targetApplication.getGraph().inject(this);

        when(bluetoothProvider.isBluetoothEnabled()).thenReturn(true);
        when(bluetoothProvider.isBluetoothSupported()).thenReturn(true);

    }

    @Override
    protected void tearDown() throws Exception {
        targetApplication.clearGraph();
    }
}
