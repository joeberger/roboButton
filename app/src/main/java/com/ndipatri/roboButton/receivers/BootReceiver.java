package com.ndipatri.roboButton.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ndipatri.roboButton.services.MonitoringService;

/**
 * Created by ndipatri on 2/6/15.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Intent buttonDiscoveryServiceIntent = new Intent(context, MonitoringService.class);
        buttonDiscoveryServiceIntent.putExtra(MonitoringService.RUN_IN_BACKGROUND, true);
        context.startService(buttonDiscoveryServiceIntent);
    }
}
