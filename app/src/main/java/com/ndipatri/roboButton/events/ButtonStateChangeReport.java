package com.ndipatri.roboButton.events;

import com.ndipatri.roboButton.enums.ButtonState;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ButtonStateChangeReport {

    public ButtonState buttonState;
    public String buttonId;

    public ButtonStateChangeReport(final String buttonId, final ButtonState buttonState) {
        this.buttonId = buttonId;
        this.buttonState = buttonState;
    }

    public ButtonState getButtonState() {
        return buttonState;
    }

    public void setButtonState(ButtonState buttonState) {
        this.buttonState = buttonState;
    }

    public String getButtonId() {
        return buttonId;
    }

    public void setButtonId(String buttonId) {
        this.buttonId = buttonId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ButtonStateChangeReport that = (ButtonStateChangeReport) o;

        if (buttonId != null ? !buttonId.equals(that.buttonId) : that.buttonId != null)
            return false;
        if (buttonState != that.buttonState) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = buttonState != null ? buttonState.hashCode() : 0;
        result = 31 * result + (buttonId != null ? buttonId.hashCode() : 0);
        return result;
    }
}