package com.ndipatri.roboButton.dagger.providers.stubs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.providers.interfaces.ButtonDiscoveryProvider;
import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.events.ButtonDiscoveryEvent;
import com.ndipatri.roboButton.models.Region;
import com.ndipatri.roboButton.utils.BusProvider;
import com.ndipatri.roboButton.utils.RegionUtils;

import javax.inject.Inject;

import nl.littlerobots.bean.Bean;
import nl.littlerobots.bean.BeanDiscoveryListener;
import nl.littlerobots.bean.BeanListener;
import nl.littlerobots.bean.BeanManager;
import nl.littlerobots.bean.message.Callback;
import nl.littlerobots.bean.message.SketchMetaData;

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
                postButtonDiscoveredEvent(new Region(1, 2, RegionUtils.LIGHTBLUE_UUID));
            }
        }
    };

    public synchronized void stopButtonDiscovery() {
        Log.d(TAG, "Stopping Button Discovery...");

        discovering = false;
    }

    protected void postButtonDiscoveredEvent(final boolean success, final BluetoothDevice buttonDevice) {
        bus.post(new ButtonDiscoveryEvent(success, buttonDevice, ButtonType.LIGHTBLUE_BUTTON));
        need to figure out how to remove buttonDevice here or absract it.. the devie itself is used in purpleButton communicator
    }

}
