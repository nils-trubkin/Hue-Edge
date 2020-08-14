package com.nilstrubkin.hueedge.resources;

import android.app.PendingIntent;
import android.content.Context;

import com.nilstrubkin.hueedge.HueEdgeProvider;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class BridgeResource implements Serializable {

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

    public int compareTo(BridgeResource br) {
        String thisClass = this.getClass().toString();
        String thatClass = br.getClass().toString();
        int categoryDiff = thisClass.compareTo(thatClass);
        if (categoryDiff != 0)
            return categoryDiff;
        else {
            String thisName = this.getName();
            String thatName = br.getName();
            int nameDiff = thisName.compareTo(thatName);
            if (nameDiff != 0)
                return nameDiff;
            else
                return this.getUnderBtnText().compareTo(br.getUnderBtnText());
        }
    }

    public String getActionUrl() {
        if (getClass().equals(SceneResource.class)) {
            return "/" + "groups" + "/" + 0 + "/action";
        }
        return "/" + getCategory() + "/" + getId() + "/action";
    }

    String post(final Context ctx, final String url, final String json) {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        Callable<String> callable = () -> {
            RequestBody body = RequestBody.create(json, JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .build();
            final OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                HueEdgeProvider.getReplyIntent(ctx).send();
                return Objects.requireNonNull(response.body()).string();
            } catch (NullPointerException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                HueEdgeProvider.getTimeoutIntent(ctx).send();
                return null;
            }
        };
        Future<String> future = pool.submit(callable);
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            try {
                HueEdgeProvider.getTimeoutIntent(ctx).send();
            } catch (PendingIntent.CanceledException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return null;
        }
    }
}

