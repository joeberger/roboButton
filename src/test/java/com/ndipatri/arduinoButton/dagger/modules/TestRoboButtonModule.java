package com.ndipatri.arduinoButton.dagger.modules;

import android.content.Context;

import com.ndipatri.arduinoButton.activities.MainControllerActivity;
import com.ndipatri.arduinoButton.dagger.providers.BeaconProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProviderTestImpl;
import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;
import com.ndipatri.arduinoButton.fragments.BeaconDetailsDialogFragment;
import com.ndipatri.arduinoButton.fragments.ButtonDetailsDialogFragment;
import com.ndipatri.arduinoButton.services.BluetoothMonitoringService;
import com.ndipatri.arduinoButton.services.BluetoothMonitoringServiceTest;
import com.ndipatri.arduinoButton.services.ButtonMonitor;
import com.ndipatri.arduinoButton.services.ButtonMonitorTest;
import com.ndipatri.arduinoButton.utils.LeDeviceListAdapter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {
                BluetoothMonitoringServiceTest.class,
                ButtonMonitorTest.class,
                ButtonDetailsDialogFragment.class,
                BeaconDetailsDialogFragment.class,
                LeDeviceListAdapter.class,
                BluetoothMonitoringService.class,
                ButtonMonitor.class,
                MainControllerActivity.class
        }
)
public class TestRoboButtonModule {

    private Context context = null;

    public TestRoboButtonModule(Context context) {
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
