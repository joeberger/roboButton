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
 * After 10 seconds, this stub will emit a ButtonFound even for the LightBlue Bean button type.
 */
public class LightBlueButtonDiscoveryProviderStub implements ButtonDiscoveryProvider {

    private static final String TAG = LightBlueButtonDiscoveryProviderStub.class.getCanonicalName();

    private Context context;

    protected boolean discovering = false;

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

        new Handler().postDelayed(buttonFoundRunnable, 10000);
    }

    private Runnable buttonFoundRunnable = new Runnable() {
        @Override
        public void run() {
            if (discovering) {
                Toast.makeText(context, "LightBlue Button Found.", Toast.LENGTH_LONG).show();
                bus.post(new ButtonDiscoveryEvent(true, ButtonType.LIGHTBLUE_BUTTON, "aa:bb:cc:dd:ee", null));
            }
        }
    };

    public synchronized void stopButtonDiscovery() {
        Log.d(TAG, "Stopping Button Discovery...");

        discovering = false;
    }

}
