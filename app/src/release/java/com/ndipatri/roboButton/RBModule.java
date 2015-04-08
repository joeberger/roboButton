package com.ndipatri.roboButton.dagger.modules;

import android.content.Context;

import com.ndipatri.roboButton.activities.MainControllerActivity;
import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.providers.EstimoteRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.providers.GeloRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.providers.RegionDiscoveryProvider;
import com.ndipatri.roboButton.providers.RegionProvider;
import com.ndipatri.roboButton.providers.BluetoothProvider;
import com.ndipatri.roboButton.providers.BluetoothProviderImpl;
import com.ndipatri.roboButton.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.providers.ButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.providers.ButtonProvider;
import com.ndipatri.roboButton.fragments.ButtonFragment;
import com.ndipatri.roboButton.fragments.ButtonDetailsDialogFragment;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.utils.ButtonCommunicator;
import com.ndipatri.roboButton.utils.LeDeviceListAdapter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RBModule {

    private Context context = null;

    public RBModule(Context context, boolean providesMocks) {
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
    @Named("ESTIMOTE")
    RegionDiscoveryProvider provideEstimoteBeaconDiscoveryProvider() {
        return new EstimoteRegionDiscoveryProviderImpl(context);
    }

    @Provides
    @Singleton
    @Named("GELO")
    RegionDiscoveryProvider provideGeloBeaconDiscoveryProvider() {
        return new GeloRegionDiscoveryProviderImpl(context);
    }

    @Provides
    @Singleton
    ButtonDiscoveryProvider provideButtonDiscoveryProvider() {
        return new ButtonDiscoveryProviderImpl(context);
    }

}
