package com.ndipatri.roboButton.utils;

import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class ButtonCommunicator$$MembersInjector implements MembersInjector<ButtonCommunicator> {
  private final Provider<ButtonDiscoveryProvider> buttonDiscoveryProvider;

  public ButtonCommunicator$$MembersInjector(Provider<ButtonDiscoveryProvider> buttonDiscoveryProvider) {  
    assert buttonDiscoveryProvider != null;
    this.buttonDiscoveryProvider = buttonDiscoveryProvider;
  }

  @Override
  public void injectMembers(ButtonCommunicator instance) {  
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    instance.buttonDiscoveryProvider = buttonDiscoveryProvider.get();
  }

  public static MembersInjector<ButtonCommunicator> create(Provider<ButtonDiscoveryProvider> buttonDiscoveryProvider) {  
      return new ButtonCommunicator$$MembersInjector(buttonDiscoveryProvider);
  }
}

