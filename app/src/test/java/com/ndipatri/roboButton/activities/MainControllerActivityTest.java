package com.ndipatri.roboButton.activities;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.TestUtils;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProviderTestImpl;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ButtonStateChangeReport;
import com.ndipatri.roboButton.fragments.ButtonFragment;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.utils.BusProvider;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.ActivityController;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MainControllerActivityTest {

    MainControllerActivity activity;
    MonitoringService monitoringService;
    Button singleButton;

    @Before
    public void setup() {

        Context context = RBApplication.getInstance().getApplicationContext();

        ActivityController controller = Robolectric.buildActivity(MainControllerActivity.class).create().start();
        activity = (MainControllerActivity) controller.get();

        BluetoothProviderTestImpl bluetoothProviderTestImpl = (BluetoothProviderTestImpl) activity.getBluetoothProvider();
        bluetoothProviderTestImpl.setIsBluetoothEnabled(true);
        bluetoothProviderTestImpl.setIsBluetoothSupported(true);

        singleButton = new Button();
        singleButton.setId("aa:bb:cc:dd:ee");

        controller.resume().visible().get();

        TestUtils.registerOrmLiteProvider();
        TestUtils.resetORMTable();

        monitoringService = TestUtils.startButtonMonitoringService(false); // should run in foreground

        // Enable Logging to stdout
        ShadowLog.stream = System.out;

        Robolectric.runUiThreadTasksIncludingDelayedTasks();
        Robolectric.runBackgroundTasks();
    }

    @Test
    public void testArduinoButtonFoundEventResultsInNewButtonFragment() {

        BusProvider.getInstance().post(new ButtonStateChangeReport(singleButton.getId(), ButtonState.ON));

        ButtonFragment fragment = (ButtonFragment) activity.getFragmentManager().findFragmentByTag(singleButton.getId());


        //assertThat("Can't find expected Fragment!", fragment != null);
    }

    @Test
    public void testOnCreate() throws Exception {
        Activity activity = Robolectric.buildActivity(MainControllerActivity.class).create().get();
        //assertThat("MainControllerActivity can't create!", activity != null);
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
        //assertThat("'mainViewGroup' does not exist!", mainViewGroup != null);
    }

    @Test
    public void testResumeActivity() {
        MainControllerActivity mockMainControllerActivity = mock(MainControllerActivity.class);

        BluetoothProvider mockBluetoothProvider = mock(BluetoothProvider.class);
        when(mockBluetoothProvider.isBluetoothEnabled()).thenReturn(false);

        mockMainControllerActivity.bluetoothProvider =  mockBluetoothProvider;
        doCallRealMethod().when(mockMainControllerActivity).resumeActivity();

        mockMainControllerActivity.resumeActivity();
        verify(mockMainControllerActivity, times(1)).registerWithOttoBus();
    }





    // NJD TODO - Need to write test around MenuItem (e.g. beaconFilterToggle)
}
