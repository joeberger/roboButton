package com.ndipatri.roboButton.dagger.modules;

import android.content.Context;

import com.ndipatri.roboButton.dagger.providers.BeaconProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProviderTestImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.services.MonitoringServiceTest;
import com.ndipatri.roboButton.services.ButtonMonitorTest;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        includes  = {ABModule.class
        },
        injects = {
                MonitoringServiceTest.class,
                ButtonMonitorTest.class,
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
    BluetoothProvider provideBluetoothProvider() {
        return new BluetoothProviderTestImpl(context);
    }

    @Provides
    @Singleton
    BeaconProvider provideBeaconProvider() {
        return new BeaconProvider(context);
    }
}
