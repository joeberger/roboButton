package com.ndipatri.arduinoButton.events;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ArduinoButtonStateChangeEvent {

    public String buttonId;

    public ArduinoButtonStateChangeEvent(String buttonId) {
        this.buttonId = buttonId;
    }
}
