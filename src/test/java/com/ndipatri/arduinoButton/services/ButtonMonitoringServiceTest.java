package com.ndipatri.arduinoButton.services;

import android.content.Context;
import android.content.Intent;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.table.TableUtils;
import com.ndipatri.arduinoButton.ArduinoButtonApplication;
import com.ndipatri.arduinoButton.activities.MainControllerActivity;
import com.ndipatri.arduinoButton.database.OrmLiteDatabaseHelper;
import com.ndipatri.arduinoButton.models.Beacon;
import com.ndipatri.arduinoButton.utils.ActivityWatcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.sql.SQLException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class ButtonMonitoringServiceTest {

    ArduinoButtonApplication application;
    MainControllerActivity activity;

    @Before
    public void setup() {

        // Enable Logging to stdout
        ShadowLog.stream = System.out;

        registerOrmLiteProvider();
        resetORMTable();

        Context context = ArduinoButtonApplication.getInstance().getApplicationContext();
        activity = Robolectric.buildActivity(MainControllerActivity.class).create().get();
        application = ArduinoButtonApplication.getInstance();
    }

    @Test
    public void buttonMonitoringServiceStartedTest() {

        // ActivityWatcher doesn't work in Robolectric.. so we have to simulate that an activity is resumed.. and thus should
        // start the monitoring service in foreground...
        ActivityWatcher activityWatcher = application.getActivityWatcher();
        activityWatcher.onActivityResumed(activity);

        Intent startedIntent = shadowOf(activity).getNextStartedService();

        assertThat("ButtonMonitoringService should have been started.", startedIntent.getComponent().getClassName().equals(ButtonMonitoringService.class.getCanonicalName()));
    }

    @Test
    public void onStartCommand() {

        ButtonMonitoringService monitoringService = startButtonMonitoringService(false); // should run in foreground

        assertThat("ButtonMonitoringService should be running.", monitoringService.isRunning());
        assertThat("ButtonMonitoringService should have been started in foreground.", !monitoringService.isRunInBackground());

        ButtonMonitoringService.MessageHandler messageHandler = monitoringService.getBluetoothMessageHandler();

        assertThat("MessageHandler thread should have been started and waiting for message.", messageHandler.getLooper().getThread().getState() == Thread.State.WAITING);
        assertThat("MessageHandler thread should have a 'DISCOVER' message pending.", messageHandler.hasMessages(ButtonMonitoringService.DISCOVER_BUTTON_DEVICES));
        assertThat("Button discovery interval should be 4 seconds.", monitoringService.getButtonDiscoveryIntervalMillis() == 4000);
        assertThat("Button communications grace period should be 10 seconds.", monitoringService.getCommunicationsGracePeriodMillis() == 10000);
    }

    @Test
    public void discoverButtonDevices() {


    }

    public static ButtonMonitoringService startButtonMonitoringService(final boolean shouldRunInBackground) {

        ButtonMonitoringService buttonMonitoringService = new ButtonMonitoringService();
        buttonMonitoringService.onCreate();

        final Intent buttonDiscoveryServiceIntent = new Intent();
        buttonDiscoveryServiceIntent.putExtra(ButtonMonitoringService.RUN_IN_BACKGROUND,  shouldRunInBackground);
        buttonMonitoringService.onStartCommand(buttonDiscoveryServiceIntent, -1, -1);

        return buttonMonitoringService;
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
