package com.ndipatri.roboButton.utils;

import com.ndipatri.roboButton.models.Region;

public class RegionUtils {
    public static final String GELO_UUID = "11e44f094ec4407e9203cf57a50fbce0";
    public static final String LIGHTBLUE_UUID = "a495ff10c5b14b44b5121370f02d74de";

    // TODO - For now, we will assume homogenous regions: same beacon types in any paritcular region.
    public static boolean isLighBlueRegion(Region region)  {
       return region.getUuid().toLowerCase().equals(LIGHTBLUE_UUID);
    }
}
