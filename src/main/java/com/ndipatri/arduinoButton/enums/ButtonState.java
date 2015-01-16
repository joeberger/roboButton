package com.ndipatri.arduinoButton.enums;

import com.ndipatri.arduinoButton.R;

/**
 * Created by ndipatri on 5/29/14.
 */
public enum ButtonState {
    NEVER_CONNECTED(R.string.is_connecting, false, false, false, R.drawable.yellow_button, R.drawable.yellow_button_small),
    ON(R.string.on, true, true, true, R.drawable.green_button, R.drawable.green_button_small),
    OFF(R.string.off, false, true, true, R.drawable.red_button, R.drawable.red_button_small),
    ON_PENDING(R.string.is_connecting, true, false, false, R.drawable.yellow_button, R.drawable.yellow_button_small),
    OFF_PENDING(R.string.is_connecting, false, false, false, R.drawable.yellow_button, R.drawable.yellow_button_small),
    DISCONNECTED(R.string.disconnected, false, false, false, R.drawable.yellow_button, R.drawable.yellow_button_small),
    ;

    public int descriptionResId;

    public boolean isCommunicating;

    public boolean value;

    // Is button enabled in this state
    public boolean enabled;

    public int drawableResourceId;

    public int smallDrawableResourceId;

    private ButtonState(final int descriptionResId, final boolean value, final boolean isCommunicating, final boolean enabled, final int drawableResourceId, final int smallDrawableResourceId) {
        this.descriptionResId = descriptionResId;
        this.value = value;
        this.isCommunicating = isCommunicating;
        this.enabled = enabled;
        this.drawableResourceId = drawableResourceId;
        this.smallDrawableResourceId = smallDrawableResourceId;
    }
}
