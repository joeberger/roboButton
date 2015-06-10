package com.ndipatri.roboButton.utils;

import android.content.Context;

import com.ndipatri.roboButton.enums.ButtonType;
import com.ndipatri.roboButton.models.Button;

/**
 * Communicates with each individual Button
 */
public class ButtonCommunicatorFactory {

    private static final String TAG = ButtonCommunicatorFactory.class.getCanonicalName();

    public static ButtonCommunicator getButtonCommunicator(final Context context, final Button button) {

        switch(ButtonType.getByType(button.getType())) {
            case PURPLE_BUTTON:
                return new PurpleButtonCommunicator(context, button);
            case LIGHTBLUE_BUTTON:
                return new LightBlueButtonCommunicator(context, button);
        }

        return null;
    }
}
