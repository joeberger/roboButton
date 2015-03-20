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
import com.ndipatri.roboButton.activities.MainControllerActivity;
import com.ndipatri.roboButton.dagger.annotations.Named;
import com.ndipatri.roboButton.dagger.modules.ABModule;
import com.ndipatri.roboButton.dagger.providers.RegionDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ButtonConnectedEvent;
import com.ndipatri.roboButton.events.ButtonDiscoveryEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.RegionFoundEvent;
import com.ndipatri.roboButton.events.RegionLostEvent;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

/**
 * This service constantly monitors for a nearby beacon Region.  If it finds one, it then tries to discover a nearby
 * Button.  If it finds one, it then spawns a ButtonCommunicator object which is responsible for communicating with
 * the Button.
 * <p/>
 * This service periodically checks to make sure this ButtonCommunicator is lively - communicating properly to the Button.
 * If not, it will destroy the ButtonCommunicator.
 * <p/>
 * Once the ButtonCommunicator exists, this service does not search for any more Buttons,
 * but it continues to scan for nearby Beacons to detect when we've left a beacon Region.
 * <p/>
 * Upon leaving a Region, an existing ButtonCommunicator is destroyed, thus ending communications with the Button.
 */
public class MonitoringService extends Service {

    public static final String TAG = MonitoringService.class.getCanonicalName();

    public static final String RUN_IN_BACKGROUND = "run_in_background";

    protected boolean runInBackground = false;

    @Inject
    protected RegionProvider regionProvider;
    @Inject
    @Named(ABModule.ESTIMOTE_BEACONS)
    protected RegionDiscoveryProvider estimoteRegionDiscoveryProvider;
    @Inject
    @Named(ABModule.GELO_BEACONS)
    protected RegionDiscoveryProvider geloRegionDiscoveryProvider;

    @Inject
    protected ButtonProvider buttonProvider;
    @Inject
    protected ButtonDiscoveryProvider buttonDiscoveryProvider;

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

        ((RBApplication) getApplication()).registerForDependencyInjection(this);

        buttonDiscoveryDurationMillis = getResources().getInteger(R.integer.button_discovery_duration_millis);

        BusProvider.getInstance().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopRegionDiscovery();
        stopButtonDiscovery();

        BusProvider.getInstance().unregister(this);

        if (buttonCommunicator != null) {
            buttonCommunicator.stop();
        }
    }

    // Recall that this can be called multiple times during the lifetime of the app...
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (null == intent) {
            String source = null == intent ? "intent" : "action";
            Log.e(TAG, source + " was null, flags=" + flags + " bits=" + Integer.toBinaryString(flags));
            return START_STICKY;
        }

        boolean newRunInBackground = intent.getBooleanExtra(RUN_IN_BACKGROUND, false);

        if (!runInBackground && newRunInBackground && buttonCommunicator != null) {
            sendNotification(buttonCommunicator.getButton().getId(), buttonCommunicator.getButtonState());
        }

        runInBackground = newRunInBackground;

        Log.d(TAG, "onStartCommand() (runInBackground='" + runInBackground + "').");

        if (buttonCommunicator == null) {
            startRegionDiscovery();
        } else {
            buttonCommunicator.setInBackground(runInBackground);
        }

        return Service.START_FLAG_REDELIVERY; // this ensure the service is restarted
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

    protected void forgetLostButton(String buttonId) {
        Log.d(TAG, "Forgetting lost button '" + buttonId + "'.");

        if (buttonCommunicator != null) {
            buttonCommunicator.shutdown();
        }

        nearbyButton = null;
        buttonCommunicator = null;

        if (runInBackground) {
            sendNotification(buttonId, ButtonState.DISCONNECTED);
        }
    }

    @Subscribe
    public void onRegionFound(RegionFoundEvent regionFoundEvent) {

        nearbyRegion = regionFoundEvent.getRegion();
        regionProvider.createOrUpdateRegion(nearbyRegion);

        Log.d(TAG, "RegionFound: ('" + nearbyRegion + "'.)");

        if (nearbyButton == null) {
            stopRegionDiscovery();
            startButtonDiscovery();
        }
    }

    @Subscribe
    public void onRegionLost(RegionLostEvent regionLostEvent) {

        nearbyRegion = null;
        stopButtonDiscovery();

        if (nearbyButton != null) {
            forgetLostButton(nearbyButton.getId());
        }
    }

    // This is a costly operation and should only be done when we have confidence button will
    // be found... (e.g. we've already detected a beacon)
    protected void startButtonDiscovery() {
        buttonDiscoveryProvider.startButtonDiscovery();
    }

    private void stopButtonDiscovery() {
        buttonDiscoveryProvider.stopButtonDiscovery();
    }

    @Subscribe
    public void onButtonDiscovered(ButtonDiscoveryEvent buttonDiscoveryEvent) {

        if (buttonDiscoveryEvent.isSuccess()) {

            if (nearbyButton == null) {

                stopButtonDiscovery();

                BluetoothDevice device = buttonDiscoveryEvent.getButtonDevice();

                Button discoveredButton;

                Button persistedButton = buttonProvider.getButton(device.getAddress());
                if (persistedButton != null) {
                    discoveredButton = persistedButton;
                } else {
                    discoveredButton = new Button(device.getAddress(), device.getAddress(), true);
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

                nearbyButton = discoveredButton;

                buttonCommunicator = new ButtonCommunicator(getApplicationContext(), nearbyButton);
            }
        }

        // We've stopped button discovery, so now we go back to monitoring for region changes...
        startDelayedRegionDiscover();
    }

    @Subscribe
    public void onButtonConnectedEvent(ButtonConnectedEvent buttonConnectedEvent) {
        if (buttonCommunicator != null) {
            ButtonState currentButtonState = buttonCommunicator.getButtonState();
            if (runInBackground && lastNotifiedState != currentButtonState) {
                sendNotification(buttonConnectedEvent.button.getId(), currentButtonState);
            }
        }
    }

    @Subscribe
    public void onButtonLostEvent(ButtonLostEvent buttonLostEvent) {
        forgetLostButton(buttonLostEvent.buttonId);

        startRegionDiscovery();
    }

    public ButtonCommunicator getButtonCommunicator() {
        return buttonCommunicator;
    }

    protected void sendNotification(String buttonId, ButtonState buttonState) {

        Log.d(TAG, "Sending notification for state '" + buttonState + "'.");

        lastNotifiedState = buttonState;
        String tickerText = this.getString(R.string.new_robo_button);
        Button button = buttonProvider.getButton(buttonId);
        if (button != null) {
            tickerText = " '" + button.getName() + "'";
        }
        tickerText += " " + this.getString(buttonState.descriptionResId);

        Intent intent = new Intent(this, MainControllerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

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
        contentView.setTextViewText(R.id.detail, tickerText);
        notification.contentView = contentView;

        notificationManager.notify(notifId, notification);
    }
}
