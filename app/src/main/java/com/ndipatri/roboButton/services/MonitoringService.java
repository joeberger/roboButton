package com.ndipatri.roboButton.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.dagger.RBModule;
import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.events.ButtonDiscoveryEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonStateChangeReport;
import com.ndipatri.roboButton.events.ButtonStateChangeRequest;
import com.ndipatri.roboButton.events.RegionFoundEvent;
import com.ndipatri.roboButton.events.RegionLostEvent;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.BusProvider;
import com.ndipatri.roboButton.utils.ButtonCommunicator;
import com.ndipatri.roboButton.utils.ButtonCommunicatorFactory;
import com.ndipatri.roboButton.utils.RegionUtils;
import com.squareup.otto.Bus;
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
    protected RegionProvider regionProvider;

    @Inject
    @Named(RBModule.ESTIMOTE_BEACONS)
    protected RegionDiscoveryProvider estimoteRegionDiscoveryProvider;

    @Inject
    @Named(RBModule.GELO_BEACONS)
    protected RegionDiscoveryProvider geloRegionDiscoveryProvider;

    @Inject
    protected ButtonProvider buttonProvider;

    @Inject
    @Named(RBModule.PURPLE_BUTTON)
    protected ButtonDiscoveryProvider purpleButtonDiscoveryProvider;

    @Inject
    @Named(RBModule.LIGHTBLUE_BUTTON)
    protected ButtonDiscoveryProvider lightBlueButtonDiscoveryProvider;

    @Inject
    protected BluetoothProvider bluetoothProvider;

    protected long buttonDiscoveryDurationMillis = -1;

    // Until we see a nearby beacon, this service does nothing...
    protected com.ndipatri.roboButton.models.Region nearbyRegion = null;

    // This is a nearby button
    Button nearbyButton = null;

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

        ((RBApplication) getApplication()).getGraph().inject(this);

        buttonDiscoveryDurationMillis = getResources().getInteger(R.integer.button_discovery_duration_millis);

        bus.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopRegionDiscovery();
        stopButtonDiscovery();

        bus.unregister(this);

        if (buttonCommunicator != null) {
            buttonCommunicator.shutdown();
        }
    }

    // Recall that this can be called multiple times during the lifetime of the app...
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (null == intent) {
            String source = null == intent ? "intent" : "action";
            Log.e(TAG, source + " was null, flags=" + flags + " bits=" + Integer.toBinaryString(flags));
            return START_STICKY;
        } else {
            if (intent.getBooleanExtra(SHOULD_TOGGLE_FLAG, false)) {
                if (buttonCommunicator != null) {
                    bus.post(new ButtonStateChangeRequest(buttonCommunicator.getButton().getId()));

                    return START_STICKY;
                }
            }
        }

        boolean newRunInBackground = intent.getBooleanExtra(RUN_IN_BACKGROUND, false);

        if (!runInBackground && newRunInBackground && buttonCommunicator != null) {
            sendNotification(buttonCommunicator.getButton().getId(), buttonCommunicator.getButtonState());
        }

        runInBackground = newRunInBackground;

        Log.d(TAG, "onStartCommand() (runInBackground='" + runInBackground + "').");

        if (buttonCommunicator == null) {
            startRegionDiscovery();
        }

        return Service.START_FLAG_REDELIVERY; // this ensure the service is restarted
    }

    @Subscribe
    public void onRegionFound(RegionFoundEvent regionFoundEvent) {

        nearbyRegion = regionFoundEvent.getRegion();
        regionProvider.createOrUpdateRegion(nearbyRegion);

        Log.d(TAG, "RegionFound: ('" + nearbyRegion + "'.)");

        if (nearbyButton == null) {
            stopRegionDiscovery();
            startButtonDiscovery(nearbyRegion);
        }
    }

    @Subscribe
    public void onRegionLost(RegionLostEvent regionLostEvent) {

        nearbyRegion = null;
        stopButtonDiscovery();

        if (nearbyButton != null) {
            nearbyButton = null;
            stopButtonCommunication();
        }
    }

    @Subscribe
    public void onButtonDiscovered(ButtonDiscoveryEvent buttonDiscoveryEvent) {

        if (buttonDiscoveryEvent.isSuccess()) {

            if (nearbyButton == null) {

                stopButtonDiscovery();

                nearbyButton = pairButtonWithRegion(buttonDiscoveryEvent.getButtonDevice(), buttonDiscoveryEvent.getButtonType());

                startButtonCommunication(nearbyButton);
            }
        }

        // We've stopped button discovery, so now we go back to monitoring for region changes...
        startDelayedRegionDiscover();
    }

    @Subscribe
    public void onButtonStateChangeReport(ButtonStateChangeReport buttonStateChangeReport) {
        if (runInBackground && lastNotifiedState != buttonStateChangeReport.buttonState) {
            sendNotification(buttonStateChangeReport.buttonId, buttonStateChangeReport.buttonState);
        }
    }

    @Subscribe
    public void onButtonLostEvent(ButtonLostEvent buttonLostEvent) {
        nearbyButton = null;
        buttonCommunicator = null;
        startRegionDiscovery();
    }

    protected void startButtonCommunication(Button nearbyButton) {
        buttonCommunicator = getButtonCommunicator(getApplicationContext(), nearbyButton);
    }

    public ButtonCommunicator getButtonCommunicator(final Context context, final Button nearbyButton) {
        return ButtonCommunicatorFactory.getButtonCommunicator(context, nearbyButton);
    }

    protected void stopButtonCommunication() {
        if (buttonCommunicator != null) {
            buttonCommunicator.shutdown();
            buttonCommunicator = null;
        }
    }

    protected void startDelayedRegionDiscover() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startRegionDiscovery();
            }
        }, buttonDiscoveryDurationMillis);
    }

    protected void startRegionDiscovery() {
        //estimoteRegionDiscoveryProvider.startRegionDiscovery(runInBackground);
        geloRegionDiscoveryProvider.startRegionDiscovery(runInBackground);
    }

    protected void stopRegionDiscovery() {
        //estimoteRegionDiscoveryProvider.stopRegionDiscovery();

        try {
            geloRegionDiscoveryProvider.stopRegionDiscovery();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to stop GELO discovery.");
        }
    }

    // This is a costly operation and should only be done when we have confidence button will
    // be found... (e.g. we've already detected a beacon)
    protected void startButtonDiscovery(com.ndipatri.roboButton.models.Region nearbyRegion) {
        if (RegionUtils.isLighBlueRegion(nearbyRegion)) {
            lightBlueButtonDiscoveryProvider.startButtonDiscovery();
        } else {
            purpleButtonDiscoveryProvider.startButtonDiscovery();
        }
    }

    private void stopButtonDiscovery() {
        purpleButtonDiscoveryProvider.stopButtonDiscovery();
        lightBlueButtonDiscoveryProvider.stopButtonDiscovery();
    }

    protected Button pairButtonWithRegion(BluetoothDevice device, ButtonType buttonType) {

        Button discoveredButton;

        Button persistedButton = buttonProvider.getButton(device.getAddress());

        if (persistedButton != null) {
            discoveredButton = persistedButton;
        } else {
            discoveredButton = new Button(device.getAddress(), device.getAddress(), true, buttonType);
        }
        discoveredButton.setBluetoothDevice(device);

        buttonProvider.createOrUpdateButton(discoveredButton);

        // we immediately pair this discovered button with our nearby region.. overwriting any
        // existing pairing.

        nearbyRegion.setName("Region for " + discoveredButton.getName());
        regionProvider.createOrUpdateRegion(nearbyRegion);

        Button button = buttonProvider.getButton(discoveredButton.getId());
        button.setRegion(nearbyRegion);
        nearbyRegion.setButton(button);

        buttonProvider.createOrUpdateButton(button);
        regionProvider.createOrUpdateRegion(nearbyRegion); // transitive persistence sucks in ormLite
        return discoveredButton;
    }

    public ButtonCommunicator getButtonCommunicator() {
        return buttonCommunicator;
    }

    protected void sendNotification(String buttonId, ButtonState buttonState) {

        Log.d(TAG, "Sending notification for state '" + buttonState + "'.");

        lastNotifiedState = buttonState;

        Button button = buttonProvider.getButton(buttonId);

        StringBuilder sbuf = new StringBuilder("Tap here to toggle '");
        sbuf.append(button.getName()).append("'.");

        Intent intent = new Intent(this, MonitoringService.class);
        intent.putExtra(SHOULD_TOGGLE_FLAG, true);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // NJD TODO - Could use 'notificationManager.cancel(NOTIFICATION_ID)' at some point for cleanup
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
        builder.setContentIntent(pendingIntent);
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
        contentView.setImageViewResource(R.id.image, buttonState.smallDrawableResourceId);
        contentView.setTextViewText(R.id.title, this.getString(R.string.robo_button));
        contentView.setTextViewText(R.id.detail, sbuf.toString());
        notification.contentView = contentView;

        notificationManager.notify(notifId, notification);
    }
}
