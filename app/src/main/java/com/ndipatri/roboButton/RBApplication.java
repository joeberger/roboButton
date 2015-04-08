package com.ndipatri.roboButton;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.ndipatri.roboButton.events.ApplicationFocusChangeEvent;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.utils.ActivityWatcher;
import com.ndipatri.roboButton.utils.BusProvider;

public abstract class RBApplication extends Application {

    private static final String TAG = RBApplication.class.getCanonicalName();
    public static final String APPLICATION_PREFS = "RoboButton.prefs";

    private ActivityWatcher activityWatcher;

    // region localVars
    private boolean inBackground = true;

    private Graph graph;

    private static RBApplication instance = null;
    // endregion

    public RBApplication() {
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

        graph = getObjectGraph();
    }

    public ActivityWatcher getActivityWatcher() {
        return activityWatcher;
    }

    public abstract Graph getObjectGraph();

    public static RBApplication getInstance() {
        return instance;
    }

    protected void onForeground() {
        Log.d(TAG, "Application foregrounded...");

        foregroundBluetoothMonitoringService();
        postApplicationForegroundedEvent();
    }

    protected void foregroundBluetoothMonitoringService() {
        startMonitoringService(false);
    }

    protected void postApplicationForegroundedEvent() {
        BusProvider.getInstance().post(new ApplicationFocusChangeEvent(false));
    }

    protected void onBackground() {
        Log.d(TAG, "Application backgrounded...");

        backgroundBluetoothMonitoringService();
        postApplicationBackgroundedEvent();
    }

    protected void backgroundBluetoothMonitoringService() {
        startMonitoringService(true);
    }

    protected void postApplicationBackgroundedEvent() {
        BusProvider.getInstance().post(new ApplicationFocusChangeEvent(true));
    }

    protected void setPreference(final String key, final int value) {
        SharedPreferences preferences = getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE);
        preferences.edit().putInt(key, value).apply();
    }

    protected void setPreference(final String key, final boolean value) {
        SharedPreferences preferences = getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(key, value).apply();
    }

    protected void setPreference(final String key, final String value) {
        SharedPreferences preferences = getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE);
        preferences.edit().putString(key, value).apply();
    }

    protected String getStringPreference(final String key, final String defaultValue) {
        return getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE).getString(key, defaultValue);
    }

    protected boolean getBooleanPreference(final String key, final boolean defaultValue) {
        return getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE).getBoolean(key, defaultValue);
    }

    protected int getIntegerPreference(final String key, final int defaultValue) {
        return getSharedPreferences(APPLICATION_PREFS, Context.MODE_PRIVATE).getInt(key, defaultValue);
    }

    public boolean getAutoModeEnabledFlag() {
        return getBooleanPreference("AUTO_MODE_ENABLED_FLAG", getResources().getBoolean(R.bool.auto_mode_enabled_default));
    }

    public void setAutoModeEnabledFlag(final boolean autoModeEnabledFlag) {
        setPreference("AUTO_MODE_ENABLED_FLAG", autoModeEnabledFlag);
    }


    protected void startMonitoringService(final boolean shouldBackground) {

        // Don't even bother starting service if BT isn't running.. The main activity will try to convince user to
        // do otherwise, and if so, will start the service manually at that time.

        if (isBluetoothSupported() && isBluetoothEnabled()) {
            final Intent buttonDiscoveryServiceIntent = new Intent(this, MonitoringService.class);
            buttonDiscoveryServiceIntent.putExtra(MonitoringService.RUN_IN_BACKGROUND, shouldBackground);
            startService(buttonDiscoveryServiceIntent);
        }
    }

    public boolean isBluetoothSupported() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    public boolean isBluetoothEnabled() {
        boolean isEnabled = false;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            isEnabled = bluetoothAdapter.isEnabled();
        }

        return isEnabled;
    }

    public void setMockMode(boolean providesMocks) {
        graph = Graph.Initializer.init(this, providesMocks);
    }

    public Graph getGraph() {
        return graph;
    }
}
