package com.ndipatri.arduinoButton.events;

import com.ndipatri.arduinoButton.enums.ButtonState;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ndipatri on 1/1/14.
 */
public class ABStateChangeReport {

    Set<ABStateChangeReportValue> changes = new HashSet<ABStateChangeReportValue>();

    public ABStateChangeReport(Set<ABStateChangeReportValue> changes) {
        this.changes = changes;
    }

    public ABStateChangeReport(ABStateChangeReportValue change) {
        changes.add(change);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ABStateChangeReport that = (ABStateChangeReport) o;

        if (changes != null ? !changes.equals(that.changes) : that.changes != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return changes != null ? changes.hashCode() : 0;
    }

    public static class ABStateChangeReportValue {
        public ButtonState buttonState;
        public String buttonId;

        public ABStateChangeReportValue(final ButtonState buttonState, final String buttonId) {
            this.buttonId = buttonId;
            this.buttonState = buttonState;
        }

        public ButtonState getButtonState() {
            return buttonState;
        }

        public String getButtonId() {
            return buttonId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ABStateChangeReportValue that = (ABStateChangeReportValue) o;

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

    public Set<ABStateChangeReportValue> getChanges() {
        return changes;
    }
}
