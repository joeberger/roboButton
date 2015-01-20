package com.ndipatri.arduinoButton.dagger.modules;

import android.content.Context;

import com.ndipatri.arduinoButton.dagger.providers.BeaconProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProviderTestImpl;
import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;
import com.ndipatri.arduinoButton.services.MonitoringServiceTest;
import com.ndipatri.arduinoButton.services.ButtonMonitorTest;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        includes  = {ABModule.class
        },
        injects = {
                MonitoringServiceTest.class,
                ButtonMonitorTest.class,
        }
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
