package com.ndipatri.roboButton.enums;

import com.ndipatri.roboButton.R;

/**
 * Created by ndipatri on 5/29/14.
 */
public enum ButtonState {
    OFFLINE(R.string.offline, false, false, R.drawable.yellow_button, R.drawable.yellow_button_small),
    ON(R.string.on, true, true, R.drawable.green_button, R.drawable.green_button_small),
    OFF(R.string.off, false, true, R.drawable.red_button, R.drawable.red_button_small),
    ON_PENDING(R.string.is_connecting, true, false, R.drawable.yellow_button, R.drawable.yellow_button_small),
    OFF_PENDING(R.string.is_connecting, false, false, R.drawable.yellow_button, R.drawable.yellow_button_small),
    CONNECTING(R.string.connecting, false, false, R.drawable.yellow_button, R.drawable.yellow_button_small),
    ;

    public int descriptionResId;

    public boolean value;

    // Is button enabled in this state
    public boolean enabled;

    public int drawableResourceId;

    public int smallDrawableResourceId;

    private ButtonState(final int descriptionResId, final boolean value, final boolean enabled, final int drawableResourceId, final int smallDrawableResourceId) {
        this.descriptionResId = descriptionResId;
        this.value = value;
        this.enabled = enabled;
        this.drawableResourceId = drawableResourceId;
        this.smallDrawableResourceId = smallDrawableResourceId;
    }
}
