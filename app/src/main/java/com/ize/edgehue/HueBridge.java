package com.ize.edgehue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
//import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HueBridge {
    private static HueBridge instance;
    private static Context ctx;
    private static String url = "http://192.168.69.166/api/aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD/";

    private static String urlHeader = "http://";
    private static String ip;
    private static String user;

    private static JSONObject state;

    private static String username = "aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD";

    private static final String TAG = HueBridge.class.getSimpleName();

    private static final String ACTION_RECEIVE_HUE_STATE = "com.ize.edgehue.ACTION_RECEIVE_HUE_STATE";
    private static final String ACTION_RECEIVE_HUE_REPLY = "com.ize.edgehue.ACTION_RECEIVE_HUE_REPLY";

    private HueBridge(Context context, String ipAddress, String userName) {
        ctx = context;
        ip = ipAddress;
        user = userName;
    }

    private PendingIntent getStateIntent(Context context, int id, int key) {
        Intent stateIntent = new Intent(context, EdgeHueProvider.class);
        stateIntent.setAction(ACTION_RECEIVE_HUE_STATE);
        stateIntent.putExtra("id", id);
        stateIntent.putExtra("key", key);
        PendingIntent pStateIntent = PendingIntent.getBroadcast(context, 1, stateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pStateIntent;
    }

    private PendingIntent getReplyIntent(Context context, int id, int key) {
        Intent replyIntent = new Intent(context, EdgeHueProvider.class);
        replyIntent.setAction(ACTION_RECEIVE_HUE_REPLY);
        replyIntent.putExtra("id", id);
        replyIntent.putExtra("key", key);
        PendingIntent pReplyIntent = PendingIntent.getBroadcast(context, 1, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pReplyIntent;
    }

    private static synchronized void deleteInstance() {
        instance = null;
    }

    public static synchronized HueBridge getInstance(){
        if (instance == null){
            return null;
        }
        return instance;
    }

    public static synchronized HueBridge getInstance(Context context, String ipAddress, String userName) {
        if (ipAddress == null || userName == null) {
            return null;
        }
        instance = new HueBridge(context, ipAddress, userName);
        return instance;
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

    public JSONObject getState() {
        return state;
    }

    private static void setState(JSONObject hueState) {
        HueBridge.state = hueState;
    }

    public static void changeHueState (final int lightId, final boolean state){
        Log.d(TAG, "changeHueState entered");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("on", state);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String fullUrl = urlHeader + ip + "/api/" + user + "/lights/" + lightId + "/state";
        JsonCustomRequest putRequest = new JsonCustomRequest(Request.Method.PUT, fullUrl, jsonObject,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "changeHueState responds");
                        boolean success = false;
                        try {
                            success =
                                response.getJSONObject(0)
                                    .getJSONObject("success")
                                    .getBoolean("/lights/" + lightId + "/state/on") == state;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if(success) {
                            Log.d(TAG, "changeHueState successful");
                            try {
                                HueBridge.getInstance().getReplyIntent(ctx, lightId, 0).send();
                            } catch (PendingIntent.CanceledException e) {
                                e.printStackTrace();
                            }
                        }
                        else{
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
        Log.d(TAG, "changeHueState putRequest created");
        Log.d(TAG, "fullUrl: " + fullUrl);
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(ctx).addToRequestQueue(putRequest);
    }

    public static void requestHueState() {
        // Request a string response from the provided URL.
        String fullUrl = urlHeader + ip + "/api/" + user;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(fullUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        state = response;
                        try {
                            HueBridge.getInstance().getStateIntent(ctx, 0, 0).send();
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, error.toString());
                    }
                });

        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(ctx).addToRequestQueue(jsonObjectRequest);
        Log.d(TAG, "request sent to queue");
    }








}