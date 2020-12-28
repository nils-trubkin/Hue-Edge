package com.nilstrubkin.hueedge.resources;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

public class SceneResource extends BridgeResource {

    private static final long serialVersionUID = 8883055438554823607L;
    String type;
    String group;
    String groupName;
    List<String> lights;

    public String getType() {
        return type;
    }

    public String getGroup() {
        return group;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<String> getLights() {
        return lights;
    }

    @Override
    public void activateResource(Context ctx) {
        try {
            String bridgeUrl = Objects.requireNonNull(HueBridge.getInstance(ctx)).getUrl();
            String actionWrite = getActionWrite();
            String id = getId();
            JSONObject jsonObject = new JSONObject().put(actionWrite, id);

            post(ctx,bridgeUrl + getActionUrl(), jsonObject.toString());
        } catch (NullPointerException | JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public String getCategory() {
        return "scenes";
    }

    @Override
    public String getActionRead() {
        return "scene";
    }

    @Override
    public String getActionWrite() {
        return "scene";
    }

    @Override
    public String getBtnText(Context ctx) {
        return getName();
    }

    @Override
    public int getBtnTextSize(Context ctx) {
        return R.dimen.resource_btn_text_size_scene;
    }

    @Override
    public int getBtnTextColor(Context ctx) {
        return ContextCompat.getColor(ctx, R.color.black);
    }

    @Override
    public int getBtnBackgroundResource() {
        return R.drawable.on_button_background;
    }

    @Override
    public String getUnderBtnText(){
        return getGroupName();
    }
}