package com.ize.hueedge;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.json.JSONException;

import java.io.Serializable;

public class BridgeResource implements Serializable {

    private static final String TAG = BridgeResource.class.getSimpleName();

    //private transient final Context ctx;
    private final String id;
    private final String category;
    private final String actionRead;
    private final String actionWrite;

    public BridgeResource(String id, String category, String actionRead, String actionWrite){
        this.id = id;
        this.category = category;
        this.actionRead = actionRead;
        this.actionWrite = actionWrite;
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
        return "bri";
    }

    public String getColorAction(){
        return "hue";
    }

    public String getSaturationAction(){
        return "sat";
    }

    public String getName(Context ctx){
        if (category.equals("scenes")) {
            HueBridge bridge = HueBridge.getInstance(ctx);
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
            }
            assert bridge != null;
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
            }
            assert bridge != null;
            return  bridge.getState().
                    getJSONObject(getCategory()).
                    getJSONObject(getId()).
                    getString("name");
        } catch (JSONException e) {
            Log.e(TAG, "Exception!!!");
            e.printStackTrace();
            String toastString = e.toString();
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            return "Not reachable";
        }
    }

    private int getState(Context ctx){
        try {
            if (getCategory().equals("scenes")) {
                Log.w(TAG,"You shouldn't use this!");
                return 1;
            }
            HueBridge bridge = HueBridge.getInstance(ctx);
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
            }
            assert bridge != null;
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
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int getColor(Context ctx){
        try {
            if (category.equals("scenes")) {
                Log.w(TAG,"You shouldn't use this!");
                return 1;
            }
            HueBridge bridge = HueBridge.getInstance(ctx);
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
            }
            assert bridge != null;
            return  (bridge.getState().
                    getJSONObject(getCategory()).
                    getJSONObject(getId()).
                    getJSONObject("state").
                    getInt(getColorAction()));
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int getSaturation(Context ctx){
        try {
            if (category.equals("scenes")) {
                Log.w(TAG,"You shouldn't use this!");
                return 1;
            }
            HueBridge bridge = HueBridge.getInstance(ctx);
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
            }
            assert bridge != null;
            return  (bridge.getState().
                    getJSONObject(getCategory()).
                    getJSONObject(getId()).
                    getJSONObject("state").
                    getInt(getSaturationAction()));
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public String getBtnText(Context ctx){
        if (category.equals("scenes")) {
            try {
                HueBridge bridge = HueBridge.getInstance(ctx);
                if(bridge == null){
                    Log.wtf(TAG, "bridge == null");
                }
                assert bridge != null;
                return  bridge.getState().
                        getJSONObject(getCategory()).
                        getJSONObject(getId()).
                        getString("name");
            } catch (JSONException e) {
                Log.e(TAG, "Exception!!!");
                e.printStackTrace();
                String toastString = e.toString();
                Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
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
}