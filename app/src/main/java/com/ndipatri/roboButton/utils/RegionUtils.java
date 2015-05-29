package com.ndipatri.roboButton.utils;

import com.ndipatri.roboButton.models.Region;

public class RegionUtils {
    public static final String GELO_UUID = "11e44f094ec4407e9203cf57a50fbce0";
    public static final String LIGHTBLUE_UUID = "a495ff10-c5b1-4b44-b512-1370f02d74de";

    // TODO - For now, we will assume homogenous regions: same beacon types in any paritcular region.
    public boolean isLighBlueRegion(Region region)  {
       return region.getUuid().toLowerCase().equals(LIGHTBLUE_UUID);
    }
}
