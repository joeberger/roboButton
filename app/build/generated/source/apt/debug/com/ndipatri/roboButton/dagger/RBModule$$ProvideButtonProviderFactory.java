package com.ndipatri.roboButton.dagger;

import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class RBModule$$ProvideButtonProviderFactory implements Factory<ButtonProvider> {
  private final RBModule module;

  public RBModule$$ProvideButtonProviderFactory(RBModule module) {  
    assert module != null;
    this.module = module;
  }

  @Override
  public ButtonProvider get() {  
    ButtonProvider provided = module.provideButtonProvider();
    if (provided == null) {
      throw new NullPointerException("Cannot return null from a non-@Nullable @Provides method");
    }
    return provided;
  }

  public static Factory<ButtonProvider> create(RBModule module) {  
    return new RBModule$$ProvideButtonProviderFactory(module);
  }
}

