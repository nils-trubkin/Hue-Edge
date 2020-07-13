package com.ize.edgehue;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;

import org.json.JSONException;

import java.io.Serializable;

public class BridgeResource implements Serializable {

    private static final String TAG = BridgeResource.class.getSimpleName();

    //private transient final Context ctx;
    private final String id;
    private final String category;
    private final String actionRead;
    private final String actionWrite;

    public BridgeResource(Context context, String id, String category, String actionRead, String actionWrite){
        //this.ctx = context;
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

    public String getName(Context ctx){
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
            Log.d(TAG, "Exception!!!");
            e.printStackTrace();
            String toastString = e.toString();
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            return "Not reachable";
        }
    }

    private int getState(Context ctx){
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
    public String getBtnText(Context ctx){
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
        Resources resources = ctx.getResources();
        switch (getState(ctx)) {
            case 0:
                return resources.getString(R.string.off_symbol);
            case 1:
                if (actionRead.equals("any_on"))
                    return resources.getString(R.string.large_minus);
                else
                    return resources.getString(R.string.on_symbol);
            default:
                return resources.getString(R.string.question_symbol);
        }
    }

    public int getBtnTextColor(Context ctx){
        if (category.equals("scenes"))
            return ContextCompat.getColor(ctx, R.color.black);
        switch (getState(ctx)) {
            case 1:
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
