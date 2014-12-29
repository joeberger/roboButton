package com.ndipatri.arduinoButton.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by ndipatri on 5/28/14.
 */
@DatabaseTable(tableName = "beacons")
public class Beacon {

    public Beacon() {}

    public Beacon(final String macAddress, final int major, final int minor, final String name) {
        this.macAddress = macAddress;
        this.major = major;
        this.minor = minor;
        this.name = name;
    }

    private static final String TAG = Beacon.class.getCanonicalName();

    public static final String MAC_ADDRESS_COLUMN_NAME = "mac_address";
    @DatabaseField(id = true, columnName = MAC_ADDRESS_COLUMN_NAME)
    private String macAddress;

    public static final String MAJOR_COLUMN_NAME = "major";
    @DatabaseField(columnName = MAJOR_COLUMN_NAME)
    private int major;

    public static final String MINOR_COLUMN_NAME = "minor";
    @DatabaseField(columnName = MINOR_COLUMN_NAME)
    private int minor;

    public static final String NAME_COLUMN_NAME = "name";
    @DatabaseField(columnName = NAME_COLUMN_NAME)
    private String name;


    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
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

        if (major != beacon.major) return false;
        if (minor != beacon.minor) return false;
        if (macAddress != null ? !macAddress.equals(beacon.macAddress) : beacon.macAddress != null)
            return false;
        if (name != null ? !name.equals(beacon.name) : beacon.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = macAddress != null ? macAddress.hashCode() : 0;
        result = 31 * result + major;
        result = 31 * result + minor;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
