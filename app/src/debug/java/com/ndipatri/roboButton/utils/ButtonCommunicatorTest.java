package com.ndipatri.roboButton.utils;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.TestUtils;
import com.ndipatri.roboButton.activities.MainControllerActivity;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonDiscoveryProviderTestImpl;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ButtonStateChangeReport;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.services.MonitoringService;
import com.squareup.otto.Subscribe;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;


@RunWith(RobolectricTestRunner.class)
public class ButtonCommunicatorTest {

    RBApplication application;
    MainControllerActivity activity;

    @Inject
    ButtonDiscoveryProvider buttonDiscoveryProvider;

    MonitoringService monitoringService;

    NotificationManager notificationManager;

    @Before
    public void setup() {

        // Enable Logging to stdout
        ShadowLog.stream = System.out;

        TestUtils.registerOrmLiteProvider();
        TestUtils.resetORMTable();

        Context context = RBApplication.getInstance().getApplicationContext();
        activity = Robolectric.buildActivity(MainControllerActivity.class).create().get();
        application = RBApplication.getInstance();

        RBApplication.getInstance().getGraph().inject(this);

        monitoringService = startButtonMonitoringService(false); // should run in foreground

        notificationManager = (NotificationManager) Robolectric.application.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Test
    public void startButtonCommunicator() {

        // This would be a standard BT device that is one of our 'Button' arduino boards..
        Button availableButton = new Button("aa:bb:cc:dd:ee", "workCubicle", false);

        Set<Button> availableButtons = new HashSet<Button>();
        availableButtons.add(availableButton);
        ((ButtonDiscoveryProviderTestImpl) buttonDiscoveryProvider).setDiscoveredButtons(availableButtons);

        OttoBusListener busListener = new OttoBusListener<ButtonStateChangeReport>();

        // NJD TODO - need to figure otu how to run this.... it's now the 'servicePollRunnable' we have to call
        //monitoringService.discoverButtonDevices();

        ButtonCommunicator buttonCommunicator = monitoringService.getButtonCommunicator();

        //assertThat("ButtonMonitor should be configured for given Button.", buttonCommunicator.getButton().equals(availableButton));
        //assertThat("ButtonMonitor should be running.", buttonCommunicator.isRunning());
        //assertThat("ButtonMonitor should be in 'NEVER_CONNECTED' state.", buttonCommunicator.getButtonState() == ButtonState.NEVER_CONNECTED);

        ButtonCommunicator.MessageHandler messageHandler = buttonCommunicator.getBluetoothMessageHandler();
        //assertThat("MessageHandler thread should have a 'QUERY_STATE_MESSAGE' message pending.", messageHandler.hasMessages(ButtonCommunicator.QUERY_STATE_MESSAGE));

        //assertThat("Query state discovery interval should be 1 second.", buttonCommunicator.getQueryStateIntervalMillis() == 1000);

        ButtonStateChangeReport expectedEvent = new ButtonStateChangeReport(availableButton.getId(), ButtonState.NEVER_CONNECTED);
        ButtonStateChangeReport receivedReport = ((ButtonStateChangeReport)busListener.getReceivedEvent());
        //assertThat("Button State Change event should have been published.", receivedReport != null && receivedReport.equals(expectedEvent));
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
