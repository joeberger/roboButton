package com.ndipatri.arduinoButton.events;

import com.ndipatri.arduinoButton.enums.ButtonState;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ABStateChangeReport {

    public ButtonState newButtonState;
    public String buttonId;

    public ABStateChangeReport(final String buttonId, final ButtonState newButtonState) {
        this.buttonId = buttonId;
        this.newButtonState = newButtonState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ABStateChangeReport that = (ABStateChangeReport) o;

        if (buttonId != null ? !buttonId.equals(that.buttonId) : that.buttonId != null)
            return false;
        if (newButtonState != that.newButtonState) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = newButtonState != null ? newButtonState.hashCode() : 0;
        result = 31 * result + (buttonId != null ? buttonId.hashCode() : 0);
        return result;
    }
}
