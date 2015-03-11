package com.ndipatri.roboButton.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.dagger.providers.RegionProvider;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.BusProvider;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.Views;

public class ButtonDetailsDialogFragment extends DialogFragment {

    private static final String TAG = ButtonDetailsDialogFragment.class.getCanonicalName();

    // ButterKnife Injected Views
    protected @InjectView(R.id.nameEditText) EditText nameEditText;
    protected @InjectView(R.id.autoModeSwitch) Switch autoModeSwitch;

    @Inject
    protected ButtonProvider buttonProvider;

    @Inject
    protected RegionProvider regionProvider;

    public static ButtonDetailsDialogFragment newInstance(String buttonId) {

        ButtonDetailsDialogFragment fragment = new ButtonDetailsDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);

        fragment.setButtonId(buttonId);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ((RBApplication)getActivity().getApplication()).registerForDependencyInjection(this);

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
                .setNeutralButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
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

    protected Button getButton() {
        return buttonProvider.getButton(getButtonId());
    }

    protected void setupViews() {

        final Button existingButton = getButton();

        nameEditText.setHint(existingButton.getName());

        autoModeSwitch.setChecked(existingButton.isAutoModeEnabled());

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Button button = getButton();
                button.setName(nameEditText.getText().toString());
                buttonProvider.createOrUpdateButton(button);
            }
        });

        autoModeSwitch.setChecked(existingButton.isAutoModeEnabled());
        autoModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Button button = getButton();
                button.setAutoModeEnabled(autoModeSwitch.isChecked());
                buttonProvider.createOrUpdateButton(button);
            }
        });
    }

    private synchronized String getButtonId() {
        return getArguments().getString("buttonId");
    }

    private synchronized void setButtonId(String buttonId) {
        getArguments().putString("buttonId", buttonId);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public Switch getAutoModeSwitch() {
        return autoModeSwitch;
    }
}

