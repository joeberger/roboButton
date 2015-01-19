package com.ndipatri.arduinoButton;

import com.ndipatri.arduinoButton.dagger.modules.RoboButtonModule;

import java.util.Arrays;
import java.util.List;

public class ABApplicationImpl extends ABApplication {

    private static final String TAG = ABApplicationImpl.class.getCanonicalName();

    protected List<? extends Object> getDependencyModules() {
        return Arrays.asList(
                new RoboButtonModule(this)
        );
    }
}
