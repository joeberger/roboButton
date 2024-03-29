package com.ndipatri.roboButton.activities;

import android.graphics.drawable.Drawable;
import android.test.suitebuilder.annotation.LargeTest;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.events.ButtonDiscoveryEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonUpdatedEvent;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.TestUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@LargeTest
public class MainActivityIntegrationTest extends MainActivityInstrumentation {

    public void testActivityTitle() {
        getActivity();

        onView(withText("RoboButton")).check(matches(isDisplayed()));
    }

    public void testButtonFound_Disconnected() throws InterruptedException {

        getActivity();

        bus.post(new ButtonDiscoveryEvent(true, ButtonType.LIGHTBLUE_BUTTON, "aa:bb:cc:dd:ee", null));

        Button mockButton = mock(Button.class);
        when(mockButton.getState()).thenReturn(ButtonState.DISCONNECTED);
        when(buttonDao.getButton("aa:bb:cc:dd:ee")).thenReturn(mockButton);

        bus.post(new ButtonUpdatedEvent("aa:bb:cc:dd:ee"));

        // Just to make the test look pretty for demonstration purposes
        Thread.sleep(2000);

        onView(withId(R.id.buttonFragmentFrameLayout)).check(matches(isDisplayed()));

        Drawable disconnectedDrawable = getActivity().getResources().getDrawable(R.drawable.yellow_button);
        onView(withId(R.id.buttonImageView)).check(matches(TestUtils.isBitmapTheSame(disconnectedDrawable)));


    }

    public void testButtonFound_Off() throws InterruptedException {

        getActivity();

        bus.post(new ButtonDiscoveryEvent(true, ButtonType.LIGHTBLUE_BUTTON, "aa:bb:cc:dd:ee", null));

        Button mockButton = mock(Button.class);
        when(mockButton.getState()).thenReturn(ButtonState.OFF);
        when(buttonDao.getButton("aa:bb:cc:dd:ee")).thenReturn(mockButton);

        bus.post(new ButtonUpdatedEvent("aa:bb:cc:dd:ee"));

        // Just to make the test look pretty for demonstration purposes
        Thread.sleep(2000);

        onView(withId(R.id.buttonFragmentFrameLayout)).check(matches(isDisplayed()));

        Drawable offDrawable = getActivity().getResources().getDrawable(R.drawable.red_button);
        onView(withId(R.id.buttonImageView)).check(matches(TestUtils.isBitmapTheSame(offDrawable)));
    }

    public void testButtonFound_On() throws InterruptedException{

        getActivity();

        bus.post(new ButtonDiscoveryEvent(true, ButtonType.LIGHTBLUE_BUTTON, "aa:bb:cc:dd:ee", null));

        Button mockButton = mock(Button.class);
        when(mockButton.getState()).thenReturn(ButtonState.ON);
        when(buttonDao.getButton("aa:bb:cc:dd:ee")).thenReturn(mockButton);

        bus.post(new ButtonUpdatedEvent("aa:bb:cc:dd:ee"));

        // Just to make the test look pretty for demonstration purposes
        Thread.sleep(2000);

        onView(withId(R.id.buttonFragmentFrameLayout)).check(matches(isDisplayed()));

        Drawable onDrawable = getActivity().getResources().getDrawable(R.drawable.green_button);
        onView(withId(R.id.buttonImageView)).check(matches(TestUtils.isBitmapTheSame(onDrawable)));
    }

    public void testButtonLostRemovesButtonFragment() throws InterruptedException {

        getActivity();

        bus.post(new ButtonDiscoveryEvent(true, ButtonType.LIGHTBLUE_BUTTON, "aa:bb:cc:dd:ee", null));

        Button mockButton = mock(Button.class);
        when(mockButton.getState()).thenReturn(ButtonState.DISCONNECTED);
        when(buttonDao.getButton("aa:bb:cc:dd:ee")).thenReturn(mockButton);

        bus.post(new ButtonUpdatedEvent("aa:bb:cc:dd:ee"));

        // Just to make the test look pretty for demonstration purposes
        Thread.sleep(2000);

        onView(withId(R.id.buttonFragmentFrameLayout)).check(matches(isDisplayed()));

        bus.post(new ButtonLostEvent("aa:bb:cc:dd:ee"));

        onView(withId(R.id.buttonFragmentFrameLayout)).check(doesNotExist());
    }
}
