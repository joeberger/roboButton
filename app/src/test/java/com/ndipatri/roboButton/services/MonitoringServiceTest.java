package com.ndipatri.roboButton.services;


import android.content.Context;
import android.content.Intent;

import com.ndipatri.roboButton.BuildConfig;
import com.ndipatri.roboButton.RBApplication;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", constants = BuildConfig.class, emulateSdk = 19)
public class MonitoringServiceTest {

    @Test
    public void testMonitoringServiceStartedWithToggle() {

        MonitoringService monitoringService = new MonitoringService();
        monitoringService.onCreate();

        final Intent monitoringServiceIntent = new Intent();
        monitoringServiceIntent.putExtra(MonitoringService.SHOULD_TOGGLE_FLAG, true);
        monitoringService.onStartCommand(monitoringServiceIntent, -1, -1);


    }

}
