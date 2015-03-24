package com.ndipatri.roboButton;

import com.ndipatri.roboButton.dagger.modules.RBModule;

import java.util.Arrays;
import java.util.List;

public class RBApplicationImpl extends RBApplication {

    private static final String TAG = RBApplicationImpl.class.getCanonicalName();

    protected List<? extends Object> getDependencyModules() {
        return Arrays.asList(
                new RBModule(this)
        );
    }
}
