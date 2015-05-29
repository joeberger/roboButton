package com.ndipatri.roboButton.activities;

import android.app.Activity;
import android.app.Instrumentation;
import android.bluetooth.BluetoothAdapter;
import android.graphics.drawable.Drawable;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonStateChangeReport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.ndipatri.roboButton.utils.TestUtils.isImageTheSame;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

@LargeTest
public class MainControllerActivityIntegrationTest extends MainControllerActivityInstrumentation {

    @Before
    public void stubAllExternalIntents() {
        // By default Espresso Intents does not stub any Intents. Stubbing needs to be setup before
        // every test run. In this case all external Intents will be blocked.
        intending(not(isInternal())).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    }

    public void testActivityTitle() {
        getActivity();

        onView(withText("RoboButton")).check(matches(isDisplayed()));
    }

    public void testButtonFound_Disconnected() {

        getActivity();

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

        getActivity();

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

        getActivity();

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

        getActivity();

        bus.post(new ButtonStateChangeReport("1", ButtonState.DISCONNECTED));

        onView(withId(R.id.buttonFragmentFrameLayout)).check(matches(isDisplayed()));

        bus.post(new ButtonLostEvent("1"));

        onView(withId(R.id.buttonFragmentFrameLayout)).check(doesNotExist());
    }
}
