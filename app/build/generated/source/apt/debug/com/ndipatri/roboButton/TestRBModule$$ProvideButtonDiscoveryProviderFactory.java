package com.ndipatri.roboButton;

import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class TestRBModule$$ProvideButtonDiscoveryProviderFactory implements Factory<ButtonDiscoveryProvider> {
  private final TestRBModule module;

  public TestRBModule$$ProvideButtonDiscoveryProviderFactory(TestRBModule module) {  
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

  public static Factory<ButtonDiscoveryProvider> create(TestRBModule module) {  
    return new TestRBModule$$ProvideButtonDiscoveryProviderFactory(module);
  }
}

