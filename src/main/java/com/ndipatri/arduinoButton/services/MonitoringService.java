package com.ndipatri.arduinoButton.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
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
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.ndipatri.arduinoButton.ABApplication;
import com.ndipatri.arduinoButton.BeaconDistanceListener;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.activities.MainControllerActivity;
import com.ndipatri.arduinoButton.dagger.providers.BeaconProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProvider;
import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;
import com.ndipatri.arduinoButton.enums.ButtonState;
import com.ndipatri.arduinoButton.events.ABFoundEvent;
import com.ndipatri.arduinoButton.events.ABLostEvent;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.BusProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

/**
 * Constantly monitors all discovered Buttons and launches a ButtonMonitor for each... sends
 * Otto events when a button is discovered or becomes unresponsive... so this service only
 * discovers buttons, it doesn't control them...
 */
public class MonitoringService extends Service {

    public static final String TAG = MonitoringService.class.getCanonicalName();

    public static final String RUN_IN_BACKGROUND = "run_in_background";

    protected boolean runInBackground = false;

    @Inject protected BeaconProvider beaconProvider;

    @Inject protected ButtonProvider buttonProvider;

    @Inject protected BluetoothProvider bluetoothProvider;

    protected long buttonDiscoveryIntervalMillis = -1;

    protected int timeMultiplier = 1;

    // This handler is used to interace with the background 'monitor' thread which is used to
    // do all 'work' in this service.
    private Handler monitorHandler;

    private boolean running = false;

    // Keeping track of all currently monitored buttons.
    HashMap<String, ButtonMonitor> buttonMonitorMap = new HashMap<String, ButtonMonitor>();

    Set<com.ndipatri.arduinoButton.models.Beacon> nearbyBeacons = new HashSet<com.ndipatri.arduinoButton.models.Beacon>();

    private int beaconDetectionThresholdMeters = -1;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ((ABApplication)getApplication()).inject(this);

        monitorRegisteredBeacons(bluetoothProvider);

        beaconDetectionThresholdMeters = getResources().getInteger(R.integer.beacon_detection_threshold_meters);

        BusProvider.getInstance().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            bluetoothProvider.stopBTMonitoring();
        } catch (RemoteException e) {
            Log.d(TAG, "Error while stopping ranging", e);
        }

        BusProvider.getInstance().unregister(this);
        running = false;

        for (final ButtonMonitor thisMonitor : buttonMonitorMap.values()) {
            thisMonitor.stop();
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

        if (!runInBackground && newRunInBackground) {
            sendABNotificationForAllButtons();
        }

        runInBackground = newRunInBackground;

        Log.d(TAG, "onStartCommand() (runInBackground='" + runInBackground + "').");

        int newTimeMultiplier = runInBackground ? getResources().getInteger(R.integer.background_time_multiplier) : 1;

        if (newTimeMultiplier != timeMultiplier) {
            timeMultiplier = newTimeMultiplier;
            for (final ButtonMonitor buttonMonitor : buttonMonitorMap.values()) {
                buttonMonitor.setTimeMultiplier(timeMultiplier);
            }
        }

        if (!running) {

            // Thread for communicating with BT services.  A different thread is used to actually communicate with buttons themsevles.
            HandlerThread messageProcessingThread = new HandlerThread("Discovery_BluetoothCommunicationThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
            messageProcessingThread.start();

            // Connect up our background thread's looper with our message processing handler.
            monitorHandler = new Handler(messageProcessingThread.getLooper());

            buttonDiscoveryIntervalMillis = getResources().getInteger(R.integer.button_discovery_interval_millis);

            scheduleImmediateButtonDiscoveryMessage();

            running = true;
        }

        return Service.START_FLAG_REDELIVERY; // this ensure the service is restarted
    }

    private void scheduleImmediateButtonDiscoveryMessage() {
        monitorHandler.post(buttonMonitorDiscoveryRunnable);
    }

    private void scheduleButtonDiscoveryMessage() {
        monitorHandler.postDelayed(buttonMonitorDiscoveryRunnable, buttonDiscoveryIntervalMillis);
    }

    // Our goal here is to decide which bonded buttons require a ButtonMonitor.  ButtonMonitors are either created, destroyed,
    // or left alone here...
    protected Runnable buttonMonitorDiscoveryRunnable = new Runnable() {
        public void run() {

            // This is the only code that distinguishes 'proximal' buttons from all available buttons... all the
            // rest of the logic is identical (yaay)
            Set<Button> buttons;
            boolean beaconFilteringOn = ABApplication.getInstance().getBooleanPreference(ABApplication.BEACON_FILTER_ON_PREF, false);
            if (beaconFilteringOn) {
                buttons = getAllNearbyBeaconPairedButtons();
            } else {
                buttons = bluetoothProvider.getAllBondedButtons();
            }

            // To keep track of buttons that have gone incommunicado.

            // This starts by containing all candidate buttons.. and then as we decide we can communicate
            // with each button, we remove them.. What is left are all buttons not talking.
            final Set<String> lostButtonSet = new HashSet<String>(buttonMonitorMap.keySet());

            // These are all buttons with which we are still communicating.
            final HashMap<String, ButtonMonitor> newAndExistingButtonMap = new HashMap<String, ButtonMonitor>();

            for (final Button pairedButton : buttons) {

                String buttonId = pairedButton.getId();

                ButtonMonitor buttonMonitor = buttonMonitorMap.get(pairedButton.getId());

                if (buttonMonitor == null) {
                    // This is a paired button with no monitor.. So start monitoring.
                    newAndExistingButtonMap.put(buttonId, new ButtonMonitor(getApplicationContext(), pairedButton));
                } else {

                    if (buttonMonitor.isCommunicating()) {

                        // This button is actively communicating
                        lostButtonSet.remove(buttonId);
                        newAndExistingButtonMap.put(buttonId, buttonMonitor);

                        // Level Trigger state notification, not edge triggered (e.g. we will send this active
                        // notification every poll interval that this button is communicating)..
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                BusProvider.getInstance().post(new ABFoundEvent(pairedButton));
                            }
                        });

                        long timeOfLastCheck = SystemClock.uptimeMillis() - buttonDiscoveryIntervalMillis;
                        boolean hasStateChangedSinceLastCheck = timeOfLastCheck > 0 &&
                                                                buttonMonitor.getLastButtonStateChangeTimeMillis() > timeOfLastCheck;
                        if (runInBackground && hasStateChangedSinceLastCheck) {
                            sendABNotification(buttonId, buttonMonitor.getButtonState());
                        }
                    }
                }
            }

            // All buttons left in 'lostButtonSet' represent buttons that are no longer communicating
            for (final String lostButtonId : lostButtonSet) {
                Log.d(TAG, "Forgetting lost button '" + lostButtonId + "'.");

                final ButtonMonitor lostButtonMonitor = buttonMonitorMap.get(lostButtonId);

                lostButtonMonitor.shutdown();

                buttonMonitorMap.remove(lostButtonId);

                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        BusProvider.getInstance().post(new ABLostEvent(lostButtonMonitor.getButton()));
                    }
                });

                if (runInBackground) {
                    sendABNotification(lostButtonId, ButtonState.DISCONNECTED);
                }
            }

            buttonMonitorMap = newAndExistingButtonMap;

            scheduleButtonDiscoveryMessage();
        }
    };

    private Set<Button> getAllNearbyBeaconPairedButtons() {

        Set<Button> nearbyPairedButtons = new HashSet<Button>();

        // All buttons that have been paired with a beacon
        List<Button> pairedButtons = buttonProvider.getBeaconPairedButtons();

        // Now see which are in range...
        for (Button button : pairedButtons) {
            com.ndipatri.arduinoButton.models.Beacon nearbyBeacon = button.getBeacon();

            if (nearbyBeacons.contains(nearbyBeacon)) {

                // Now that we know this Button is relevant, we need to get the actual
                // nearby Button
                Button nearbyButton = bluetoothProvider.getBondedButton(button.getId());
                nearbyPairedButtons.add(nearbyButton);
            }
        }

        return nearbyPairedButtons;
    }

    protected String getButtonId(final BluetoothDevice bluetoothDevice) {
        return bluetoothDevice.getAddress();
    }

    protected void sendABNotificationForAllButtons() {
        for (Map.Entry<String, ButtonMonitor> entry : buttonMonitorMap.entrySet()) {
            sendABNotification(entry.getKey(), entry.getValue().getButtonState());
        }
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

        NotificationManager notificationManager = (NotificationManager) ABApplication.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);

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

    private void monitorRegisteredBeacons(final BluetoothProvider bluetoothProvider) {
        bluetoothProvider.startBTMonitoring(new BeaconDistanceListener() {

            @Override
            public void beaconDistanceUpdate(com.ndipatri.arduinoButton.models.Beacon beacon, double distanceInMeters) {

                if (distanceInMeters < (double)beaconDetectionThresholdMeters) {
                    String msg = "Beacon detected.";
                    Log.d(TAG, msg + " ('" + beacon + "'.)");
                    //Toast.makeText(MonitoringService.this, msg, Toast.LENGTH_SHORT).show();
                    nearbyBeacons.add(beacon);
                } else {
                    String msg = "Beacon lost.";
                    Log.d(TAG, msg + " ('" + beacon + "'.)");
                    //Toast.makeText(MonitoringService.this, msg, Toast.LENGTH_SHORT).show();
                    nearbyBeacons.remove(beacon);
                }
            }

            @Override
            public void leftRegion(Region region) {

                if (region == bluetoothProvider.getMonitoredRegion()) {
                    // I'm guessing, we get this callback when there are NO more detected beacons in the given region
                    Log.d(TAG, "Left beacon region.");
                    nearbyBeacons.clear();
                }
            }
        });
    }

    public boolean isRunInBackground() {
        return runInBackground;
    }

    public Handler getMonitorHandler() {
        return monitorHandler;
    }

    public long getButtonDiscoveryIntervalMillis() {
        return buttonDiscoveryIntervalMillis;
    }

    public boolean isRunning() {
        return running;
    }

    public HashMap<String, ButtonMonitor> getButtonMonitorMap() {
        return buttonMonitorMap;
    }
}
