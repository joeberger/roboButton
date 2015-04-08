package com.ndipatri.roboButton;

import android.content.Context;

import com.ndipatri.roboButton.activities.MainControllerActivity;
import com.ndipatri.roboButton.fragments.ButtonDetailsDialogFragment;
import com.ndipatri.roboButton.fragments.ButtonFragment;
import com.ndipatri.roboButton.dagger.providers.*;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.services.MonitoringServiceTest;
import com.ndipatri.roboButton.utils.ButtonCommunicator;
import com.ndipatri.roboButton.utils.ButtonCommunicatorTest;
import com.ndipatri.roboButton.utils.LeDeviceListAdapter;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {TestRBModule.class})
public interface Graph {

    void inject(MonitoringServiceTest thingy);
    void inject(LeDeviceListAdapter thingy);
    void inject(ButtonFragment thingy);
    void inject(ButtonDetailsDialogFragment thingy);
    void inject(MonitoringService thingy);
    void inject(ButtonCommunicator thingy);
    void inject(ButtonCommunicatorTest thingy);
    void inject(MainControllerActivity thingy);
    void inject(BluetoothProviderImpl thingy);
    void inject(ButtonProvider thingy);
    void inject(ButtonDiscoveryProviderImpl thingy);

    public final static class Initializer {
        public static Graph init(Context context, boolean providesMocks) {
            return Dagger_Graph.builder()
                    .testRBModule(new TestRBModule(context, providesMocks))
                    .build();
        }
    }
}
