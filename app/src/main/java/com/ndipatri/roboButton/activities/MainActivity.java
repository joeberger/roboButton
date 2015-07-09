package com.ndipatri.roboButton.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
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
import com.ndipatri.roboButton.events.BluetoothDisabledEvent;
import com.ndipatri.roboButton.events.ButtonDiscoveryEvent;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.fragments.ButtonFragment;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.Views;

public class MainActivity extends Activity {

    @Inject
    ButtonDao buttonDao;

    @Inject
    BusProvider bus;

    // region localVariables
    @Inject protected BluetoothProvider bluetoothProvider;

    private static final int REQUEST_ENABLE_BT = -101;

    private static final String TAG = MainActivity.class.getCanonicalName();

    // All buttons that have been rendered into fragments.
    private Set<String> buttonsWithFragments = new HashSet<String>();

    @InjectView(R.id.mainViewGroup) ViewGroup mainViewGroup;

    @InjectView(R.id.enableBluetoothButton) android.widget.Button enableBluetoothButton;
    //endregion
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

        Views.inject(this);

        ((RBApplication)getApplicationContext()).getGraph().inject(this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        resumeActivity();
    }

    @Override
    public void onPause() {
        super.onPause();

        bus.unregister(this);

        forgetAllArduinoButtons();
    }

    protected void resumeActivity() {
        registerWithOttoBus();
        promptUserForBluetoothActivationIfNecessary();
        renderAnyConnectedButtons();
    }

    protected void renderAnyConnectedButtons() {
        // TODO - For now, only consider a single connected Button
        Button connectedButton = buttonDao.getConnectedButton();
        if (connectedButton != null) {
            renderButtonFragmentIfNotAlready(connectedButton.getId());
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

    private synchronized void forgetAllArduinoButtons() {
        for (final String buttonFragmentId : buttonsWithFragments) {
            forgetArduinoButton(buttonFragmentId);
        }
    }

    private void viewNearbyBeacons() {
        startActivity(new Intent(this, ViewNearbyBeaconsActivity.class));
    }

    private synchronized void forgetArduinoButton(final String lostButtonId) {
        final ButtonFragment ButtonFragment = lookupButtonFragment(lostButtonId);
        if (ButtonFragment != null) {
            getFragmentManager().beginTransaction().remove(ButtonFragment).commitAllowingStateLoss();
            buttonsWithFragments.remove(lostButtonId);
        }
    }

    @Subscribe
    public void onButtonLostEvent(ButtonLostEvent ButtonLostEvent) {
        String lostButtonId = ButtonLostEvent.getButtonId();

        forgetArduinoButton(lostButtonId);
    }

    @Subscribe
    public void onArduinoButtonBluetoothDisabled(BluetoothDisabledEvent bluetoothDisabledEvent) {
        Log.d(TAG, "Bluetooth disabled!");

        promptUserForBluetoothActivationIfNecessary();
    }

    @Subscribe
    public void onButtonDiscoveredEvent(ButtonDiscoveryEvent buttonDiscoveryEvent) {

        // The button itself handles its own change of state.. We use this event as an indication that we need
        // to render the button if we haven't already done so.

        if (buttonDiscoveryEvent.isSuccess()) {
            renderButtonFragmentIfNotAlready(buttonDiscoveryEvent.getDeviceAddress());
        }
    }

    //endregion

    private void renderButtonFragmentIfNotAlready(final String buttonId) {
        ButtonFragment existingButtonFragment = lookupButtonFragment(buttonId);
        if (existingButtonFragment == null) {
            final ButtonFragment newButtonFragment = ButtonFragment.newInstance(buttonId);
            getFragmentManager().beginTransaction().add(R.id.mainViewGroup, newButtonFragment, getButtonFragmentTag(buttonId)).commitAllowingStateLoss();
            buttonsWithFragments.add(buttonId);
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
}
