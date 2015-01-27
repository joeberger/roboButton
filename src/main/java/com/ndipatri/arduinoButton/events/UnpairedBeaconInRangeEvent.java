package com.ndipatri.arduinoButton.events;

import com.estimote.sdk.Beacon;

/**
 * Created by ndipatri on 1/1/14.
 */
public class UnpairedBeaconInRangeEvent {

    public Beacon beacon;

    public UnpairedBeaconInRangeEvent(final Beacon beacon) {
        this.beacon = beacon;
    }
}
