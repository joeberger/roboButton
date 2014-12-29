package com.ndipatri.arduinoButton;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.ndipatri.arduinoButton.dagger.modules.RoboButtonModule;
import com.ndipatri.arduinoButton.services.ButtonMonitoringService;
import com.ndipatri.arduinoButton.utils.ActivityWatcher;

import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;

public abstract class ArduinoButtonApplication extends Application {

    private static final String TAG = ArduinoButtonApplication.class.getCanonicalName();

    // region localVars
    private boolean inBackground = true;

    private ObjectGraph graph;

    private static ArduinoButtonApplication instance = null;
    // endregion

    public ArduinoButtonApplication() {
        instance = this;
    }

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

        graph = ObjectGraph.create(getDependencyModules().toArray());
    }

    protected abstract List<? extends Object> getDependencyModules();

    public static ArduinoButtonApplication getInstance() {
        return instance;
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

    public void inject(Object object) {
        graph.inject(object);
    }
}
