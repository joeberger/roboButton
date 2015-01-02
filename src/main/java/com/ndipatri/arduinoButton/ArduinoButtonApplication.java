package com.ndipatri.arduinoButton;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.ndipatri.arduinoButton.services.BluetoothMonitoringService;
import com.ndipatri.arduinoButton.utils.ActivityWatcher;

import java.util.List;

import dagger.ObjectGraph;

public abstract class ArduinoButtonApplication extends Application {

    private static final String TAG = ArduinoButtonApplication.class.getCanonicalName();
    public static final String APPLICATION_PREFS = "RoboButton.prefs";

    public static final String BEACON_FILTER_ON_PREF = "BEACON_FILTER_ON_PREF";

    private ActivityWatcher activityWatcher;

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

        activityWatcher = new ActivityWatcher();
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

    public ActivityWatcher getActivityWatcher() {
        return activityWatcher;
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

    public void setPreference(final String key, final int value) {
        SharedPreferences preferences = getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE);
        preferences.edit().putInt(key, value).apply();
    }

    public void setPreference(final String key, final boolean value) {
        SharedPreferences preferences = getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(key, value).apply();
    }

    public void setPreference(final String key, final String value) {
        SharedPreferences preferences = getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE);
        preferences.edit().putString(key, value).apply();
    }

    public String getStringPreference(final String key, final String defaultValue) {
        return getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE).getString(key, defaultValue);
    }

    public boolean getBooleanPreference(final String key, final boolean defaultValue) {
        return getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE).getBoolean(key, defaultValue);
    }

    public int getIntegerPreference(final String key, final int defaultValue) {
        return getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE).getInt(key, defaultValue);
    }

    protected void startMonitoringService(final boolean shouldBackground) {
        final Intent buttonDiscoveryServiceIntent = new Intent(this, BluetoothMonitoringService.class);
        buttonDiscoveryServiceIntent.putExtra(BluetoothMonitoringService.RUN_IN_BACKGROUND,  shouldBackground);
        startService(buttonDiscoveryServiceIntent);
    }

    public void inject(Object object) {
        graph.inject(object);
    }
}
