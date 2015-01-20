package com.ndipatri.arduinoButton;

import com.ndipatri.arduinoButton.dagger.modules.TestABModule;

import java.util.Arrays;
import java.util.List;

public class TestABApplicationImpl extends ABApplication {

    private static final String TAG = TestABApplicationImpl.class.getCanonicalName();

    protected List<? extends Object> getDependencyModules() {
        return Arrays.asList(
                new TestABModule(this)
        );
    }
}
