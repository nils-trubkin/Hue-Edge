package com.nilstrubkin.hueedge.resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import androidx.core.content.ContextCompat;

import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Objects;

public class GroupResource extends BridgeResourceSliders {

    static class State implements Serializable {
        boolean all_on;
        boolean any_on;

        private boolean isAll_on() {
            return all_on;
        }

        private boolean isAny_on() {
            return any_on;
        }
    }

    static class Action implements Serializable{
        int bri;
        int hue;
        int sat;

        public int getBri() {
            return bri;
        }

        public int getHue() {
            return hue;
        }

        public int getSat() {
            return sat;
        }
    }

    private String type;
    private State state;
    private Action action;

    public String getType() {
        return type;
    }

    public State getState() {
        return state;
    }

    public Action getAction(){
        return action;
    }

    public boolean isAll_off() {
        return !getState().isAll_on();
    }

    public boolean isAny_on() {
        return getState().isAny_on();
    }

    public int getBri() {
        return getAction().getBri();
    }

    public int getHue() {
        return getAction().getHue();
    }

    public int getSat() {
        return getAction().getSat();
    }

    @Override
    public void activateResource(Context ctx) {
        String actionWrite = getActionWrite();
        boolean newState = !isAny_on();
        sendValue(ctx, actionWrite, newState);
    }

    public String sendValue(Context ctx, String key, Object value){
        try {
            String bridgeUrl = Objects.requireNonNull(HueBridge.getInstance(ctx)).getUrl();
            JSONObject jsonObject = new JSONObject().put(key, value);
            return post(ctx,bridgeUrl + getActionUrl(), jsonObject.toString());
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getCategory() {
        return "groups";
    }

    @Override
    public String getActionRead() {
        return "any_on";
    }

    @Override
    public String getActionWrite() {
        return "on";
    }

    @Override
    public String getBtnText(Context ctx) {
        Resources resources = ctx.getResources();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean noSymbols = settings.getBoolean(ctx.getResources().getString(R.string.no_symbols_preference), false);
        if(isAny_on())
            if(!isAll_off())
                return noSymbols ? resources.getString(R.string.on_no_symbol) : resources.getString(R.string.on_symbol);
            else
                return noSymbols ? resources.getString(R.string.some_no_symbol) : resources.getString(R.string.some_symbol);
        else
            return noSymbols ? resources.getString(R.string.off_no_symbol) : resources.getString(R.string.off_symbol);
    }

    @Override
    public int getBtnTextColor(Context ctx) {
        if(isAny_on())
            return ContextCompat.getColor(ctx, R.color.black);
        else
            return ContextCompat.getColor(ctx, R.color.white);
    }

    @Override
    public int getBtnBackgroundResource() {
        if(isAny_on())
            return R.drawable.on_button_background;
        else
            return R.drawable.off_button_background;
    }
}