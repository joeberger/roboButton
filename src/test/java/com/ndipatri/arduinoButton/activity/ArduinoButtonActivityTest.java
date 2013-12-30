package com.ndipatri.arduinoButton.activity;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ArduinoButtonActivityTest {

    @Test
    public void testSomething() throws Exception {
        Activity activity = Robolectric.buildActivity(MainControllerActivity.class).create().get();
        assertTrue(activity != null);
    }


}
