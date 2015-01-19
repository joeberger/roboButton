package com.ndipatri.arduinoButton.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.enums.ButtonState;
import com.ndipatri.arduinoButton.events.ABStateChangeReport;
import com.ndipatri.arduinoButton.events.ABStateChangeRequest;
import com.ndipatri.arduinoButton.utils.BusProvider;
import com.squareup.otto.Subscribe;

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
public class ABFragment extends Fragment {

    private static final String TAG = ABFragment.class.getCanonicalName();

    // ButterKnife Injected Views
    protected
    @InjectView(R.id.imageView)
    ImageView imageView;

    protected ButtonState buttonState = null;

    public static ABFragment newInstance(String buttonId) {

        ABFragment ABFragment = new ABFragment();
        Bundle args = new Bundle();
        ABFragment.setArguments(args);

        ABFragment.setButtonId(buttonId);

        return ABFragment;
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

                ButtonDetailsDialogFragment dialog = ButtonDetailsDialogFragment.newInstance(getButtonId());
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

        BusProvider.getInstance().post(new ABStateChangeRequest(getButtonId(), buttonState));
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

    private void setButtonState(ButtonState buttonState) {
        this.buttonState = buttonState;

        imageView.setImageResource(buttonState.drawableResourceId);
    }

    @Subscribe
    public void onArduinoButtonStateChangeReportEvent(final ABStateChangeReport event) {
        if (event.buttonId.equals(getButtonId())) {
            setButtonState(event.newButtonState);
        }
    }
}