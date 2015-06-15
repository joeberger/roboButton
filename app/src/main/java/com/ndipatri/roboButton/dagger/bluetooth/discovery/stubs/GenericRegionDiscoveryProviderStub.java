package com.ndipatri.roboButton.dagger.bluetooth.discovery.stubs;

import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.dagger.bluetooth.discovery.interfaces.RegionDiscoveryProvider;
import com.ndipatri.roboButton.events.RegionFoundEvent;
import com.ndipatri.roboButton.events.RegionLostEvent;
import com.ndipatri.roboButton.models.Region;
import com.ndipatri.roboButton.utils.BusProvider;
import com.ndipatri.roboButton.utils.RegionUtils;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

/**
 *
 * 10 seconds after starting, this stub will emit a RegionFound event.  30 seconds after that, it will
 * emit a RegionLost event.
 *
 * This behavior will continue regardless of background 'mode'
 *
 */
public class GenericRegionDiscoveryProviderStub implements RegionDiscoveryProvider {

    private static final String TAG = GenericRegionDiscoveryProviderStub.class.getCanonicalName();

    final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    // List of desired Regions.
    protected List<String> regionUUIDPatternList;

    // List of parse offsets associated with each Region UUID.
    protected List<Integer> regionUUIDOffsetList;

    private Context context;

    private boolean running = false;

    @Inject
    BusProvider bus;

    public GenericRegionDiscoveryProviderStub(final Context context, final String[] regionUUIDPatternArray, final Integer[] regionUUIDOffsetArray) {
        
        this.context = context;
        this.regionUUIDPatternList = Arrays.asList(regionUUIDPatternArray);
        this.regionUUIDOffsetList= Arrays.asList(regionUUIDOffsetArray);

        RBApplication.getInstance().getGraph().inject(this);

        bus.register(this);
    }

    // This is a non-blocking call.
    @Override
    public void startRegionDiscovery(final boolean inBackground) {

        running = true;

        Log.d(TAG, "Beginning Beacon Monitoring Process...");

        new Handler().postDelayed(beaconFoundRunnable, 10000);
    }

    @Override
    public void stopRegionDiscovery() throws RemoteException {
        Log.d(TAG, "Stopping region discovery ...");

        running = false;
    }

    private Runnable beaconFoundRunnable = new Runnable() {
        @Override
        public void run() {
            if (running) {
                Toast.makeText(context, "Beacon region found.", Toast.LENGTH_SHORT).show();
                postRegionFoundEvent(new Region(1, 2, RegionUtils.LIGHTBLUE_UUID));

                new Handler().postDelayed(beaconLostRunnable, 30000);
            }
        }
    };

    private Runnable beaconLostRunnable = new Runnable() {
        @Override
        public void run() {
            if (running) {
                Toast.makeText(context, "Beacon region lost.", Toast.LENGTH_SHORT).show();
                postRegionLostEvent(new Region(1, 2, RegionUtils.LIGHTBLUE_UUID));

                new Handler().postDelayed(beaconFoundRunnable, 10000);
            }
        }
    };

    protected void postRegionFoundEvent(final com.ndipatri.roboButton.models.Region region) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.post(new RegionFoundEvent(region));
            }
        });
    }

    protected void postRegionLostEvent(final com.ndipatri.roboButton.models.Region region) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                bus.post(new RegionLostEvent(region));
            }
        });
    }
}
