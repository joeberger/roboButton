package com.ndipatri.roboButton.dagger.bluetooth.communication.interfaces;

import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.models.Button;

/**
 * Communicates with each individual Button
 */
public interface ButtonCommunicator {
    void shutdown();
    Button getButton();
    ButtonState getButtonState();
}
