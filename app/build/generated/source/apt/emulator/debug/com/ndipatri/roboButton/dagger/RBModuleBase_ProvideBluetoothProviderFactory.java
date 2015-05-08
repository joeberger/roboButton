package com.ndipatri.roboButton.dagger;

import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import dagger.internal.Factory;
import javax.annotation.Generated;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class RBModuleBase_ProvideBluetoothProviderFactory implements Factory<BluetoothProvider> {
  private final RBModuleBase module;

  public RBModuleBase_ProvideBluetoothProviderFactory(RBModuleBase module) {  
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

  public static Factory<BluetoothProvider> create(RBModuleBase module) {  
    return new RBModuleBase_ProvideBluetoothProviderFactory(module);
  }
}

