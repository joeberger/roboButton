package com.ndipatri.arduinoButton.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.table.TableUtils;
import com.ndipatri.arduinoButton.ArduinoButtonApplication;
import com.ndipatri.arduinoButton.TestUtils;
import com.ndipatri.arduinoButton.activities.MainControllerActivity;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProviderTestImpl;
import com.ndipatri.arduinoButton.database.OrmLiteDatabaseHelper;
import com.ndipatri.arduinoButton.enums.ButtonState;
import com.ndipatri.arduinoButton.events.ArduinoButtonFoundEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonLostEvent;
import com.ndipatri.arduinoButton.models.Beacon;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.ActivityWatcher;
import com.ndipatri.arduinoButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowPendingIntent;
import org.robolectric.shadows.ShadowSystemClock;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class BluetoothMonitoringServiceTest {

    ArduinoButtonApplication application;
    MainControllerActivity activity;

    @Inject
    BluetoothProvider bluetoothProvider;

    BluetoothMonitoringService monitoringService;

    NotificationManager notificationManager;

    @Before
    public void setup() {

        // Enable Logging to stdout
        ShadowLog.stream = System.out;

        TestUtils.registerOrmLiteProvider();
        TestUtils.resetORMTable();

        Context context = ArduinoButtonApplication.getInstance().getApplicationContext();
        activity = Robolectric.buildActivity(MainControllerActivity.class).create().get();
        application = ArduinoButtonApplication.getInstance();

        ArduinoButtonApplication.getInstance().inject(this);

        monitoringService = startButtonMonitoringService(false); // should run in foreground

        notificationManager = (NotificationManager) Robolectric.application.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Test
    public void buttonMonitoringServiceStartedTest() {

        // ActivityWatcher doesn't work in Robolectric.. so we have to simulate that an activity is resumed.. and thus should
        // start the monitoring service in foreground...
        ActivityWatcher activityWatcher = application.getActivityWatcher();
        activityWatcher.onActivityResumed(activity);

        Intent startedIntent = shadowOf(activity).getNextStartedService();

        assertThat("ButtonMonitoringService should have been started.", startedIntent.getComponent().getClassName().equals(BluetoothMonitoringService.class.getCanonicalName()));
    }

    @Test
    public void onStartCommand() {

        assertThat("ButtonMonitoringService should be running.", monitoringService.isRunning());
        assertThat("ButtonMonitoringService should have been started in foreground.", !monitoringService.isRunInBackground());

        BluetoothMonitoringService.MessageHandler messageHandler = monitoringService.getBluetoothMessageHandler();

        assertThat("MessageHandler thread should have been started and waiting for message.", messageHandler.getLooper().getThread().getState() == Thread.State.WAITING);
        assertThat("MessageHandler thread should have a 'DISCOVER' message pending.", messageHandler.hasMessages(BluetoothMonitoringService.DISCOVER_BUTTON_DEVICES));

        assertThat("Button discovery interval should be 4 seconds.", monitoringService.getButtonDiscoveryIntervalMillis() == 4000);
        assertThat("Button communications grace period should be 10 seconds.", monitoringService.getCommunicationsGracePeriodMillis() == 10000);
    }

    @Test
    public void discoverButtonDevices_newButtonDiscovered() {

        // This would be a standard BT device that is one of our 'Button' arduino boards..
        Button availableButton = new Button("aa:bb:cc:dd:ee", "workCubicle", false, "/tmp/sdcard/image.png");

        Set<Button> availableButtons = new HashSet<Button>();
        availableButtons.add(availableButton);
        ((BluetoothProviderTestImpl) bluetoothProvider).setAvailableButtons(availableButtons);

        monitoringService.discoverButtonDevices();

        HashMap<String, ButtonMonitor> currentButtonMap = monitoringService.getCurrentButtonMap();

        assertThat("Should be one ButtonMonitor.", currentButtonMap.size() == 1);
        assertThat("ButtonMonitor should be configured for new Button.", currentButtonMap.get(availableButton.getId()).getButton().equals(availableButton));
        assertThat("ButtonMonitor should be running.", currentButtonMap.get(availableButton.getId()).isRunning());

        // The idea here is that after a 'discovering' pass, another 'discover' message should be scheduled...
        BluetoothMonitoringService.MessageHandler messageHandler = monitoringService.getBluetoothMessageHandler();
        assertThat("MessageHandler thread should have a 'DISCOVER' message pending.", messageHandler.hasMessages(BluetoothMonitoringService.DISCOVER_BUTTON_DEVICES));
    }

    @Test
    public void discoverButtonDevices_oldButtonNotCommunicating() {
        // NJD - Not really sure why, but I had to do this in order for 'advanceBy' to work...
        ShadowSystemClock.setCurrentTimeMillis(0);

        // This would be a standard BT device that is one of our 'Button' arduino boards..
        Button availableButton = new Button("aa:bb:cc:dd:ee", "workCubicle", false, "/tmp/sdcard/image.png");

        Set<Button> availableButtons = new HashSet<Button>();
        availableButtons.add(availableButton);
        ((BluetoothProviderTestImpl) bluetoothProvider).setAvailableButtons(availableButtons);

        monitoringService.discoverButtonDevices();

        OttoBusListener busListener = new OttoBusListener<ArduinoButtonLostEvent>();

        Robolectric.getUiThreadScheduler().advanceBy(50000);  // communications grace period is 10 seconds, so we want to make

        // this button expire
        monitoringService.discoverButtonDevices();

        HashMap<String, ButtonMonitor> currentButtonMap = monitoringService.getCurrentButtonMap();

        assertThat("ButtonMonitor should have been killed.", currentButtonMap.size() == 0);

        ArduinoButtonLostEvent expectedEvent = new ArduinoButtonLostEvent(availableButton);
        assertThat("Lost Button event should have been published.", busListener.getReceivedEvent() != null && busListener.getReceivedEvent().equals(expectedEvent));
    }

    @Test
    public void discoverButtonDevices_buttonCommunicating_foreground() {
        discoverButtonDevices_buttonCommunicating(true);
    }

    @Test
    public void discoverButtonDevices_buttonCommunicating_background() {
        discoverButtonDevices_buttonCommunicating(false);
    }

    public void discoverButtonDevices_buttonCommunicating(boolean runInForeground) {

        // we want to test this while app is running in background, so the notification happens
        monitoringService = startButtonMonitoringService(!runInForeground); // should run in foreground

        // NJD - Not really sure why, but I had to do this in order for 'advanceBy' to work...
        ShadowSystemClock.setCurrentTimeMillis(0);

        // This would be a standard BT device that is one of our 'Button' arduino boards..
        Button availableButton = new Button("aa:bb:cc:dd:ee", "workCubicle", false, "/tmp/sdcard/image.png");

        Set<Button> availableButtons = new HashSet<Button>();
        availableButtons.add(availableButton);
        ((BluetoothProviderTestImpl) bluetoothProvider).setAvailableButtons(availableButtons);

        monitoringService.discoverButtonDevices();

        OttoBusListener busListener = new OttoBusListener<ArduinoButtonFoundEvent>();

        ButtonMonitor buttonMonitor = monitoringService.getCurrentButtonMap().get(availableButton.getId());
        buttonMonitor.setLocalButtonState(ButtonState.ON);

        Robolectric.getUiThreadScheduler().advanceBy(5000);  // communications grace period is 10 seconds, so we want stay inside this.

        monitoringService.discoverButtonDevices();

        HashMap<String, ButtonMonitor> currentButtonMap = monitoringService.getCurrentButtonMap();

        assertThat("Should be one ButtonMonitor.", currentButtonMap.size() == 1);
        assertThat("ButtonMonitor should be configured for new Button.", currentButtonMap.get(availableButton.getId()).getButton().equals(availableButton));
        assertThat("ButtonMonitor should be running.", currentButtonMap.get(availableButton.getId()).isRunning());

        ArduinoButtonFoundEvent expectedEvent = new ArduinoButtonFoundEvent(availableButton);
        assertThat("Found Button event should have been published.", busListener.getReceivedEvent() != null && busListener.getReceivedEvent().equals(expectedEvent));

        // Test that system notification was emitted
        Intent intent = new Intent(application, MainControllerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(application, 0, intent, 0);

        Notification.Builder builder = new Notification.Builder(application);
        builder.setWhen(System.currentTimeMillis());
        //builder.setContentText(tickerText);
        builder.setSmallIcon(buttonMonitor.getButtonState().smallDrawableResourceId); // this is what shows up in notification bar before you pull it down
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
        Notification desiredNotification = builder.build();

        List<Notification> notifications = Robolectric.shadowOf(notificationManager).getAllNotifications();

        String msg = (runInForeground ? "Zero" : "One") + " notification should have been sent.";

        assertThat(msg, notifications.size() == (runInForeground ? 0 : 1));

        if (!runInForeground) {
            ShadowPendingIntent shadowPendingIntent = shadowOf(notifications.get(0).contentIntent);
            assertThat("Notification should launch the MainControllerActivity.", shadowPendingIntent.getSavedIntent().getComponent().getClassName().equals(MainControllerActivity.class.getCanonicalName()));
        }
    }

    // NJD TODO - Need to write tests around new Beacon monitoring...(nearbyBeacons, etc.)

    private class OttoBusListener<T> {

        private boolean success = false;

        T receivedEvent;

        public OttoBusListener() {
            BusProvider.getInstance().register(this);
        }

        @Subscribe
        public void onEvent(T receivedEvent) {
            this.receivedEvent = receivedEvent;
        }

        public Object getReceivedEvent() {
            return receivedEvent;
        }
    }

    public static BluetoothMonitoringService startButtonMonitoringService(final boolean shouldRunInBackground) {

        BluetoothMonitoringService bluetoothMonitoringService = new BluetoothMonitoringService();
        bluetoothMonitoringService.onCreate();

        final Intent buttonDiscoveryServiceIntent = new Intent();
        buttonDiscoveryServiceIntent.putExtra(BluetoothMonitoringService.RUN_IN_BACKGROUND, shouldRunInBackground);
        bluetoothMonitoringService.onStartCommand(buttonDiscoveryServiceIntent, -1, -1);

        return bluetoothMonitoringService;
    }
}
