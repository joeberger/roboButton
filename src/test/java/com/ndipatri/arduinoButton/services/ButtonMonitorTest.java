package com.ndipatri.arduinoButton.services;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.ndipatri.arduinoButton.ABApplication;
import com.ndipatri.arduinoButton.TestUtils;
import com.ndipatri.arduinoButton.activities.MainControllerActivity;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProvider;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProviderTestImpl;
import com.ndipatri.arduinoButton.enums.ButtonState;
import com.ndipatri.arduinoButton.events.ABStateChangeReport;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class ButtonMonitorTest {

    ABApplication application;
    MainControllerActivity activity;

    @Inject
    BluetoothProvider bluetoothProvider;

    MonitoringService monitoringService;

    NotificationManager notificationManager;

    @Before
    public void setup() {

        // Enable Logging to stdout
        ShadowLog.stream = System.out;

        TestUtils.registerOrmLiteProvider();
        TestUtils.resetORMTable();

        Context context = ABApplication.getInstance().getApplicationContext();
        activity = Robolectric.buildActivity(MainControllerActivity.class).create().get();
        application = ABApplication.getInstance();

        ABApplication.getInstance().inject(this);

        monitoringService = startButtonMonitoringService(false); // should run in foreground

        notificationManager = (NotificationManager) Robolectric.application.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Test
    public void startButtonMonitor() {

        // This would be a standard BT device that is one of our 'Button' arduino boards..
        Button availableButton = new Button("aa:bb:cc:dd:ee", "workCubicle", false);

        Set<Button> availableButtons = new HashSet<Button>();
        availableButtons.add(availableButton);
        ((BluetoothProviderTestImpl) bluetoothProvider).setAvailableButtons(availableButtons);

        OttoBusListener busListener = new OttoBusListener<ABStateChangeReport>();

        // NJD TODO - need to figure otu how to run this.... it's now the 'buttonMonitorDiscoveryRunnable' we have to call
        //monitoringService.discoverButtonDevices();

        HashMap<String, ButtonMonitor> currentButtonMap = monitoringService.getButtonMonitorMap();

        assertThat("Should be one ButtonMonitor.", currentButtonMap.size() == 1);

        ButtonMonitor buttonMonitor = currentButtonMap.get(availableButton.getId());

        assertThat("ButtonMonitor should be configured for given Button.", buttonMonitor.getButton().equals(availableButton));
        assertThat("ButtonMonitor should be running.", buttonMonitor.isRunning());
        assertThat("ButtonMonitor should be in 'NEVER_CONNECTED' state.", buttonMonitor.getButtonState() == ButtonState.NEVER_CONNECTED);

        ButtonMonitor.MessageHandler messageHandler = buttonMonitor.getBluetoothMessageHandler();
        assertThat("MessageHandler thread should have a 'QUERY_STATE_MESSAGE' message pending.", messageHandler.hasMessages(ButtonMonitor.QUERY_STATE_MESSAGE));

        assertThat("Query state discovery interval should be 1 second.", buttonMonitor.getQueryStateIntervalMillis() == 1000);

        ABStateChangeReport expectedEvent = new ABStateChangeReport(new ABStateChangeReport.ABStateChangeReportValue(ButtonState.NEVER_CONNECTED, availableButton.getId()));
        ABStateChangeReport receivedReport = ((ABStateChangeReport)busListener.getReceivedEvent());
        assertThat("Button State Change event should have been published.", receivedReport != null && receivedReport.equals(expectedEvent));
    }

    // NJD TODO - need to test actual communications with BT device and how that effects 'liveliness' of monitor...
    // This is necessary to get the BTMonitoringService to fire of a monitor 'iteration'
    //BluetoothMonitoringService.MessageHandler messageHandler = monitoringService.getMonitorHandler();
    //Robolectric.shadowOf(messageHandler.getLooper()).getScheduler().runOneTask();

    // We know we have one Button so after above 'iteration' there should be a ButtonMonitor assigned to
    // this Button

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

    public static MonitoringService startButtonMonitoringService(final boolean shouldRunInBackground) {

        MonitoringService monitoringService = new MonitoringService();
        monitoringService.onCreate();

        final Intent buttonDiscoveryServiceIntent = new Intent();
        buttonDiscoveryServiceIntent.putExtra(MonitoringService.RUN_IN_BACKGROUND, shouldRunInBackground);
        monitoringService.onStartCommand(buttonDiscoveryServiceIntent, -1, -1);

        return monitoringService;
    }
}
