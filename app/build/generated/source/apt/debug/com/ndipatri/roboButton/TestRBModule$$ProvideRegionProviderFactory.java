package com.ndipatri.roboButton;

import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class TestRBModule$$ProvideRegionProviderFactory implements Factory<RegionProvider> {
  private final TestRBModule module;

  public TestRBModule$$ProvideRegionProviderFactory(TestRBModule module) {  
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

  public static Factory<RegionProvider> create(TestRBModule module) {  
    return new TestRBModule$$ProvideRegionProviderFactory(module);
  }
}

