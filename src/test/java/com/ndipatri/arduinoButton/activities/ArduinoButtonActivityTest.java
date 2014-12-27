package com.ndipatri.arduinoButton.activities;

import android.app.Activity;
import android.view.View;

import com.ndipatri.arduinoButton.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class ArduinoButtonActivityTest {

    @Test
    public void testOnCreate() throws Exception {
        Activity activity = Robolectric.buildActivity(MainControllerActivity.class).create().get();
        assertThat("MainControllerActivity can't create!", activity != null);
    }

    @Test
    public void testOnResume() throws Exception {
        MainControllerActivity activity = Robolectric.buildActivity(MainControllerActivity.class).create().get();
        MainControllerActivity spyMainControllerActivity = spy(activity);

        spyMainControllerActivity.onResume();

        verify(spyMainControllerActivity).resumeActivity();
    }

    @Test
    public void testMainViewGroupExists() throws Exception {
        Activity activity = Robolectric.buildActivity(MainControllerActivity.class).create().get();

        View rootView = activity.getWindow().getDecorView();
        View mainViewGroup = rootView.findViewById(R.id.mainViewGroup);
        assertThat("'mainViewGroup' does not exist!", mainViewGroup != null);
    }

    @Test
    public void testResumeActivity() {
        MainControllerActivity mockMainControllerActivity = mock(MainControllerActivity.class);
        doCallRealMethod().when(mockMainControllerActivity).onResume();

        mockMainControllerActivity.onResume();
        verify(mockMainControllerActivity, times(1)).registerWithOttoBus();
    }



}
