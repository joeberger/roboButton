package com.ndipatri.arduinoButton.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "beacon_button_xref")
public class BeaconButtonAssociation {

    public BeaconButtonAssociation() {}

    public BeaconButtonAssociation(final String beaconId, final Button button) {
        this.beaconId = beaconId;
        this.button = button;
    }

    // NJD - This is how we do composite keys in ORMLite
    @DatabaseField(id=true, useGetSet=true)
    private String id;

    public String getId(){
        return getBeaconId() + "-" + getButton().getId();
    }

    public void setId(String id){
        this.id = id;
    }

    public void setId(String beaconId, String buttonId){
        this.id = beaconId + "-" + buttonId;
    }

    private static final String TAG = BeaconButtonAssociation.class.getCanonicalName();

    public static final String BUTTON_ID = "button_id";
    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = BUTTON_ID)
    private Button button;

    public static final String BEACON_ID = "beacon_id";
    @DatabaseField(columnName = BEACON_ID)
    private String beaconId;

    public String getBeaconId() {
        return beaconId;
    }

    public void setBeaconId(String beaconId) {
        this.beaconId = beaconId;
    }

    public Button getButton() {
        return button;
    }

    public void setButton(Button button) {
        this.button = button;
    }
}
