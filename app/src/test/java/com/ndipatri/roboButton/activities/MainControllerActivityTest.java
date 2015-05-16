package com.ndipatri.roboButton.activities;

import com.ndipatri.roboButton.BuildConfig;
import com.ndipatri.roboButton.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", constants = BuildConfig.class, emulateSdk = 19)
public class MainControllerActivityTest {

    @Test
    public void mainControllerActivityResumes() {
        ActivityController<MainControllerActivity> controller = Robolectric.buildActivity(MainControllerActivity.class);
        MainControllerActivity activity = controller.create().start().resume().visible().get();

        assertThat(activity.findViewById(R.id.mainViewGroup), notNullValue());
    }
}
