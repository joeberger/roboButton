package com.ndipatri.roboButton.dagger;

import javax.inject.Singleton;

import dagger.Component;

import com.ndipatri.roboButton.fragments.*;
import com.ndipatri.roboButton.services.*;
import com.ndipatri.roboButton.utils.*;
import com.ndipatri.roboButton.activities.*;
import com.ndipatri.roboButton.providers.*;

@Singleton
@Component(modules = {RBModule.class})
public interface Graph {

    void inject(MonitoringService thingy);
    void inject(LeDeviceListAdapter thingy);
    void inject(ButtonFragment thingy);
    void inject(ButtonDetailsDialogFragment thingy,
    void inject(ButtonDetailsDialogFragment thingy,
    void inject(MonitoringService thingy,
    void inject(ButtonCommunicator thingy,
    void inject(MainControllerActivity thingy,
    void inject(BluetoothProviderImpl thingy,
    void inject(ButtonProvider thingy,
    void inject(ButtonDiscoveryProviderImpl thingy,

    public final static class Initializer {
        public static Graph init(Context context) {
            return Dagger_Graph.builder()
                    .debugDataModule(new RBModule(context))
                    .build();
        }
    }
}
