package com.ize.edgehue.resource;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.ize.edgehue.EdgeHueProvider;
import com.ize.edgehue.HueBridge;
import com.ize.edgehue.R;

import org.json.JSONException;

public class BridgeResource {

    private static final String TAG = BridgeResource.class.getSimpleName();

    final Context ctx;
    final String id;
    final String category;
    String actionRead;
    String actionWrite;

    public BridgeResource(Context context, String id, String category, String actionRead, String actionWrite){
        this.ctx = context;
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

    public String getName(){
        try {
            HueBridge bridge = HueBridge.getInstance();
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
            }
            assert bridge != null;
            return  bridge.getState().
                    getJSONObject(getCategory()).
                    getJSONObject(getId()).
                    getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
            return "Not reachable";
        }
    }

    public int getState(){
        try {
            HueBridge bridge = HueBridge.getInstance();
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
            }
            assert bridge != null;
            return  (bridge.getState().
                    getJSONObject(getCategory()).
                    getJSONObject(getId()).
                    getJSONObject("state").
                    getBoolean(actionRead) ?
                    1 : 0);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }
    public String getBtnText(){
        switch (getState()) {
            case 0:
                return "◯";
            case 1:
                return "|";
            default:
                return "?";
        }
    }

    public int getBtnTextColor(){
        switch (getState()) {
            case 1:
                return ContextCompat.getColor(ctx, R.color.black);
            case 0:
            default:
                return ContextCompat.getColor(ctx, R.color.white);
        }
    }

    public int getBtnBackgroundResource(){
        switch (getState()) {
            case 0:
                return R.drawable.off_button_background;
            case 1:
                return R.drawable.on_button_background;
            default:
                return R.drawable.add_button_background;
        }
    }

    public String getStateUrl(){
        return "/" + getCategory() + "/" + getId() + "/state";
    }

    public String getActionUrl(){
        return "/" + getCategory() + "/" + getId() + "/action";
    }
}
