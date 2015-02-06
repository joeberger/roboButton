package com.ndipatri.roboButton.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Region;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.BeaconDistanceListener;
import com.ndipatri.roboButton.ButtonDiscoveryListener;
import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.activities.MainControllerActivity;
import com.ndipatri.roboButton.dagger.providers.BeaconProvider;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ButtonFoundEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.BusProvider;

import javax.inject.Inject;

/**
 *  This service constantly monitors for nearby Beacons.  If it finds one, it then try to discover a nearby
 *  Button.  If it finds one, it then spawns a ButtonMonitor object.  Once the ButtonMonitor exists, this service
 *  does not search for any more Buttons.
 *
 *  This service then periodically checks to make sure this ButtonMonitor is lively - connecting properly to the Button.
 *  If not, it will destroy the ButtonMonitor.
 *
 *  This service is also responsible for posting Otto events when a ButtonMonitor successfully communicates with
 *  a Button (ABFound) and when it loses this communication (ABLost).
 */
public class MonitoringService extends Service {

    public static final String TAG = MonitoringService.class.getCanonicalName();

    public static final String RUN_IN_BACKGROUND = "run_in_background";

    protected boolean runInBackground = false;

    @Inject protected BeaconProvider beaconProvider;

    @Inject protected ButtonProvider buttonProvider;

    @Inject protected BluetoothProvider bluetoothProvider;

    protected long monitorIntervalPollIntervalMillis = -1;

    protected long buttonDiscoveryDurationMillis = -1;

    protected int timeMultiplier = 1;

    // This handler is used to interace with the background 'monitor' thread which is used to
    // do all 'work' in this service.
    private Handler monitorHandler;

    private boolean running = false;

    // Until we see a nearby beacon, this service does nothing...
    com.ndipatri.roboButton.models.Beacon nearbyBeacon = null;

    // This is a nearby button
    Button nearbyButton = null;

    // This is the monitor associated with the nearby button
    ButtonMonitor buttonMonitor = null;

    private int beaconDetectionThresholdMeters = -1;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ((RBApplication)getApplication()).registerForDependencyInjection(this);

        monitorRegisteredBeacons(bluetoothProvider);

        // Thread for communicating with BT services.  A different thread is used to actually communicate with buttons themsevles.
        HandlerThread messageProcessingThread = new HandlerThread("Discovery_BluetoothCommunicationThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        messageProcessingThread.start();

        // Connect up our background thread's looper with our message processing handler.
        monitorHandler = new Handler(messageProcessingThread.getLooper());

        beaconDetectionThresholdMeters = getResources().getInteger(R.integer.beacon_detection_threshold_meters);
        monitorIntervalPollIntervalMillis = getResources().getInteger(R.integer.monitor_service_poll_interval_millis);
        buttonDiscoveryDurationMillis = getResources().getInteger(R.integer.button_discovery_duration_millis);

        BusProvider.getInstance().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            bluetoothProvider.stopBTMonitoring();
            bluetoothProvider.stopButtonDiscovery();
        } catch (RemoteException e) {
            Log.d(TAG, "Error while stopping ranging and discovery", e);
        }

        BusProvider.getInstance().unregister(this);
        running = false;

        if (buttonMonitor != null) {
            buttonMonitor.stop();
        }
    }

    // Recall that this can be called multiple times during the lifetime of the app...
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (null == intent) {
            String source = null == intent ? "intent" : "action";
            Log.e (TAG, source + " was null, flags=" + flags + " bits=" + Integer.toBinaryString (flags));
            return START_STICKY;
        }

        boolean newRunInBackground = intent.getBooleanExtra(RUN_IN_BACKGROUND, false);

        if (!runInBackground && newRunInBackground && buttonMonitor != null) {
            sendABNotification(buttonMonitor.getButton().getId(), buttonMonitor.getButtonState());
        }

        runInBackground = newRunInBackground;

        Log.d(TAG, "onStartCommand() (runInBackground='" + runInBackground + "').");

        // When in the background, this service wakes up less often to do its thing...
        int newTimeMultiplier = runInBackground ? getResources().getInteger(R.integer.background_time_multiplier) : 1;
        if (newTimeMultiplier != timeMultiplier) {
            timeMultiplier = newTimeMultiplier;
            if (buttonMonitor != null) {
                buttonMonitor.setTimeMultiplier(timeMultiplier);
            }
        }

        if (!running) {

            scheduleImmediatePoll();

            running = true;
        }

        return Service.START_FLAG_REDELIVERY; // this ensure the service is restarted
    }

    private void scheduleImmediatePoll() {
        monitorHandler.post(servicePollRunnable);
    }

    private void scheduleDelayedPoll() {
        monitorHandler.postDelayed(servicePollRunnable, monitorIntervalPollIntervalMillis);
    }

    protected Runnable servicePollRunnable = new Runnable() {
        public void run() {

            Log.d(TAG, "Monitor awake...");

            if (nearbyButton == null) {
                if (getNearbyBeacon() != null) {
                    // we have a nearby beacon.. start looking for a button
                    Log.d(TAG, "Nearby beacon detected.. Trying to discover buttons...");
                    startButtonDiscovery();
                } else {
                    Log.d(TAG, "No nearby beacon or button found. Going back to sleep.");
                }

                scheduleDelayedPoll();
                return;
            } else {
                Log.d(TAG, "Nearby button found.  Stopping discovery.");
                stopButtonDiscovery();
            }

            String buttonId = nearbyButton.getId();

            if (buttonMonitor == null) {
                // This is a button with no monitor.. So start monitoring.
                buttonMonitor = new ButtonMonitor(getApplicationContext(), nearbyButton);
            } else {

                // We communicate with a button until that fails or until its associated beacon is gone..
                if (buttonMonitor.isCommunicating() && getNearbyBeacon() != null) {

                    // Level Trigger state notification, not edge triggered (e.g. we will send this active
                    // notification every poll interval that this button is communicating)..
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            BusProvider.getInstance().post(new ButtonFoundEvent(nearbyButton));
                        }
                    });

                    long timeOfLastCheck = SystemClock.uptimeMillis() - monitorIntervalPollIntervalMillis;
                    boolean hasStateChangedSinceLastCheck = timeOfLastCheck > 0 &&
                            buttonMonitor.getLastButtonStateChangeTimeMillis() > timeOfLastCheck;
                    if (runInBackground && hasStateChangedSinceLastCheck) {
                        sendABNotification(buttonId, buttonMonitor.getButtonState());
                    }
                } else {

                    Log.d(TAG, "Forgetting lost button '" + buttonId + "'.");

                    buttonMonitor.shutdown();

                    final String forgottonButtonId = nearbyButton.getId();

                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            BusProvider.getInstance().post(new ButtonLostEvent(forgottonButtonId));
                        }
                    });

                    nearbyButton = null;
                    buttonMonitor = null;

                    if (runInBackground) {
                        sendABNotification(buttonId, ButtonState.DISCONNECTED);
                    }
                }
            }

            scheduleDelayedPoll();
        }
    };

    // Before this service can do anything, it will wait for the close proximity of a Beacon to be confirmed.
    // This is a low cost BTLE operation and can run in perpetuity.
    protected void monitorRegisteredBeacons(final BluetoothProvider bluetoothProvider) {
        bluetoothProvider.startBeaconDiscovery(new BeaconDistanceListener() {

            @Override
            public void beaconDistanceUpdate(final Beacon estimoteBeacon, double distanceInMeters) {

                Toast.makeText(MonitoringService.this, "BeaconDistance: '" + distanceInMeters + "m'", Toast.LENGTH_SHORT).show();

                com.ndipatri.roboButton.models.Beacon beacon = new com.ndipatri.roboButton.models.Beacon(estimoteBeacon.getMacAddress(), estimoteBeacon.getName());
                beaconProvider.createOrUpdateBeacon(beacon);

                if (distanceInMeters < (double) beaconDetectionThresholdMeters) {
                    setNearbyBeacon(beacon);
                    String msg = "Beacon detected.";
                } else {
                    // not in range!
                    String msg = "Beacon lost.";
                    Log.d(TAG, msg + " ('" + beacon + "'.)");
                    setNearbyBeacon(null);
                }
            }

            @Override
            public void leftRegion(Region region) {

                if (region == bluetoothProvider.getMonitoredRegion()) {
                    // I'm guessing, we get this callback when there are NO more detected beacons in the given region
                    Log.d(TAG, "Left beacon region.");
                    setNearbyBeacon(null);
                }
            }
        });
    }

    // This is a costly operation and should only be done when we have confidence button will
    // be found... (e.g. we've already detected a beacon)
    protected void startButtonDiscovery() {

        bluetoothProvider.startButtonDiscovery(new ButtonDiscoveryListener() {
            @Override
            public void buttonDiscovered(Button nearbyCandidateButton) {

                // We only care about new nearby button notifications if we aren't already
                // processing a nearby button
                if (nearbyButton == null) {

                    // we immediately pair this discovered button with our nearby beacon.. overwriting any
                    // existing pairing.
                    com.ndipatri.roboButton.models.Beacon nearbyBeaconThreadSafe = getNearbyBeacon();
                    if (nearbyBeaconThreadSafe != null) {

                        nearbyBeaconThreadSafe.setName("Beacon for " + nearbyCandidateButton.getName());
                        beaconProvider.createOrUpdateBeacon(nearbyBeaconThreadSafe);

                        Button button = buttonProvider.getButton(nearbyCandidateButton.getId());
                        button.setBeacon(nearbyBeaconThreadSafe);
                        nearbyBeaconThreadSafe.setButton(button);

                        buttonProvider.createOrUpdateButton(button);
                        beaconProvider.createOrUpdateBeacon(nearbyBeaconThreadSafe); // transitive persistence sucks in ormLite

                        nearbyButton = nearbyCandidateButton;
                    }
                }
            }
        });
    }

    private void stopButtonDiscovery() {
        bluetoothProvider.stopButtonDiscovery();
    }

    public ButtonMonitor getButtonMonitor() {
        return buttonMonitor;
    }

    protected void sendABNotification(String buttonId, ButtonState buttonState) {

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

    protected void setNearbyBeacon(com.ndipatri.roboButton.models.Beacon nearbyBeacon) {
        // reference assignments are atomic
        this.nearbyBeacon = nearbyBeacon;
    }

    protected com.ndipatri.roboButton.models.Beacon getNearbyBeacon() {
        return this.nearbyBeacon;
    }
}
