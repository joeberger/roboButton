package com.ndipatri.roboButton.dagger;

import android.content.Context;

import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProviderImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.LightBlueButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.PurpleButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.dagger.providers.EstimoteRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.GenericRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.LightBlueRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import com.ndipatri.roboButton.BuildVariant;
import com.ndipatri.roboButton.utils.BusProvider;
import com.ndipatri.roboButton.utils.RegionUtils;
import com.squareup.otto.Bus;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.mockito.Mockito.mock;

@Module
public class RBModule {

    public static final String ESTIMOTE_BEACONS = "ESTIMOTE_BEACONS";
    public static final String GELO_BEACONS = "GELO_BEACONS";
    public static final String LIGHTBLUE_BUTTON = "LIGHTBLUE_BUTTON";
    public static final String PURPLE_BUTTON = "PURPLE_BUTTON";
    protected Context context = null;

    public RBModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    Bus provideBus() {
        return new BusProvider();
    }

    @Provides
    @Singleton
    ButtonProvider provideButtonProvider() {
        if (BuildVariant.useMocks) {
            return mock(ButtonProvider.class);
        } else {
            return new ButtonProvider(context);
        }
    }

    @Provides
    @Singleton
    RegionProvider provideRegionProvider() {
        if (BuildVariant.useMocks) {
            return mock(RegionProvider.class);
        } else {
            return new RegionProvider(context);
        }
    }

    @Provides
    @Singleton
    BluetoothProvider provideBluetoothProvider() {
        if (BuildVariant.useMocks) {
            return mock(BluetoothProvider.class);
        } else {
            return new BluetoothProviderImpl(context);
        }
    }

    @Provides
    @Singleton
    @Named(ESTIMOTE_BEACONS)
    RegionDiscoveryProvider provideEstimoteBeaconDiscoveryProvider() {
        if (BuildVariant.useMocks) {
            return mock(RegionDiscoveryProvider.class);
        } else {
            return new EstimoteRegionDiscoveryProviderImpl(context);
        }
    }

    @Provides
    @Singleton
    @Named(GELO_BEACONS)
    RegionDiscoveryProvider provideRegionDiscoveryProvider() {

        List regionScanList = Arrays.asList(
            new String[] {
                    RegionUtils.GELO_UUID,
                    RegionUtils.LIGHTBLUE_UUID});

        if (BuildVariant.useMocks) {
            return mock(RegionDiscoveryProvider.class);
        } else {
            return new GenericRegionDiscoveryProviderImpl(context, regionScanList);
        }
    }

    @Provides
    @Singleton
    @Named(LIGHTBLUE_BUTTON)
    ButtonDiscoveryProvider provideLightBlueButtonDiscoveryProvider() {
        if (BuildVariant.useMocks) {
            return mock(ButtonDiscoveryProvider.class);
        } else {
            return new LightBlueButtonDiscoveryProviderImpl(context);
        }
    }

    @Provides
    @Singleton
    @Named(PURPLE_BUTTON)
    ButtonDiscoveryProvider providePurpleButtonDiscoveryProvider() {
        if (BuildVariant.useMocks) {
            return mock(ButtonDiscoveryProvider.class);
        } else {
            return new PurpleButtonDiscoveryProviderImpl(context);
        }
    }
}
