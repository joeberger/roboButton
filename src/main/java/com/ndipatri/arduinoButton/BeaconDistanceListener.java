package com.ndipatri.arduinoButton;

import com.estimote.sdk.Region;
import com.ndipatri.arduinoButton.models.Beacon;

/**
 * Created by ndipatri on 1/19/15.
 */
public interface BeaconDistanceListener {
    public void beaconDistanceUpdate(Beacon beacon, double distanceInMeters);
    public void leftRegion(Region region);
}
