package com.ize.edgehue.resource;

import android.content.Context;

public class ZoneResource extends BridgeResource{
    public ZoneResource(Context context, int id) {
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
