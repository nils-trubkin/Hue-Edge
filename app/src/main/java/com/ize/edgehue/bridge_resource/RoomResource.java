package com.ize.edgehue.bridge_resource;

import com.ize.edgehue.HueBridge;

import org.json.JSONException;

public class RoomResource extends BridgeResource{
    private HueBridge bridge;
    int id;
    LightResource[] lights;

    public RoomResource(int id){
        bridge = HueBridge.getInstance();
        this.id = id;
    }

    @Override
    public String getName() throws JSONException {
        return  bridge.getState().
                getJSONObject("lights").
                getJSONObject(String.valueOf(id)).
                getString("name");
    }

    public boolean getState() throws JSONException {
        return  bridge.getState().
                getJSONObject("lights").
                getJSONObject(String.valueOf(id)).
                getJSONObject("state").
                getBoolean("on");
    }

    @Override
    public void activateResource() throws JSONException {
        for (LightResource light : lights){
            bridge.toggleHueState(light.id);
        }
    }
}
