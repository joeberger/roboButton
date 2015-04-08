package com.ndipatri.roboButton.dagger;

import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class RBModule$$ProvideBluetoothProviderFactory implements Factory<BluetoothProvider> {
  private final RBModule module;

  public RBModule$$ProvideBluetoothProviderFactory(RBModule module) {  
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

  public static Factory<BluetoothProvider> create(RBModule module) {  
    return new RBModule$$ProvideBluetoothProviderFactory(module);
  }
}

