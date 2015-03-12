package com.ndipatri.roboButton.dagger.modules;

import android.content.Context;

import com.ndipatri.roboButton.activities.MainControllerActivity;
import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.providers.EstimoteRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.GeloRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;
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
                ButtonProvider.class,
                ButtonDiscoveryProviderImpl.class,
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
    RegionProvider provideRegionProvider() {
        return new RegionProvider(context);
    }

    @Provides
    @Singleton
    BluetoothProvider provideBluetoothProvider() {
        return new BluetoothProviderImpl(context);
    }

    @Provides
    @Singleton
    @Named(ESTIMOTE_BEACONS)
    RegionDiscoveryProvider provideEstimoteBeaconDiscoveryProvider() {
        return new EstimoteRegionDiscoveryProviderImpl(context);
    }

    @Provides
    @Singleton
    @Named(GELO_BEACONS)
    RegionDiscoveryProvider provideGeloBeaconDiscoveryProvider() {
        return new GeloRegionDiscoveryProviderImpl(context);
    }

    @Provides
    @Singleton
    ButtonDiscoveryProvider provideButtonDiscoveryProvider() {
        return new ButtonDiscoveryProviderImpl(context);
    }

}
