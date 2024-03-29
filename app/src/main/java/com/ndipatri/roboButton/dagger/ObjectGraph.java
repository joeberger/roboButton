package com.ndipatri.roboButton.dagger;

import android.content.Context;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.activities.MainActivity;
import com.ndipatri.roboButton.activities.MainActivityInstrumentation;
import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.PurpleButtonCommunicatorImpl;
import com.ndipatri.roboButton.dagger.bluetooth.communication.stubs.GenericButtonCommunicatorStub;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.ButtonDiscoveryManager;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.daos.ButtonDao;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.BluetoothProviderImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.GenericRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.LightBlueButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.EstimoteRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.PurpleButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.stubs.GenericRegionDiscoveryProviderStub;
import com.ndipatri.roboButton.fragments.ButtonDetailsDialogFragment;
import com.ndipatri.roboButton.fragments.ButtonFragment;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.ButtonCommunicator;
import com.ndipatri.roboButton.utils.LeDeviceListAdapter;
import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.LightBlueButtonCommunicatorImpl;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {RBModule.class})
public interface ObjectGraph {

    void inject(GenericRegionDiscoveryProviderImpl thingy);
    void inject(GenericRegionDiscoveryProviderStub thingy);

    void inject(PurpleButtonCommunicatorImpl thingy);

    void inject(EstimoteRegionDiscoveryProviderImpl thingy);

    void inject(RBApplication thingy);

    void inject(ButtonFragment thingy);

    void inject(ButtonDetailsDialogFragment thingy);

    void inject(LeDeviceListAdapter thingy);

    void inject(MonitoringService thingy);

    void inject(ButtonCommunicator thingy);

    void inject(MainActivity thingy);

    void inject(BluetoothProviderImpl thingy);

    void inject(ButtonDao thingy);

    void inject(PurpleButtonDiscoveryProviderImpl thingy);

    void inject(MainActivityInstrumentation thingy);

    void inject(ButtonDiscoveryProvider thingy);

    void inject(LightBlueButtonDiscoveryProviderImpl thingy);

    void inject(ButtonDiscoveryManager thingy);

    void inject(LightBlueButtonCommunicatorImpl thingy);
    void inject(GenericButtonCommunicatorStub thingy);

    public final static class Initializer {
        public static ObjectGraph init(Context context) {
            return DaggerObjectGraph.builder().rBModule(new RBModule(context)).build();
        }
    }
}
