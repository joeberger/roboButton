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
    }

    public Set<ABStateChangeReportValue> getChanges() {
        return changes;
    }
}
