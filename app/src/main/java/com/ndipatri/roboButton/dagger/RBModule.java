package com.ndipatri.roboButton.dagger;

import android.content.Context;

import com.ndipatri.roboButton.BuildVariant;
import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.BluetoothProviderImpl;
import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.LightBlueButtonCommunicatorFactoryImpl;
import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.PurpleButtonCommunicatorFactoryImpl;
import com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces.ButtonCommunicatorFactory;
import com.ndipatri.roboButton.dagger.bluetooth.communication.stubs.LightBlueButtonCommunicatorFactoryStub;
import com.ndipatri.roboButton.dagger.bluetooth.communication.stubs.PurpleButtonCommunicatorFactoryStub;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.GenericRegionDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.LightBlueButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.PurpleButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.stubs.BluetoothProviderStub;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.stubs.GenericRegionDiscoveryProviderStub;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.stubs.LightBlueButtonDiscoveryProviderStub;
import com.ndipatri.roboButton.dagger.daos.ButtonDao;
import com.ndipatri.roboButton.dagger.daos.RegionDao;
import com.ndipatri.roboButton.utils.BusProvider;
import com.ndipatri.roboButton.utils.RegionUtils;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.mockito.Mockito.mock;

@Module
public class RBModule {

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
    ButtonDao provideButtonProvider() {
        if (BuildVariant.useMocks) {
            return mock(ButtonDao.class);
        } else {
            return new ButtonDao(context);
        }
    }

    @Provides
    @Singleton
    RegionDao provideRegionProvider() {
        if (BuildVariant.useMocks) {
            return mock(RegionDao.class);
        } else {
            return new RegionDao(context);
        }
    }

    @Provides
    @Singleton
    com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.BluetoothProvider provideBluetoothProvider() {
        if (BuildVariant.useMocks) {
            return mock(com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.BluetoothProvider.class);
        } else
        if (BuildVariant.useStubs) {
            return new BluetoothProviderStub();
        } else {
            return new BluetoothProviderImpl();
        }
    }

    @Provides
    @Singleton
    RegionDiscoveryProvider provideRegionDiscoveryProvider() {

        // The UUIDs of the regions we care about.  These UUIDs are parsed
        // from the 'scanRecord' which is returned by a BLE scan.
        String[] regionUUIDPatternArray =
            new String[] {
                    RegionUtils.LIGHTBLUE_UUID,
                    RegionUtils.GELO_UUID,
                    };

        // This is the number of bytes into the BLE 'scanRecord' where the 16-byte
        // UUID begins.
        Integer[] regionUUIDOffsetArray = new Integer[] {new Integer(2),
                                                         new Integer(0),
                                                        };

        if (BuildVariant.useMocks) {
            return mock(RegionDiscoveryProvider.class);
        } else
        if (BuildVariant.useStubs) {
            return new GenericRegionDiscoveryProviderStub(context, regionUUIDPatternArray, regionUUIDOffsetArray);
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
                return new LightBlueButtonDiscoveryProviderStub(context);
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

    @Provides
    @Singleton
    @Named(LIGHTBLUE_BUTTON)
    ButtonCommunicatorFactory provideLightBlueButtonCommunicatorFactory() {
        if (BuildVariant.useMocks) {
            return mock(ButtonCommunicatorFactory.class);
        } else
        if (BuildVariant.useStubs) {
            return new LightBlueButtonCommunicatorFactoryStub();
        } else {
            return new LightBlueButtonCommunicatorFactoryImpl();
        }
    }

    @Provides
    @Singleton
    @Named(PURPLE_BUTTON)
    ButtonCommunicatorFactory providePurpleButtonCommunicatorFactory() {
        if (BuildVariant.useMocks) {
            return mock(ButtonCommunicatorFactory.class);
        } else
        if (BuildVariant.useStubs) {
            return new PurpleButtonCommunicatorFactoryStub();
        } else {
            return new PurpleButtonCommunicatorFactoryImpl();
        }
    }
}
