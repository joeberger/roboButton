package com.ndipatri.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ndipatri.R;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.InjectView;
import butterknife.Views;

public class MainControllerActivity extends Activity {

    public static final String EXTERNAL_BLUETOOTH_DEVICE_MAC = "macAddress";

    private static final String TAG = MainControllerActivity.class.getCanonicalName();

    private BluetoothAdapter mBluetoothAdapter;

    protected @InjectView(R.id.textView) TextView textView;
    protected @InjectView(R.id.progressBar) android.widget.ProgressBar progressBar;
    protected @InjectView(R.id.toggleButton) ToggleButton toggleButton;
    protected @InjectView(R.id.toggleButton2) ToggleButton toggleButton2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_controller);

        Views.inject(this);

        BluetoothDevice selectedBluetoothDevice = getIntent().getParcelableExtra(EXTERNAL_BLUETOOTH_DEVICE_MAC);
        Toast.makeText(this, "Bluetooth Device Selected: '" + selectedBluetoothDevice + "'.", Toast.LENGTH_SHORT).show();
    }
}
