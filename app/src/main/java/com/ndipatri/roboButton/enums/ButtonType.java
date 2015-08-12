package com.ndipatri.roboButton.enums;

public enum ButtonType {

    PURPLE_BUTTON(1),
    LIGHTBLUE_BUTTON(2),
    COMPOSITE_BUTTON(3),
    UNKNOWN(3);

    private int type;

    ButtonType(final int type) {
        this.type = type;
    }

    public static ButtonType getByType(final int type) {
        for (ButtonType buttonType : values()) {
            if (buttonType.type == type) {
                return buttonType;
            }
        }

        return UNKNOWN;
    }

    public int getTypeValue() {
        return type;
    }
}
