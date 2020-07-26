package com.nilstrubkin.hueedge.discovery;

public class DiscoveryEntry {
    public String ip;
    public final String friendlyName;
    public final String serialNumber;
    /*public final String logoUrl;*/

    public DiscoveryEntry(String friendlyName, String serialNumber/*, String logoUrl*/) {
        ip = null;
        this.friendlyName = friendlyName;
        this.serialNumber = serialNumber;
        /*this.logoUrl = logoUrl;*/
    }
}