package com.ndipatri.roboButton.utils;

import android.widget.BaseAdapter;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import dagger.MembersInjector;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated("dagger.internal.codegen.ComponentProcessor")
public final class LeDeviceListAdapter_MembersInjector implements MembersInjector<LeDeviceListAdapter> {
  private final MembersInjector<BaseAdapter> supertypeInjector;
  private final Provider<RegionProvider> regionProvider;

  public LeDeviceListAdapter_MembersInjector(MembersInjector<BaseAdapter> supertypeInjector, Provider<RegionProvider> regionProvider) {  
    assert supertypeInjector != null;
    this.supertypeInjector = supertypeInjector;
    assert regionProvider != null;
    this.regionProvider = regionProvider;
  }

  @Override
  public void injectMembers(LeDeviceListAdapter instance) {  
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    supertypeInjector.injectMembers(instance);
    instance.regionProvider = regionProvider.get();
  }

  public static MembersInjector<LeDeviceListAdapter> create(MembersInjector<BaseAdapter> supertypeInjector, Provider<RegionProvider> regionProvider) {  
      return new LeDeviceListAdapter_MembersInjector(supertypeInjector, regionProvider);
  }
}

