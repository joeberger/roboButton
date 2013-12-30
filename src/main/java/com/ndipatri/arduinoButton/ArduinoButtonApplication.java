package com.ndipatri.arduinoButton;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

public class ArduinoButtonApplication extends Application {

    public static final String APPLICATION_PREFS = "ArduinoButton.prefs";

    private SharedPreferences preferences;

    private static ArduinoButtonApplication instance = null;

    public static ArduinoButtonApplication getInstance() {
        return instance;
    }

    public ArduinoButtonApplication() {
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = getSharedPreferences(ArduinoButtonApplication.APPLICATION_PREFS, Context.MODE_PRIVATE);
    }
}
