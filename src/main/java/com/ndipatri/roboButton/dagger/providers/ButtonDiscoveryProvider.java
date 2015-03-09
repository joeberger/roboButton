package com.ndipatri.roboButton.dagger.providers;

import com.ndipatri.roboButton.ButtonDiscoveryListener;

public interface ButtonDiscoveryProvider {
    public void startButtonDiscovery(ButtonDiscoveryListener listener);
    public void stopButtonDiscovery();
}
