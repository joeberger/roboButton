package com.ndipatri.arduinoButton.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.events.ArduinoButtonBluetoothDisabledEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonFoundEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonInformationEvent;
import com.ndipatri.arduinoButton.events.ArduinoButtonLostEvent;
import com.ndipatri.arduinoButton.events.ButtonImageRequestEvent;
import com.ndipatri.arduinoButton.events.ButtonImageResponseEvent;
import com.ndipatri.arduinoButton.fragments.ArduinoButtonFragment;
import com.ndipatri.arduinoButton.services.ButtonMonitoringService;
import com.ndipatri.arduinoButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.InjectView;
import butterknife.Views;

// This class coordinates all BT communications from its various ArduinoButtonFragments.  This class
// schedules and provides thread resources for the querying and setting of button state.
public class MainControllerActivity extends Activity {

    // region localVariables
    private AtomicInteger imageRequestIdGenerator = new AtomicInteger();

    // These are all outstanding intents to retrieve an image for a particular buttonId
    protected SparseArray<String> pendingImageRequestIdToButtonIdMap = new SparseArray<String>();

    private static final int REQUEST_ENABLE_BT = -101;

    private static final String TAG = MainControllerActivity.class.getCanonicalName();

    // All buttons that have been rendered into fragments.
    private Set<String> buttonFragmentIds = new HashSet<String>();

    // The main ViewGroup to which all ArduinoButtonFragments are added.
    protected @InjectView(R.id.mainViewGroup) ViewGroup mainViewGroup;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

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

        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                requestUserToEnableBluetooth();
            } else {
                startBluetoothMonitoringService();
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

    protected BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    protected void startBluetoothMonitoringService() {
        final Intent buttonDiscoveryServiceIntent = new Intent(this, ButtonMonitoringService.class);
        startService(buttonDiscoveryServiceIntent);
    }

    public void chooseImage(String requestingButtonId) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");

        startActivityForResult(photoPickerIntent, getNextImageRequestId(requestingButtonId));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startBluetoothMonitoringService();
            } else {
                Toast.makeText(this, "This application cannot run without Bluetooth enabled!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            String requestingButtonId = getButtonForRequestId(requestCode);
            if (requestingButtonId != null && resultCode == RESULT_OK) {

                Uri selectedImage = returnedIntent.getData();
                BusProvider.getInstance().post(new ButtonImageResponseEvent(requestingButtonId, selectedImage));
                finishedRequest(requestCode);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // region OTTO Subscriptions

    @Subscribe
    public void onArduinoButtonInformation(ArduinoButtonInformationEvent arduinoButtonInformationEvent) {
        Log.d(TAG, "Information received detected for button '" + arduinoButtonInformationEvent.buttonId + "(" + arduinoButtonInformationEvent.buttonId + ")'.");

        publishProgress(arduinoButtonInformationEvent.message);
    }

    protected String getButtonId(BluetoothDevice bluetoothDevice) {
        return bluetoothDevice.getAddress();
    }

    private ArduinoButtonFragment lookupButtonFragment(String buttonId) {
        return (ArduinoButtonFragment) getFragmentManager().findFragmentByTag(getButtonFragmentTag(buttonId));
    }

    private synchronized void forgetAllArduinoButtons() {
        for (final String buttonFragmentId : buttonFragmentIds) {
            forgetArduinoButton(buttonFragmentId);
        }
    }

    private synchronized void forgetArduinoButton(final String lostButtonId) {

        final ArduinoButtonFragment arduinoButtonFragment = lookupButtonFragment(lostButtonId);
        if (arduinoButtonFragment != null) {
            getFragmentManager().beginTransaction().remove(arduinoButtonFragment).commitAllowingStateLoss();
            buttonFragmentIds.remove(lostButtonId);
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
    public void onButtonLostEvent(ArduinoButtonLostEvent arduinoButtonLostEvent) {
        String lostButtonId = getButtonId(arduinoButtonLostEvent.bluetoothDevice);

        forgetArduinoButton(lostButtonId);
    }

    @Subscribe
    public void onArduinoButtonBluetoothDisabled(ArduinoButtonBluetoothDisabledEvent arduinoButtonBluetoothDisabledEvent) {
        Log.d(TAG, "Bluetooth disabled!");

        resumeActivity();
    }

    @Subscribe
    public void onButtonImageRequestEvent(ButtonImageRequestEvent request) {
        chooseImage(request.buttonId);
    }

    @Subscribe
    public void onArduinoButtonFoundEvent(ArduinoButtonFoundEvent arduinoButtonFoundEvent) {

        String foundButtonId = getButtonId(arduinoButtonFoundEvent.bluetoothDevice);

        ArduinoButtonFragment existingButtonFragment = lookupButtonFragment(foundButtonId);
        if (existingButtonFragment == null) {
            final ArduinoButtonFragment newArduinoButtonFragment = ArduinoButtonFragment.newInstance(foundButtonId);
            getFragmentManager().beginTransaction().add(R.id.mainViewGroup, newArduinoButtonFragment, getButtonFragmentTag(foundButtonId)).commitAllowingStateLoss();
            buttonFragmentIds.add(foundButtonId);
        }
    }

    //endregion
}

