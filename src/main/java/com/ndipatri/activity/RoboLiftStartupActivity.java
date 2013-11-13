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
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ndipatri.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.InjectView;
import butterknife.Views;

public class RoboLiftStartupActivity extends Activity {

    private static final String TAG = RoboLiftStartupActivity.class.getCanonicalName();

    private static final int REQUEST_ENABLE_BT = 1;

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

            SpinnerAdapter adapter = new SpinnerAdapter(this,
                                                        R.layout.spinner_layout,
                                                        R.id.textView,
                                                        new ArrayList<BluetoothDevice>(pairedDevices));

            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (selectedOnce) {

                        ViewHolder viewHolder = (ViewHolder) view.getTag();

                        Intent controllerIntent = new Intent(RoboLiftStartupActivity.this, MainControllerActivity.class);
                        controllerIntent.putExtra(MainControllerActivity.EXTERNAL_BLUETOOTH_DEVICE_MAC, viewHolder.bluetoothDevice);
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
        BluetoothDevice bluetoothDevice;

    }

    private static class SpinnerAdapter extends BaseAdapter {

        private LayoutInflater inflater;
        private int selectionLayoutId;
        private int textViewId;
        private List<BluetoothDevice> bluetoothDevices;

        public SpinnerAdapter(Context context, int selectionLayoutId, int textViewId, List<BluetoothDevice> bluetoothDevices) {
            inflater = LayoutInflater.from(context);

            this.bluetoothDevices = bluetoothDevices;
            this.selectionLayoutId = selectionLayoutId;
            this.textViewId = textViewId;
        }

        @Override
        public int getCount() {
            return bluetoothDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return bluetoothDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder;
            if (view == null) {
                view = inflater.inflate(selectionLayoutId , parent, false);

                holder = new ViewHolder();
                holder.deviceTextView = (TextView) view.findViewById(textViewId);
                view.setTag(holder);
            }
            holder = (ViewHolder) view.getTag();

            holder.bluetoothDevice = (BluetoothDevice) getItem(position);
            holder.deviceTextView.setText(holder.bluetoothDevice.getName());

            return view;
        }
    }
}
