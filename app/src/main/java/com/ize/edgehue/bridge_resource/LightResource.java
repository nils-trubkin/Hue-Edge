package com.ize.edgehue.bridge_resource;

import android.content.Context;
import androidx.core.content.ContextCompat;
import com.ize.edgehue.R;
import org.json.JSONException;

public class LightResource extends BridgeResource{

    public LightResource(Context context, int id){
        super(context, id);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        try {
            return  bridge.getState().
                    getJSONObject("lights").
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
            return  (bridge.getState().
                    getJSONObject("lights").
                    getJSONObject(String.valueOf(id)).
                    getJSONObject("state").
                    getBoolean("on")) ?
                    "◯" : "|";
        } catch (JSONException e) {
            e.printStackTrace();
            return "?";
        }
    }

    @Override
    public int getBtnTextColor() {
        try {
            return  (bridge.getState().
                    getJSONObject("lights").
                    getJSONObject(String.valueOf(id)).
                    getJSONObject("state").
                    getBoolean("on")) ?
                    ContextCompat.getColor(ctx, R.color.black) : ContextCompat.getColor(ctx, R.color.white);
        } catch (JSONException e) {
            e.printStackTrace();
            return ContextCompat.getColor(ctx, R.color.white);
        }
    }

    @Override
    public int getBtnBackgroundResource() {
        try {
            return  (bridge.getState().
                    getJSONObject("lights").
                    getJSONObject(String.valueOf(id)).
                    getJSONObject("state").
                    getBoolean("on")) ?
                    R.drawable.on_button_background : R.drawable.off_button_background;
        } catch (JSONException e) {
            e.printStackTrace();
            return R.drawable.add_button_background;
        }
    }

    @Override
    public void activateResource(Context context) {
        bridge.toggleHueState(context, this);
    }

    @Override
    public String getStateUrl(){
        return "/lights/" + getId() + "/state";
    }
}
