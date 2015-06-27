package com.ndipatri.roboButton.events;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ButtonUpdatedEvent {

    public String buttonId;

    public ButtonUpdatedEvent(final String buttonId) {
        this.buttonId = buttonId;
    }

    public String getButtonId() {
        return buttonId;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ButtonUpdatedEvent that = (ButtonUpdatedEvent) o;

        if (buttonId != null ? !buttonId.equals(that.buttonId) : that.buttonId != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return buttonId != null ? buttonId.hashCode() : 0;
    }
}
