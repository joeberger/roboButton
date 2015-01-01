package com.ndipatri.arduinoButton.services;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.table.TableUtils;
import com.ndipatri.arduinoButton.ArduinoButtonApplication;
import com.ndipatri.arduinoButton.activities.MainControllerActivity;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProviderTestImpl;
import com.ndipatri.arduinoButton.database.OrmLiteDatabaseHelper;
import com.ndipatri.arduinoButton.models.Beacon;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.ActivityWatcher;
import com.ndipatri.arduinoButton.utils.ButtonMonitor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowSystemClock;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class BluetoothMonitoringServiceTest {

    ArduinoButtonApplication application;
    MainControllerActivity activity;

    @Inject
    BluetoothProvider bluetoothProvider;

    BluetoothMonitoringService monitoringService;

    @Before
    public void setup() {

        // Enable Logging to stdout
        ShadowLog.stream = System.out;

        registerOrmLiteProvider();
        resetORMTable();

        Context context = ArduinoButtonApplication.getInstance().getApplicationContext();
        activity = Robolectric.buildActivity(MainControllerActivity.class).create().get();
        application = ArduinoButtonApplication.getInstance();

        ArduinoButtonApplication.getInstance().inject(this);

        monitoringService = startButtonMonitoringService(false); // should run in foreground
    }

    @Test
    public void buttonMonitoringServiceStartedTest() {

        // ActivityWatcher doesn't work in Robolectric.. so we have to simulate that an activity is resumed.. and thus should
        // start the monitoring service in foreground...
        ActivityWatcher activityWatcher = application.getActivityWatcher();
        activityWatcher.onActivityResumed(activity);

        Intent startedIntent = shadowOf(activity).getNextStartedService();

        assertThat("ButtonMonitoringService should have been started.", startedIntent.getComponent().getClassName().equals(BluetoothMonitoringService.class.getCanonicalName()));
    }

    @Test
    public void onStartCommand() {

        assertThat("ButtonMonitoringService should be running.", monitoringService.isRunning());
        assertThat("ButtonMonitoringService should have been started in foreground.", !monitoringService.isRunInBackground());

        BluetoothMonitoringService.MessageHandler messageHandler = monitoringService.getBluetoothMessageHandler();

        assertThat("MessageHandler thread should have been started and waiting for message.", messageHandler.getLooper().getThread().getState() == Thread.State.WAITING);
        assertThat("MessageHandler thread should have a 'DISCOVER' message pending.", messageHandler.hasMessages(BluetoothMonitoringService.DISCOVER_BUTTON_DEVICES));

        assertThat("Button discovery interval should be 4 seconds.", monitoringService.getButtonDiscoveryIntervalMillis() == 4000);
        assertThat("Button communications grace period should be 10 seconds.", monitoringService.getCommunicationsGracePeriodMillis() == 10000);
    }

    @Test
    public void discoverButtonDevices_newButtonDiscovered() {

        // This would be a standard BT device that is one of our 'Button' arduino boards..
        Button availableButton = new Button("aa:bb:cc:dd:ee", "workCubicle", false, "/tmp/sdcard/image.png");

        Set<Button> availableButtons = new HashSet<Button>();
        availableButtons.add(availableButton);
        ((BluetoothProviderTestImpl)bluetoothProvider).setAvailableButtons(availableButtons);

        monitoringService.discoverButtonDevices();

        HashMap<String, ButtonMonitor> currentButtonMap = monitoringService.getCurrentButtonMap();

        assertThat("Should be one ButtonMonitor.", currentButtonMap.size() == 1);
        assertThat("ButtonMonitor should be configured for new Button.", currentButtonMap.get(availableButton.getId()).getButton().equals(availableButton));
        assertThat("ButtonMonitor should be running.", currentButtonMap.get(availableButton.getId()).isRunning());

        // The idea here is that after a 'discovering' pass, another 'discover' message should be scheduled...
        BluetoothMonitoringService.MessageHandler messageHandler = monitoringService.getBluetoothMessageHandler();
        assertThat("MessageHandler thread should have a 'DISCOVER' message pending.", messageHandler.hasMessages(BluetoothMonitoringService.DISCOVER_BUTTON_DEVICES));
    }

    @Test
    public void discoverButtonDevices_oldButtonNotCommunicating() {

        // This would be a standard BT device that is one of our 'Button' arduino boards..
        Button availableButton = new Button("aa:bb:cc:dd:ee", "workCubicle", false, "/tmp/sdcard/image.png");

        Set<Button> availableButtons = new HashSet<Button>();
        availableButtons.add(availableButton);
        ((BluetoothProviderTestImpl)bluetoothProvider).setAvailableButtons(availableButtons);

        monitoringService.discoverButtonDevices();

        need to figure out how to advance time.. this doesn't work...
        Robolectric.getUiThreadScheduler().advanceBy(50000);  // communications grace period is 10 seconds, so we want to make
                                                              // this button expire

        monitoringService.discoverButtonDevices();

        HashMap<String, ButtonMonitor> currentButtonMap = monitoringService.getCurrentButtonMap();

        assertThat("Should be no ButtonMonitors.", currentButtonMap.size() == 0);

    }

    private class OttoBusListener {

        public OttoBusListener(Object desiredEventObject)

    }
    //BusProvider.getInstance().post(new ArduinoButtonLostEvent(lostButtonMonitor.getButton()));

    public static BluetoothMonitoringService startButtonMonitoringService(final boolean shouldRunInBackground) {

        BluetoothMonitoringService bluetoothMonitoringService = new BluetoothMonitoringService();
        bluetoothMonitoringService.onCreate();

        final Intent buttonDiscoveryServiceIntent = new Intent();
        buttonDiscoveryServiceIntent.putExtra(BluetoothMonitoringService.RUN_IN_BACKGROUND,  shouldRunInBackground);
        bluetoothMonitoringService.onStartCommand(buttonDiscoveryServiceIntent, -1, -1);

        return bluetoothMonitoringService;
    }

    public static void registerOrmLiteProvider() {
        OrmLiteDatabaseHelper
                helper = OpenHelperManager.getHelper(ArduinoButtonApplication.getInstance().getApplicationContext(),
                OrmLiteDatabaseHelper.class);
        helper.onCreate(helper.getWritableDatabase(), helper.getConnectionSource());
        helper.deleteDataFromAllTables();
        OpenHelperManager.releaseHelper();
    }

    private void resetORMTable() {
        OrmLiteDatabaseHelper
                helper = OpenHelperManager.getHelper(ArduinoButtonApplication.getInstance().getApplicationContext(),
                OrmLiteDatabaseHelper.class);
        try {
            TableUtils.dropTable(helper.getConnectionSource(), Beacon.class, true);
            TableUtils.createTable(helper.getConnectionSource(), Beacon.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        OpenHelperManager.releaseHelper();
    }
}
