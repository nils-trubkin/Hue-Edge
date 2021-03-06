package com.nilstrubkin.hueedge.resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import androidx.preference.PreferenceManager;

import androidx.core.content.ContextCompat;

import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Objects;

class LightResource extends BridgeResourceSliders {
    private static final long serialVersionUID = -4487639315415671159L;

    static class State implements Serializable {
        private static final long serialVersionUID = -8111864201961818989L;
        boolean on;
        //int bri;
        int hue;
        int sat;

        public boolean isOn() {
            return on;
        }

        /*public int getBri() {
            return bri;
        }*/

        public int getHue() {
            return hue;
        }

        public int getSat() {
            return sat;
        }
    }

    private State state;

    private State getState(){
        return state;
    }

    private boolean isOn(){
        return getState().isOn();
    }

    @Override
    protected boolean isAll_off(){
        return !getState().isOn();
    }

    /*public int getBri() {
        return getState().getBri();
    }*/

    public int getHue() {
        return getState().getHue();
    }

    public int getSat() {
        return getState().getSat();
    }

    @Override
    public void activateResource(Context ctx) {
        String actionWrite = getActionWrite();
        boolean newState = !isOn();
        sendValue(ctx, actionWrite, newState);
    }

    @Override
    protected void sendValue(Context ctx, String key, Object value){
        try {
            String bridgeUrl = Objects.requireNonNull(HueBridge.getInstance(ctx)).getUrl();
            JSONObject jsonObject = new JSONObject().put(key, value);
            post(ctx,bridgeUrl + getStateUrl(), jsonObject.toString());
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCategory() {
        return "lights";
    }

    @Override
    public String getActionRead() {
        return "on";
    }

    @Override
    public String getActionWrite() {
        return "on";
    }

    @Override
    public String getBtnText(Context ctx) {
        Resources resources = ctx.getResources();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean noSymbols = settings.getBoolean(ctx.getResources().getString(R.string.preference_no_symbols), false);
        if(isOn())
            return noSymbols ? resources.getString(R.string.on_no_symbol) : resources.getString(R.string.on_symbol);
        else
            return noSymbols ? resources.getString(R.string.off_no_symbol) : resources.getString(R.string.off_symbol);
    }

    @Override
    public int getBtnTextColor(Context ctx) {
        if(isOn())
            return ContextCompat.getColor(ctx, R.color.black);
        else
            return ContextCompat.getColor(ctx, R.color.white);
    }

    @Override
    public int getBtnBackgroundResource() {
        if(isOn())
            return R.drawable.on_button_background;
        else
            return R.drawable.off_button_background;
    }
}
