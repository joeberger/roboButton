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
        startMonitoringService(true);
    }

    protected void foregroundBluetoothMonitoringService() {
        startMonitoringService(false);
    }

    protected void startMonitoringService(final boolean shouldBackground) {
        final Intent buttonDiscoveryServiceIntent = new Intent(this, ButtonMonitoringService.class);
        buttonDiscoveryServiceIntent.putExtra(ButtonMonitoringService.RUN_IN_BACKGROUND,  shouldBackground);
        startService(buttonDiscoveryServiceIntent);
    }
}
