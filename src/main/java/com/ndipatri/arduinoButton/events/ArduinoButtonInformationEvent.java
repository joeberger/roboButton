package com.ndipatri.arduinoButton.events;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ArduinoButtonInformationEvent {

    public String message;
    public String buttonDescription;
    public String buttonId;

    public ArduinoButtonInformationEvent(String message, String buttonDescription, String buttonId) {
        this.message = message;
        this.buttonDescription = buttonDescription;
        this.buttonId = buttonId;
    }
}
