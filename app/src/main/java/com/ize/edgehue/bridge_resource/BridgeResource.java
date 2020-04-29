package com.ize.edgehue.bridge_resource;

import android.content.Context;

import com.ize.edgehue.HueBridge;

import org.json.JSONException;

public abstract class BridgeResource {
    protected Context ctx;
    protected HueBridge bridge;
    protected int id;

    public BridgeResource(Context context, int id){
        ctx = context;
        bridge = HueBridge.getInstance();
        this.id = id;
    }

    public int getId(){
        return id;
    }

    public abstract String getName();
    public abstract String getBtnText();
    public abstract int getBtnTextColor();
    public abstract int getBtnBackgroundResource();
    public abstract void activateResource();
    public abstract String getStateUrl();
}
