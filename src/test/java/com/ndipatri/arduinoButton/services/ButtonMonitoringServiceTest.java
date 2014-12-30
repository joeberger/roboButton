package com.ndipatri.arduinoButton.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.table.TableUtils;
import com.ndipatri.arduinoButton.ArduinoButtonApplication;
import com.ndipatri.arduinoButton.activities.MainControllerActivity;
import com.ndipatri.arduinoButton.dagger.providers.BeaconProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProviderImpl;
import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;
import com.ndipatri.arduinoButton.database.OrmLiteDatabaseHelper;
import com.ndipatri.arduinoButton.models.Beacon;
import com.ndipatri.arduinoButton.utils.ActivityWatcher;

import org.hamcrest.MatcherAssert;
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
        assertThat("ButtonMonitoringService should have been started in foreground.", startedIntent.getExtras().get(ButtonMonitoringService.RUN_IN_BACKGROUND).equals(false));
    }

    @Test
    public void buttonMonitoringServiceStartedTest() {

        next make sure the handler starts?? then do mockito testing 


    }

    public static ButtonMonitoringService startButtonMonitoringService() {

        ButtonMonitoringService buttonMonitoringService = new ButtonMonitoringService();
        buttonMonitoringService.onCreate();

        final Intent buttonDiscoveryServiceIntent = new Intent();
        buttonDiscoveryServiceIntent.putExtra(ButtonMonitoringService.RUN_IN_BACKGROUND,  false);
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
