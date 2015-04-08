package com.ndipatri.roboButton.activities;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@LargeTest
public class MainControllerActivityIntegrationTest extends ActivityInstrumentationTestCase2<MainControllerActivity> {

    public MainControllerActivityIntegrationTest() {
        super(MainControllerActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        /**
        RBApplication app = (App) getInstrumentation().getTargetContext().getApplicationContext();
        app.setMockMode(true);
        app.graph().inject(this);
         **/

        getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        //App.getInstance().setMockMode(false);
    }

    public void testListGoesOverTheFold() {
        onView(withText("RoboButton")).check(matches(isDisplayed()));
    }
}
