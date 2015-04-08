package com.ndipatri.roboButton;

import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class TestRBModule$$ProvideButtonProviderFactory implements Factory<ButtonProvider> {
  private final TestRBModule module;

  public TestRBModule$$ProvideButtonProviderFactory(TestRBModule module) {  
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

  public static Factory<ButtonProvider> create(TestRBModule module) {  
    return new TestRBModule$$ProvideButtonProviderFactory(module);
  }
}

