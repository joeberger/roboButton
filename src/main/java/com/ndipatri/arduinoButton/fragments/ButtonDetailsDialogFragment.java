package com.ndipatri.arduinoButton.fragments;

import android.app.Activity;
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
import android.widget.Switch;
import android.widget.TextView;

import com.ndipatri.arduinoButton.ABApplication;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.dagger.providers.BeaconProvider;
import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;
import com.ndipatri.arduinoButton.events.ButtonImageRequestEvent;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.BusProvider;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.Views;

public class ButtonDetailsDialogFragment extends DialogFragment {

    private static final String TAG = ButtonDetailsDialogFragment.class.getCanonicalName();

    // ButterKnife Injected Views
    protected @InjectView(R.id.nameEditText) EditText nameEditText;
    protected @InjectView(R.id.autoModeSwitch) Switch autoModeSwitch;
    protected @InjectView(R.id.pairButton) android.widget.Button pairButton;

    @Inject
    protected ButtonProvider buttonProvider;

    @Inject
    protected BeaconProvider beaconProvider;

    public static ButtonDetailsDialogFragment newInstance(String buttonId) {

        ButtonDetailsDialogFragment fragment = new ButtonDetailsDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);

        fragment.setButtonId(buttonId);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ((ABApplication)getActivity().getApplication()).inject(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String dialogTitle = getResources().getString(R.string.configure_button);
        TextView titleView = new TextView(getActivity());
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleView.setText(dialogTitle);
        titleView.setGravity(Gravity.CENTER);

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_button_details, null);

        // Use ButterKnife for view injection (http://jakewharton.github.io/butterknife/)
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

                        Button button = getButton();

                        button.setName(nameEditText.getText().toString());
                        button.setAutoModeEnabled(autoModeSwitch.isChecked());

                        if (shouldUnpair()) {
                            beaconProvider.delete(button.getBeacon());
                            button.setBeacon(null);
                        }

                        buttonProvider.createOrUpdateButton(button);
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

    protected Button getButton() {
        return buttonProvider.getButton(getButtonId());
    }

    protected void populateViewsWithExistingData() {

        final Button existingButton = getButton();

        nameEditText.setText(existingButton.getName());
        autoModeSwitch.setChecked(existingButton.isAutoModeEnabled());

        if (existingButton.getBeacon() != null) {

            // We need to give user the ability to unpair, since a pairing exists...

            setPairButtonState(true);

            pairButton.setEnabled(true);
            pairButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // We're essentially implementing a 'toggle' here
                    togglePairButtonState();
                }
            });
        } else {
            setPairButtonState(false);
            pairButton.setEnabled(false);
        }
    }

    protected boolean shouldUnpair() {
        return getButton().getBeacon() != null &&
               pairButton.getText().equals(getString(R.string.not_paired_with_beacon));
    }

    protected void togglePairButtonState() {
        if (shouldUnpair()) {
            setPairButtonState(true);
        } else {
            setPairButtonState(false);
        }
    }

    protected void setPairButtonState(final boolean paired) {
        if (paired) {
            pairButton.setText(getString(R.string.unpair_from_beacon));
        } else {
            pairButton.setText(getString(R.string.not_paired_with_beacon));
        }
    }

    private synchronized String getButtonId() {
        return getArguments().getString("buttonId");
    }

    private synchronized void setButtonId(String buttonId) {
        getArguments().putString("buttonId", buttonId);
    }

    protected void requestImageFromUser() {
        BusProvider.getInstance().post(new ButtonImageRequestEvent(getButtonId()));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public Switch getAutoModeSwitch() {
        return autoModeSwitch;
    }
}

