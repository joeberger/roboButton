package com.ndipatri.arduinoButton.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.view.View;
import android.widget.Spinner;

import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.TestUtils;
import com.ndipatri.arduinoButton.activities.MainControllerActivity;
import com.ndipatri.arduinoButton.dagger.providers.BeaconProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProviderTestImpl;
import com.ndipatri.arduinoButton.models.Beacon;
import com.ndipatri.arduinoButton.services.BluetoothMonitoringService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAbsSpinner;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.ActivityController;

import java.awt.image.RasterOp;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class ButtonDetailsDialogFragmentTest {

    private static final String TAG = ButtonDetailsDialogFragmentTest.class.getCanonicalName();

    protected MainControllerActivity activity;

    protected BluetoothMonitoringService monitoringService;

    @Before
    public void setUp() throws Exception {
        ActivityController controller = Robolectric.buildActivity(MainControllerActivity.class).create().start();
        activity = (MainControllerActivity) controller.get();

        BluetoothProviderTestImpl bluetoothProviderTestImpl = (BluetoothProviderTestImpl) activity.getBluetoothProvider();
        bluetoothProviderTestImpl.setIsBluetoothEnabled(true);
        bluetoothProviderTestImpl.setIsBluetoothSupported(true);

        controller.resume().visible().get();

        TestUtils.registerOrmLiteProvider();
        TestUtils.resetORMTable();

        monitoringService = TestUtils.startButtonMonitoringService(false); // should run in foreground

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
        TestUtils.startDialogFragment(activity, buttonDetailsDialogFragment);

        Robolectric.runUiThreadTasksIncludingDelayedTasks();

        buttonDetailsDialogFragment.getAutoModeSwitch().performClick();

        Spinner beaconSpinner = (Spinner) buttonDetailsDialogFragment.beaconSpinner;

        ShadowAbsSpinner shadowAbsSpinner = (ShadowAbsSpinner) shadowOf(beaconSpinner);

        shadowAbsSpinner.setSelection(0, false); // This should be none

        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();

        //ShadowAlertDialog shadowAlertDialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog());
        //shadowAlertDialog.clickOnItem(0);

        assertThat("shouldn't crash.", true);
    }
}
