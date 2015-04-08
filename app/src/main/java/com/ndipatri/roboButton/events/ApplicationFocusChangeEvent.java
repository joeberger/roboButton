package com.ndipatri.roboButton.events;

/**
 * Created by ndipatri on 1/1/14.
 */
// Informational event only
public class ApplicationFocusChangeEvent {

    public boolean inBackground;

    public ApplicationFocusChangeEvent(final boolean inBackground) {
        this.inBackground = inBackground;
    }
}

