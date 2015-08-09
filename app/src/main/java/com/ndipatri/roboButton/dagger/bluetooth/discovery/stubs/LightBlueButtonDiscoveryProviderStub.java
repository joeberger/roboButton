package com.ndipatri.roboButton.dagger.bluetooth.discovery.stubs;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.events.ButtonDiscoveryEvent;
import com.ndipatri.roboButton.utils.BusProvider;

import javax.inject.Inject;

/**
 * After 5 seconds, this stub will emit a ButtonFound even for the LightBlue Bean button type.
 * 5 seconds after that, this stub will emit another ButtonFound even for the LightBlue Bean button type.
 */
public class LightBlueButtonDiscoveryProviderStub implements ButtonDiscoveryProvider {

    private static final String TAG = LightBlueButtonDiscoveryProviderStub.class.getCanonicalName();

    private Context context;

    protected boolean discovering = false;

    protected int DISCOVERY_DELAY_MILLIS = 5000;
    protected int NUMBER_OF_DISCOVERED_BUTTONS = 2;

    protected int buttonCount = 0;

    @Inject
    BusProvider bus;

    public LightBlueButtonDiscoveryProviderStub(Context context) {
        this.context = context;

        RBApplication.getInstance().getGraph().inject(this);
    }

    @Override
    public synchronized void startButtonDiscovery() {

        Log.d(TAG, "Beginning LightBlue Button Monitoring Process...");
        if (discovering) {
            // make this request idempotent
            return;
        }

        discovering = true;
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
                Toast.makeText(context, "LightBlue Button Found.", Toast.LENGTH_SHORT).show();
                buttonCount++;
                bus.post(new ButtonDiscoveryEvent(true, ButtonType.LIGHTBLUE_BUTTON, getButtonId(), null));

                if (buttonCount < NUMBER_OF_DISCOVERED_BUTTONS) {
                    discoverButtonAfterDelay();
                }
            }
        }
    };

    public synchronized void stopButtonDiscovery() {
        Log.d(TAG, "Stopping Button Discovery...");

        discovering = false;

    }

    private String[] octets = new String[] {"aa", "bb", "cc", "dd", "ee"};

    private String getButtonId() {

        StringBuilder buttonIdBuf = new StringBuilder();

        for (int i=0; i < octets.length; i++) {
            int index = (int)(Math.random()*5.0);

            buttonIdBuf.append(octets[index]);

            if (i < octets.length-1) {
                buttonIdBuf.append(":");
            }
        }
        return buttonIdBuf.toString();
    }
}
