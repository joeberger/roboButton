package com.ndipatri.roboButton.dagger.modules;

import android.content.Context;

import com.ndipatri.roboButton.activities.MainControllerActivityTest;
import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProviderTestImpl;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProviderTestImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProviderTestImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.services.ButtonCommunicatorTest;
import com.ndipatri.roboButton.services.MonitoringServiceTest;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        includes  = {ABModule.class
        },
        injects = {
                MainControllerActivityTest.class,
                MonitoringServiceTest.class,
                ButtonCommunicatorTest.class,
        },
        overrides = true
)
public class TestABModule {

    private Context context = null;

    public TestABModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    ButtonProvider provideButtonProvider() {
        return new ButtonProvider(context);
    }

    @Provides
    @Singleton
    RegionProvider provideRegionProvider() {
        return new RegionProvider(context);
    }

    @Provides
    @Singleton
    BluetoothProvider provideBluetoothProvider() {
        return new BluetoothProviderTestImpl(context);
    }

    @Provides
    @Singleton
    @Named(ABModule.ESTIMOTE_BEACONS)
    RegionDiscoveryProvider provideEstimoteBeaconDiscoveryProvider() {
        return new RegionDiscoveryProviderTestImpl(context);
    }

    @Provides
    @Singleton
    @Named(ABModule.GELO_BEACONS)
    RegionDiscoveryProvider provideGeloBeaconDiscoveryProvider() {
        return new RegionDiscoveryProviderTestImpl(context);
    }

    @Provides
    @Singleton
    ButtonDiscoveryProvider provideButtonDiscoveryProvider() {
        return new ButtonDiscoveryProviderTestImpl(context);
    }
}
