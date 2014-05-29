package com.ndipatri.arduinoButton.events;

import android.net.Uri;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ButtonImageResponseEvent {

    public String buttonId;
    public Uri selectedImage;

    public ButtonImageResponseEvent(final String buttonId, final Uri selectedImage) {
        this.buttonId = buttonId;
        this.selectedImage = selectedImage;
    }
}
