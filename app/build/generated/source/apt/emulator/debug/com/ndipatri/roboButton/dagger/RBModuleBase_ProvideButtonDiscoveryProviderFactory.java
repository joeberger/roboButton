package com.ndipatri.roboButton.dagger;

import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class RBModuleBase_ProvideButtonDiscoveryProviderFactory implements Factory<ButtonDiscoveryProvider> {
  private final RBModuleBase module;

  public RBModuleBase_ProvideButtonDiscoveryProviderFactory(RBModuleBase module) {  
    assert module != null;
    this.module = module;
  }

  @Override
  public ButtonDiscoveryProvider get() {  
    ButtonDiscoveryProvider provided = module.provideButtonDiscoveryProvider();
    if (provided == null) {
      throw new NullPointerException("Cannot return null from a non-@Nullable @Provides method");
    }
    return provided;
  }

  public static Factory<ButtonDiscoveryProvider> create(RBModuleBase module) {  
    return new RBModuleBase_ProvideButtonDiscoveryProviderFactory(module);
  }
}

