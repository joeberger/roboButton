package com.ndipatri.arduinoButton.dagger.providers;

import android.content.Context;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.table.TableUtils;
import com.ndipatri.arduinoButton.ArduinoButtonApplication;
import com.ndipatri.arduinoButton.TestUtils;
import com.ndipatri.arduinoButton.database.OrmLiteDatabaseHelper;
import com.ndipatri.arduinoButton.models.Beacon;
import com.ndipatri.arduinoButton.models.Button;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.sql.SQLException;

@RunWith(RobolectricTestRunner.class)
public class ButtonProviderTest {

    ButtonProvider buttonProvider;
    BeaconProvider beaconProvider;

    @Before
    public void setup() {

        Context context = ArduinoButtonApplication.getInstance().getApplicationContext();

        buttonProvider = new ButtonProvider(context);
        beaconProvider = new BeaconProvider(context);

        TestUtils.registerOrmLiteProvider();
        TestUtils.resetORMTable();
    }

    @Test
    public void testButtonSave() {

        Button button = new Button();
        button.setId("123");
        button.setName("mrButton");
        button.setIconFileName("/mnt/sdcard/file.png");
        button.setAutoModeEnabled(true);

        buttonProvider.createOrUpdateButton(button);

        Button retrievedButton = buttonProvider.getButton("123");

        MatcherAssert.assertThat("Button not saved properly", retrievedButton.equals(button));
    }

    @Test
    public void testButtonDelete() {

        Button button = new Button();
        button.setId("123");
        button.setName("mrButton");
        button.setIconFileName("/mnt/sdcard/file.png");
        button.setAutoModeEnabled(true);

        buttonProvider.createOrUpdateButton(button);

        Button retrievedButton = buttonProvider.getButton("123");

        MatcherAssert.assertThat("Button not saved properly", retrievedButton.equals(button));

        buttonProvider.delete(button);

        retrievedButton = buttonProvider.getButton("123");

        MatcherAssert.assertThat("Button not deleted properly", retrievedButton == null);
    }

    @Test
    public void testBeaconButtonAssociation() {

        Button button = new Button();
        button.setId("123");
        button.setName("mrButton");
        button.setIconFileName("/mnt/sdcard/file.png");
        button.setAutoModeEnabled(true);

        buttonProvider.createOrUpdateButton(button);

        Beacon beacon = new Beacon();
        beacon.setName("aBeacon");
        beacon.setMajor(1);
        beacon.setMinor(2);
        beacon.setMacAddress("aa:bb:cc:dd");
        beacon.setButton(button);

        beaconProvider.createOrUpdateBeacon(beacon);

        Beacon retrievedBeacon = beaconProvider.getBeacon("aa:bb:cc:dd");

        MatcherAssert.assertThat("Beacon not associated to Button properly.", retrievedBeacon.getButton().getId().equals("123"));

        // NJD TODO - Testing that a Button has a set of beacons doesn't seem to work in Robolectric.. not sure why.. but in practice
        // this one-to-many association does work.
    }
}

