package com.ndipatri.arduinoButton.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;

import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.activities.MainControllerActivity;
import com.ndipatri.arduinoButton.database.ButtonProvider;
import com.ndipatri.arduinoButton.enums.ButtonState;
import com.ndipatri.arduinoButton.events.ArduinoButtonFoundEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonLostEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonStateChangeReportEvent;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.BusProvider;
import com.ndipatri.arduinoButton.utils.ButtonMonitor;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ButtonMonitoringService extends Service {

    public static final String TAG = ButtonMonitoringService.class.getCanonicalName();

    public static final String RUN_IN_BACKGROUND = "run_in_background";

    private static final int DISCOVER_BUTTON_DEVICES = -102;

    // region localVars

    protected boolean runInBackground = false;

    // NJD TODO - Should use Dagger for this to be cool.
    protected ButtonProvider buttonProvider = new ButtonProvider();

    protected long buttonDiscoveryIntervalMillis = -1;

    protected long communicationsGracePeriodMillis = -1;

    protected int timeMultiplier = 1;

    // Handler which uses background thread to handle BT communications
    private MessageHandler bluetoothMessageHandler;

    private boolean running = false;

    // Keeping track of all currently monitored buttons.
    HashMap<String, ButtonMonitor> currentButtonMap = new HashMap<String, ButtonMonitor>();

    // Keeping track of last time a monitor checked in with a status (to detect blocked reads)
    HashMap<String, Long> buttonToLastCommunicationsTimeMap = new HashMap<String, Long>();

    // Keeping track of last value received from button.
    HashMap<String, ButtonState> buttonToLastButtonStateMap = new HashMap<String, ButtonState>();

    //endregion

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        BusProvider.getInstance().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BusProvider.getInstance().unregister(this);
        running = false;

        for (final ButtonMonitor thisMonitor : currentButtonMap.values()) {
            thisMonitor.stop();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        runInBackground = intent.getBooleanExtra(RUN_IN_BACKGROUND, false);
        Log.d(TAG, "onStartCommand() (runInBackground='" + runInBackground + "').");

        int newTimeMultiplier = runInBackground ? getResources().getInteger(R.integer.background_time_multiplier) : 1;

        if (newTimeMultiplier != timeMultiplier) {
            timeMultiplier = newTimeMultiplier;
            for (final ButtonMonitor buttonMonitor : currentButtonMap.values()) {
                buttonMonitor.setTimeMultiplier(timeMultiplier);
            }
        }

        if (!running) {

            // Create thread for handling communication with Bluetooth
            // This thread only runs if it's passed a message.. so no need worrying about if it's running or not after this point.
            HandlerThread messageProcessingThread = new HandlerThread("Discovery_BluetoothCommunicationThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
            messageProcessingThread.start();

            // Connect up our background thread's looper with our message processing handler.
            bluetoothMessageHandler = new MessageHandler(messageProcessingThread.getLooper());

            buttonDiscoveryIntervalMillis = getResources().getInteger(R.integer.button_discovery_interval_millis);

            communicationsGracePeriodMillis = getResources().getInteger(R.integer.communications_grace_period_millis);

            scheduleImmediateButtonDiscoveryMessage();

            running = true;
        }

        return Service.START_FLAG_REDELIVERY; // this ensure the service is restarted
    }

    private void scheduleImmediateButtonDiscoveryMessage() {
        bluetoothMessageHandler.queueDiscoverButtonRequest(0);
    }

    private void scheduleButtonDiscoveryMessage() {
        bluetoothMessageHandler.queueDiscoverButtonRequest(buttonDiscoveryIntervalMillis);
    }

    // Hands outgoing bluetooth messages to background thread.
    private final class MessageHandler extends Handler {

        public MessageHandler(Looper looper) {
            super(looper);
        }

        public void queueDiscoverButtonRequest(final long offsetMillis) {

            Message rawMessage = obtainMessage();
            rawMessage.what = DISCOVER_BUTTON_DEVICES;

            // To be handled by separate thread.
            sendMessageDelayed(rawMessage, offsetMillis);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case DISCOVER_BUTTON_DEVICES:

                    if (running) {
                        discoverButtonDevices();
                    }

                    break;
            }
        }
    }

    // Here we determin if the device is actually on and communicating..
    // so we launch a monitor for each potential device to ascertain this...
    private synchronized void discoverButtonDevices() {

        final Set<BluetoothDevice> pairedButtons = new HashSet<BluetoothDevice>();

        String discoverableButtonPatternString = getString(R.string.button_discovery_pattern);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().contains(discoverableButtonPatternString)) {
                    Log.d(TAG, "We have a paired ArduinoButton device! + '" + device + "'.");

                    pairedButtons.add(device);
                }
            }
        }

        // To keep track of buttons that have gone incommunicado.

        // This starts by containing all paired buttons.. and then as we decide we can communicate
        // with each button, we remove them.. What is left are all buttons not talking.
        final Set<String> lostButtonSet = new HashSet<String>(currentButtonMap.keySet());

        // These are all buttons with which we are still communicating.
        final HashMap<String, ButtonMonitor> newAndExistingButtonMap = new HashMap<String, ButtonMonitor>();

        for (final BluetoothDevice pairedButton : pairedButtons) {

            final String buttonId = getButtonId(pairedButton);

            ButtonMonitor buttonMonitor = currentButtonMap.get(buttonId);

            if (buttonMonitor == null) {
                // This is a paired button with no monitor.. So start monitoring.
                newAndExistingButtonMap.put(buttonId, new ButtonMonitor(getApplicationContext(), pairedButton));
            } else {

                // Ok, first make sure monitor isn't dead...
                boolean buttonUnresponsive = false;
                Long lastCommuncationsTimeMillis = buttonToLastCommunicationsTimeMap.get(buttonId);
                if ((System.currentTimeMillis() - lastCommuncationsTimeMillis) > communicationsGracePeriodMillis) {
                    Log.d(TAG, "Button has become unresponsive for '" + buttonId + "'");
                    buttonUnresponsive = true;
                }

                if (buttonMonitor.getButtonState().isCommunicating && !buttonUnresponsive) {

                    // This button is actively communicating
                    lostButtonSet.remove(buttonId);
                    newAndExistingButtonMap.put(buttonId, buttonMonitor);

                    final ButtonState currentButtonState = buttonMonitor.getButtonState();

                    // Level Trigger state notification, not edge triggered (e.g. we will send this active
                    // notification every poll interval that this button is communicating)..
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            BusProvider.getInstance().post(new ArduinoButtonFoundEvent(pairedButton));

                            if (runInBackground) {
                                sendActiveButtonNotification(buttonId, currentButtonState);
                            }
                        }
                    });

                    ButtonState previousButtonState = buttonToLastButtonStateMap.get(buttonId);
                    if (runInBackground && (previousButtonState == null || (previousButtonState.value != currentButtonState.value))) {
                        sendActiveButtonNotification(buttonId, currentButtonState);
                    }
                    buttonToLastButtonStateMap.put(buttonId, currentButtonState);
                }
            }
        }

        // All buttons left in 'lostButtonSet' represent buttons that are no longer communicating
        for (final String lostButtonId : lostButtonSet) {
            Log.d(TAG, "Forgetting lost button '" + lostButtonId + "'.");

            final ButtonMonitor lostButtonMonitor = currentButtonMap.get(lostButtonId);
            lostButtonMonitor.stop();

            currentButtonMap.remove(lostButtonId);
            buttonToLastCommunicationsTimeMap.remove(lostButtonId);
            buttonToLastButtonStateMap.remove(lostButtonId);

            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    BusProvider.getInstance().post(new ArduinoButtonLostEvent(lostButtonMonitor.getBluetoothDevice()));
                }
            });
        }

        currentButtonMap = newAndExistingButtonMap;

        scheduleButtonDiscoveryMessage();
    }

    protected String getButtonId(final BluetoothDevice bluetoothDevice) {
        return bluetoothDevice.getAddress();
    }

    protected void sendActiveButtonNotification(String buttonId, ButtonState buttonState) {

        String tickerText = this.getString(R.string.new_robo_button);
        Button button = buttonProvider.getButton(this, buttonId);
        if (button != null) {
            tickerText = " '" + button.getName() + "'";
        }
        tickerText += " " + this.getString(buttonState.descriptionResId);

        Intent intent = new Intent(this, MainControllerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        int notifId = 1234;

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

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
                1000,  // off
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

    @Subscribe
    public void onArduinoButtonStateChangeReportEvent(final ArduinoButtonStateChangeReportEvent event) {

        // The purpose of subscribing is just to ensure buttonMonitor is still communicating successfully.
        // This service serves as a watchdog to terminate any monitors that have become unresponsive
        buttonToLastCommunicationsTimeMap.put(event.buttonId, System.currentTimeMillis());
    }
}
