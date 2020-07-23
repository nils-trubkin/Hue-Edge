package com.nilstrubkin.hueedge;

public class DiscoveryEntry {
    public final String friendlyName;
    public final String modelDescription;
    public final String serialNumber;
    public final String logoUrl;

    public DiscoveryEntry(String friendlyName, String modelDescription, String serialNumber, String logoUrl) {
        this.friendlyName = friendlyName;
        this.modelDescription = modelDescription;
        this.serialNumber = serialNumber;
        this.logoUrl = logoUrl;
    }
}