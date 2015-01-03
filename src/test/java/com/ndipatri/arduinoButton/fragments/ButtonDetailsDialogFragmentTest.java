package com.ndipatri.arduinoButton.fragments;

import android.view.View;
import android.widget.Spinner;

import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.TestUtils;
import com.ndipatri.arduinoButton.activities.MainControllerActivity;
import com.ndipatri.arduinoButton.dagger.providers.BeaconProvider;
import com.ndipatri.arduinoButton.models.Beacon;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAbsSpinner;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ButtonDetailsDialogFragmentTest {

    private static final String TAG = ButtonDetailsDialogFragmentTest.class.getCanonicalName();

    protected MainControllerActivity activity;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(MainControllerActivity.class).create().resume().visible().get();

        TestUtils.registerOrmLiteProvider();
        TestUtils.resetORMTable();

        // Enable Logging to stdout
        ShadowLog.stream = System.out;
    }

    @Test
    public void testBeaconSelection() {

        // By default, this will be a newly detected button that has never been persisted.
        ButtonDetailsDialogFragment buttonDetailsDialogFragment = ButtonDetailsDialogFragment.newInstance("123");

        Beacon beacon = new Beacon();
        beacon.setName("workDesk");
        beacon.setMacAddress("aa:bb:cc:dd:ee");

        TestUtils.createOrUpdateBeacon(beacon);

        // Now we start fragment
        TestUtils.startFragment(activity, buttonDetailsDialogFragment);

        Robolectric.runUiThreadTasksIncludingDelayedTasks();

        Spinner beaconSpinner = (Spinner) buttonDetailsDialogFragment.beaconSpinner;

        ShadowAbsSpinner shadowAbsSpinner = (ShadowAbsSpinner) Robolectric.shadowOf(beaconSpinner);

        shadowAbsSpinner.setSelection(1, false);

        assertThat("shouldn't crash.", true);
    }
}
