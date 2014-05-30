package com.ndipatri.arduinoButton;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.ndipatri.arduinoButton.services.ButtonMonitoringService;
import com.ndipatri.arduinoButton.utils.ActivityWatcher;

public class ArduinoButtonApplication extends Application {

    private static final String TAG = ArduinoButtonApplication.class.getCanonicalName();

    private boolean inBackground = true;

    @Override
    public void onCreate() {
        super.onCreate();

        ActivityWatcher activityWatcher = new ActivityWatcher();
        activityWatcher.registerActivityWatcherCallbacks(new ActivityWatcher.ActivityWatcherCallbacks() {

            @Override
            public void onAnyActivityResumed(Class activityClass) {

                if (inBackground) {
                    inBackground = false;
                    onForeground();
                }
            }

            @Override
            public void onLastActivityPaused() {
                inBackground = true;
                onBackground();
            }
        });
        registerActivityLifecycleCallbacks(activityWatcher);
    }

    protected void onForeground() {
        Log.d(TAG, "Application foregrounded...");

        foregroundBluetoothMonitoringService();
    }

    protected void onBackground() {
        Log.d(TAG, "Application backgrounded...");

        backgroundBluetoothMonitoringService();
    }

    protected void backgroundBluetoothMonitoringService() {
        // We still want it to run for notification purposes.. but with much slower intervals to save power.
        final Intent buttonDiscoveryServiceIntent = new Intent(this, ButtonMonitoringService.class);
        buttonDiscoveryServiceIntent.putExtra(ButtonMonitoringService.SLEEP_STATE_TIME_MULTIPLIER, getResources().getInteger(R.integer.background_time_multiplier));
        startService(buttonDiscoveryServiceIntent);
    }

    protected void foregroundBluetoothMonitoringService() {
        // We still want it to run for notification purposes.. but with much slower intervals to save power.
        final Intent buttonDiscoveryServiceIntent = new Intent(this, ButtonMonitoringService.class);
        buttonDiscoveryServiceIntent.putExtra(ButtonMonitoringService.SLEEP_STATE_TIME_MULTIPLIER, 1);
        startService(buttonDiscoveryServiceIntent);
    }
}
