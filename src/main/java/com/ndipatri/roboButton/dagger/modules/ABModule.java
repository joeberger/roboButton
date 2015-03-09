package com.ndipatri.roboButton.dagger.modules;

import android.content.Context;

import com.ndipatri.roboButton.activities.MainControllerActivity;
import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.providers.BeaconDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.EstimoteBeaconDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.BeaconProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProviderImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.fragments.ABFragment;
import com.ndipatri.roboButton.fragments.ButtonDetailsDialogFragment;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.services.ButtonMonitor;
import com.ndipatri.roboButton.utils.LeDeviceListAdapter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {
                ABFragment.class,
                ButtonDetailsDialogFragment.class,
                LeDeviceListAdapter.class,
                MonitoringService.class,
                ButtonMonitor.class,
                MainControllerActivity.class,
                BluetoothProviderImpl.class,
                ButtonProvider.class
        }
)
public class ABModule {

    public static final String ESTIMOTE_BEACONS = "ESTIMOTE_BEACONS";
    public static final String GELO_BEACONS = "GELO_BEACONS";
    private Context context = null;

    public ABModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    ButtonProvider provideButtonProvider() {
        return new ButtonProvider(context);
    }

    @Provides
    @Singleton
    BeaconProvider provideBeaconProvider() {
        return new BeaconProvider(context);
    }

    @Provides
    @Singleton
    BluetoothProvider provideBluetoothProvider() {
        return new BluetoothProviderImpl(context);
    }

    @Provides
    @Singleton
    @Named(ESTIMOTE_BEACONS)
    BeaconDiscoveryProvider provideEstimoteBeaconDiscoveryProvider() {
        return new EstimoteBeaconDiscoveryProviderImpl(context);
    }

    @Provides
    @Singleton
    @Named(GELO_BEACONS)
    BeaconDiscoveryProvider provideGeloBeaconDiscoveryProvider() {
        // NJD TODO - Need to implement Gelo Discovery Impl
        return new EstimoteBeaconDiscoveryProviderImpl(context);
    }

    @Provides
    @Singleton
    ButtonDiscoveryProvider provideButtonDiscoveryProvider() {
        return new ButtonDiscoveryProviderImpl(context);
    }

}
