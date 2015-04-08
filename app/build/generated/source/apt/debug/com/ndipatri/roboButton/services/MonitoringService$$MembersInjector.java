package com.ndipatri.roboButton.services;

import android.app.Service;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class MonitoringService$$MembersInjector implements MembersInjector<MonitoringService> {
  private final MembersInjector<Service> supertypeInjector;
  private final Provider<RegionProvider> regionProvider;
  private final Provider<RegionDiscoveryProvider> estimoteRegionDiscoveryProvider;
  private final Provider<RegionDiscoveryProvider> geloRegionDiscoveryProvider;
  private final Provider<ButtonProvider> buttonProvider;
  private final Provider<ButtonDiscoveryProvider> buttonDiscoveryProvider;
  private final Provider<BluetoothProvider> bluetoothProvider;

  public MonitoringService$$MembersInjector(MembersInjector<Service> supertypeInjector, Provider<RegionProvider> regionProvider, Provider<RegionDiscoveryProvider> estimoteRegionDiscoveryProvider, Provider<RegionDiscoveryProvider> geloRegionDiscoveryProvider, Provider<ButtonProvider> buttonProvider, Provider<ButtonDiscoveryProvider> buttonDiscoveryProvider, Provider<BluetoothProvider> bluetoothProvider) {  
    assert supertypeInjector != null;
    this.supertypeInjector = supertypeInjector;
    assert regionProvider != null;
    this.regionProvider = regionProvider;
    assert estimoteRegionDiscoveryProvider != null;
    this.estimoteRegionDiscoveryProvider = estimoteRegionDiscoveryProvider;
    assert geloRegionDiscoveryProvider != null;
    this.geloRegionDiscoveryProvider = geloRegionDiscoveryProvider;
    assert buttonProvider != null;
    this.buttonProvider = buttonProvider;
    assert buttonDiscoveryProvider != null;
    this.buttonDiscoveryProvider = buttonDiscoveryProvider;
    assert bluetoothProvider != null;
    this.bluetoothProvider = bluetoothProvider;
  }

  @Override
  public void injectMembers(MonitoringService instance) {  
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    supertypeInjector.injectMembers(instance);
    instance.regionProvider = regionProvider.get();
    instance.estimoteRegionDiscoveryProvider = estimoteRegionDiscoveryProvider.get();
    instance.geloRegionDiscoveryProvider = geloRegionDiscoveryProvider.get();
    instance.buttonProvider = buttonProvider.get();
    instance.buttonDiscoveryProvider = buttonDiscoveryProvider.get();
    instance.bluetoothProvider = bluetoothProvider.get();
  }

  public static MembersInjector<MonitoringService> create(MembersInjector<Service> supertypeInjector, Provider<RegionProvider> regionProvider, Provider<RegionDiscoveryProvider> estimoteRegionDiscoveryProvider, Provider<RegionDiscoveryProvider> geloRegionDiscoveryProvider, Provider<ButtonProvider> buttonProvider, Provider<ButtonDiscoveryProvider> buttonDiscoveryProvider, Provider<BluetoothProvider> bluetoothProvider) {  
      return new MonitoringService$$MembersInjector(supertypeInjector, regionProvider, estimoteRegionDiscoveryProvider, geloRegionDiscoveryProvider, buttonProvider, buttonDiscoveryProvider, bluetoothProvider);
  }
}

