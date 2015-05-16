package com.ndipatri.roboButton.activities;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import com.ndipatri.roboButton.RBApplication;

public class InjectableActivityInstrumentation<T extends Activity> extends ActivityInstrumentationTestCase2<T> {

    protected RBApplication targetApplication;

    public InjectableActivityInstrumentation(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        targetApplication = (RBApplication)getInstrumentation().getTargetContext().getApplicationContext();
    }
}