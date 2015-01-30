package com.ndipatri.arduinoButton.dagger.providers;

import android.content.Context;

import com.ndipatri.arduinoButton.ABApplication;
import com.ndipatri.arduinoButton.TestUtils;
import com.ndipatri.arduinoButton.models.Beacon;
import com.ndipatri.arduinoButton.models.Button;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ButtonProviderTest {

    ButtonProvider buttonProvider;
    BeaconProvider beaconProvider;

    @Before
    public void setup() {

        Context context = ABApplication.getInstance().getApplicationContext();

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
        button.setAutoModeEnabled(true);

        buttonProvider.createOrUpdateButton(button);

        Beacon beacon = new Beacon();
        beacon.setName("aBeacon");
        beacon.setMacAddress("aa:bb:cc:dd");
        beacon.setButton(button);

        beaconProvider.createOrUpdateBeacon(beacon);

        Beacon retrievedBeacon = beaconProvider.getBeacon("aa:bb:cc:dd");

        MatcherAssert.assertThat("Beacon not associated to Button properly.", retrievedBeacon.getButton().getId().equals("123"));
    }
}

