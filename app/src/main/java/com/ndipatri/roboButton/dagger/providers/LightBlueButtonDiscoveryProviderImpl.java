package com.ndipatri.roboButton.dagger.providers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.events.ButtonDiscoveryEvent;
import com.ndipatri.roboButton.utils.BusProvider;
import com.squareup.otto.Bus;

import javax.inject.Inject;

import nl.littlerobots.bean.Bean;
import nl.littlerobots.bean.BeanDiscoveryListener;
import nl.littlerobots.bean.BeanListener;
import nl.littlerobots.bean.BeanManager;
import nl.littlerobots.bean.message.Callback;
import nl.littlerobots.bean.message.SketchMetaData;

/**
 * This class will perform a Bluetooth Classic 'Discovery' operation.  After a defined timeout period, the scan will
 * be stopped.  At that time a 'success' or 'failure' event will be emitted based on whether a device matching the
 * defined 'discoveryPattern' was found.
 */
public class LightBlueButtonDiscoveryProviderImpl implements ButtonDiscoveryProvider {

    private static final String TAG = LightBlueButtonDiscoveryProviderImpl.class.getCanonicalName();

    private static final String BUTTON_SKETCH_NAME = "lightBlueButton";

    private Context context;

    BluetoothAdapter bluetoothAdapter = null;

    protected int buttonDiscoveryDurationMillis;

    String discoverableButtonPatternString;

    protected boolean discovering = false;

    protected Bean discoveredBean;
    protected boolean isConnectedBeanAButton = false;

    @Inject
    BusProvider bus;

    public LightBlueButtonDiscoveryProviderImpl(Context context) {
        this.context = context;

        RBApplication.getInstance().getGraph().inject(this);

        discoverableButtonPatternString = context.getString(R.string.button_discovery_pattern);
        buttonDiscoveryDurationMillis = context.getResources().getInteger(R.integer.button_discovery_duration_millis);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public synchronized void startButtonDiscovery() {

        Log.d(TAG, "Beginning LightBlue Button Monitoring Process...");
        if (discovering) {
            // make this request idempotent
            return;
        }

        //Check to see if the device supports Bluetooth and that it's turned on
        if (!discovering && bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {

            discovering = true;

            getBeanManager().startDiscovery(getButtonDiscoveryListener());
        } else {
            discovering = false;
        }
    }

    protected BeanDiscoveryListener getButtonDiscoveryListener() {
        return new BeanDiscoveryListener() {
            @Override
            public void onBeanDiscovered(Bean discoveredBean) {
                LightBlueButtonDiscoveryProviderImpl.this.discoveredBean = discoveredBean;
                discoveredBean.connect(context, getBeanConnectionListener());
            }

            @Override
            public void onDiscoveryComplete() {
                if (discoveredBean == null || !isConnectedBeanAButton) {
                    postButtonDiscoveredEvent(false, null);
                }
            }
        };
    }

    protected BeanListener getBeanConnectionListener() {
        return new BeanListener() {

            @Override
            public void onConnected() {
                discoveredBean.readSketchMetaData(new Callback<SketchMetaData>() {
                    @Override
                    public void onResult(SketchMetaData sketchMetaData) {
                        if (sketchMetaData.name().equals(BUTTON_SKETCH_NAME)) {
                            // We're confident we are talking to a LightBlue Bean that
                            // is running the Button sketch.
                            postButtonDiscoveredEvent(true, discoveredBean.getDevice());
                        }
                    }
                });
            }

            @Override
            public void onConnectionFailed() {

            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onSerialMessageReceived(byte[] bytes) {

            }

            @Override
            public void onScratchValueChanged(int i, byte[] bytes) {

            }
        };
    }

    @Override
    public synchronized void stopButtonDiscovery() {
        Log.d(TAG, "Stopping Button Discovery...");

        discovering = false;

        getBeanManager().cancelDiscovery();
    }

    protected void postButtonDiscoveredEvent(final boolean success, final BluetoothDevice buttonDevice) {
        bus.post(new ButtonDiscoveryEvent(success, buttonDevice, ButtonType.LIGHTBLUE_BUTTON));
    }

    protected BeanManager getBeanManager() {
        return BeanManager.getInstance();
    }
}
