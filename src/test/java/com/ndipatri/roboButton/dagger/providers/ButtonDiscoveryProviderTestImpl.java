package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;

import com.ndipatri.roboButton.models.Button;

import java.util.HashSet;
import java.util.Set;

public class ButtonDiscoveryProviderTestImpl implements ButtonDiscoveryProvider {

    private static final String TAG = BluetoothProviderImpl.class.getCanonicalName();

    private Context context;

    private Set<Button> discoveredButtons = new HashSet<Button>();

    private boolean isDiscoveryCancelled = false;

    public ButtonDiscoveryProviderTestImpl(Context context) {
        this.context = context;
    }

    @Override
    public void startButtonDiscovery() {

    }

    @Override
    public void stopButtonDiscovery() {
        this.isDiscoveryCancelled = true;
    }

    public boolean isDiscoveryCancelled() {
        return isDiscoveryCancelled;
    }

    public void setDiscoveredButtons(Set<Button> discoveredButtons) {
        this.discoveredButtons = discoveredButtons;
    }
}
