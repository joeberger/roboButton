package com.ndipatri.roboButton;

import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class TestRBModule$$ProvideEstimoteBeaconDiscoveryProviderFactory implements Factory<RegionDiscoveryProvider> {
  private final TestRBModule module;

  public TestRBModule$$ProvideEstimoteBeaconDiscoveryProviderFactory(TestRBModule module) {  
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

  public static Factory<RegionDiscoveryProvider> create(TestRBModule module) {  
    return new TestRBModule$$ProvideEstimoteBeaconDiscoveryProviderFactory(module);
  }
}

