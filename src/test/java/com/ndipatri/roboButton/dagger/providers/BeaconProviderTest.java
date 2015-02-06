package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.TestUtils;
import com.ndipatri.roboButton.models.Beacon;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BeaconProviderTest {

    BeaconProvider beaconProvider;

    @Before
    public void setup() {

        Context context = RBApplication.getInstance().getApplicationContext();

        beaconProvider = new BeaconProvider(context);

        TestUtils.registerOrmLiteProvider();
        TestUtils.resetORMTable();
    }

    @Test
    public void testBeaconSave() {

        Beacon beacon = new Beacon();
        beacon.setName("aBeacon");
        beacon.setMacAddress("aa:bb:cc:dd");

        beaconProvider.createOrUpdateBeacon(beacon);

        Beacon retrievedBeacon = beaconProvider.getBeacon("aa:bb:cc:dd");

        MatcherAssert.assertThat("Beacon not saved properly", retrievedBeacon.equals(beacon));
    }

    @Test
    public void testBeaconDelete() {

        Beacon beacon = new Beacon();
        beacon.setName("aBeacon");
        beacon.setMacAddress("aa:bb:cc:dd");

        beaconProvider.createOrUpdateBeacon(beacon);

        Beacon retrievedBeacon = beaconProvider.getBeacon("aa:bb:cc:dd");

        MatcherAssert.assertThat("Beacon not saved properly", retrievedBeacon.equals(beacon));

        beaconProvider.delete(beacon);

        retrievedBeacon = beaconProvider.getBeacon("aa:bb:cc:dd");

        MatcherAssert.assertThat("Beacon not deleted properly", retrievedBeacon == null);
    }
}
