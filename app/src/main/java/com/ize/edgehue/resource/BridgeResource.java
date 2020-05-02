package com.ize.edgehue.resource;

import android.content.Context;
import com.ize.edgehue.HueBridge;

public abstract class BridgeResource {
    final Context ctx;
    final HueBridge bridge;
    final int id;

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
