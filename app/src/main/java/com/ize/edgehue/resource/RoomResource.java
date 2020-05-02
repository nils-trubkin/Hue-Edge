package com.ize.edgehue.resource;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.ize.edgehue.HueBridge;
import com.ize.edgehue.R;

import org.json.JSONException;

public class RoomResource extends BridgeResource{

    private static final String TAG = RoomResource.class.getSimpleName();

    public RoomResource(Context context, int id) {
        super(context, id);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        try {
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
            }
            assert bridge != null;
            return  bridge.getState().
                    getJSONObject("groups").
                    getJSONObject(String.valueOf(id)).
                    getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
            return "Not reachable";
        }
    }

    @Override
    public String getBtnText() {
        try {
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
            }
            assert bridge != null;
            return  (bridge.getState().
                    getJSONObject("groups").
                    getJSONObject(String.valueOf(id)).
                    getJSONObject("state").
                    getBoolean("any_on")) ?
                    "◯" : "|";
        } catch (JSONException e) {
            e.printStackTrace();
            return "?";
        }
    }

    @Override
    public int getBtnTextColor() {
        try {
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
            }
            assert bridge != null;
            return  (bridge.getState().
                    getJSONObject("groups").
                    getJSONObject(String.valueOf(id)).
                    getJSONObject("state").
                    getBoolean("any_on")) ?
                    ContextCompat.getColor(ctx, R.color.black) : ContextCompat.getColor(ctx, R.color.white);
        } catch (JSONException e) {
            e.printStackTrace();
            return ContextCompat.getColor(ctx, R.color.white);
        }
    }

    @Override
    public int getBtnBackgroundResource() {
        try {
            if(bridge == null){
                Log.wtf(TAG, "bridge == null");
            }
            assert bridge != null;
            return  (bridge.getState().
                    getJSONObject("groups").
                    getJSONObject(String.valueOf(id)).
                    getJSONObject("state").
                    getBoolean("any_on")) ?
                    R.drawable.on_button_background : R.drawable.off_button_background;
        } catch (JSONException e) {
            e.printStackTrace();
            return R.drawable.add_button_background;
        }
    }

    @Override
    public void activateResource(Context context) {
        boolean any_on = false;
        if(bridge == null){
            Log.wtf(TAG, "bridge == null");
        }
        assert bridge != null;
        try {
            assert HueBridge.getInstance() != null;
            any_on = HueBridge.getInstance().
                    getState().
                    getJSONObject("groups").
                    getJSONObject(String.valueOf(id)).
                    getJSONObject("state").
                    getBoolean("any_on");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        bridge.setHueState(context, getStateUrl(), !any_on);
    }

    @Override
    public String getStateUrl(){
        return "/groups/" + getId() + "/action";
    }
}
