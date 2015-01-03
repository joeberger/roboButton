package com.ndipatri.arduinoButton.activities;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.table.TableUtils;
import com.ndipatri.arduinoButton.ArduinoButtonApplication;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.TestUtils;
import com.ndipatri.arduinoButton.dagger.providers.BeaconProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProviderImpl;
import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;
import com.ndipatri.arduinoButton.database.OrmLiteDatabaseHelper;
import com.ndipatri.arduinoButton.models.Beacon;
import com.ndipatri.arduinoButton.models.Button;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.sql.SQLException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class MainControllerActivityTest {

    BluetoothProvider bluetoothProvider;
    ButtonProvider buttonProvider;
    BeaconProvider beaconProvider;

    @Before
    public void setup() {

        Context context = ArduinoButtonApplication.getInstance().getApplicationContext();

        bluetoothProvider = new BluetoothProviderImpl(context);
        buttonProvider = new ButtonProvider(context);
        beaconProvider = new BeaconProvider(context);

        TestUtils.registerOrmLiteProvider();
        TestUtils.resetORMTable();
    }

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
        doCallRealMethod().when(mockMainControllerActivity).resumeActivity();

        mockMainControllerActivity.resumeActivity();
        verify(mockMainControllerActivity, times(1)).registerWithOttoBus();
    }

    // NJD TODO - Need to write test around MenuItem (e.g. beaconFilterToggle)
}
