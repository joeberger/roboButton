package com.ndipatri.roboButton.dagger.providers;

import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class ButtonProvider_MembersInjector implements MembersInjector<ButtonProvider> {
  private final Provider<RegionProvider> regionProvider;

  public ButtonProvider_MembersInjector(Provider<RegionProvider> regionProvider) {  
    assert regionProvider != null;
    this.regionProvider = regionProvider;
  }

  @Override
  public void injectMembers(ButtonProvider instance) {  
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    instance.regionProvider = regionProvider.get();
  }

  public static MembersInjector<ButtonProvider> create(Provider<RegionProvider> regionProvider) {  
      return new ButtonProvider_MembersInjector(regionProvider);
  }
}

