package com.ndipatri.roboButton.events;

import com.ndipatri.roboButton.enums.ButtonState;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ButtonStateChangeRequest {

    public ButtonState requestedButtonState;
    public String buttonId;

    public ButtonStateChangeRequest(final String buttonId, final ButtonState requestedButtonState) {
        this.buttonId = buttonId;
        this.requestedButtonState = requestedButtonState;
    }
}
