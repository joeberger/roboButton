package com.ndipatri.roboButton.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.impl.ButtonDiscoveryManager;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.daos.ButtonDao;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ButtonDiscoveryFinished;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.RegionFoundEvent;
import com.ndipatri.roboButton.events.RegionLostEvent;
import com.ndipatri.roboButton.events.MonitoringServiceDestroyedEvent;
import com.ndipatri.roboButton.events.ToggleButtonStateRequest;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.BusProvider;
import com.ndipatri.roboButton.utils.NotificationHelper;
import com.squareup.otto.Subscribe;

import java.util.List;

import javax.inject.Inject;

/**
 * This service constantly monitors for a nearby beacon Region.  If it finds one, it then tries to discover a nearby
 * Button.  If it finds one, it then spawns a ButtonCommunicator object which is responsible for communicating with
 * the Button.
 * <p/>
 * Once the ButtonCommunicator exists, this service does not search for any more Buttons,
 * but it continues to scan for nearby Beacons to detect when we've left a beacon Region.
 * <p/>
 * Upon leaving a Region, an existing ButtonCommunicator is destroyed, thus ending communications with the Button.
 * <p/>
 * The Service is purely driven by external events:
 */
public class MonitoringService extends Service {

    public static final String TAG = MonitoringService.class.getCanonicalName();

    public static final String RUN_IN_BACKGROUND = "run_in_background";

    protected boolean runInBackground = false;

    public static final String SHOULD_TOGGLE_FLAG = "should_toggle_flag";
    public static final String BUTTON_ID = "button_id";

    @Inject
    BusProvider bus;

    @Inject
    ButtonDao buttonDao;

    @Inject
    NotificationHelper notificationHelper;

    @Inject
    protected RegionDiscoveryProvider regionDiscoveryProvider;

    @Inject
    protected ButtonDiscoveryManager buttonDiscoveryManager;

    protected long beaconScanStartupDelayAfterButtonDiscoveryMillis = -1;

    public IBinder onBind(Intent intent) {
        return null;
    }

    // The last button state for which we sent a notification.
    ButtonState lastNotifiedState = null;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate()");

        ((RBApplication) getApplication()).getGraph().inject(this);

        beaconScanStartupDelayAfterButtonDiscoveryMillis = getResources().getInteger(R.integer.beacon_scan_startup_delay_after_button_discovery_millis);

        bus.register(this);

        // We need to reset the monitored state of all buttons...
        buttonDao.clearStateOfAllButtons();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy()");

        stopRegionDiscovery();
        stopButtonDiscovery();

        bus.post(new MonitoringServiceDestroyedEvent());
        bus.unregister(this);
    }

    // Recall that this can be called multiple times during the lifetime of the app...
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "Starting.");
        boolean newRunInBackground = false;

        if (intent != null) {
            if (intent.getBooleanExtra(SHOULD_TOGGLE_FLAG, false)) {
                String buttonId = intent.getStringExtra(MonitoringService.BUTTON_ID);
                Log.d(TAG, "Toggling.");
                bus.post(new ToggleButtonStateRequest(buttonId));

                return START_STICKY;
            }

            newRunInBackground = intent.getBooleanExtra(RUN_IN_BACKGROUND, false);
        }

        if (!newRunInBackground) {
            notificationHelper.clearAllNotifications();
        }

        runInBackground = newRunInBackground;

        Log.d(TAG, "onStartCommand() (runInBackground='" + runInBackground + "').");

        if (notCommunicatingWithButtons()) {
            startRegionDiscovery();
        }

        return START_STICKY;
    }

    protected void startRegionDiscovery() {
        regionDiscoveryProvider.startRegionDiscovery(runInBackground);
    }

    @Subscribe
    public void onRegionFound(RegionFoundEvent regionFoundEvent) {

        Log.d(TAG, "RegionFound: ('" + regionFoundEvent.getRegion().toString() + "'.)");

        if (notCommunicatingWithButtons()) {
            Log.d(TAG, ".. currently not talking to a button, so let's look for one!");
            stopRegionDiscovery();
            startButtonDiscovery();
        }
    }

    @Subscribe
    public void onRegionLost(RegionLostEvent regionLostEvent) {

        Log.d(TAG, "RegionLost: ('" + regionLostEvent.getRegion().toString() + "'.)");

        stopButtonDiscovery();
    }

    @Subscribe
    public void onButtonDiscoveryFinished(ButtonDiscoveryFinished buttonDiscoveryFinished) {

        // Button discovery has ended, so now we go back to monitoring for region changes...
        startDelayedRegionDiscover();
    }

    @Subscribe
    public void onButtonLostEvent(ButtonLostEvent buttonLostEvent){
        startRegionDiscovery();
    }

    protected void startDelayedRegionDiscover() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startRegionDiscovery();
            }
        }, beaconScanStartupDelayAfterButtonDiscoveryMillis);
    }


    protected void stopRegionDiscovery() {
        try {
            regionDiscoveryProvider.stopRegionDiscovery();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop GELO discovery.");
        }
    }

    protected void startButtonDiscovery() {
        buttonDiscoveryManager.startButtonDiscovery();
    }

    private void stopButtonDiscovery() {
        buttonDiscoveryManager.stopButtonDiscovery();
    }

    protected boolean notCommunicatingWithButtons() {
        List<Button> communicatingButtons = buttonDao.getCommunicatingButtons();
        return (communicatingButtons == null || communicatingButtons.isEmpty());
    }
}
