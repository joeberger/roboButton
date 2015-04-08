package com.ndipatri.roboButton;

public class TestRBApplicationImpl extends RBApplication {

    private static final String TAG = TestRBApplicationImpl.class.getCanonicalName();

    public Graph getObjectGraph() {
        return Graph.Initializer.init(this, true);
    }
}
