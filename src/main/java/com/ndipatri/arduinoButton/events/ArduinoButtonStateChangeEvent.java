package com.ndipatri.arduinoButton.events;

import com.ndipatri.arduinoButton.activity.ArduinoButton;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ArduinoButtonStateChangeEvent {

    public ArduinoButton arduinoButton;

    public ArduinoButtonStateChangeEvent(ArduinoButton arduinoButton) {
        this.arduinoButton = arduinoButton;
    }
}
