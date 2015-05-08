/*
package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.TestUtils;
import com.ndipatri.roboButton.models.Region;

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
    public void testRegionSave() {

        Region region = new Region();
        region.setMinor(1);
        region.setMajor(2);
        region.setUuid("1234");

        regionProvider.createOrUpdateRegion(region);

        Region retrievedRegion = regionProvider.getRegion(1, 2, "1234");

        //MatcherAssert.assertThat("Region not saved properly", retrievedRegion.equals(region));
    }

    @Test
    public void testRegionDelete() {

        Region region = new Region();
        region.setMinor(1);
        region.setMajor(2);
        region.setUuid("1234");

        regionProvider.createOrUpdateRegion(region);

        Region retrievedRegion = regionProvider.getRegion(1, 2, "1234");

        //MatcherAssert.assertThat("Beacon not saved properly", retrievedRegion.equals(region));

        regionProvider.delete(region);

        retrievedRegion = regionProvider.getRegion(1, 2, "1234");

        //MatcherAssert.assertThat("Beacon not deleted properly", retrievedRegion == null);
    }
}

*/
