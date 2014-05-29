package com.ndipatri.arduinoButton.events;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ButtonImageRequestEvent {

    public String buttonId;

    public ButtonImageRequestEvent(final String buttonId) {
        this.buttonId = buttonId;
    }
}
