package com.nilstrubkin.hueedge.resources;

import android.app.PendingIntent;
import android.content.Context;

import com.nilstrubkin.hueedge.HueEdgeProvider;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class BridgeResource implements Serializable {
    private static final long serialVersionUID = 6709319326596725753L;
    private String id;
    private String name;

    private static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    public abstract void activateResource(Context ctx);

    public abstract String getCategory();

    public abstract String getActionRead();

    public String getActionWrite() {
        return getActionRead();
    }

    public abstract String getBtnText(Context ctx);

    public abstract int getBtnTextSize(Context ctx);

    public abstract int getBtnTextColor(Context ctx);

    public abstract int getBtnBackgroundResource();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        if (getId().equals("0"))
            return "All";
        else
            return name;
    }

    public String getUnderBtnText() {
        return getName();
    }

    public String getActionUrl() {
        if (getClass().equals(SceneResource.class)) {
            return "/" + "groups" + "/" + 0 + "/action";
        }
        return "/" + getCategory() + "/" + getId() + "/action";
    }

    void post(final Context ctx, final String url, final String json){
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();
        HueEdgeProvider.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    HueEdgeProvider.getReplyIntent(ctx).send();
                    response.close();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                try {
                    HueEdgeProvider.getTimeoutIntent(ctx).send();
                } catch (PendingIntent.CanceledException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

}

