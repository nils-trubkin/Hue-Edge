package com.ize.edgehue.bridge_resource;

import android.content.Context;

public class SceneResource extends BridgeResource {
    public SceneResource(Context context, int id) {
        super(context, id);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getBtnText() {
        return null;
    }

    @Override
    public int getBtnTextColor() {
        return 0;
    }

    @Override
    public int getBtnBackgroundResource() {
        return 0;
    }

    @Override
    public void activateResource(Context context) {
    }

    @Override
    public String getStateUrl() {
        return null;
    }
}
