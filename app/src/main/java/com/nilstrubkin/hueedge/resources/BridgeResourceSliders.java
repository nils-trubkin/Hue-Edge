package com.nilstrubkin.hueedge.resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.nilstrubkin.hueedge.R;

public abstract class BridgeResourceSliders extends BridgeResource {
    private static final long serialVersionUID = 1372126609639954673L;

    //public abstract int getBri();
    public abstract int getHue();
    public abstract int getSat();
    protected abstract boolean isAll_off();
    protected abstract void sendValue(Context ctx, String key, Object value);

    private void enableResource(Context ctx) {
        String actionWrite = getActionWrite();
        sendValue(ctx, actionWrite, true);
    }

    public void setBri(Context ctx, int value) {
        String briAction = "bri";
        if(isAll_off())
            enableResource(ctx);
        sendValue(ctx, briAction, value);
    }
    public void setHue(Context ctx, int value) {
        String hueAction = "hue";
        if(isAll_off())
            enableResource(ctx);
        sendValue(ctx, hueAction, value);
    }
    public void setSat(Context ctx, int value) {
        String satAction = "sat";
        if(isAll_off())
            enableResource(ctx);
        sendValue(ctx, satAction, value);
    }

    public void setCt(Context ctx, int value) {
        String ctAction = "ct";
        if(isAll_off())
            enableResource(ctx);
        sendValue(ctx, ctAction, value);
    }

    public String getStateUrl() {
        return "/" + getCategory() + "/" + getId() + "/state";
    }

    @Override
    public int getBtnTextSize(Context ctx) {
        Resources resources = ctx.getResources();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean noSymbols = settings.getBoolean(resources.getString(R.string.preference_no_symbols), false);
        if(noSymbols)
            return R.dimen.resource_btn_text_size_scene;
        else
            return R.dimen.resource_btn_text_size_symbol;
    }

}
