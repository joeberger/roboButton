package com.ndipatri.arduinoButton.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
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
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.ndipatri.arduinoButton.ABApplication;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.dagger.providers.BeaconProvider;
import com.ndipatri.arduinoButton.dagger.providers.ButtonProvider;
import com.ndipatri.arduinoButton.events.ButtonImageRequestEvent;
import com.ndipatri.arduinoButton.events.ButtonImageResponseEvent;
import com.ndipatri.arduinoButton.models.Beacon;
import com.ndipatri.arduinoButton.models.Button;
import com.ndipatri.arduinoButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.Views;

public class ButtonDetailsDialogFragment extends DialogFragment {

    private static final String TAG = ButtonDetailsDialogFragment.class.getCanonicalName();

    private Animation shrinkAnimation = null;

    // ButterKnife Injected Views
    protected @InjectView(R.id.nameEditText) EditText nameEditText;
    protected @InjectView(R.id.autoModeSwitch) Switch autoModeSwitch;
    protected @InjectView(R.id.overlayImageButton) ImageButton overlayImageButton;
    protected @InjectView(R.id.beaconSpinner) Spinner beaconSpinner;

    @Inject
    protected ButtonProvider buttonProvider;

    @Inject
    protected BeaconProvider beaconProvider;

    // Need to integrate this with view..
    protected String iconFileNameString = "";

    private LayoutInflater inflater;

    public static ButtonDetailsDialogFragment newInstance(String buttonId) {

        ButtonDetailsDialogFragment fragment = new ButtonDetailsDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);

        fragment.setButtonId(buttonId);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        this.inflater = LayoutInflater.from(getActivity());

        ((ABApplication)getActivity().getApplication()).inject(this);

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

                        // Until now, we don't necessarily have a persisted Button (for example, we could
                        // just have a Button that is nearby.... if the user 'saves', we remember this button
                        Button button = getButton();
                        if (button == null) {
                            button = new Button();
                            button.setId(getButtonId());
                        }

                        button.setName(nameEditText.getText().toString());
                        button.setAutoModeEnabled(autoModeSwitch.isChecked());
                        button.setIconFileName(iconFileNameString);

                        Beacon selectedBeacon = (Beacon) beaconSpinner.getSelectedItem();

                        if (selectedBeacon.getName().equals("None")) {
                            if (button.getBeacon() != null) {
                                button.setBeacon(null);
                            }
                        } else {
                            if (button.getBeacon() == null || !button.getBeacon().equals(selectedBeacon)) {
                                button.setBeacon(selectedBeacon);
                                selectedBeacon.setButton(button);
                            }
                        }

                        buttonProvider.createOrUpdateButton(button);
                        beaconProvider.createOrUpdateBeacon(selectedBeacon); // transitive persistence sucks in
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

    protected Button getButton() {
        return buttonProvider.getButton(getButtonId());
    }

    protected void populateViewsWithExistingData() {

        final Button existingButton = getButton();

        if (existingButton != null) {
            nameEditText.setText(existingButton.getName());
            autoModeSwitch.setChecked(existingButton.isAutoModeEnabled());

            iconFileNameString = existingButton.getIconFileName();
            // NJD TODO - Need to retrieve image from file name and populate the overlayImageButton view
        }

        List<Beacon> availableBeacons = beaconProvider.getUnpairedBeacons();

        Beacon dummyBeacon = new Beacon();
        dummyBeacon.setName("None");

        if (existingButton != null && existingButton.getBeacon() != null) {
            availableBeacons.add(0, getButton().getBeacon());
            availableBeacons.add(dummyBeacon);
        } else {
            availableBeacons.add(0, dummyBeacon);
        }

        if (!availableBeacons.isEmpty()) {
            beaconSpinner.setVisibility(View.VISIBLE);

            final BeaconAdapter adapter = new BeaconAdapter(getActivity(), availableBeacons);
            beaconSpinner.setAdapter(adapter);
        } else {
            beaconSpinner.setVisibility(View.GONE);
        }
    }

    private class BeaconAdapter extends BaseAdapter {

        Context context;
        List<Beacon> availableBeacons;

        public BeaconAdapter(final Context context, final List<Beacon> availableBeacons) {
            this.context = context;
            this.availableBeacons = availableBeacons;
        }

        @Override
        public int getCount() {
            return availableBeacons.size();
        }

        @Override
        public Object getItem(int position) {
            return availableBeacons.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            view = inflateIfRequired(view, position, parent);
            bind((Beacon) getItem(position), view);
            return view;
        }

        private void bind(Beacon beacon, View view) {
            final ViewHolder holder = (ViewHolder) view.getTag();

            holder.beaconTextView.setText(beacon.getName());
        }

        private View inflateIfRequired(View view, int position, ViewGroup parent) {
            if (view == null) {
                view = inflater.inflate(R.layout.beacon_item, null);
                view.setTag(new ViewHolder(view));
            }
            return view;
        }
    }

    private class ViewHolder {
        public TextView beaconTextView;

        ViewHolder(View view) {
            beaconTextView = (TextView) view.findViewById(R.id.beaconTextView);
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

    public Switch getAutoModeSwitch() {
        return autoModeSwitch;
    }
}

