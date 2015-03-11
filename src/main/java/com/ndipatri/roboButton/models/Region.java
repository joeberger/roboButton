package com.ndipatri.roboButton.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by ndipatri on 5/28/14.
 */
@DatabaseTable(tableName = "regions")
public class Region {

    public Region() {
    }

    public Region(final Integer major) {
        this.major = major;
    }

    public static final String MAJOR_COLUMN_NAME = "major";
    @DatabaseField(id = true, columnName = MAJOR_COLUMN_NAME)
    private Integer major;

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

    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
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

        Region region = (Region) o;

        if (button != null ? !button.equals(region.button) : region.button != null) return false;
        if (major != null ? !major.equals(region.major) : region.major != null) return false;
        if (name != null ? !name.equals(region.name) : region.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = major != null ? major.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (button != null ? button.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Region{" +
                "major=" + major +
                ", name='" + name + '\'' +
                ", button=" + button +
                '}';
    }
}
