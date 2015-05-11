package com.ndipatri.roboButton.activities;

import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class MainControllerActivityInstrumentationTest_MembersInjector implements MembersInjector<MainControllerActivityInstrumentationTest> {
  private final MembersInjector<InjectableActivityInstrumentationTest<MainControllerActivity>> supertypeInjector;
  private final Provider<BluetoothProvider> bluetoothProvider;

  public MainControllerActivityInstrumentationTest_MembersInjector(MembersInjector<InjectableActivityInstrumentationTest<MainControllerActivity>> supertypeInjector, Provider<BluetoothProvider> bluetoothProvider) {  
    assert supertypeInjector != null;
    this.supertypeInjector = supertypeInjector;
    assert bluetoothProvider != null;
    this.bluetoothProvider = bluetoothProvider;
  }

  @Override
  public void injectMembers(MainControllerActivityInstrumentationTest instance) {  
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    supertypeInjector.injectMembers(instance);
    instance.bluetoothProvider = bluetoothProvider.get();
  }

  public static MembersInjector<MainControllerActivityInstrumentationTest> create(MembersInjector<InjectableActivityInstrumentationTest<MainControllerActivity>> supertypeInjector, Provider<BluetoothProvider> bluetoothProvider) {  
      return new MainControllerActivityInstrumentationTest_MembersInjector(supertypeInjector, bluetoothProvider);
  }
}

