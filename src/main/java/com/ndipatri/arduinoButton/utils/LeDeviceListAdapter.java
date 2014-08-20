package com.ndipatri.arduinoButton.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Utils;
import com.ndipatri.arduinoButton.ArduinoButtonApplication;
import com.ndipatri.arduinoButton.R;
import com.ndipatri.arduinoButton.dagger.providers.BeaconProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.inject.Inject;

/**
 * Created by ndipatri on 7/9/14.
 */
public class LeDeviceListAdapter extends BaseAdapter {
    private ArrayList<Beacon> beacons;
    private LayoutInflater inflater;
    private Context context;

    @Inject
    protected BeaconProvider beaconProvider;

    public LeDeviceListAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.beacons = new ArrayList<Beacon>();

        ((ArduinoButtonApplication)context.getApplicationContext()).inject(this);
    }

    public void replaceWith(Collection<Beacon> newBeacons) {
        this.beacons.clear();
        this.beacons.addAll(newBeacons);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return beacons.size();
    }

    @Override
    public Beacon getItem(int position) {
        return beacons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        view = inflateIfRequired(view, position, parent);
        bind(getItem(position), view);
        return view;
    }

    private void bind(Beacon beacon, View view) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        com.ndipatri.arduinoButton.models.Beacon localBeacon = beaconProvider.getBeacon(beacon.getMacAddress());
        if (localBeacon != null) {
            holder.nameTextView.setText(localBeacon.getName());
        } else {
            holder.nameTextView.setText(context.getString(R.string.new_beacon));
        }

        holder.macTextView.setText(String.format("MAC: %s (%.2fm)", beacon.getMacAddress(), Utils.computeAccuracy(beacon)));
        holder.majorTextView.setText("Major: " + beacon.getMajor());
        holder.minorTextView.setText("Minor: " + beacon.getMinor());
        holder.measuredPowerTextView.setText("MPower: " + beacon.getMeasuredPower());
        holder.rssiTextView.setText("RSSI: " + beacon.getRssi());

        holder.infoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.detailViewGroup.getVisibility() == View.VISIBLE) {
                    holder.detailViewGroup.setVisibility(View.GONE);
                } else {
                    holder.detailViewGroup.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private View inflateIfRequired(View view, int position, ViewGroup parent) {
        if (view == null) {
            view = inflater.inflate(R.layout.device_item, null);
            view.setTag(new ViewHolder(view));
        }
        return view;
    }

    static class ViewHolder {
        final TextView nameTextView;
        final TextView macTextView;
        final TextView majorTextView;
        final TextView minorTextView;
        final TextView measuredPowerTextView;
        final TextView rssiTextView;
        final ViewGroup detailViewGroup;
        final View infoImageView;

        ViewHolder(View view) {
            nameTextView = (TextView) view.findViewWithTag("name");
            macTextView = (TextView) view.findViewWithTag("mac");
            majorTextView = (TextView) view.findViewWithTag("major");
            minorTextView = (TextView) view.findViewWithTag("minor");
            measuredPowerTextView = (TextView) view.findViewWithTag("mpower");
            rssiTextView = (TextView) view.findViewWithTag("rssi");
            detailViewGroup = (ViewGroup) view.findViewWithTag("detailViewGroup");
            infoImageView = (View) view.findViewWithTag("infoImageView");
        }
    }
}
