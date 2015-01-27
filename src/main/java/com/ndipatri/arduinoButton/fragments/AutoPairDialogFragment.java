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
import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.BusProvider;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.Views;

public class AutoPairDialogFragment extends DialogFragment {

    // ButterKnife Injected Views
    protected @InjectView(R.id.text1TextView) TextView text1TextView;
    protected @InjectView(R.id.text2TextView) TextView text2TextView;

    @Inject
    protected BeaconProvider beaconProvider;

    @Inject
    protected ButtonProvider buttonProvider;

    public static AutoPairDialogFragment newInstance(Beacon beacon, String buttonId) {

        AutoPairDialogFragment fragment = new AutoPairDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);

        fragment.setMacAddress(beacon.getMacAddress());
        fragment.setButtonId(buttonId);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ((ABApplication)getActivity().getApplication()).inject(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String dialogTitle = getResources().getString(R.string.unpaired_beacon_detected);
        TextView titleView = new TextView(getActivity());
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleView.setText(dialogTitle);
        titleView.setGravity(Gravity.CENTER);

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_auto_pair_details, null);

        Views.inject(this, dialogView);

        builder.setTitle(dialogTitle)
                //.setCustomTitle(titleView)
                .setView(dialogView)
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Button button = buttonProvider.getButton(getButtonId());
                        beaconProvider.createOrUpdateBeacon(new com.ndipatri.arduinoButton.models.Beacon(getMacAddress(), "Beacon for " + button.getName()));
                        com.ndipatri.arduinoButton.models.Beacon beacon = beaconProvider.getBeacon(getMacAddress());

                        button.setBeacon(beacon);
                        beacon.setButton(button);

                        buttonProvider.createOrUpdateButton(button);
                        beaconProvider.createOrUpdateBeacon(beacon); // transitive persistence sucks in
                                                                     // ormLite so we need to be explicit here...
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
        Button button = buttonProvider.getButton(getButtonId());

        text1TextView.setText("Beacon Found ('" + getMacAddress() + "')");
        text2TextView.setText("Shall we pair it with the '" + button.getName() + "' Button?");
    }

    public String getMacAddress() {
        return getArguments().getString("macAddress");
    }

    private void setMacAddress(String macAddress) {
        getArguments().putString("macAddress", macAddress);
    }

    public String getButtonId() {
        return getArguments().getString("buttonId");
    }

    private void setButtonId(String buttonId) {
        getArguments().putString("buttonId", buttonId);
    }
}

