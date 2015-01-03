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
import com.ndipatri.arduinoButton.events.ArduinoButtonStateChangeReportEvent;
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
public class ButtonMonitorTest {

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
    public void startButtonMonitor() {

        // This would be a standard BT device that is one of our 'Button' arduino boards..
        Button availableButton = new Button("aa:bb:cc:dd:ee", "workCubicle", false, "/tmp/sdcard/image.png");

        Set<Button> availableButtons = new HashSet<Button>();
        availableButtons.add(availableButton);
        ((BluetoothProviderTestImpl) bluetoothProvider).setAvailableButtons(availableButtons);

        OttoBusListener busListener = new OttoBusListener<ArduinoButtonStateChangeReportEvent>();

        monitoringService.discoverButtonDevices();

        HashMap<String, ButtonMonitor> currentButtonMap = monitoringService.getCurrentButtonMap();

        assertThat("Should be one ButtonMonitor.", currentButtonMap.size() == 1);

        ButtonMonitor buttonMonitor = currentButtonMap.get(availableButton.getId());

        assertThat("ButtonMonitor should be configured for given Button.", buttonMonitor.getButton().equals(availableButton));
        assertThat("ButtonMonitor should be running.", buttonMonitor.isRunning());
        assertThat("ButtonMonitor should be in 'NEVER_CONNECTED' state.", buttonMonitor.getButtonState() == ButtonState.NEVER_CONNECTED);

        ButtonMonitor.MessageHandler messageHandler = buttonMonitor.getBluetoothMessageHandler();
        assertThat("MessageHandler thread should have a 'QUERY_STATE_MESSAGE' message pending.", messageHandler.hasMessages(ButtonMonitor.QUERY_STATE_MESSAGE));

        assertThat("Query state discovery interval should be 1 second.", buttonMonitor.getQueryStateIntervalMillis() == 1000);

        ArduinoButtonStateChangeReportEvent expectedEvent = new ArduinoButtonStateChangeReportEvent(availableButton.getId(), ButtonState.NEVER_CONNECTED);
        assertThat("Button State Change event should have been published.", busListener.getReceivedEvent() != null && busListener.getReceivedEvent().equals(expectedEvent));
    }

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
