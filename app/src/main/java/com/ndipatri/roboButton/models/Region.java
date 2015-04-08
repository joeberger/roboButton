package com.ndipatri.roboButton.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by ndipatri on 5/28/14.
 */
@DatabaseTable(tableName = "regions")
public class Region {

    public Region() {}

    public Region(final Integer minor, final Integer major, final String uuid) {
        this.minor = minor;
        this.major = major;
        this.uuid = uuid;

        this.name = generateRegionName(minor, major, uuid);
    }

    protected String generateRegionName(final int minor, final int major, final String uuid) {
        return "Region('" + getId() + "')";
    }

    @DatabaseField(id = true, useGetSet = true)
    private String id;

    public String getId() {
        return getMinor() + "-" + getMajor() + "-" + getUuid();
    }

    public void setId(String id) {
        this.id = id;
    }

    public static final String MAJOR_COLUMN_NAME = "major";
    @DatabaseField(columnName = MAJOR_COLUMN_NAME)
    private Integer major;

    public static final String MINOR_COLUMN_NAME = "minor";
    @DatabaseField(columnName = MINOR_COLUMN_NAME)
    private Integer minor;

    public static final String UUID_COLUMN_NAME = "uuid";
    @DatabaseField(columnName = UUID_COLUMN_NAME)
    private String uuid;

    public static final String NAME_COLUMN_NAME = "name";
    @DatabaseField(columnName = NAME_COLUMN_NAME)
    private String name;

    public static final String BUTTON_ID = "button_id";
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = BUTTON_ID)
    private Button button;

    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }

    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Button getButton() {
        return button;
    }

    public void setButton(Button button) {
        this.button = button;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Region region = (Region) o;

        if (button != null ? !button.equals(region.button) : region.button != null) return false;
        if (id != null ? !id.equals(region.id) : region.id != null) return false;
        if (major != null ? !major.equals(region.major) : region.major != null) return false;
        if (minor != null ? !minor.equals(region.minor) : region.minor != null) return false;
        if (name != null ? !name.equals(region.name) : region.name != null) return false;
        if (uuid != null ? !uuid.equals(region.uuid) : region.uuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (major != null ? major.hashCode() : 0);
        result = 31 * result + (minor != null ? minor.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (button != null ? button.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Region{" +
                "id='" + getId() + '\'' +
                ", major=" + major +
                ", minor=" + minor +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", button=" + button +
                '}';
    }
}
