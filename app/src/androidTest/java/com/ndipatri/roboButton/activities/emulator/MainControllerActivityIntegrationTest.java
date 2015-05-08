package com.ndipatri.roboButton.activities.emulator;

import android.test.suitebuilder.annotation.LargeTest;

import com.ndipatri.roboButton.activities.MainControllerActivityInstrumentationTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@LargeTest
public class MainControllerActivityIntegrationTest extends MainControllerActivityInstrumentationTest {

    public void testActivityTitle() {
        onView(withText("RoboButton")).check(matches(isDisplayed()));
    }
}
