package com.ize.edgehue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.ize.edgehue.api.JsonCustomRequest;
import com.ize.edgehue.api.RequestQueueSingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class HueBridge implements Serializable {
    private static final String TAG = HueBridge.class.getSimpleName();

    private static HueBridge instance;
    private static Context ctx;

    private static String url;
    private static JSONObject state;

    private static HashMap<String, BridgeResource> lights = new HashMap<>();
    private static HashMap<String, BridgeResource> rooms = new HashMap<>();
    private static HashMap<String, BridgeResource> zones = new HashMap<>();
    private static HashMap<String, BridgeResource> scenes = new HashMap<>();



    //Default constructor with http header
    private HueBridge(Context ctx, String ip, String userName) {
        this(ctx, ip, userName, ctx.getString(R.string.http_header));
    }

    //Custom constructor for future use TODO HTTPS
    private HueBridge(Context ctx, String ip, String userName, String urlHeader) {
        HueBridge.ctx = Objects.requireNonNull(ctx);
        HueBridge.url =
                Objects.requireNonNull(urlHeader) +
                Objects.requireNonNull(ip) +
                "/api/" +
                Objects.requireNonNull(userName);
    }

    //Get instance of instantiated HueBridge
    public static synchronized HueBridge getInstance() {
        if (instance == null) {
            Log.w(TAG, "getInstance() null! Is this the first startup?");
            return null;
        }
        return instance;
    }

    //Constructor for instance, first time setup
    public static synchronized HueBridge getInstance(Context ctx, String ip, String userName) {
        instance = new HueBridge(ctx, ip, userName);
        return instance;
    }

    //Delete the instance TODO App Reset
    public static synchronized void deleteInstance() {
        Log.i(TAG, "Deleting instance of HueBridge");
        instance = null;
    }

    public JSONObject getState() {
        return state;
    }

    public HashMap<String, BridgeResource> getLights() {
        return lights;
    }

    public HashMap<String, BridgeResource> getRooms() {
        return rooms;
    }

    public HashMap<String, BridgeResource> getZones() {
        return zones;
    }

    public HashMap<String, BridgeResource> getScenes() {
        return scenes;
    }

    public String getSceneGroup(BridgeResource br) {
        try {
            return  state.
                    getJSONObject("scenes").
                    getJSONObject(br.getId()).
                    getString("group");
        } catch (JSONException e) {
            Log.e(TAG, "Can't getSceneGroup()");
            e.printStackTrace();
            return null;
        }
    }

    public void saveConfigurationToMemory() {
        try {
            Log.d(TAG, "saveConfigurationToMemory()");
            SharedPreferences sharedPref = ctx.getSharedPreferences(ctx.getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            Gson gson = new Gson();
            String json = gson.toJson(HueBridge.getInstance());
            editor.putString(ctx.getResources().getString(R.string.hue_bridge_config_file), json);
            editor.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void loadConfigurationFromMemory(){
        try {
            Log.d(TAG, "loadConfigurationFromMemory()");
            SharedPreferences sharedPref = ctx.getSharedPreferences(ctx.getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            Gson gson = new Gson();
            String json = sharedPref.getString(ctx.getResources().getString(R.string.hue_bridge_config_file), "");
            instance = gson.fromJson(json, HueBridge.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshAllHashMaps(){
        Iterator<String> keys = state.keys();
        while(keys.hasNext()){ // iterate over categories
            String key = keys.next();
            if(key.equals("lights") || key.equals("groups") || key.equals("scenes")) {
                try {
                    JSONObject resources = state.getJSONObject(key); // get all res. in category
                    Iterator<String> resourcesKeys = resources.keys();
                    while (resourcesKeys.hasNext()) {    // iterate over one res. at a time
                        String resourcesKey = resourcesKeys.next();
                        JSONObject resource = resources.getJSONObject(resourcesKey);
                        if (key.equals("lights")) {
                            BridgeResource br = new BridgeResource(
                                    ctx,
                                    resourcesKey,
                                    key,
                                    "on",
                                    "on");
                            lights.put(resourcesKey, br);
                        }
                        else if (key.equals("groups")) {
                            BridgeResource br = new BridgeResource(
                                    ctx,
                                    resourcesKey,
                                    key,
                                    "any_on",
                                    "on");
                            if(resource.getString("type").equals("Room"))
                                rooms.put(resourcesKey, br);
                            else if(resource.getString("type").equals("Zone"))
                                zones.put(resourcesKey, br);
                        }
                        else if (key.equals("scenes")) {
                            Iterator<String> sceneKeys = resource.keys();
                            while (sceneKeys.hasNext()){
                                if(sceneKeys.next().equals("group")){
                                    BridgeResource br = new BridgeResource(
                                            ctx,
                                            resourcesKey,
                                            key,
                                            "scene",
                                            "scene");
                                    scenes.put(resourcesKey, br);
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.wtf(TAG, "Can't find lights in state!");
                    e.printStackTrace();
                }
            }
        }
    }

    public void toggleHueState(BridgeResource br){
        String id = br.getId();
        String category = br.getCategory();
        String actionUrl = category.equals("lights") ? br.getStateUrl() : br.getActionUrl();
        String actionRead = br.getActionRead();
        String actionWrite = br.getActionWrite();
        Log.d(TAG, "toggleHueState entered for: " + category + " " + id);
        boolean lastState;
        if(category.equals("scenes")){
            setHueState(actionUrl, actionWrite, id);
        }
        else {
            try {
                lastState = state.
                        getJSONObject(category).
                        getJSONObject(id).
                        getJSONObject("state").
                        getBoolean(actionRead);
            } catch (JSONException e) {
                Log.e(TAG, "Can't get lastState");
                e.printStackTrace();
                return;
            }
            setHueState(actionUrl, actionWrite, !lastState);
        }
    }

    //Set given pair to resourceUrl
    private void setHueState(final String resourceUrl, final String key, final Object value) {
        Log.d(TAG, "setHueState entered");
        JSONObject j = createJsonOnObject(key, value);
        assert j != null;
        JsonCustomRequest jcr = getJsonCustomRequest(j, resourceUrl);
        Log.d(TAG, "changeHueState putRequest created for this url\n" + resourceUrl);
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(ctx).addToRequestQueue(jcr);
    }

    //Construct intent for incoming state JsonObject
    private PendingIntent getStateIntent() {
        Intent stateIntent = new Intent(ctx, EdgeHueProvider.class);
        stateIntent.setAction(EdgeHueProvider.ACTION_RECEIVE_HUE_STATE);
        return PendingIntent.getBroadcast(ctx, 1, stateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Construct intent for incoming reply JsonArray
    private PendingIntent getReplyIntent() {
        Intent replyIntent = new Intent(ctx, EdgeHueProvider.class);
        replyIntent.setAction(EdgeHueProvider.ACTION_RECEIVE_HUE_REPLY);
        return PendingIntent.getBroadcast(ctx, 1, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Create a JsonObject to send to hue bridge
    public static JSONObject createJsonOnObject(String k, Object v) {
        try {
            return new JSONObject().put(k, v);
        } catch (JSONException e) {
            Log.wtf(TAG, "Can not create JsonObject. Missing dep?");
            e.printStackTrace();
        }
        return null;
    }

    //GET method
    public void requestHueState() {
        JsonObjectRequest jor = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        state = response;
                        if (HueBridge.getInstance() == null){
                            Log.wtf(TAG, "HueBridge.getInstance() == null");
                        }
                        assert HueBridge.getInstance() != null;
                        HueBridge bridge = HueBridge.getInstance();
                        bridge.refreshAllHashMaps();
                        try {
                            bridge.getStateIntent().send();
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.toString());
                    }
                });
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(ctx).addToRequestQueue(jor);
        Log.d(TAG, "request sent to queue");
    }

    //PUT method
    private JsonCustomRequest getJsonCustomRequest(final JSONObject jsonObject, final String resourceUrl){
        if(!jsonObject.keys().hasNext()){ // make sure we get an object that is not empty
            Log.wtf(TAG, "!jsonObject.keys().hasNext() Is this an empty request?");
            return null;
        }
        assert jsonObject.keys().hasNext();
        Log.d(TAG, "setHueState url " + url + resourceUrl); // this is the actual resource path
        return new JsonCustomRequest(Request.Method.PUT, url + resourceUrl, jsonObject,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    Log.d(TAG, "setHueState responds " + response.toString());
                    boolean success = false; // assume the worst
                    Iterator<String> responseKeys; // iterator for response JSONObject
                    String responseKey = null; // index for response JSONObject
                    Iterator<String> requestKeys; // iterator for arg JSONObject jsonObject
                    String requestKey = null; // index for arg JSONObject jsonObject

                    String responseState = null; //
                    String requestedState = null;

                    try {
                        JSONObject jsonResponse = response.getJSONObject(0);
                        responseKeys = jsonResponse.keys();
                        if(responseKeys.hasNext()) {
                            responseKey = responseKeys.next();
                            if(!responseKey.equals("success")){  //response key should be success
                                Log.e(TAG, "Unsuccesfull! Check reply");
                                return;
                            }
                            requestKeys = jsonObject.keys();    // this can be "on" or "all_on" for example
                            if(requestKeys.hasNext()) {
                                requestKey = requestKeys.next();
                                requestedState = jsonObject.getString(requestKey);
                                assert responseKey.equals("success");
                                JSONObject responseValue = jsonResponse.getJSONObject(responseKey); // get key for success
                                responseState = responseValue.getString(resourceUrl + "/" + requestKey); //get response for request
                            }
                        }
                        success = requestedState.equals(responseState);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (success ) {
                        Log.d(TAG, "changeHueState successful");
                        if (HueBridge.getInstance() == null){
                            Log.wtf(TAG, "HueBridge.getInstance() == null");
                        }
                        assert HueBridge.getInstance() != null;
                        try {
                            HueBridge.getInstance().getReplyIntent().send();
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
                    Log.e(TAG, error.toString());
                }
            }
        );
    }

}