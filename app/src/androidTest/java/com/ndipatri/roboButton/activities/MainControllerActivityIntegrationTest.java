package com.ndipatri.roboButton.activities;

import android.graphics.drawable.Drawable;
import android.test.suitebuilder.annotation.LargeTest;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonStateChangeReport;
import com.ndipatri.roboButton.fragments.ButtonFragment;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.Is.is;

@LargeTest
public class MainControllerActivityIntegrationTest extends MainControllerActivityInstrumentation {

    public void testActivityTitle() {
        onView(withText("RoboButton")).check(matches(isDisplayed()));
    }

    public void testButtonFoundShowsButtonFragment() {
        bus.post(new ButtonStateChangeReport("1", ButtonState.DISCONNECTED));

        onView(withId(R.id.buttonFragmentFrameLayout)).check(matches(isDisplayed()));
    }

    public void testButtonLostRemovesButtonFragment() {
        bus.post(new ButtonStateChangeReport("1", ButtonState.DISCONNECTED));

        onView(withId(R.id.buttonFragmentFrameLayout)).check(matches(isDisplayed()));

        bus.post(new ButtonLostEvent("1"));

        onView(withId(R.id.buttonFragmentFrameLayout)).check(doesNotExist());
    }
}
