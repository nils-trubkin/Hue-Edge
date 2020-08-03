package com.nilstrubkin.hueedge.resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.nilstrubkin.hueedge.R;

public abstract class BridgeResourceSliders extends BridgeResource {
    public abstract int getBri();
    public abstract int getHue();
    public abstract int getSat();
    protected abstract boolean isAll_on();
    protected abstract String sendValue(Context ctx, String key, Object value);

    public String getBriAction(){
        return "bri";
    }

    public String getHueAction(){
        return "hue";
    }

    public String getSatAction(){
        return "sat";
    }

    public void enableResource(Context ctx) {
        String actionWrite = getActionWrite();
        sendValue(ctx, actionWrite, true);
    }

    public void setBri(Context ctx, int value) {
        String briAction = getBriAction();
        if(!isAll_on())
            activateResource(ctx);
        sendValue(ctx, briAction, value);
    }
    public void setHue(Context ctx, int value) {
        String hueAction = getHueAction();
        if(!isAll_on())
            activateResource(ctx);
        sendValue(ctx, hueAction, value);
    }
    public void setSat(Context ctx, int value) {
        String satAction = getSatAction();
        if(!isAll_on())
            activateResource(ctx);
        sendValue(ctx, satAction, value);
    }

    public String getStateUrl() {
        return "/" + getCategory() + "/" + getId() + "/state";
    }

    @Override
    public int getBtnTextSize(Context ctx) {
        Resources resources = ctx.getResources();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean noSymbols = settings.getBoolean(resources.getString(R.string.no_symbols_preference), false);
        if(noSymbols)
            return R.dimen.resource_btn_text_size_scene;
        else
            return R.dimen.resource_btn_text_size_symbol;
    }

}
