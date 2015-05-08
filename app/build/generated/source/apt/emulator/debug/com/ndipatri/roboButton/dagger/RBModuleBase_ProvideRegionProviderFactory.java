package com.ndipatri.roboButton.dagger;

import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class RBModuleBase_ProvideRegionProviderFactory implements Factory<RegionProvider> {
  private final RBModuleBase module;

  public RBModuleBase_ProvideRegionProviderFactory(RBModuleBase module) {  
    assert module != null;
    this.module = module;
  }

  @Override
  public RegionProvider get() {  
    RegionProvider provided = module.provideRegionProvider();
    if (provided == null) {
      throw new NullPointerException("Cannot return null from a non-@Nullable @Provides method");
    }
    return provided;
  }

  public static Factory<RegionProvider> create(RBModuleBase module) {  
    return new RBModuleBase_ProvideRegionProviderFactory(module);
  }
}

