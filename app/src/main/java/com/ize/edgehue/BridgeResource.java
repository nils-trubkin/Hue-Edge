package com.ize.edgehue;

import android.content.Context;
import android.util.Log;
import androidx.core.content.ContextCompat;

import org.json.JSONException;

public class BridgeResource {

    private static final String TAG = BridgeResource.class.getSimpleName();

    private final Context ctx;
    private final String id;
    private final String category;
    private String actionRead;
    private String actionWrite;

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
            e.printStackTrace();
            return "Not reachable";
        }
    }

    private int getState(){
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
                    getBoolean(actionRead) ?
                    1 : 0);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }
    public String getBtnText(){
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
        switch (getState()) {
            case 0:
                return "◯";
            case 1:
                if (actionRead.equals("any_on"))
                    return "—";
                else
                    return "|";
            default:
                return "?";
        }
    }

    public int getBtnTextColor(){
        if (category.equals("scenes"))
            return ContextCompat.getColor(ctx, R.color.black);
        switch (getState()) {
            case 1:
                return ContextCompat.getColor(ctx, R.color.black);
            case 0:
            default:
                return ContextCompat.getColor(ctx, R.color.white);
        }
    }

    public int getBtnBackgroundResource(){
        if (category.equals("scenes"))
            return R.drawable.on_button_background;
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
