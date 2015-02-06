package com.ndipatri.roboButton.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by ndipatri on 5/28/14.
 */
@DatabaseTable(tableName = "beacons")
public class Beacon {

    public Beacon() {}

    // NJD TODO - Need to add UUID to this (which by default is the same for all estimotes, but can be made unique)
    public Beacon(final String macAddress, final String name) {
        this.macAddress = macAddress;
        this.name = name;
    }

    private static final String TAG = Beacon.class.getCanonicalName();

    public static final String MAC_ADDRESS_COLUMN_NAME = "mac_address";
    @DatabaseField(id = true, columnName = MAC_ADDRESS_COLUMN_NAME)
    private String macAddress;

    public static final String NAME_COLUMN_NAME = "name";
    @DatabaseField(columnName = NAME_COLUMN_NAME)
    private String name;

    public static final String BUTTON_ID = "button_id";
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = BUTTON_ID)
    private Button button;

    public void setButton(Button button) {
        this.button = button;
    }

    public Button getButton() {
        return button;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Beacon beacon = (Beacon) o;

        if (macAddress != null ? !macAddress.equals(beacon.macAddress) : beacon.macAddress != null)
            return false;
        if (name != null ? !name.equals(beacon.name) : beacon.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = macAddress != null ? macAddress.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
