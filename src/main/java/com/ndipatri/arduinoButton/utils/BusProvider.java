package com.ndipatri.arduinoButton.utils;

import com.squareup.otto.Bus;

/**
 * Created by ndipatri on 1/1/14.
 */
public class BusProvider {

    private static final Bus BUS = new Bus();

    public static Bus getInstance() {
        return BUS;
    }

    private BusProvider() {
        // No instances.
    }
}
