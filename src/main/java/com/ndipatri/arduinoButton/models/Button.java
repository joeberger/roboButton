package com.ndipatri.arduinoButton.models;

import android.bluetooth.BluetoothDevice;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import com.ndipatri.arduinoButton.database.OrmLiteDatabaseHelper;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ndipatri on 5/28/14.
 */
@DatabaseTable(tableName = "buttons")
public class Button {

    public Button() {}

    public Button(final String id, final String name, final boolean autoModeEnabled, final String iconFileName) {
        this.id = id;
        this.name = name;
        this.autoModeEnabled = autoModeEnabled;
        this.iconFileName = iconFileName;
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

    public static final String ICONFILENAME_COLUMN_NAME = "icon_file_name";
    @DatabaseField(columnName = ICONFILENAME_COLUMN_NAME)
    private String iconFileName;

    @ForeignCollectionField
    private ForeignCollection<Beacon> beacons;

    private BluetoothDevice bluetoothDevice;

    public ForeignCollection<Beacon> getBeacons() {
        return beacons;
    }

    public void setBeacons(ForeignCollection<Beacon> beacons) {
        this.beacons = beacons;
    }

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

    public String getIconFileName() {
        return iconFileName;
    }

    public void setIconFileName(String iconFileName) {
        this.iconFileName = iconFileName;
    }

    public boolean isAutoModeEnabled() {
        return autoModeEnabled;
    }

    public void setAutoModeEnabled(boolean autoModeEnabled) {
        this.autoModeEnabled = autoModeEnabled;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Button button = (Button) o;

        if (autoModeEnabled != button.autoModeEnabled) return false;
        if (iconFileName != null ? !iconFileName.equals(button.iconFileName) : button.iconFileName != null)
            return false;
        if (id != null ? !id.equals(button.id) : button.id != null) return false;
        if (name != null ? !name.equals(button.name) : button.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (autoModeEnabled ? 1 : 0);
        result = 31 * result + (iconFileName != null ? iconFileName.hashCode() : 0);
        return result;
    }
}
