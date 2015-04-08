package com.ndipatri.roboButton.utils;

import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class ButtonCommunicatorTest$$MembersInjector implements MembersInjector<ButtonCommunicatorTest> {
  private final Provider<ButtonDiscoveryProvider> buttonDiscoveryProvider;

  public ButtonCommunicatorTest$$MembersInjector(Provider<ButtonDiscoveryProvider> buttonDiscoveryProvider) {  
    assert buttonDiscoveryProvider != null;
    this.buttonDiscoveryProvider = buttonDiscoveryProvider;
  }

  @Override
  public void injectMembers(ButtonCommunicatorTest instance) {  
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    instance.buttonDiscoveryProvider = buttonDiscoveryProvider.get();
  }

  public static MembersInjector<ButtonCommunicatorTest> create(Provider<ButtonDiscoveryProvider> buttonDiscoveryProvider) {  
      return new ButtonCommunicatorTest$$MembersInjector(buttonDiscoveryProvider);
  }
}

