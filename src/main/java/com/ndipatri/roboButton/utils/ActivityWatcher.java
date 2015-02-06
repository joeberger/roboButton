package com.ndipatri.roboButton.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;

import java.lang.ref.WeakReference;

public class ActivityWatcher implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = ActivityWatcher.class.getCanonicalName();

    private int activeActivities = 0;
    private ActivityWatcherCallbacks callbacks;

    private WeakReference<Activity> currentActivity;

    public interface ActivityWatcherCallbacks {
        public void onAnyActivityResumed(Class activityClass);
        public void onLastActivityPaused();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        activeActivities += 1;

        if (activeActivities > 0 && callbacks != null) {
            Log.d(TAG, "onActivityResumed()");
            currentActivity = new WeakReference<Activity>(activity);
            callbacks.onAnyActivityResumed(activity.getClass());
        }
    }

    public void registerActivityWatcherCallbacks(ActivityWatcherCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void unregisterActivityWatcherCallbacks() {
        this.callbacks = null;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        activeActivities -= 1;

        if(activeActivities < 1) {
            currentActivity = null;
            Log.d(TAG, "onActivityPaused()");

            // there's a brief time during transition from one activity to another
            // when the count is zero, so we wait 1 second to see if another activity is resumed
            new CountDownTimer(1000, 1000) {

                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    if(activeActivities < 1 && callbacks != null) {
                        Log.d(TAG, "declaringLastActivityPaused!");
                        callbacks.onLastActivityPaused();
                    }
                }
            }.start();
        }
    }

    public WeakReference<Activity> getCurrentActivityReference() {
        return currentActivity;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }
    @Override
    public void onActivityStarted(Activity activity) {
    }
    @Override
    public void onActivityStopped(Activity activity) {
    }
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }
    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}

