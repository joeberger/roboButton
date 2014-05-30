package com.ndipatri.arduinoButton.enums;

import com.ndipatri.arduinoButton.R;

/**
 * Created by ndipatri on 5/29/14.
 */
public enum ButtonState {
    NEVER_CONNECTED(false, true, false, R.drawable.yellow_button),
    ON(true, true, true, R.drawable.green_button),
    OFF(false, true, true, R.drawable.red_button),
    ON_PENDING(true, true, false, R.drawable.yellow_button),
    OFF_PENDING(false, true, false, R.drawable.yellow_button),
    DISCONNECTED(false, false, false, R.drawable.yellow_button),
    ;

    public boolean isCommunicating;

    public boolean value;

    // Is button enabled in this state
    public boolean enabled;

    public int drawableResourceId;

    private ButtonState(final boolean value, final boolean isCommunicating, final boolean enabled, final int drawableResourceId) {
        this.value = value;
        this.isCommunicating = isCommunicating;
        this.enabled = enabled;
        this.drawableResourceId = drawableResourceId;
    }
}
