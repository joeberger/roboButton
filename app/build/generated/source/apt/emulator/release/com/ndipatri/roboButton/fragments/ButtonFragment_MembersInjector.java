package com.ndipatri.roboButton.fragments;

import android.app.Fragment;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class ButtonFragment_MembersInjector implements MembersInjector<ButtonFragment> {
  private final MembersInjector<Fragment> supertypeInjector;
  private final Provider<ButtonProvider> buttonProvider;

  public ButtonFragment_MembersInjector(MembersInjector<Fragment> supertypeInjector, Provider<ButtonProvider> buttonProvider) {  
    assert supertypeInjector != null;
    this.supertypeInjector = supertypeInjector;
    assert buttonProvider != null;
    this.buttonProvider = buttonProvider;
  }

  @Override
  public void injectMembers(ButtonFragment instance) {  
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    supertypeInjector.injectMembers(instance);
    instance.buttonProvider = buttonProvider.get();
  }

  public static MembersInjector<ButtonFragment> create(MembersInjector<Fragment> supertypeInjector, Provider<ButtonProvider> buttonProvider) {  
      return new ButtonFragment_MembersInjector(supertypeInjector, buttonProvider);
  }
}

