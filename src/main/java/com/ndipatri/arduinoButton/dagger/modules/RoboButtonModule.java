package com.ndipatri.arduinoButton.dagger.modules;

import android.content.Context;

import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;
import com.ndipatri.arduinoButton.fragments.ButtonDetailsDialogFragment;
import com.ndipatri.arduinoButton.services.ButtonMonitoringService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {
                ButtonDetailsDialogFragment.class,
                ButtonMonitoringService.class
        }
)
public class RoboButtonModule {

    private Context context = null;

    public RoboButtonModule (Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    ButtonProvider provideButtonProvider() {
        return new ButtonProvider(context);
    }
}
