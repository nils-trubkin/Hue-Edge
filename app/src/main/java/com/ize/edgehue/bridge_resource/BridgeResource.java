package com.ize.edgehue.bridge_resource;

import android.content.Context;

import com.ize.edgehue.HueBridge;

import org.json.JSONException;

public abstract class BridgeResource {
    /*private Context ctx;
    private HueBridge bridge;
    private int id;*/

    public abstract int getId();
    public abstract String getName();
    public abstract String getBtnText();
    public abstract int getBtnTextColor();
    public abstract int getBtnBackgroundResource();
    public abstract void activateResource();
}
