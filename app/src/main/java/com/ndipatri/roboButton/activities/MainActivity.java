package com.ndipatri.roboButton.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.BluetoothProvider;
import com.ndipatri.roboButton.dagger.daos.ButtonDao;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.BluetoothDisabledEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.ButtonUpdatedEvent;
import com.ndipatri.roboButton.fragments.ButtonFragment;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.Views;

public class MainActivity extends Activity {

    // Because MainActivity depends on the correctness of the MonitoringService state,
    // we want this Activity to wait until the service is started before resuming, itself.
    boolean monitoringServiceStarted = false;

    @Inject
    ButtonDao buttonDao;

    @Inject
    BusProvider bus;

    // region localVariables
    @Inject protected BluetoothProvider bluetoothProvider;

    private static final int REQUEST_ENABLE_BT = -101;

    private static final String TAG = MainActivity.class.getCanonicalName();

    // All buttons that have been rendered into fragments.
    private Set<String> buttonsWithFragments = Collections.synchronizedSet(new HashSet<String>());

    @InjectView(R.id.mainViewGroup) ViewGroup mainViewGroup;

    @InjectView(R.id.enableBluetoothButton) android.widget.Button enableBluetoothButton;
    //endregion
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_main);

        Views.inject(this);

        ((RBApplication)getApplicationContext()).getGraph().inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, MonitoringService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (monitoringServiceStarted) {
            unbindService(serviceConnection);
            monitoringServiceStarted = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (monitoringServiceStarted) {
            resumeActivity();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "onPause()");

        bus.unregister(this);

        forgetAllArduinoButtons();
    }

    protected void resumeActivity() {
        if (monitoringServiceStarted) {
            registerWithOttoBus();
            promptUserForBluetoothActivationIfNecessary();
            renderAnyConnectedButtons();
        }
    }

    protected void renderAnyConnectedButtons() {
        List<Button> connectedButtons = buttonDao.getCommunicatingButtons();
        for (Button communicatingButton : connectedButtons) {
            Log.d(TAG, "Rendering fragment for button '" + communicatingButton + "'.");
            renderButtonFragmentIfNotAlready(communicatingButton.getId());
        }
    }

    protected void promptUserForBluetoothActivationIfNecessary() {
        if (!bluetoothProvider.isBluetoothSupported()) {
            Toast.makeText(this, "Bluetooth not supported on this device!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!bluetoothProvider.isBluetoothEnabled()) {
                enableBluetoothButton.setVisibility(View.VISIBLE);
                enableBluetoothButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestUserToEnableBluetooth();
                    }
                });
            } else{
                postBluetoothConfirmationSetup();
            }
        }
    }

    protected void registerWithOttoBus() {
        bus.register(this);
    }

    protected void requestUserToEnableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    protected void startBluetoothMonitoringService() {
        final Intent buttonDiscoveryServiceIntent = new Intent(this, MonitoringService.class);
        startService(buttonDiscoveryServiceIntent);
    }

    protected void stopBluetoothMonitoringService() {
        final Intent buttonDiscoveryServiceIntent = new Intent(this, MonitoringService.class);
        stopService(buttonDiscoveryServiceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                postBluetoothConfirmationSetup();
            } else {
                Toast.makeText(this, "This application cannot run without Bluetooth enabled!", Toast.LENGTH_SHORT).show();
                stopBluetoothMonitoringService();
                finish();
            }
        }
    }

    protected void postBluetoothConfirmationSetup() {
        startBluetoothMonitoringService();
    }

    private void publishProgress(final String progressString)  {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, progressString, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getButtonFragmentTag(String buttonId) {
        return "button_" + buttonId;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_controller_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem toggleAutoControlMenuItem = menu.findItem(R.id.toggle_auto_control);
        
        int autoControlStringResource = isAutoModeEnabled() ? R.string.disable_auto_control :
                                                              R.string.enable_auto_control;
        
        toggleAutoControlMenuItem.setTitle(autoControlStringResource);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.view_nearby_beacons:

                viewNearbyBeacons();

                return true;
            
            case R.id.toggle_auto_control:
                
                setAutoModeEnabled(!isAutoModeEnabled()); 
                
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // region OTTO Subscriptions

    private ButtonFragment lookupButtonFragment(String buttonId) {
        return (ButtonFragment) getFragmentManager().findFragmentByTag(getButtonFragmentTag(buttonId));
    }

    private void forgetAllArduinoButtons() {
        Iterator<String> fragIter = buttonsWithFragments.iterator();
        while(fragIter.hasNext()) {
            String buttonId = fragIter.next();

            ButtonFragment ButtonFragment = lookupButtonFragment(buttonId);
            if (ButtonFragment != null) {
                getFragmentManager().beginTransaction().remove(ButtonFragment).commitAllowingStateLoss();
                fragIter.remove();
            }
        }
    }

    private void viewNearbyBeacons() {
        startActivity(new Intent(this, ViewNearbyBeaconsActivity.class));
    }

    @Subscribe
    public void onButtonLostEvent(ButtonLostEvent ButtonLostEvent) {
        Log.d(TAG, "onButtonLostEvent()");

        String lostButtonId = ButtonLostEvent.getButtonId();

        final ButtonFragment ButtonFragment = lookupButtonFragment(lostButtonId);
        if (ButtonFragment != null) {
            getFragmentManager().beginTransaction().remove(ButtonFragment).commitAllowingStateLoss();
            buttonsWithFragments.remove(lostButtonId);
        }
    }

    @Subscribe
    public void onArduinoButtonBluetoothDisabled(BluetoothDisabledEvent bluetoothDisabledEvent) {
        Log.d(TAG, "Bluetooth disabled!");

        promptUserForBluetoothActivationIfNecessary();
    }

    @Subscribe
    public void onButtonUpdatedEvent(ButtonUpdatedEvent buttonUpdatedEvent) {

        Button updatedButton = buttonDao.getButton(buttonUpdatedEvent.getButtonId());

        Log.d(TAG, "onButtonUpdatedEvent('" + updatedButton + ").')");

        renderButtonFragmentIfNotAlready(buttonUpdatedEvent.buttonId);
    }

    //endregion

    private void renderButtonFragmentIfNotAlready(final String buttonId) {
        if (!buttonsWithFragments.contains(buttonId)) {

            Button button = buttonDao.getButton(buttonId);

            // We don't want to render a button in a transient state...
            if (button.getState() == ButtonState.ON || button.getState() == ButtonState.OFF) {
                buttonsWithFragments.add(buttonId);
                final ButtonFragment newButtonFragment = ButtonFragment.newInstance(buttonId);
                getFragmentManager().beginTransaction().add(R.id.mainViewGroup, newButtonFragment, getButtonFragmentTag(buttonId)).commitAllowingStateLoss();
            }
        }
    }

    public BluetoothProvider getBluetoothProvider() {
        return bluetoothProvider;
    }

    protected boolean isAutoModeEnabled() {
        return RBApplication.getInstance().getAutoModeEnabledFlag();
    }

    protected void setAutoModeEnabled(boolean enabled) {
        RBApplication.getInstance().setAutoModeEnabledFlag(enabled);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            monitoringServiceStarted = true;
            resumeActivity();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            monitoringServiceStarted = false;
        }
    };
}

