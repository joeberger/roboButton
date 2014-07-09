package com.ndipatri.arduinoButton.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import com.estimote.sdk.Beacon;
import com.ndipatri.arduinoButton.ArduinoButtonApplication;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;
import com.ndipatri.arduinoButton.events.ButtonImageRequestEvent;
import com.ndipatri.arduinoButton.events.ButtonImageResponseEvent;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import java.io.FileNotFoundException;
import java.io.InputStream;

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

        fragment.setBeacon(beacon);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ((ArduinoButtonApplication)getActivity().getApplication()).inject(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String dialogTitle = getResources().getString(R.string.configure_beacon);
        TextView titleView = new TextView(getActivity());
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleView.setText(dialogTitle);
        titleView.setGravity(Gravity.CENTER);

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_beacon_details, null);

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
                        need to create a local object that represents a BT Beacon....
                        beaconProvider.createOrUpdateButton(new Button(getButtonId(), nameEditText.getText().toString(), autoModeSwitch.isChecked(), iconFileNameString));
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

        /**
        Button existingButton = buttonProvider.getButton(getButtonId());
        if (existingButton != null) {
            nameEditText.setText(existingButton.getName());
            autoModeSwitch.setChecked(existingButton.isAutoModeEnabled());

            iconFileNameString = existingButton.getIconFileName();
            // NJD TODO - Need to retrieve image from file name and populate the overlayImageButton view
        }
         **/
    }

    private synchronized Beacon getBeacon() {
        return getArguments().getParcelable("beacon");
    }

    private synchronized void setBeacon(Beacon beacon) {
        getArguments().putParcelable("beacon", beacon);
    }
}

