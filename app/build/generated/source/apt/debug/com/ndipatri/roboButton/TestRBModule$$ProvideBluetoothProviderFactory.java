package com.ndipatri.roboButton;

import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class TestRBModule$$ProvideBluetoothProviderFactory implements Factory<BluetoothProvider> {
  private final TestRBModule module;

  public TestRBModule$$ProvideBluetoothProviderFactory(TestRBModule module) {  
    assert module != null;
    this.module = module;
  }

  @Override
  public BluetoothProvider get() {  
    BluetoothProvider provided = module.provideBluetoothProvider();
    if (provided == null) {
      throw new NullPointerException("Cannot return null from a non-@Nullable @Provides method");
    }
    return provided;
  }

  public static Factory<BluetoothProvider> create(TestRBModule module) {  
    return new TestRBModule$$ProvideBluetoothProviderFactory(module);
  }
}

