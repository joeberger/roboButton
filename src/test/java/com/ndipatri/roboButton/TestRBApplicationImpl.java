package com.ndipatri.roboButton;

import com.ndipatri.roboButton.dagger.modules.TestABModule;

import java.util.Arrays;
import java.util.List;

public class TestRBApplicationImpl extends RBApplication {

    private static final String TAG = TestRBApplicationImpl.class.getCanonicalName();

    protected List<? extends Object> getDependencyModules() {
        return Arrays.asList(
                new TestABModule(this)
        );
    }
}
