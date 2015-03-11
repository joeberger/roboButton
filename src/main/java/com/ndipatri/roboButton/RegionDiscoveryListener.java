package com.ndipatri.roboButton;

import com.ndipatri.roboButton.models.Region;

/**
 * Created by ndipatri on 1/19/15.
 */
public interface RegionDiscoveryListener {

    // It's up to the individual implementation to decide what is concidered 'found'.. In general, this should be
    // within a few feet of a beacon that is a member of the given region.
    public void regionFound(Region region);
    public void regionLost(Region region);
}
