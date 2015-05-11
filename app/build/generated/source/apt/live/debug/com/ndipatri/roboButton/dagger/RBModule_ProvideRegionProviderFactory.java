package com.ndipatri.roboButton.dagger;

import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class RBModule_ProvideRegionProviderFactory implements Factory<RegionProvider> {
  private final RBModule module;

  public RBModule_ProvideRegionProviderFactory(RBModule module) {  
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

  public static Factory<RegionProvider> create(RBModule module) {  
    return new RBModule_ProvideRegionProviderFactory(module);
  }
}

