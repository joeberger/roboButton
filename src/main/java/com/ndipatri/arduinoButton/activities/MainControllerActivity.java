package com.ndipatri.arduinoButton.activities;

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

import com.ndipatri.arduinoButton.ABApplication;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.dagger.providers.BluetoothProvider;
import com.ndipatri.arduinoButton.events.ABFoundEvent;
import com.ndipatri.arduinoButton.events.ABLostEvent;
import com.ndipatri.arduinoButton.events.BluetoothDisabledEvent;
import com.ndipatri.arduinoButton.fragments.ABFragment;
import com.ndipatri.arduinoButton.services.MonitoringService;
import com.ndipatri.arduinoButton.utils.BusProvider;
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

    private static final String TAG = MainControllerActivity.class.getCanonicalName();

    // All buttons that have been rendered into fragments.
    private Set<String> buttonsWithFragments = new HashSet<String>();

    // The main ViewGroup to which all ArduinoButtonFragments are added.
    protected @InjectView(R.id.mainViewGroup) ViewGroup mainViewGroup;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

        ((ABApplication)getApplicationContext()).registerForDependencyInjection(this);

        Views.inject(this);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.forget_all_buttons:

                forgetAllArduinoButtons();

                return true;

            case R.id.view_nearby_beacons:

                viewNearbyBeacons();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // region OTTO Subscriptions

    private ABFragment lookupButtonFragment(String buttonId) {
        return (ABFragment) getFragmentManager().findFragmentByTag(getButtonFragmentTag(buttonId));
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

        final ABFragment ABFragment = lookupButtonFragment(lostButtonId);
        if (ABFragment != null) {
            getFragmentManager().beginTransaction().remove(ABFragment).commitAllowingStateLoss();
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
    public void onButtonLostEvent(ABLostEvent ABLostEvent) {
        String lostButtonId = ABLostEvent.button.getId();

        forgetArduinoButton(lostButtonId);
    }

    @Subscribe
    public void onArduinoButtonBluetoothDisabled(BluetoothDisabledEvent bluetoothDisabledEvent) {
        Log.d(TAG, "Bluetooth disabled!");

        promptUserForBluetoothActivationIfNecessary();
    }

    @Subscribe
    public void onArduinoButtonFoundEvent(ABFoundEvent ABFoundEvent) {

        String foundButtonId = ABFoundEvent.button.getId();

        ABFragment existingButtonFragment = lookupButtonFragment(foundButtonId);
        if (existingButtonFragment == null) {
            final ABFragment newABFragment = ABFragment.newInstance(foundButtonId);
            getFragmentManager().beginTransaction().add(R.id.mainViewGroup, newABFragment, getButtonFragmentTag(foundButtonId)).commitAllowingStateLoss();
            buttonsWithFragments.add(foundButtonId);
        }
    }

    //endregion

    public BluetoothProvider getBluetoothProvider() {
        return bluetoothProvider;
    }
}

