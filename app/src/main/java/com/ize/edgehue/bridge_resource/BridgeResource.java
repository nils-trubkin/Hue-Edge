package com.ize.edgehue.bridge_resource;

import com.ize.edgehue.HueBridge;

import org.json.JSONException;

public abstract class BridgeResource {
    private HueBridge bridge;

    public abstract String getName() throws JSONException;
    public abstract void activateResource() throws JSONException;
}
