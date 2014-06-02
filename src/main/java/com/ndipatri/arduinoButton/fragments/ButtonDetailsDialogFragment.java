package com.ndipatri.arduinoButton.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;
import com.ndipatri.arduinoButton.events.ButtonImageRequestEvent;
import com.ndipatri.arduinoButton.events.ButtonImageResponseEvent;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import java.io.FileNotFoundException;
import java.io.InputStream;

import butterknife.InjectView;
import butterknife.Views;

public class ButtonDetailsDialogFragment extends DialogFragment {


    private Animation shrinkAnimation = null;

    // ButterKnife Injected Views
    protected
    @InjectView(R.id.nameEditText)
    EditText nameEditText;
    protected
    @InjectView(R.id.autoModeSwitch)
    Switch autoModeSwitch;
    protected
    @InjectView(R.id.overlayImageButton)
    ImageButton overlayImageButton;

    // NJD TODO - Should use Dagger for this to be cool.
    protected ButtonProvider buttonProvider = new ButtonProvider();

    // Need to integrate this with view..
    protected String iconFileNameString = "";

    public static ButtonDetailsDialogFragment newInstance(String buttonId) {

        ButtonDetailsDialogFragment fragment = new ButtonDetailsDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);

        fragment.setButtonId(buttonId);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        shrinkAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.button_shrink);
        shrinkAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                requestImageFromUser();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

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
                        buttonProvider.createOrUpdateButton(getActivity(), new Button(getButtonId(), nameEditText.getText().toString(), autoModeSwitch.isChecked(), iconFileNameString));
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

        overlayImageButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    overlayImageButton.startAnimation(shrinkAnimation);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    protected void populateViewsWithExistingData() {

        Button existingButton = buttonProvider.getButton(getActivity(), getButtonId());
        if (existingButton != null) {
            nameEditText.setText(existingButton.getName());
            autoModeSwitch.setChecked(existingButton.isAutoModeEnabled());

            iconFileNameString = existingButton.getIconFileName();
            // NJD TODO - Need to retrieve image from file name and populate the overlayImageButton view
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

    @Subscribe
    public void onButtonImageResponseEvent(ButtonImageResponseEvent response) {
        Uri selectedImageUri = response.selectedImage;

        InputStream imageStream = null;
        try {
            imageStream = getActivity().getContentResolver().openInputStream(selectedImageUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
        overlayImageButton.setImageBitmap(selectedImage);
    }
}

