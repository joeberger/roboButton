package com.ndipatri.arduinoButton.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "beacon_button_xref")
public class BeaconButtonAssociation {

    public BeaconButtonAssociation() {}

    public BeaconButtonAssociation(final String beaconId, final String buttonId) {
        this.beaconId = beaconId;
        this.buttonId = buttonId;
    }

    // NJD - This is how we do composite keys in ORMLite
    @DatabaseField(id=true, useGetSet=true)
    private String id;

    public String getId(){
        return getBeaconId() + "-" + getButtonId();
    }
    public void setId(String beaconId, String buttonId){
        this.id = beaconId + "-" + buttonId;
    }

    private static final String TAG = BeaconButtonAssociation.class.getCanonicalName();

    public static final String BEACON_ID = "beaconId";
    @DatabaseField(id = true, columnName = BEACON_ID)
    private String beaconId;

    public static final String BUTTON_ID = "buttonId";
    @DatabaseField(columnName = BUTTON_ID)
    private String buttonId;

    public String getBeaconId() {
        return beaconId;
    }

    public void setBeaconId(String beaconId) {
        this.beaconId = beaconId;
    }

    public String getButtonId() {
        return buttonId;
    }

    public void setButtonId(String buttonId) {
        this.buttonId = buttonId;
    }
}
