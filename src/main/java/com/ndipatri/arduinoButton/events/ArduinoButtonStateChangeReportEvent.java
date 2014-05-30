package com.ndipatri.arduinoButton.events;

import com.ndipatri.arduinoButton.enums.ButtonState;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ArduinoButtonStateChangeReportEvent {

    public ButtonState newButtonState;
    public String buttonId;

    public ArduinoButtonStateChangeReportEvent(final String buttonId, final ButtonState newButtonState) {
        this.buttonId = buttonId;
        this.newButtonState = newButtonState;
    }
}
