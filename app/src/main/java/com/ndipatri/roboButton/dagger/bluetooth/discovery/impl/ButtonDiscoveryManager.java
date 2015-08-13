package com.ndipatri.roboButton.dagger.bluetooth.discovery.impl;

import android.content.Context;
import android.util.Log;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.RBModule;
import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.events.ButtonDiscoveryFinished;
import com.ndipatri.roboButton.utils.BusProvider;

import javax.inject.Inject;

/**
 * This provider will alternately scan for Blue (BLE) and Purple (BT Classic)
 * Buttons.  Due to Android specs, these scans cannot occur simultaneously, but
 * the Blue will be given priority here due to its prevalence.
 */
public class ButtonDiscoveryManager {

    private static final String TAG = ButtonDiscoveryManager.class.getCanonicalName();

    private Context context;

    @Inject
    BusProvider bus;

    @Inject
    @Named(RBModule.LIGHTBLUE_BUTTON)
    protected ButtonDiscoveryProvider lightBlueButtonDiscoveryProvider;

    @Inject
    @Named(RBModule.PURPLE_BUTTON)
    protected ButtonDiscoveryProvider purpleButtonDiscoveryProvider;

    public ButtonDiscoveryManager(Context context) {
        this.context = context;
        RBApplication.getInstance().getGraph().inject(this);
    }

    public synchronized void startButtonDiscovery() {

        Log.d(TAG, "Beginning Composite Button Discovery Process...");

        lightBlueButtonDiscoveryProvider.startButtonDiscovery(new ButtonDiscoveryListener() {
            @Override
            public void buttonDiscoveryFinished() {
                purpleButtonDiscoveryProvider.startButtonDiscovery(new ButtonDiscoveryListener() {
                    @Override
                    public void buttonDiscoveryFinished() {
                        bus.post(new ButtonDiscoveryFinished());
                    }
                });
            }
        });
    }

    public synchronized void stopButtonDiscovery() {
        Log.d(TAG, "Stopping Composite Button Discovery...");

        lightBlueButtonDiscoveryProvider.stopButtonDiscovery();
        purpleButtonDiscoveryProvider.stopButtonDiscovery();
    }
}
