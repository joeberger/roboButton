package com.ndipatri.arduinoButton.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.estimote.sdk.Beacon;
import com.ndipatri.arduinoButton.ABApplication;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.dagger.providers.BeaconProvider;
import com.ndipatri.arduinoButton.utils.BusProvider;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.Views;

public class BeaconDetailsDialogFragment extends DialogFragment {

    // ButterKnife Injected Views
    protected @InjectView(R.id.nameEditText) EditText nameEditText;

    @Inject
    protected BeaconProvider beaconProvider;

    public static BeaconDetailsDialogFragment newInstance(Beacon beacon) {

        BeaconDetailsDialogFragment fragment = new BeaconDetailsDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);

        fragment.setMacAddress(beacon.getMacAddress());
        fragment.setMinor(beacon.getMinor());
        fragment.setMajor(beacon.getMajor());

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ((ABApplication)getActivity().getApplication()).inject(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String dialogTitle = getResources().getString(R.string.configure_beacon);
        TextView titleView = new TextView(getActivity());
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleView.setText(dialogTitle);
        titleView.setGravity(Gravity.CENTER);

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_beacon_details, null);

        Views.inject(this, dialogView);

        builder.setTitle(dialogTitle)
                //.setCustomTitle(titleView)
                .setView(dialogView)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        beaconProvider.createOrUpdateBeacon(new com.ndipatri.arduinoButton.models.Beacon(getMacAddress(), getMajor(), getMinor(), nameEditText.getText().toString()));
                    }
                });

        Dialog dialog = builder.create();

        dialog.getWindow().getAttributes().windowAnimations = R.style.slideup_dialog_animation;
        dialog.setCanceledOnTouchOutside(false);

        setupViews();

        return dialog;
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }

    private void setupViews() {
        populateViewsWithExistingData();
    }

    protected void populateViewsWithExistingData() {
        com.ndipatri.arduinoButton.models.Beacon existingBeacon = beaconProvider.getBeacon(getMacAddress());
        if (existingBeacon != null) {
            nameEditText.setText(existingBeacon.getName());
        }
    }

    public String getMacAddress() {
        return getArguments().getString("macAddress");
    }

    private void setMacAddress(String macAddress) {
        getArguments().putString("macAddress", macAddress);
    }

    public int getMinor() {
        return getArguments().getInt("minor");
    }

    private void setMinor(int minor) {
        getArguments().putInt("minor", minor);
    }

    public int getMajor() {
        return getArguments().getInt("major");
    }

    private void setMajor(int major) {
        getArguments().putInt("major", major);
    }
}

