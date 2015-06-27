package com.ndipatri.roboButton.models;

import android.bluetooth.BluetoothDevice;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.enums.ButtonType;

/**
 * Created by ndipatri on 5/28/14.
 */
@DatabaseTable(tableName = "buttons")
public class Button {

    public Button() {}

    public Button(final String id, final String name, final boolean autoModeEnabled) {
        this.id = id;
        this.name = name;
        this.autoModeEnabled = autoModeEnabled;
    }

    private static final String TAG = Button.class.getCanonicalName();

    public static final String ID_COLUMN_NAME = "id";
    @DatabaseField(id = true, columnName = ID_COLUMN_NAME)
    private String id;

    public static final String NAME_COLUMN_NAME = "name";
    @DatabaseField(columnName = NAME_COLUMN_NAME)
    private String name;

    public static final String AUTOMODEENABLED_COLUMN_NAME = "auto_mode_enabled";

    @DatabaseField(columnName = AUTOMODEENABLED_COLUMN_NAME)
    private boolean autoModeEnabled;

    public static final String STATE_COLUMN_NAME = "state";
    @DatabaseField(columnName = STATE_COLUMN_NAME)
    private ButtonState state;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAutoModeEnabled() {
        return autoModeEnabled;
    }

    public void setAutoModeEnabled(boolean autoModeEnabled) {
        this.autoModeEnabled = autoModeEnabled;
    }

    public ButtonState getState() {
        return state;
    }

    public void setState(ButtonState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Button button = (Button) o;

        if (autoModeEnabled != button.autoModeEnabled) return false;
        if (id != null ? !id.equals(button.id) : button.id != null) return false;
        return  name != null ? !name.equals(button.name) : button.name != null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (autoModeEnabled ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Button{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", autoModeEnabled=" + autoModeEnabled +
                ", state=" + state +
                '}';
    }
}
