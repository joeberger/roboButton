package com.ndipatri.roboButton.dagger;

import android.content.Context;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.activities.MainControllerActivity;
import com.ndipatri.roboButton.activities.MainControllerActivityInstrumentation;
import com.ndipatri.roboButton.dagger.providers.BluetoothProviderImpl;
import com.ndipatri.roboButton.dagger.providers.LightBlueButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.PurpleButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.dagger.providers.EstimoteRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.GenericRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.LightBlueRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.fragments.ButtonDetailsDialogFragment;
import com.ndipatri.roboButton.fragments.ButtonFragment;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.utils.ButtonCommunicator;
import com.ndipatri.roboButton.utils.LeDeviceListAdapter;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {RBModule.class})
public interface ObjectGraph {

    void inject(LightBlueRegionDiscoveryProviderImpl thingy);

    void inject(GenericRegionDiscoveryProviderImpl thingy);

    void inject(EstimoteRegionDiscoveryProviderImpl thingy);

    void inject(RBApplication thingy);

    void inject(ButtonFragment thingy);

    void inject(ButtonDetailsDialogFragment thingy);

    void inject(LeDeviceListAdapter thingy);

    void inject(MonitoringService thingy);

    void inject(ButtonCommunicator thingy);

    void inject(MainControllerActivity thingy);

    void inject(BluetoothProviderImpl thingy);

    void inject(ButtonProvider thingy);

    void inject(PurpleButtonDiscoveryProviderImpl thingy);

    void inject(MainControllerActivityInstrumentation thingy);

    void inject(LightBlueButtonDiscoveryProviderImpl thingy);

    public final static class Initializer {
        public static ObjectGraph init(Context context) {
            return DaggerObjectGraph.builder().rBModule(new RBModule(context)).build();
        }
    }
}
