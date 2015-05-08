package com.ndipatri.roboButton.activities;

import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;

import javax.inject.Inject;

public abstract class MainControllerActivityInstrumentationTest extends InjectableActivityInstrumentationTest<MainControllerActivity> {

    @Inject protected BluetoothProvider bluetoothProvider;

    public MainControllerActivityInstrumentationTest() {
        super(MainControllerActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();

        // By creating the graph here BEFORE it's used by the activity, we can inject our mock collaborator.
        targetApplication.getGraph().inject(this);

        getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        targetApplication.clearGraph();
    }
}
