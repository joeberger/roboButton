package com.ndipatri.roboButton.dagger.bluetooth.discovery.stubs;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.communication.stubs.GenericButtonCommunicatorStub;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.ButtonDiscoveryProvider;

/**
 * After 5 seconds, this stub will emit a ButtonFound even for the LightBlue Bean button type.
 * 5 seconds after that, this stub will emit another ButtonFound even for the LightBlue Bean button type.
 */
public class GenericButtonDiscoveryProviderStub extends ButtonDiscoveryProvider {

    private static final String TAG = GenericButtonDiscoveryProviderStub.class.getCanonicalName();

    protected int DISCOVERY_DELAY_MILLIS = 5000;
    protected int NUMBER_OF_DISCOVERED_BUTTONS = 2;

    protected int buttonCount = 0;

    private String buttonLabelPrefix;

    public GenericButtonDiscoveryProviderStub(Context context, String buttonLabelPrefix) {
        super(context);

        RBApplication.getInstance().getGraph().inject(this);

        this.buttonLabelPrefix = buttonLabelPrefix;
    }

    @Override
    public synchronized void _startButtonDiscovery() {

        Log.d(TAG, "Beginning '" + buttonLabelPrefix + "' Button Monitoring Process...");
        buttonCount = 0;

        discoverButtonAfterDelay();
    }

    protected void discoverButtonAfterDelay() {
        new Handler().postDelayed(buttonFoundRunnable, DISCOVERY_DELAY_MILLIS);
    }

    private Runnable buttonFoundRunnable = new Runnable() {
        @Override
        public void run() {
            if (discovering) {
                Toast.makeText(context, "'" + buttonLabelPrefix + "' Button Found.", Toast.LENGTH_SHORT).show();
                buttonCount++;
                startButtonCommunicator(null);

                if (buttonCount < NUMBER_OF_DISCOVERED_BUTTONS) {
                    discoverButtonAfterDelay();
                } else {
                    buttonDiscoveryFinished();
                }
            }
        }
    };

    public synchronized void _stopButtonDiscovery() {}

    protected void startButtonCommunicator(BluetoothDevice discoveredDevice) {
        new GenericButtonCommunicatorStub(context, discoveredDevice, getButtonId());
    }

    private String[] octets = new String[] {"aa", "bb", "cc"};
    private String getButtonId() {

        StringBuilder buttonIdBuf = new StringBuilder(buttonLabelPrefix).append(":");

        for (int i=0; i < octets.length; i++) {
            int index = (int)(Math.random()*octets.length);

            buttonIdBuf.append(octets[index]);

            if (i < octets.length-1) {
                buttonIdBuf.append(":");
            }
        }
        return buttonIdBuf.toString();
    }
}
