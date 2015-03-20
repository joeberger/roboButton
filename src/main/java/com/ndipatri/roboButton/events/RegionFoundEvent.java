package com.ndipatri.roboButton.events;

import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.models.Region;

/**
 * Created by ndipatri on 1/1/14.
 */
public class RegionFoundEvent {

    public Region region;

    public RegionFoundEvent(final Region region) {
        this.region = region;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegionFoundEvent that = (RegionFoundEvent) o;

        if (region != null ? !region.equals(that.region) : that.region != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return region != null ? region.hashCode() : 0;
    }
}
