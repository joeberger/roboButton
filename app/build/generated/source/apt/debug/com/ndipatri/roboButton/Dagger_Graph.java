package com.ndipatri.roboButton;

import com.ndipatri.roboButton.activities.InjectableActivityInstrumentationTest;
import com.ndipatri.roboButton.activities.MainControllerActivity;
import com.ndipatri.roboButton.activities.MainControllerActivity$$MembersInjector;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProviderImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProviderImpl;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider$$MembersInjector;
import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import com.ndipatri.roboButton.fragments.ButtonDetailsDialogFragment;
import com.ndipatri.roboButton.fragments.ButtonDetailsDialogFragment$$MembersInjector;
import com.ndipatri.roboButton.fragments.ButtonFragment;
import com.ndipatri.roboButton.fragments.ButtonFragment$$MembersInjector;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.services.MonitoringService$$MembersInjector;
import com.ndipatri.roboButton.utils.ButtonCommunicator;
import com.ndipatri.roboButton.utils.ButtonCommunicator$$MembersInjector;
import com.ndipatri.roboButton.utils.LeDeviceListAdapter;
import com.ndipatri.roboButton.utils.LeDeviceListAdapter$$MembersInjector;
import dagger.MembersInjector;
import dagger.internal.MembersInjectors;
import dagger.internal.ScopedProvider;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class Dagger_Graph implements Graph {
  private Provider<RegionProvider> provideRegionProvider;
  private MembersInjector<LeDeviceListAdapter> leDeviceListAdapterMembersInjector;
  private Provider<ButtonProvider> provideButtonProvider;
  private MembersInjector<ButtonFragment> buttonFragmentMembersInjector;
  private MembersInjector<ButtonDetailsDialogFragment> buttonDetailsDialogFragmentMembersInjector;
  private Provider<RegionDiscoveryProvider> provideEstimoteBeaconDiscoveryProvider;
  private Provider<RegionDiscoveryProvider> provideGeloBeaconDiscoveryProvider;
  private Provider<ButtonDiscoveryProvider> provideButtonDiscoveryProvider;
  private Provider<BluetoothProvider> provideBluetoothProvider;
  private MembersInjector<MonitoringService> monitoringServiceMembersInjector;
  private MembersInjector<ButtonCommunicator> buttonCommunicatorMembersInjector;
  private MembersInjector<MainControllerActivity> mainControllerActivityMembersInjector;
  private MembersInjector<ButtonProvider> buttonProviderMembersInjector;

  private Dagger_Graph(Builder builder) {  
    assert builder != null;
    initialize(builder);
  }

  public static Builder builder() {  
    return new Builder();
  }

  private void initialize(final Builder builder) {  
    this.provideRegionProvider = ScopedProvider.create(TestRBModule$$ProvideRegionProviderFactory.create(builder.testRBModule));
    this.leDeviceListAdapterMembersInjector = LeDeviceListAdapter$$MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideRegionProvider);
    this.provideButtonProvider = ScopedProvider.create(TestRBModule$$ProvideButtonProviderFactory.create(builder.testRBModule));
    this.buttonFragmentMembersInjector = ButtonFragment$$MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideButtonProvider);
    this.buttonDetailsDialogFragmentMembersInjector = ButtonDetailsDialogFragment$$MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideButtonProvider, provideRegionProvider);
    this.provideEstimoteBeaconDiscoveryProvider = ScopedProvider.create(TestRBModule$$ProvideEstimoteBeaconDiscoveryProviderFactory.create(builder.testRBModule));
    this.provideGeloBeaconDiscoveryProvider = ScopedProvider.create(TestRBModule$$ProvideGeloBeaconDiscoveryProviderFactory.create(builder.testRBModule));
    this.provideButtonDiscoveryProvider = ScopedProvider.create(TestRBModule$$ProvideButtonDiscoveryProviderFactory.create(builder.testRBModule));
    this.provideBluetoothProvider = ScopedProvider.create(TestRBModule$$ProvideBluetoothProviderFactory.create(builder.testRBModule));
    this.monitoringServiceMembersInjector = MonitoringService$$MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideRegionProvider, provideEstimoteBeaconDiscoveryProvider, provideGeloBeaconDiscoveryProvider, provideButtonProvider, provideButtonDiscoveryProvider, provideBluetoothProvider);
    this.buttonCommunicatorMembersInjector = ButtonCommunicator$$MembersInjector.create(provideButtonDiscoveryProvider);
    this.mainControllerActivityMembersInjector = MainControllerActivity$$MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideBluetoothProvider);
    this.buttonProviderMembersInjector = ButtonProvider$$MembersInjector.create(provideRegionProvider);
  }

  @Override
  public void inject(LeDeviceListAdapter thingy) {  
    leDeviceListAdapterMembersInjector.injectMembers(thingy);
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
  public void inject(InjectableActivityInstrumentationTest thingy) {
    MembersInjectors.noOp().injectMembers(thingy);
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

  public static final class Builder {
    private TestRBModule testRBModule;
  
    private Builder() {  
    }
  
    public Graph build() {  
      if (testRBModule == null) {
        throw new IllegalStateException("testRBModule must be set");
      }
      return new Dagger_Graph(this);
    }
  
    public Builder testRBModule(TestRBModule testRBModule) {  
      if (testRBModule == null) {
        throw new NullPointerException("testRBModule");
      }
      this.testRBModule = testRBModule;
      return this;
    }
  }
}

