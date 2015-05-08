/*
package com.ndipatri.roboButton.dagger.providers;

import android.content.Context;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.TestUtils;
import com.ndipatri.roboButton.models.Region;
import com.ndipatri.roboButton.models.Button;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ButtonProviderTest {

    ButtonProvider buttonProvider;
    RegionProvider regionProvider;

    @Before
    public void setup() {

        Context context = RBApplication.getInstance().getApplicationContext();

        buttonProvider = new ButtonProvider(context);
        regionProvider = new RegionProvider(context);

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

        //MatcherAssert.assertThat("Button not saved properly", retrievedButton.equals(button));
    }

    @Test
    public void testButtonDelete() {

        Button button = new Button();
        button.setId("123");
        button.setName("mrButton");
        button.setAutoModeEnabled(true);

        buttonProvider.createOrUpdateButton(button);

        Button retrievedButton = buttonProvider.getButton("123");

        //MatcherAssert.assertThat("Button not saved properly", retrievedButton.equals(button));

        buttonProvider.delete(button);

        retrievedButton = buttonProvider.getButton("123");

        //MatcherAssert.assertThat("Button not deleted properly", retrievedButton == null);
    }

    @Test
    public void testBeaconButtonAssociation() {

        Button button = new Button();
        button.setId("123");
        button.setName("mrButton");
        button.setAutoModeEnabled(true);

        buttonProvider.createOrUpdateButton(button);

        Region region = new Region();
        region.setMinor(1);
        region.setMajor(2);
        region.setUuid("1234");
        region.setButton(button);

        regionProvider.createOrUpdateRegion(region);

        Region retrievedRegion = regionProvider.getRegion(1,2,"1234");

        //MatcherAssert.assertThat("Region not associated to Button properly.", retrievedRegion.getButton().getId().equals("123"));
    }
}

*/
