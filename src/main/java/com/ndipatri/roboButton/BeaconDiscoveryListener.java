package com.ndipatri.roboButton;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Region;

/**
 * Created by ndipatri on 1/19/15.
 */
public interface BeaconDiscoveryListener {
    public void beaconDistanceUpdate(Beacon beacon, double distanceInMeters);
    public void leftRegion(Region region);
}
