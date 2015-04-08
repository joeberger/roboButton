package com.ndipatri.roboButton;

import android.content.Context;

import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProviderImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.dagger.providers.EstimoteRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.GeloRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.mockito.Mockito.mock;

@Module
public class TestRBModule {

    private Context context = null;

    private boolean providesMocks = false;

    public TestRBModule(Context context, boolean providesMocks) {
        this.context = context;
        this.providesMocks = providesMocks;
    }

    @Provides
    @Singleton
    ButtonProvider provideButtonProvider() {
        if (providesMocks) {
            return mock(ButtonProvider.class);
        } else {
            return new ButtonProvider(context);
        }
    }

    @Provides
    @Singleton
    RegionProvider provideRegionProvider() {
        if (providesMocks) {
            return mock(RegionProvider.class);
        } else {
            return new RegionProvider(context);
        }
    }

    @Provides
    @Singleton
    BluetoothProvider provideBluetoothProvider() {
        if (providesMocks) {
            return mock(BluetoothProvider.class);
        } else {
            return new BluetoothProviderImpl(context);
        }
    }

    @Provides
    @Singleton
    @Named("ESTIMOTE")
    RegionDiscoveryProvider provideEstimoteBeaconDiscoveryProvider() {
        if (providesMocks) {
            return mock(RegionDiscoveryProvider.class);
        } else {
            return new EstimoteRegionDiscoveryProviderImpl(context);
        }
    }

    @Provides
    @Singleton
    @Named("GELO")
    RegionDiscoveryProvider provideGeloBeaconDiscoveryProvider() {
        if (providesMocks) {
            return mock(RegionDiscoveryProvider.class);
        } else {
            return new GeloRegionDiscoveryProviderImpl(context);
        }
    }

    @Provides
    @Singleton
    ButtonDiscoveryProvider provideButtonDiscoveryProvider() {
        if (providesMocks) {
            return mock(ButtonDiscoveryProvider.class);
        } else {
            return new ButtonDiscoveryProviderImpl(context);
        }
    }

}
