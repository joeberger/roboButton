package com.ndipatri.arduinoButton;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.ndipatri.arduinoButton.dagger.modules.RoboButtonModule;
import com.ndipatri.arduinoButton.services.ButtonMonitoringService;
import com.ndipatri.arduinoButton.utils.ActivityWatcher;

import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;

public class ArduinoButtonApplicationImpl extends ArduinoButtonApplication {

    private static final String TAG = ArduinoButtonApplicationImpl.class.getCanonicalName();

    protected List<? extends Object> getDependencyModules() {
        return Arrays.asList(
                new RoboButtonModule(this)
        );
    }
}
