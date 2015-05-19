package com.ndipatri.roboButton.activities;

import android.graphics.drawable.Drawable;
import android.test.suitebuilder.annotation.LargeTest;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonStateChangeReport;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.ndipatri.roboButton.utils.TestUtils.isImageTheSame;

@LargeTest
public class MainControllerActivityIntegrationTest extends MainControllerActivityInstrumentation {

    public void testActivityTitle() {
        onView(withText("RoboButton")).check(matches(isDisplayed()));
    }

    public void testButtonFound_Disconnected() {

        bus.post(new ButtonStateChangeReport("1", ButtonState.DISCONNECTED));

        onView(withId(R.id.buttonFragmentFrameLayout)).check(matches(isDisplayed()));

        Drawable disconnectedDrawable = getActivity().getResources().getDrawable(R.drawable.yellow_button);
        onView(withId(R.id.buttonImageView)).check(matches(isImageTheSame(disconnectedDrawable)));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testButtonFound_Off() {
        bus.post(new ButtonStateChangeReport("1", ButtonState.OFF));
        onView(withId(R.id.buttonFragmentFrameLayout)).check(matches(isDisplayed()));

        Drawable offDrawable = getActivity().getResources().getDrawable(R.drawable.red_button);
        onView(withId(R.id.buttonImageView)).check(matches(isImageTheSame(offDrawable)));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testButtonFound_On() {
        bus.post(new ButtonStateChangeReport("1", ButtonState.ON));

        onView(withId(R.id.buttonFragmentFrameLayout)).check(matches(isDisplayed()));

        Drawable onDrawable = getActivity().getResources().getDrawable(R.drawable.red_button);
        onView(withId(R.id.buttonImageView)).check(matches(isImageTheSame(onDrawable)));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testButtonLostRemovesButtonFragment() {
        bus.post(new ButtonStateChangeReport("1", ButtonState.DISCONNECTED));

        onView(withId(R.id.buttonFragmentFrameLayout)).check(matches(isDisplayed()));

        bus.post(new ButtonLostEvent("1"));

        onView(withId(R.id.buttonFragmentFrameLayout)).check(doesNotExist());
    }
}
