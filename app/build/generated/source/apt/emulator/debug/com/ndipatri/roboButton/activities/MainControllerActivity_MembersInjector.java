package com.ndipatri.roboButton.activities;

import android.app.Activity;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class MainControllerActivity_MembersInjector implements MembersInjector<MainControllerActivity> {
  private final MembersInjector<Activity> supertypeInjector;
  private final Provider<BluetoothProvider> bluetoothProvider;

  public MainControllerActivity_MembersInjector(MembersInjector<Activity> supertypeInjector, Provider<BluetoothProvider> bluetoothProvider) {  
    assert supertypeInjector != null;
    this.supertypeInjector = supertypeInjector;
    assert bluetoothProvider != null;
    this.bluetoothProvider = bluetoothProvider;
  }

  @Override
  public void injectMembers(MainControllerActivity instance) {  
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    supertypeInjector.injectMembers(instance);
    instance.bluetoothProvider = bluetoothProvider.get();
  }

  public static MembersInjector<MainControllerActivity> create(MembersInjector<Activity> supertypeInjector, Provider<BluetoothProvider> bluetoothProvider) {  
      return new MainControllerActivity_MembersInjector(supertypeInjector, bluetoothProvider);
  }
}

