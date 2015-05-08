package com.ndipatri.roboButton.dagger;

import com.ndipatri.roboButton.activities.MainControllerActivity;
import com.ndipatri.roboButton.activities.MainControllerActivityInstrumentationTest;
import com.ndipatri.roboButton.activities.MainControllerActivityInstrumentationTest_MembersInjector;
import com.ndipatri.roboButton.activities.MainControllerActivity_MembersInjector;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProviderImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider_MembersInjector;
import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import com.ndipatri.roboButton.fragments.ButtonDetailsDialogFragment;
import com.ndipatri.roboButton.fragments.ButtonDetailsDialogFragment_MembersInjector;
import com.ndipatri.roboButton.fragments.ButtonFragment;
import com.ndipatri.roboButton.fragments.ButtonFragment_MembersInjector;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.services.MonitoringService_MembersInjector;
import com.ndipatri.roboButton.utils.ButtonCommunicator;
import com.ndipatri.roboButton.utils.ButtonCommunicator_MembersInjector;
import com.ndipatri.roboButton.utils.LeDeviceListAdapter;
import com.ndipatri.roboButton.utils.LeDeviceListAdapter_MembersInjector;
import dagger.MembersInjector;
import dagger.internal.MembersInjectors;
import dagger.internal.ScopedProvider;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class DaggerObjectGraph implements ObjectGraph {
  private Provider<ButtonProvider> provideButtonProvider;
  private MembersInjector<ButtonFragment> buttonFragmentMembersInjector;
  private Provider<RegionProvider> provideRegionProvider;
  private MembersInjector<ButtonDetailsDialogFragment> buttonDetailsDialogFragmentMembersInjector;
  private MembersInjector<LeDeviceListAdapter> leDeviceListAdapterMembersInjector;
  private Provider<RegionDiscoveryProvider> provideEstimoteBeaconDiscoveryProvider;
  private Provider<RegionDiscoveryProvider> provideGeloBeaconDiscoveryProvider;
  private Provider<ButtonDiscoveryProvider> provideButtonDiscoveryProvider;
  private Provider<BluetoothProvider> provideBluetoothProvider;
  private MembersInjector<MonitoringService> monitoringServiceMembersInjector;
  private MembersInjector<ButtonCommunicator> buttonCommunicatorMembersInjector;
  private MembersInjector<MainControllerActivity> mainControllerActivityMembersInjector;
  private MembersInjector<ButtonProvider> buttonProviderMembersInjector;
  private MembersInjector<MainControllerActivityInstrumentationTest> mainControllerActivityInstrumentationTestMembersInjector;

  private DaggerObjectGraph(Builder builder) {  
    assert builder != null;
    initialize(builder);
  }

  public static Builder builder() {  
    return new Builder();
  }

  private void initialize(final Builder builder) {  
    this.provideButtonProvider = ScopedProvider.create(RBModule_ProvideButtonProviderFactory.create(builder.rBModule));
    this.buttonFragmentMembersInjector = ButtonFragment_MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideButtonProvider);
    this.provideRegionProvider = ScopedProvider.create(RBModule_ProvideRegionProviderFactory.create(builder.rBModule));
    this.buttonDetailsDialogFragmentMembersInjector = ButtonDetailsDialogFragment_MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideButtonProvider, provideRegionProvider);
    this.leDeviceListAdapterMembersInjector = LeDeviceListAdapter_MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideRegionProvider);
    this.provideEstimoteBeaconDiscoveryProvider = ScopedProvider.create(RBModule_ProvideEstimoteBeaconDiscoveryProviderFactory.create(builder.rBModule));
    this.provideGeloBeaconDiscoveryProvider = ScopedProvider.create(RBModule_ProvideGeloBeaconDiscoveryProviderFactory.create(builder.rBModule));
    this.provideButtonDiscoveryProvider = ScopedProvider.create(RBModule_ProvideButtonDiscoveryProviderFactory.create(builder.rBModule));
    this.provideBluetoothProvider = ScopedProvider.create(RBModule_ProvideBluetoothProviderFactory.create(builder.rBModule));
    this.monitoringServiceMembersInjector = MonitoringService_MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideRegionProvider, provideEstimoteBeaconDiscoveryProvider, provideGeloBeaconDiscoveryProvider, provideButtonProvider, provideButtonDiscoveryProvider, provideBluetoothProvider);
    this.buttonCommunicatorMembersInjector = ButtonCommunicator_MembersInjector.create(provideButtonDiscoveryProvider);
    this.mainControllerActivityMembersInjector = MainControllerActivity_MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideBluetoothProvider);
    this.buttonProviderMembersInjector = ButtonProvider_MembersInjector.create(provideRegionProvider);
    this.mainControllerActivityInstrumentationTestMembersInjector = MainControllerActivityInstrumentationTest_MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideBluetoothProvider);
  }

  @Override
  public void inject(ButtonFragment thingy) {  
    buttonFragmentMembersInjector.injectMembers(thingy);
  }

  @Override
  public void inject(ButtonDetailsDialogFragment thingy) {  
    buttonDetailsDialogFragmentMembersInjector.injectMembers(thingy);
  }

  @Override
  public void inject(LeDeviceListAdapter thingy) {  
    leDeviceListAdapterMembersInjector.injectMembers(thingy);
  }

  @Override
  public void inject(MonitoringService thingy) {  
    monitoringServiceMembersInjector.injectMembers(thingy);
  }

  @Override
  public void inject(ButtonCommunicator thingy) {  
    buttonCommunicatorMembersInjector.injectMembers(thingy);
  }

  @Override
  public void inject(MainControllerActivity thingy) {  
    mainControllerActivityMembersInjector.injectMembers(thingy);
  }

  @Override
  public void inject(BluetoothProviderImpl thingy) {  
    MembersInjectors.noOp().injectMembers(thingy);
  }

  @Override
  public void inject(ButtonProvider thingy) {  
    buttonProviderMembersInjector.injectMembers(thingy);
  }

  @Override
  public void inject(ButtonDiscoveryProviderImpl thingy) {  
    MembersInjectors.noOp().injectMembers(thingy);
  }

  @Override
  public void inject(MainControllerActivityInstrumentationTest thingy) {  
    mainControllerActivityInstrumentationTestMembersInjector.injectMembers(thingy);
  }

  public static final class Builder {
    private RBModule rBModule;
  
    private Builder() {  
    }
  
    public ObjectGraph build() {  
      if (rBModule == null) {
        throw new IllegalStateException("rBModule must be set");
      }
      return new DaggerObjectGraph(this);
    }
  
    public Builder rBModule(RBModule rBModule) {  
      if (rBModule == null) {
        throw new NullPointerException("rBModule");
      }
      this.rBModule = rBModule;
      return this;
    }
  }
}

