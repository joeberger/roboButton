package com.ndipatri.roboButton.dagger;

import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class RBModule$$ProvideGeloBeaconDiscoveryProviderFactory implements Factory<RegionDiscoveryProvider> {
  private final RBModule module;

  public RBModule$$ProvideGeloBeaconDiscoveryProviderFactory(RBModule module) {  
    assert module != null;
    this.module = module;
  }

  @Override
  public RegionDiscoveryProvider get() {  
    RegionDiscoveryProvider provided = module.provideGeloBeaconDiscoveryProvider();
    if (provided == null) {
      throw new NullPointerException("Cannot return null from a non-@Nullable @Provides method");
    }
    return provided;
  }

  public static Factory<RegionDiscoveryProvider> create(RBModule module) {  
    return new RBModule$$ProvideGeloBeaconDiscoveryProviderFactory(module);
  }
}

