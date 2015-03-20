package com.ndipatri.roboButton.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.dagger.providers.ButtonProvider;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ButtonStateChangeReport;
import com.ndipatri.roboButton.events.ButtonStateChangeRequest;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import butterknife.InjectView;
import butterknife.Views;

/**
 * Created by ndipatri on 12/31/13.
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
    protected ButtonProvider buttonProvider;

    // ButterKnife Injected Views
    protected
    @InjectView(R.id.imageView)
    ImageView imageView;

    protected ButtonState buttonState = null;

    ButtonDetailsDialogFragment dialog = null;

    public ButtonFragment() {
        RBApplication.getInstance().registerForDependencyInjection(this);
    }

    public static ButtonFragment newInstance(String buttonId) {

        ButtonFragment ButtonFragment = new ButtonFragment();
        Bundle args = new Bundle();
        ButtonFragment.setArguments(args);

        ButtonFragment.setButtonId(buttonId);

        return ButtonFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.button_layout, container, false);

        // Use ButterKnife for view injection (http://jakewharton.github.io/butterknife/)
        Views.inject(this, rootView);

        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        if (buttonState.value) {
            setButtonState(ButtonState.OFF_PENDING);
        } else {
            setButtonState(ButtonState.ON_PENDING);
        }

        BusProvider.getInstance().post(new ButtonStateChangeRequest(getButtonId(), buttonState));
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        BusProvider.getInstance().unregister(this);
    }

    private synchronized String getButtonId() {
        return getArguments().getString("buttonId");
    }

    private synchronized void setButtonId(String buttonId) {
        getArguments().putString("buttonId", buttonId);
    }

    private Button getButton() {
        return buttonProvider.getButton(getButtonId());
    }

    private void setButtonState(ButtonState buttonState) {
        this.buttonState = buttonState;

        imageView.setImageResource(buttonState.drawableResourceId);
    }

    @Subscribe
    public void onArduinoButtonStateChangeReportEvent(final ButtonStateChangeReport event) {
        if (event.getButtonId().equals(getButtonId())) {
            setButtonState(event.getButtonState());
        }
    }
}
