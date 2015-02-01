package com.ndipatri.arduinoButton.events;

import com.ndipatri.arduinoButton.models.Button;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ABLostEvent {

    public String buttonId;

    public ABLostEvent(final String buttonId) {
        this.buttonId = buttonId;
    }

    public String getButtonId() {
        return buttonId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ABLostEvent that = (ABLostEvent) o;

        if (buttonId != null ? !buttonId.equals(that.buttonId) : that.buttonId != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return buttonId != null ? buttonId.hashCode() : 0;
    }
}
