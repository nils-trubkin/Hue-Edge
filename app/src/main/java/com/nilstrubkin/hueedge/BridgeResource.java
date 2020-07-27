package com.nilstrubkin.hueedge;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONException;

import java.io.Serializable;

public class BridgeResource implements Serializable, Comparable<BridgeResource> {

    private static final String TAG = BridgeResource.class.getSimpleName();

    //private transient final Context ctx;
    private final String id;
    private final String name;
    private final String category;
    private final String actionRead;
    private final String actionWrite;
    private final String actionBrightness;
    private final String actionColor;
    private final String actionSaturation;

    public BridgeResource(String id, String name, String category, String actionRead, String actionWrite){
        this.id = id;
        this.name = name;
        this.category = category;
        this.actionRead = actionRead;
        this.actionWrite = actionWrite;
        this.actionBrightness = "bri";
        this.actionColor = "hue";
        this.actionSaturation = "sat";
    }

    public String getId(){
        return id;
    }

    public String getCategory(){
        return category;
    }

    public String getActionRead(){
        return actionRead;
    }

    public String getActionWrite(){
        return actionWrite;
    }

    public String getBrightnessAction(){
        return actionBrightness;
    }

    public String getColorAction(){
        return actionColor;
    }

    public String getSaturationAction(){
        return actionSaturation;
    }

    public String getName(){
        return name;
    }
    /*public String getName(Context ctx){
        if (category.equals("scenes")) {
            HueBridge bridge = HueBridge.getInstance(ctx);
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
                return "";
            }
            try {
                if (bridge.getSceneGroup(this).equals("0")) {
                    return "All";
                }
                else {
                    return bridge.getState().
                            getJSONObject("groups").
                            getJSONObject(bridge.getSceneGroup(this)).
                            getString("name");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            HueBridge bridge = HueBridge.getInstance(ctx);
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
                return "";
            }
            return  bridge.getState().
                    getJSONObject(getCategory()).
                    getJSONObject(getId()).
                    getString("name");
        } catch (JSONException ex) {
            Log.e(TAG, "JSONException");
            ex.printStackTrace();
            return "Not reachable";
        }
    }*/

    private int getState(Context ctx){
        try {
            if (getCategory().equals("scenes")) {
                Log.w(TAG,"You shouldn't use this!");
                return 1;
            }
            HueBridge bridge = HueBridge.getInstance(ctx);
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
                return 0;
            }
            if (getActionRead().equals("any_on")){
                if (bridge.getState().
                        getJSONObject(getCategory()).
                        getJSONObject(getId()).
                        getJSONObject("state").
                        getBoolean("all_on"))
                    return 1;
                return bridge.getState().
                        getJSONObject(getCategory()).
                        getJSONObject(getId()).
                        getJSONObject("state").
                        getBoolean("any_on") ? 2 : 0;
            }
            return  (bridge.getState().
                    getJSONObject(getCategory()).
                    getJSONObject(getId()).
                    getJSONObject("state").
                    getBoolean(getActionRead()) ?
                    1 : 0);
        } catch (JSONException ex) {
            Log.e(TAG, "JSONException");
            ex.printStackTrace();
            return -1;
        }
    }

    public int getColor(Context ctx){
        try {
            if (category.equals("scenes") || category.equals("groups")) {
                Log.e(TAG,"You shouldn't use this!");
                return 0;
            }
            HueBridge bridge = HueBridge.getInstance(ctx);
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
                return 0;
            }
            return  (bridge.getState().
                    getJSONObject(getCategory()).
                    getJSONObject(getId()).
                    getJSONObject("state").
                    getInt(getColorAction()));
        } catch (JSONException ex) {
            Log.e(TAG, "JSONException");
            ex.printStackTrace();
            return 0;
        }
    }

    public int getSaturation(Context ctx){
        try {
            if (category.equals("scenes") || category.equals("groups")) {
                Log.e(TAG,"You shouldn't use this!");
                return 255;
            }
            HueBridge bridge = HueBridge.getInstance(ctx);
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
                return 255;
            }
            return  (bridge.getState().
                    getJSONObject(getCategory()).
                    getJSONObject(getId()).
                    getJSONObject("state").
                    getInt(getSaturationAction()));
        } catch (JSONException ex) {
            Log.e(TAG, "JSONException");
            ex.printStackTrace();
            return 255;
        }
    }

    public String getBtnText(Context ctx){
        if (category.equals("scenes")) {
            try {
                HueBridge bridge = HueBridge.getInstance(ctx);
                if(bridge == null){
                    Log.wtf(TAG, "bridge == null");
                    return "";
                }
                return  bridge.getState().
                        getJSONObject(getCategory()).
                        getJSONObject(getId()).
                        getString("name");
            } catch (JSONException ex) {
                Log.e(TAG, "JSONException");
                ex.printStackTrace();
                return "Not reachable";
            }
        }
        Resources resources = ctx.getResources();
        switch (getState(ctx)) {
            case 0:
                return resources.getString(R.string.off_symbol);
            case 1:
                return resources.getString(R.string.on_symbol);
            case 2:
                return resources.getString(R.string.large_minus);
            default:
                return resources.getString(R.string.question_symbol);
        }
    }

    public int getBtnTextColor(Context ctx){
        if (category.equals("scenes"))
            return ContextCompat.getColor(ctx, R.color.black);
        switch (getState(ctx)) {
            case 1:
            case 2:
                return ContextCompat.getColor(ctx, R.color.black);
            case 0:
            default:
                return ContextCompat.getColor(ctx, R.color.white);
        }
    }

    public int getBtnBackgroundResource(Context ctx){
        if (category.equals("scenes"))
            return R.drawable.on_button_background;
        switch (getState(ctx)) {
            case 0:
                return R.drawable.off_button_background;
            case 1:
            case 2:
                return R.drawable.on_button_background;
            default:
                return R.drawable.add_button_background;
        }
    }

    public String getStateUrl(){
        if (category.equals("scenes")) {
            Log.w(TAG,"You shouldn't use this!");
            return null;
        }
        return "/" + getCategory() + "/" + getId() + "/state";
    }

    public String getActionUrl(){
        if (category.equals("scenes")) {
            return "/" + "groups" + "/" + 0 + "/action";
        }
        return "/" + getCategory() + "/" + getId() + "/action";
    }

    @Override
    public int compareTo(BridgeResource bridgeResource) {
        return this.name.compareTo(bridgeResource.name);
    }
}
