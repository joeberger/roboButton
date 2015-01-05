package com.ndipatri.arduinoButton;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.table.TableUtils;
import com.ndipatri.arduinoButton.database.OrmLiteDatabaseHelper;
import com.ndipatri.arduinoButton.fragments.ButtonDetailsDialogFragment;
import com.ndipatri.arduinoButton.models.Beacon;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.services.BluetoothMonitoringService;

import java.sql.SQLException;

/**
 * Created by ndipatri on 1/3/15.
 */
public class TestUtils {

    public static void registerOrmLiteProvider() {
        OrmLiteDatabaseHelper
                helper = OpenHelperManager.getHelper(ArduinoButtonApplication.getInstance().getApplicationContext(),
                OrmLiteDatabaseHelper.class);
        helper.onCreate(helper.getWritableDatabase(), helper.getConnectionSource());
        helper.deleteDataFromAllTables();
        OpenHelperManager.releaseHelper();
    }

    public static void resetORMTable() {
        OrmLiteDatabaseHelper
                helper = OpenHelperManager.getHelper(ArduinoButtonApplication.getInstance().getApplicationContext(),
                OrmLiteDatabaseHelper.class);
        try {
            TableUtils.dropTable(helper.getConnectionSource(), Button.class, true);
            TableUtils.dropTable(helper.getConnectionSource(), Beacon.class, true);
            TableUtils.createTable(helper.getConnectionSource(), Button.class);
            TableUtils.createTable(helper.getConnectionSource(), Beacon.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        OpenHelperManager.releaseHelper();
    }

    public static void createOrUpdateBeacon(Beacon beacon) {
        OrmLiteDatabaseHelper
                helper = OpenHelperManager.getHelper(ArduinoButtonApplication.getInstance().getApplicationContext(),
                OrmLiteDatabaseHelper.class);
        helper.getBeaconDao().createOrUpdate(beacon);

        OpenHelperManager.releaseHelper();
    }

    public static void createOrUpdateButton(Button button) {
        OrmLiteDatabaseHelper
                helper = OpenHelperManager.getHelper(ArduinoButtonApplication.getInstance().getApplicationContext(),
                OrmLiteDatabaseHelper.class);
        helper.getButtonDao().createOrUpdate(button);

        OpenHelperManager.releaseHelper();
    }

    public static Fragment startFragment(Activity activity, Fragment fragment) {
        FragmentManager fragmentManager = activity.getFragmentManager();
        fragmentManager.beginTransaction()
                .add(fragment, null)
                .commit();
        return fragment;
    }

    public static void startDialogFragment(Activity activity, DialogFragment dialogFragment) {
        dialogFragment.show(activity.getFragmentManager().beginTransaction(), "dialog fragment");
    }

    public static BluetoothMonitoringService startButtonMonitoringService(final boolean shouldRunInBackground) {

        BluetoothMonitoringService bluetoothMonitoringService = new BluetoothMonitoringService();
        bluetoothMonitoringService.onCreate();

        final Intent buttonDiscoveryServiceIntent = new Intent();
        buttonDiscoveryServiceIntent.putExtra(BluetoothMonitoringService.RUN_IN_BACKGROUND, shouldRunInBackground);
        bluetoothMonitoringService.onStartCommand(buttonDiscoveryServiceIntent, -1, -1);

        return bluetoothMonitoringService;
    }
}
