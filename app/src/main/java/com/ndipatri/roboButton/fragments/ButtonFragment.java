package com.ndipatri.roboButton.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Views;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.dagger.daos.ButtonDao;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ButtonStateChangeRequest;
import com.ndipatri.roboButton.events.ButtonUpdatedEvent;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.BusProvider;
import com.ndipatri.roboButton.views.ProgressView;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import butterknife.InjectView;

/**
 * <p/>
 * This fragment presents the user with a button which can be pressed to toggle on/off state.  When
 * the button is pressed, the state is 'pending' and the button is disabled. A single attempt is then
 * made to change remote arduino button based on this state.
 * <p/>
 * Periodically, this fragment overwrites the current button state with the remote arduino button state.
 */
public class ButtonFragment extends Fragment {

    private static final String TAG = ButtonFragment.class.getCanonicalName();

    @Inject
    BusProvider bus;

    @Inject
    ButtonDao buttonDao;

    // ButterKnife Injected Views
    protected @InjectView(R.id.buttonImageView) ImageView imageView;
    protected @InjectView(R.id.progressView) ProgressView progressView;
    protected @InjectView(R.id.buttonLabelTextView) TextView buttonLabelTextView;

    ButtonDetailsDialogFragment dialog = null;

    public ButtonFragment() {
        RBApplication.getInstance().getGraph().inject(this);
    }

    public static ButtonFragment newInstance(String buttonId) {

        ButtonFragment buttonFragment = new ButtonFragment();
        Bundle args = new Bundle();
        buttonFragment.setArguments(args);

        buttonFragment.setButtonId(buttonId);

        return buttonFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_button, container, false);

        // Use ButterKnife for view injection (http://jakewharton.github.io/butterknife/)
        Views.inject(this, rootView);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ButtonState buttonState = getButton().getState();
                if (buttonState != null && buttonState.enabled) {
                    Log.d(TAG, "Button Pressed!");
                    toggleButtonState();
                }
            }
        });

        rootView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                dialog = ButtonDetailsDialogFragment.newInstance(getButtonId());
                dialog.show(getFragmentManager().beginTransaction(), "button details dialog");

                return false;
            }
        });

        return rootView;
    }

    // This sets a pending local state then requests a remote state change...
    public void toggleButtonState() {

        // We immediately change local state to pending...
        ButtonState currentButtonState = getButton().getState();
        ButtonState proposedButtonState;
        if (currentButtonState.value) {
            proposedButtonState = ButtonState.OFF_PENDING;
        } else {
            proposedButtonState = ButtonState.ON_PENDING;
        }

        updateButtonState(proposedButtonState);
        bus.post(new ButtonStateChangeRequest(getButtonId(), proposedButtonState));
    }

    @Override
    public void onResume() {
        super.onResume();

        bus.register(this);

        updateButtonState();
    }

    @Override
    public void onPause() {
        super.onPause();

        bus.unregister(this);
    }

    private Button getButton() {
        return buttonDao.getButton(getButtonId());
    }

    private String getButtonId() {
        return getArguments().getString("buttonId");
    }

    private void setButtonId(String buttonId) {
        getArguments().putString("buttonId", buttonId);
    }

    private void updateButtonState() {
        Button button = getButton();

        // If communications is just starting with the button, it might not be created yet and
        // we're just waiting
        if (button != null) {
            updateButtonState(button.getState());
        }
    }

    /**
     * Use this when we want to force a local button state representation regardless reality
     */
    private void updateButtonState(ButtonState buttonState) {

        buttonLabelTextView.setText(buttonState == null ? "" : getButton().getName());
        imageView.setImageResource(buttonState.drawableResourceId);
        progressView.render(buttonState);
    }

    @Subscribe
    public void onButtonUpdatedEvent(final ButtonUpdatedEvent event) {
        if (event.getButtonId().equals(getButtonId())) {
            updateButtonState();
        }
    }
}
