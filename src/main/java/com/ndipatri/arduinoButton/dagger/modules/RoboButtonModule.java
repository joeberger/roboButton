package com.ndipatri.arduinoButton.dagger.modules;

import com.ndipatri.arduinoButton.ArduinoButtonApplication;
import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {
                ButtonProvider.class
        },

        library = false
)
public class RoboButtonModule {

    @Provides
    @Singleton
    ButtonProvider provideButtonProvider() {
        return new ButtonProvider(ArduinoButtonApplication.getInstance());
    }
}
