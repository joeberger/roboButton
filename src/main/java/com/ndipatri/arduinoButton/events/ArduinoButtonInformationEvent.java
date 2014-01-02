package com.ndipatri.arduinoButton.events;

import com.ndipatri.arduinoButton.activity.ArduinoButton;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ArduinoButtonInformationEvent {

    public String message;
    public ArduinoButton arduinoButton;

    public ArduinoButtonInformationEvent(String message, ArduinoButton arduinoButton) {
        this.message = message;
        this.arduinoButton = arduinoButton;
    }
}
