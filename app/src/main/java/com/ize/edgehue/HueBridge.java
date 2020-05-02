package com.ize.edgehue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.ize.edgehue.resource.BridgeResource;
import com.ize.edgehue.resource.LightResource;
import com.ize.edgehue.api.JsonCustomRequest;
import com.ize.edgehue.api.RequestQueueSingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HueBridge {
    private static final String TAG = HueBridge.class.getSimpleName();
    private static final String ACTION_RECEIVE_HUE_STATE = "com.ize.edgehue.ACTION_RECEIVE_HUE_STATE";
    private static final String ACTION_RECEIVE_HUE_REPLY = "com.ize.edgehue.ACTION_RECEIVE_HUE_REPLY";
    private static HueBridge instance;
    private static String url = "http://192.168.69.166/api/aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD";
    private static String urlHeader = "http://";
    private static String ip;
    private static String user;
    private static JSONObject state;
    private static String username = "aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD";

    private HueBridge(String ipAddress, String userName) {
        ip = ipAddress;
        user = userName;
    }

    private static synchronized void deleteInstance() {
        instance = null;
    }

    public static synchronized HueBridge getInstance() {
        if (instance == null) {
            return null;
        }
        return instance;
    }

    public static synchronized HueBridge getInstance(String ipAddress, String userName) {
        if (ipAddress == null || userName == null) {
            return null;
        }
        instance = new HueBridge(ipAddress, userName);
        return instance;
    }


    public JSONObject getState() {
        return state;
    }

    private static void setState(JSONObject hueState) {
        HueBridge.state = hueState;
    }

    public static String getUrl() {
        return url;
    }

    public static void setUrl(String url) {
        HueBridge.url = url;
    }

    public static String getUsername() {
        return user;
    }

    public static void setUsername(String username) {
        HueBridge.user = username;
    }

    public void toggleHueState(Context context, BridgeResource br){
        if(br instanceof LightResource) {
            Log.d(TAG, "toggleHueState entered for id: "+ br.getId());
            int lightId = br.getId();
            String stateUrl = br.getStateUrl();
            boolean lastState;
            try {
                lastState = state.getJSONObject("lights").
                        getJSONObject(String.valueOf(lightId)).
                        getJSONObject("state").getBoolean("on");
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            setHueState(context, stateUrl, !lastState);
        }
    }

    public void setHueState(Context context, final String resourceUrl, final boolean state) {
        Log.d(TAG, "setHueState entered");
        JSONObject jo = getJsonOnObject(state);
        JsonCustomRequest jcr = getJsonCustomRequest(context, jo, resourceUrl);
        Log.d(TAG, "changeHueState putRequest created");
        Log.d(TAG, "url: " + resourceUrl);
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(context).addToRequestQueue(jcr);
    }

    public void requestHueState(final Context context) {
        // Request a string response from the provided URL.
        String url = urlHeader + ip + "/api/" + user;
        JsonObjectRequest jor = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        state = response;
                        try {
                            HueBridge.getInstance().getStateIntent(context, 0, 0).send();
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, error.toString());
                    }
                });

        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(context).addToRequestQueue(jor);
        Log.d(TAG, "request sent to queue");
    }

    private PendingIntent getStateIntent(Context context, int id, int key) {
        Intent stateIntent = new Intent(context, EdgeHueProvider.class);
        stateIntent.setAction(ACTION_RECEIVE_HUE_STATE);
        stateIntent.putExtra("id", id);
        stateIntent.putExtra("key", key);
        return PendingIntent.getBroadcast(context, 1, stateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getReplyIntent(Context context, int id, int key) {
        Intent replyIntent = new Intent(context, EdgeHueProvider.class);
        replyIntent.setAction(ACTION_RECEIVE_HUE_REPLY);
        replyIntent.putExtra("id", id);
        replyIntent.putExtra("key", key);
        return PendingIntent.getBroadcast(context, 1, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private JSONObject getJsonOnObject(boolean state) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("on", state);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private JsonCustomRequest getJsonCustomRequest(final Context context, final JSONObject jo, final String resourceUrl){
        return new JsonCustomRequest(Request.Method.PUT, url+resourceUrl, jo,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "setHueState url " + url+resourceUrl);
                        Log.d(TAG, "setHueState responds " + response.toString());
                        boolean success = false;
                        try {
                            boolean requestedState = jo.getBoolean("on");
                            boolean responseState = response.getJSONObject(0).
                                    getJSONObject("success").
                                    getBoolean(resourceUrl+"/on"); //"/lights/" + id + "/state/on"
                            success = requestedState == responseState;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (success) {
                            Log.d(TAG, "changeHueState successful");
                            try {
                                HueBridge.getInstance().getReplyIntent(context, 0, 0).send();
                            } catch (PendingIntent.CanceledException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.d(TAG, "changeHueState unsuccessful");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, error.toString());
                    }
                }
        );
    }
}