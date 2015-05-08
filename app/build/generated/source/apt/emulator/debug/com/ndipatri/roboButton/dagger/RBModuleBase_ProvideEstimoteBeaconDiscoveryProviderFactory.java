package com.ndipatri.roboButton.dagger;

import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class RBModuleBase_ProvideEstimoteBeaconDiscoveryProviderFactory implements Factory<RegionDiscoveryProvider> {
  private final RBModuleBase module;

  public RBModuleBase_ProvideEstimoteBeaconDiscoveryProviderFactory(RBModuleBase module) {  
    assert module != null;
    this.module = module;
  }

  @Override
  public RegionDiscoveryProvider get() {  
    RegionDiscoveryProvider provided = module.provideEstimoteBeaconDiscoveryProvider();
    if (provided == null) {
      throw new NullPointerException("Cannot return null from a non-@Nullable @Provides method");
    }
    return provided;
  }

  public static Factory<RegionDiscoveryProvider> create(RBModuleBase module) {  
    return new RBModuleBase_ProvideEstimoteBeaconDiscoveryProviderFactory(module);
  }
}

