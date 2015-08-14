package com.ndipatri.roboButton.dagger.bluetooth.discovery.impl;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.communication.impl.LightBlueButtonCommunicatorImpl;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.dagger.daos.ButtonDao;
import com.punchthrough.bean.sdk.Bean;
import com.punchthrough.bean.sdk.BeanDiscoveryListener;
import com.punchthrough.bean.sdk.BeanListener;
import com.punchthrough.bean.sdk.BeanManager;
import com.punchthrough.bean.sdk.message.BeanError;
import com.punchthrough.bean.sdk.message.Callback;
import com.punchthrough.bean.sdk.message.ScratchBank;
import com.punchthrough.bean.sdk.message.SketchMetadata;

import javax.inject.Inject;

/**
 * This class will look for LightBlue Bean 'beacons' and if confirmed that it is running the right Arduino 'sketch', we will
 * declare this a button as well.
 *
 * After a defined timeout period a 'success' or 'failure' event will be emitted based on whether a device matching the
 * defined 'discoveryPattern' was found.
 */
public class LightBlueButtonDiscoveryProviderImpl extends ButtonDiscoveryProvider {

    private static final String TAG = LightBlueButtonDiscoveryProviderImpl.class.getCanonicalName();

    private static final String BUTTON_SKETCH_PREFIX = "lightBlueButton";

    String discoverableButtonPatternString;

    public LightBlueButtonDiscoveryProviderImpl(Context context) {
        super(context);

        RBApplication.getInstance().getGraph().inject(this);

        discoverableButtonPatternString = context.getString(R.string.button_discovery_pattern);
    }

    @Override
    public synchronized void _startButtonDiscovery() {

        Log.d(TAG, "Beginning LightBlue Button Discovery Process...");

        getBeanManager().startDiscovery(getButtonDiscoveryListener());

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                buttonDiscoveryFinished();
            }
        }, context.getResources().getInteger(R.integer.light_blue_button_discovery_period_millis));
    }

    protected BeanDiscoveryListener getButtonDiscoveryListener() {
        return new BeanDiscoveryListener() {
            @Override
            public void onBeanDiscovered(Bean discoveredBean, int receivedRSSI) {
                Log.d(TAG, "onBeanDiscovered():");
                discoveredBean.connect(context, getBeanConnectionListener(discoveredBean));
            }

            @Override
            public void onDiscoveryComplete() {
                Log.d(TAG, "onBeanDiscoveryComplete():");

                buttonDiscoveryFinished();
            }
        };
    }

    protected BeanListener getBeanConnectionListener(final Bean discoveredBean) {
        return new BeanListener() {

            @Override
            public void onConnected() {
                Log.d(TAG, "onConnected");
                discoveredBean.readSketchMetadata(new Callback<SketchMetadata>() {
                    @Override
                    public void onResult(SketchMetadata sketchMetaData) {
                        String sketchName = sketchMetaData.hexName();
                        discoveredBean.disconnect();

                        if (sketchName.contains(BUTTON_SKETCH_PREFIX)) {
                            // We're confident we are talking to a LightBlue Bean that
                            // is running the Button sketch.
                            startButtonCommunicator(discoveredBean.getDevice());
                        }
                    }
                });
            }

            @Override
            public void onConnectionFailed() {
                Log.d(TAG, "onConnectionFailed");
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "onDisconnected");
            }

            @Override
            public void onSerialMessageReceived(byte[] bytes) {
                Log.d(TAG, "onSerialMessageReceived");
            }

            @Override
            public void onError(BeanError beanError) {
                Log.d(TAG, "onError()");
            }

            @Override
            public void onScratchValueChanged(ScratchBank scratchBank, byte[] bytes) {
                Log.d(TAG, "onScratchValueChanged");
            }
        };
    }

    @Override
    public synchronized void _stopButtonDiscovery() {
        Log.d(TAG, "Stopping Button Discovery...");

        getBeanManager().cancelDiscovery();
    }


    protected BeanManager getBeanManager() {
        return BeanManager.getInstance();
    }

    protected void startButtonCommunicator(BluetoothDevice discoveredDevice) {
        new LightBlueButtonCommunicatorImpl(context, discoveredDevice, discoveredDevice.getAddress());
    }
}
