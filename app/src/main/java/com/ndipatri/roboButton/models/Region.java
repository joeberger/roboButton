package com.ndipatri.roboButton.models;


/**
 * Created by ndipatri on 5/28/14.
 */
public class Region {

    public Region() {}

    public Region(final Integer minor, final Integer major, final String uuid) {
        this.minor = minor;
        this.major = major;
        this.uuid = uuid;

        this.name = generateRegionName(minor, major, uuid);
    }

    protected String generateRegionName(final int minor, final int major, final String uuid) {
        return "Region('" + getMinor() + "-" + getMajor() + "-" + getUuid() + "')";
    }

    private Integer major;

    private Integer minor;

    private String uuid;

    private String name;

    public Integer getMinor() {
        return minor;
    }

    public Integer getMajor() {
        return major;
    }

    public String getUuid() {
        return uuid;
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

        if (major != null ? !major.equals(region.major) : region.major != null) return false;
        if (minor != null ? !minor.equals(region.minor) : region.minor != null) return false;
        return !(uuid != null ? !uuid.equals(region.uuid) : region.uuid != null);

    }

    @Override
    public int hashCode() {
        int result = major != null ? major.hashCode() : 0;
        result = 31 * result + (minor != null ? minor.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Region{" +
                "major=" + major +
                ", minor=" + minor +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
