package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.TestUtils;
import com.ndipatri.roboButton.models.Region;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RegionProviderTest {

    RegionProvider regionProvider;

    @Before
    public void setup() {

        Context context = RBApplication.getInstance().getApplicationContext();

        regionProvider = new RegionProvider(context);

        TestUtils.registerOrmLiteProvider();
        TestUtils.resetORMTable();
    }

    @Test
    public void testBeaconSave() {

        Region region = new Region();
        region.setName("aBeacon");
        region.setMacAddress("aa:bb:cc:dd");

        regionProvider.createOrUpdateBeacon(region);

        Region retrievedRegion = regionProvider.getBeacon("aa:bb:cc:dd");

        MatcherAssert.assertThat("Beacon not saved properly", retrievedRegion.equals(region));
    }

    @Test
    public void testBeaconDelete() {

        Region region = new Region();
        region.setName("aBeacon");
        region.setMacAddress("aa:bb:cc:dd");

        regionProvider.createOrUpdateBeacon(region);

        Region retrievedRegion = regionProvider.getBeacon("aa:bb:cc:dd");

        MatcherAssert.assertThat("Beacon not saved properly", retrievedRegion.equals(region));

        regionProvider.delete(region);

        retrievedRegion = regionProvider.getBeacon("aa:bb:cc:dd");

        MatcherAssert.assertThat("Beacon not deleted properly", retrievedRegion == null);
    }
}

