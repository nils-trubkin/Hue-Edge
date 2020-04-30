package com.ize.edgehue.bridge_resource;

import android.content.Context;
import com.ize.edgehue.HueBridge;

public abstract class BridgeResource {
    Context ctx;
    HueBridge bridge;
    int id;

    BridgeResource(Context context, int id){
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
    public abstract void activateResource(Context context);
    public abstract String getStateUrl();
}
