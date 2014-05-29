package com.ndipatri.arduinoButton.models;

import com.j256.ormlite.field.DatabaseField;
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
}
