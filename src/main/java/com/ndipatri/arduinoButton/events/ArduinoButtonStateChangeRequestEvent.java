package com.ndipatri.arduinoButton.events;

import com.ndipatri.arduinoButton.enums.ButtonState;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ArduinoButtonStateChangeRequestEvent {

    public ButtonState requestedButtonState;
    public String buttonId;

    public ArduinoButtonStateChangeRequestEvent(final String buttonId, final ButtonState requestedButtonState) {
        this.buttonId = buttonId;
        this.requestedButtonState = requestedButtonState;
    }
}
