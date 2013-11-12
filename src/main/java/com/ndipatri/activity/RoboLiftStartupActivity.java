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

import com.ndipatri.R;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.InjectView;
import butterknife.Views;

public class RoboLiftStartupActivity extends Activity {

    private static final String TAG = RoboLiftStartupActivity.class.getCanonicalName();

    private static final int REQUEST_ENABLE_BT = 1;

    private static final Pattern bluetoothNamePattern = Pattern.compile("^(.*?)\\(.*?\\)$");

    private BluetoothAdapter mBluetoothAdapter;

    protected @InjectView(R.id.textView) TextView textView;
    protected @InjectView(R.id.progressBar) android.widget.ProgressBar progressBar;
    protected @InjectView(R.id.spinner) Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        Views.inject(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device!", Toast.LENGTH_SHORT).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                setupBluetoothDeviceSpinner();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                setupBluetoothDeviceSpinner();
            } else {
                Toast.makeText(this, "This application cannot run without Bluetooth enabled!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean selectedOnce = false;
    private void setupBluetoothDeviceSpinner() {


        textView.setText(R.string.select_paired_device);
        progressBar.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.isEmpty()) {
            textView.setText(getString(R.string.no_paired_bluetooth_devices));
        } else {
            String[] deviceNames = new String[pairedDevices.size()];
            int index=0;
            for (BluetoothDevice bluetoothDevice : pairedDevices) {
                deviceNames[index++] = bluetoothDevice.getName() + "(" + bluetoothDevice.getAddress() + ")";
            }

            SpinnerAdapter adapter = new SpinnerAdapter(this,
                                                        R.layout.spinner_layout,
                                                        R.id.textView,
                                                        deviceNames);

            adapter.setDropDownViewResource(R.layout.spinner_layout);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (selectedOnce) {

                        ViewHolder viewHolder = (ViewHolder) view.getTag();

                        Toast.makeText(RoboLiftStartupActivity.this, "Bluetooth device selected:" + viewHolder.deviceName, Toast.LENGTH_SHORT).show();

                        Intent controllerIntent = new Intent(RoboLiftStartupActivity.this, MainControllerActivity.class);
                        controllerIntent.putExtra(MainControllerActivity.EXTERNAL_BLUETOOTH_DEVICE_MAC, viewHolder.deviceMacAddress);
                        startActivity(controllerIntent);
                        finish();
                    } else {
                        selectedOnce = true;
                    }
                }

                @Override public void onNothingSelected(AdapterView<?> parent) {}

            });
        }
    }

    private static class ViewHolder {
        TextView deviceTextView;
        String deviceName;
        String deviceMacAddress;

    }

    private static class SpinnerAdapter extends ArrayAdapter<String> {

        private int selectionLayoutId;
        private int textViewId;

        public SpinnerAdapter(Context context, int selectionLayoutId, int textViewId, String[] deviceDescriptions) {
            super(context, 0, deviceDescriptions);
            this.selectionLayoutId = selectionLayoutId;
            this.textViewId = textViewId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(selectionLayoutId , parent, false);

                holder = new ViewHolder();
                holder.deviceTextView = (TextView) view.findViewById(textViewId);
                view.setTag(holder);
            }
            holder = (ViewHolder) view.getTag();

            String deviceDescription = getItem(position);
            Matcher matcher = bluetoothNamePattern.matcher(deviceDescription);
            if (matcher.find())
            {
                holder.deviceMacAddress = matcher.group(1);
                holder.deviceName = matcher.group(0);
            }
            holder.deviceTextView.setText(deviceDescription);

            return view;
        }

        @Override
        public int getItemViewType(int position) {
            return -1;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }
    }
}
