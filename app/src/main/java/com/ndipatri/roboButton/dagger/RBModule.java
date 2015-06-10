package com.ndipatri.roboButton.dagger;

import android.content.Context;

import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.daos.ButtonDAO;
import com.ndipatri.roboButton.dagger.daos.RegionDAO;
import com.ndipatri.roboButton.dagger.providers.impl.BluetoothProviderImpl;
import com.ndipatri.roboButton.dagger.providers.impl.EstimoteRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.impl.GenericRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.impl.LightBlueButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.interfaces.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.interfaces.RegionDiscoveryProvider;
import com.ndipatri.roboButton.BuildVariant;
import com.ndipatri.roboButton.utils.BusProvider;
import com.ndipatri.roboButton.utils.RegionUtils;

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
    BusProvider provideBus() {
        return new BusProvider();
    }

    @Provides
    @Singleton
    ButtonDAO provideButtonProvider() {
        if (BuildVariant.useMocks) {
            return mock(ButtonDAO.class);
        } else {
            return new ButtonDAO(context);
        }
    }

    @Provides
    @Singleton
    RegionDAO provideRegionProvider() {
        if (BuildVariant.useMocks) {
            return mock(RegionDAO.class);
        } else {
            return new RegionDAO(context);
        }
    }

    @Provides
    @Singleton
    com.ndipatri.roboButton.dagger.providers.interfaces.BluetoothProvider provideBluetoothProvider() {
        if (BuildVariant.useMocks) {
            return mock(com.ndipatri.roboButton.dagger.providers.interfaces.BluetoothProvider.class);
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

        // The UUIDs of the regions we care about.  These UUIDs are parsed
        // from the 'scanRecord' which is returned by a BLE scan.
        String[] regionUUIDPatternArray =
            new String[] {
                    RegionUtils.LIGHTBLUE_UUID,
                    //RegionUtils.GELO_UUID,
                    //RegionUtils.ESTIMOTE_UUID
                    };

        // This is the number of bytes into the BLE 'scanRecord' where the 16-byte
        // UUID begins.
        Integer[] regionUUIDOffsetArray = new Integer[] {new Integer(3),
                                                         //new Integer(0),
                                                         //new Integer(0)
                                                        };

        if (BuildVariant.useMocks) {
            return mock(RegionDiscoveryProvider.class);
        } else {
            return new GenericRegionDiscoveryProviderImpl(context, regionUUIDPatternArray, regionUUIDOffsetArray);
        }
    }

    @Provides
    @Singleton
    @Named(LIGHTBLUE_BUTTON)
    ButtonDiscoveryProvider provideLightBlueButtonDiscoveryProvider() {
        if (BuildVariant.useMocks) {
            return mock(ButtonDiscoveryProvider.class);
        } else
        if (BuildVariant.useStubs) {
                return new LightBlueButtonDiscoveryProviderImpl(context);
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
            return new BluetoothProviderImpl.PurpleButtonDiscoveryProviderImpl(context);
        }
    }
}
