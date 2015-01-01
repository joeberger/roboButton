package com.ndipatri.arduinoButton;

import com.ndipatri.arduinoButton.dagger.modules.RoboButtonModule;
import com.ndipatri.arduinoButton.dagger.modules.TestRoboButtonModule;

import java.util.Arrays;
import java.util.List;

public class TestArduinoButtonApplicationImpl extends ArduinoButtonApplication {

    private static final String TAG = TestArduinoButtonApplicationImpl.class.getCanonicalName();

    protected List<? extends Object> getDependencyModules() {
        return Arrays.asList(
                new TestRoboButtonModule(this)
        );
    }
}
