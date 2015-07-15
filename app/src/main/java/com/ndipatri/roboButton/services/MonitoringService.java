package com.ndipatri.roboButton.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.activities.MainActivity;
import com.ndipatri.roboButton.dagger.RBModule;
import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.ButtonCommunicator;
import com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces.ButtonCommunicatorFactory;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.daos.ButtonDao;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.events.ButtonDiscoveryEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonStateChangeRequest;
import com.ndipatri.roboButton.events.ButtonUpdatedEvent;
import com.ndipatri.roboButton.events.RegionFoundEvent;
import com.ndipatri.roboButton.events.RegionLostEvent;
import com.ndipatri.roboButton.utils.BusProvider;
import com.ndipatri.roboButton.utils.RegionUtils;
import com.squareup.otto.Subscribe;

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
 *
 * The Service is purely driven by external events:
 */
public class MonitoringService extends Service {

    public static final String TAG = MonitoringService.class.getCanonicalName();

    public static final String RUN_IN_BACKGROUND = "run_in_background";

    protected boolean runInBackground = false;

    public static final String SHOULD_TOGGLE_FLAG = "should_toggle_flag";

    @Inject
    BusProvider bus;

    @Inject
    ButtonDao buttonDao;

    @Inject
    protected RegionDiscoveryProvider regionDiscoveryProvider;

    @Inject
    @Named(RBModule.LIGHTBLUE_BUTTON)
    protected ButtonCommunicatorFactory lightBlueButtonCommunicatorFactory;
    @Inject
    @Named(RBModule.LIGHTBLUE_BUTTON)
    protected ButtonDiscoveryProvider lightBlueButtonDiscoveryProvider;

    @Inject
    @Named(RBModule.PURPLE_BUTTON)
    protected ButtonCommunicatorFactory purpleButtonCommunicatorFactory;
    @Inject
    @Named(RBModule.PURPLE_BUTTON)
    protected ButtonDiscoveryProvider purpleButtonDiscoveryProvider;

    protected long beaconScanStartupDelayAfterButtonDiscoveryMillis = -1;

    // Until we see a nearby beacon, this service does nothing...
    protected com.ndipatri.roboButton.models.Region nearbyRegion = null;

    // This is the monitor associated with the nearby button
    ButtonCommunicator buttonCommunicator = null;

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

        // We need to reset the monitored state of all buttons...
        buttonDao.clearStateOfAllButtons();

        Log.d(TAG, "Connected button: '" + buttonDao.getConnectedButton() + "'.");

        registerForScreenWakeBroadcast();

        bus.register(this);
    }

    private BroadcastReceiver screenWakeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive()");
            if (buttonCommunicator != null) {
                sendNotification(buttonCommunicator.getButton().getId(), buttonCommunicator.getButton().getState());
            }
        }
    };

    private void registerForScreenWakeBroadcast() {
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenWakeReceiver, screenStateFilter);
    }

    private void unRegisterForScreenWakeBroadcast() {
        unregisterReceiver(screenWakeReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy()");

        stopRegionDiscovery();
        stopButtonDiscovery();

        bus.unregister(this);

        if (buttonCommunicator != null) {
            buttonCommunicator.shutdown();
        }

        unRegisterForScreenWakeBroadcast();
    }

    // Recall that this can be called multiple times during the lifetime of the app...
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "Starting.");
        boolean newRunInBackground = false;

        if (intent != null) {
            if (intent.getBooleanExtra(SHOULD_TOGGLE_FLAG, false)) {
                if (buttonCommunicator != null) {
                    Log.d(TAG, "Toggling.");
                    bus.post(new ButtonStateChangeRequest(buttonCommunicator.getButton().getId()));

                    return START_STICKY;
                }
            }

            newRunInBackground = intent.getBooleanExtra(RUN_IN_BACKGROUND, false);
        }

        if (appBackgroundedWhileCommunicating(newRunInBackground)) {
            sendNotification(buttonCommunicator.getButton().getId(), buttonCommunicator.getButton().getState());
        } else
        if (!newRunInBackground) {
            clearNotification();
        }

        runInBackground = newRunInBackground;

        Log.d(TAG, "onStartCommand() (runInBackground='" + runInBackground + "').");

        if (buttonCommunicator == null) {
            startRegionDiscovery();
        }

        return START_STICKY;
    }

    protected boolean appBackgroundedWhileCommunicating(boolean newRunInBackground) {
        return !runInBackground && newRunInBackground && buttonCommunicator != null;
    }

    @Subscribe
    public void onRegionFound(RegionFoundEvent regionFoundEvent) {

        Log.d(TAG, "RegionFound: ('" + regionFoundEvent.getRegion().toString() + "'.)");

        nearbyRegion = regionFoundEvent.getRegion();

        if (buttonCommunicator == null) {
            Log.d(TAG, ".. currently not talking to a button, so let's look for one!");
            stopRegionDiscovery();
            startButtonDiscovery(nearbyRegion);
        }
    }

    @Subscribe
    public void onRegionLost(RegionLostEvent regionLostEvent) {

        Log.d(TAG, "RegionLost: ('" + regionLostEvent.getRegion().toString() + "'.)");

        nearbyRegion = null;
        stopButtonDiscovery();
        clearNotification();

        if (buttonCommunicator != null) {
            stopButtonCommunication();
        }
    }

    @Subscribe
    public void onButtonDiscovered(ButtonDiscoveryEvent buttonDiscoveryEvent) {

        if (buttonDiscoveryEvent.isSuccess()) {
            if (buttonCommunicator == null) {
                buttonCommunicator = getButtonCommunicator(this, buttonDiscoveryEvent.getDeviceAddress(), buttonDiscoveryEvent.getButtonType(), buttonDiscoveryEvent.getDevice());
            }
        }

        // Button discovery has ended, so now we go back to monitoring for region changes...
        startDelayedRegionDiscover();
    }

    public ButtonCommunicator getButtonCommunicator(final Context context, final String buttonId, final ButtonType type, final BluetoothDevice device) {

        switch(type) {
            case PURPLE_BUTTON:
                return purpleButtonCommunicatorFactory.getButtonCommunicator(context, device, buttonId);
            case LIGHTBLUE_BUTTON:
                return lightBlueButtonCommunicatorFactory.getButtonCommunicator(context, device, buttonId);
        }

        return null;
    }

    protected void stopButtonCommunication() {
        if (buttonCommunicator != null) {
            buttonCommunicator.shutdown();
            buttonCommunicator = null;
        }
    }

    @Subscribe
    public void onButtonUpdatedEvent(ButtonUpdatedEvent event) {
        if (buttonCommunicator != null && runInBackground && lastNotifiedState != buttonCommunicator.getButton().getState()) {
            sendNotification(event.getButtonId(), buttonCommunicator.getButton().getState());
        }
    }

    @Subscribe
    public void onButtonLostEvent(ButtonLostEvent buttonLostEvent) {
        buttonCommunicator = null;
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

    protected void startRegionDiscovery() {
        regionDiscoveryProvider.startRegionDiscovery(runInBackground);
    }

    protected void stopRegionDiscovery() {
        try {
            regionDiscoveryProvider.stopRegionDiscovery();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop GELO discovery.");
        }
    }

    // This is a costly operation and should only be done when we have confidence button will
    // be found... (e.g. we've already detected a beacon)
    //
    // TODO - For simplicity, assume a region type implies a button type...This is mostly so we don't have to
    // deal with multiple simulateneous button communications.  Which is problemetically particularly because we have
    // classic and BLE buttons
    protected void startButtonDiscovery(com.ndipatri.roboButton.models.Region nearbyRegion) {
        switch (nearbyRegion.getUuid())  {
            case RegionUtils.GELO_UUID:
                purpleButtonDiscoveryProvider.startButtonDiscovery();
                break;

            case RegionUtils.ESTIMOTE_UUID:
                lightBlueButtonDiscoveryProvider.startButtonDiscovery();
                break;
        }
    }

    private void stopButtonDiscovery() {
        purpleButtonDiscoveryProvider.stopButtonDiscovery();
        lightBlueButtonDiscoveryProvider.stopButtonDiscovery();
    }

    public ButtonCommunicator getButtonCommunicator() {
        return buttonCommunicator;
    }

    protected void clearNotification() {
        NotificationManager notificationManager = (NotificationManager) RBApplication.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1234);
    }

    protected void sendNotification(String buttonId, ButtonState buttonState) {

        Log.d(TAG, "Sending notification for state '" + buttonState + "'.");

        lastNotifiedState = buttonState;

        StringBuilder sbuf = new StringBuilder("Tap here to toggle '");
        sbuf.append(buttonCommunicator.getButton().getName()).append("'.");

        int notifId = 1234;

        NotificationManager notificationManager = (NotificationManager) RBApplication.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);

        // construct the Notification object.
        Notification.Builder builder = new Notification.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        //builder.setContentText(tickerText);
        builder.setSmallIcon(buttonState.smallDrawableResourceId); // this is what shows up in notification bar before you pull it down
        builder.setNumber(1234);
        builder.setAutoCancel(true);
        builder.setOnlyAlertOnce(true);
        ///builder.setContentIntent(pendingIntent);
        builder.setVibrate(new long[]{0,     // start immediately
                200,   // on
                1000,  // off
                200,   // on
                -1});  // no repeat

        // This provides sub-information in the notification. not using right now.
        //String notifyTitle = "Test app for ORMLite";
        //builder.addAction(R.drawable.green_button_small, notifyTitle, pendingIntent);

        Notification notification = builder.build();

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        contentView.setImageViewResource(R.id.buttonImageView, buttonState.smallDrawableResourceId);
        contentView.setTextViewText(R.id.detailTextView, sbuf.toString());
        notification.contentView = contentView;

        Intent serviceIntent = new Intent(this, MonitoringService.class);
        serviceIntent.putExtra(SHOULD_TOGGLE_FLAG, true);
        PendingIntent togglePendingIntent = PendingIntent.getService(this, 0, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        contentView.setOnClickPendingIntent(R.id.detailTextView, togglePendingIntent);
        contentView.setOnClickPendingIntent(R.id.buttonImageView, launchPendingIntent);

        notificationManager.notify(notifId, notification);
    }
}
