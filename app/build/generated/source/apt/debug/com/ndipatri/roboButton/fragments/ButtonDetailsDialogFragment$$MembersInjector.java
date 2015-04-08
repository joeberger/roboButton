package com.ndipatri.roboButton.fragments;

import android.app.DialogFragment;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class ButtonDetailsDialogFragment$$MembersInjector implements MembersInjector<ButtonDetailsDialogFragment> {
  private final MembersInjector<DialogFragment> supertypeInjector;
  private final Provider<ButtonProvider> buttonProvider;
  private final Provider<RegionProvider> regionProvider;

  public ButtonDetailsDialogFragment$$MembersInjector(MembersInjector<DialogFragment> supertypeInjector, Provider<ButtonProvider> buttonProvider, Provider<RegionProvider> regionProvider) {  
    assert supertypeInjector != null;
    this.supertypeInjector = supertypeInjector;
    assert buttonProvider != null;
    this.buttonProvider = buttonProvider;
    assert regionProvider != null;
    this.regionProvider = regionProvider;
  }

  @Override
  public void injectMembers(ButtonDetailsDialogFragment instance) {  
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    supertypeInjector.injectMembers(instance);
    instance.buttonProvider = buttonProvider.get();
    instance.regionProvider = regionProvider.get();
  }

  public static MembersInjector<ButtonDetailsDialogFragment> create(MembersInjector<DialogFragment> supertypeInjector, Provider<ButtonProvider> buttonProvider, Provider<RegionProvider> regionProvider) {  
      return new ButtonDetailsDialogFragment$$MembersInjector(supertypeInjector, buttonProvider, regionProvider);
  }
}

