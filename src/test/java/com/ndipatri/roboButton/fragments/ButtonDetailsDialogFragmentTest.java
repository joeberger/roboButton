package com.ndipatri.roboButton.fragments;

import com.ndipatri.roboButton.TestUtils;
import com.ndipatri.roboButton.activities.MainControllerActivity;
import com.ndipatri.roboButton.dagger.providers.BluetoothProviderTestImpl;
import com.ndipatri.roboButton.services.MonitoringService;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.ActivityController;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class ButtonDetailsDialogFragmentTest {

    private static final String TAG = ButtonDetailsDialogFragmentTest.class.getCanonicalName();

    protected MainControllerActivity activity;

    protected MonitoringService monitoringService;

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
}
