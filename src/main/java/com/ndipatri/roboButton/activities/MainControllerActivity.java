package com.ndipatri.roboButton.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.dagger.providers.BluetoothProvider;
import com.ndipatri.roboButton.events.ButtonLostEvent;
import com.ndipatri.roboButton.events.BluetoothDisabledEvent;
import com.ndipatri.roboButton.events.ButtonStateChangeReport;
import com.ndipatri.roboButton.fragments.ButtonFragment;
import com.ndipatri.roboButton.services.MonitoringService;
import com.ndipatri.roboButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.Views;

public class MainControllerActivity extends Activity {

    // region localVariables
    @Inject protected BluetoothProvider bluetoothProvider;

    private AtomicInteger imageRequestIdGenerator = new AtomicInteger();

    // These are all outstanding intents to retrieve an image for a particular buttonId
    protected SparseArray<String> pendingImageRequestIdToButtonIdMap = new SparseArray<String>();

    private static final int REQUEST_ENABLE_BT = -101;
    public static final String SHOULD_TOGGLE_FLAG = "should_toggle_flag";

    private static final String TAG = MainControllerActivity.class.getCanonicalName();

    // All buttons that have been rendered into fragments.
    private Set<String> buttonsWithFragments = new HashSet<String>();

    // The main ViewGroup to which all ArduinoButtonFragments are added.
    protected @InjectView(R.id.mainViewGroup) ViewGroup mainViewGroup;
    //endregion
    
    // Should toggle the first button to which we attach.
    protected boolean shouldToggleFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

        ((RBApplication)getApplicationContext()).registerForDependencyInjection(this);

        Views.inject(this);
        
        Intent intent = getIntent();
        if (intent != null) {
            shouldToggleFlag = intent.getBooleanExtra(SHOULD_TOGGLE_FLAG, false);
        }
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

        BusProvider.getInstance().unregister(this);

        forgetAllArduinoButtons();
    }

    protected void resumeActivity() {
        registerWithOttoBus();
        promptUserForBluetoothActivationIfNecessary();
    }

    protected void promptUserForBluetoothActivationIfNecessary() {
        if (!bluetoothProvider.isBluetoothSupported()) {
            Toast.makeText(this, "Bluetooth not supported on this device!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!bluetoothProvider.isBluetoothEnabled()) {
                requestUserToEnableBluetooth();
            }
        }
    }

    protected void registerWithOttoBus() {
        BusProvider.getInstance().register(this);
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
                startBluetoothMonitoringService();
            } else {
                Toast.makeText(this, "This application cannot run without Bluetooth enabled!", Toast.LENGTH_SHORT).show();
                stopBluetoothMonitoringService();
                finish();
            }
        }
    }

    private void publishProgress(final String progressString)  {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainControllerActivity.this, progressString, Toast.LENGTH_SHORT).show();
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

    protected synchronized Integer getNextImageRequestId(String buttonId) {
        Integer nextImageRequestId = imageRequestIdGenerator.incrementAndGet();
        pendingImageRequestIdToButtonIdMap.put(nextImageRequestId, buttonId);

        return nextImageRequestId;
    }

    protected synchronized String getButtonForRequestId(Integer imageRequestId) {
        return pendingImageRequestIdToButtonIdMap.get(imageRequestId);
    }

    protected synchronized  void finishedRequest(Integer imageRequestId) {
        pendingImageRequestIdToButtonIdMap.remove(imageRequestId);
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
    public void onButtonStateChangeReport(ButtonStateChangeReport buttonStateChangeReport) {
        
        Log.d(TAG, "onButtonConnectedEvent()");

        String foundButtonId = buttonStateChangeReport.buttonId;

        ButtonFragment existingButtonFragment = lookupButtonFragment(foundButtonId);
        if (existingButtonFragment == null) {
            final ButtonFragment newButtonFragment = ButtonFragment.newInstance(foundButtonId, shouldToggleFlag);
            getFragmentManager().beginTransaction().add(R.id.mainViewGroup, newButtonFragment, getButtonFragmentTag(foundButtonId)).commitAllowingStateLoss();
            buttonsWithFragments.add(foundButtonId);
        }
    }

    //endregion

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

