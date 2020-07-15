package com.ize.edgehue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
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
    private transient static final String TAG = HueBridge.class.getSimpleName();

    private static HueBridge instance;

    private final String url;
    private final String ip;
    private transient JSONObject stateJson;
    private String state;

    private final HashMap<String, BridgeResource> lights = new HashMap<>();
    private final HashMap<String, BridgeResource> rooms = new HashMap<>();
    private final HashMap<String, BridgeResource> zones = new HashMap<>();
    private final HashMap<String, BridgeResource> scenes = new HashMap<>();

    //Default constructor with http header
    private HueBridge(Context ctx, String ip, String userName) {
        this(ctx, ip, userName, ctx.getString(R.string.http_header)); // String "http://"
    }

    //Custom constructor for future use TODO HTTPS
    private HueBridge(Context ctx, String ip, String userName, String urlHeader) {
        this.ip = ip;
        this.url =
                Objects.requireNonNull(urlHeader) +
                        Objects.requireNonNull(ip) +
                        ctx.getString(R.string.api_path) + // String "/api/"
                        Objects.requireNonNull(userName);
    }

    //Delete the instance TODO App Reset
    public static synchronized void deleteInstance() {
        Log.i(TAG, "Deleting instance of HueBridge");
        instance = null;
    }

    //Get instance of instantiated HueBridge
    public static synchronized HueBridge getInstance(Context ctx) {
        if (instance == null) {
            Log.i(TAG, "HueBridge instance or state is null. Attempting to load config...");
            EdgeHueProvider.loadAllConfiguration(ctx);
            if (instance == null) {
                Log.w(TAG, "HueBridge instance is still null after loading config. Is this the first startup?");
                return null;
            }
        }
        //Log.d(TAG, "HueBridge instance returned successfully, state is " + instance.state);
        return instance;
    }

    //Constructor for instance, first time setup
    public static synchronized HueBridge getInstance(Context ctx, String ipAddress, String userName) {
        instance = new HueBridge(ctx, ipAddress, userName);
        return instance;
    }

    //Setting the instance for config loading
    public static synchronized void setInstance(HueBridge bridge) {
        instance = bridge;
    }

    public String getIp() {
        return ip;
    }

    public JSONObject getState() {
        if (stateJson != null)
            return stateJson;
        else {
            try {
                stateJson = new JSONObject(state);
                return stateJson;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void setState(JSONObject state) {
        stateJson = state;
        this.state = state.toString();
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
            return getState().
                    getJSONObject("scenes").
                    getJSONObject(br.getId()).
                    getString("group");
        } catch (JSONException e) {
            Log.e(TAG, "Can't getSceneGroup()");
            e.printStackTrace();
            return null;
        }
    }

    private void refreshAllHashMaps() {
        Iterator<String> keys = getState().keys();
        while (keys.hasNext()) { // iterate over categories
            String key = keys.next();
            if (key.equals("lights") || key.equals("groups") || key.equals("scenes")) {
                try {
                    JSONObject resources = getState().getJSONObject(key); // get all res. in category
                    Iterator<String> resourcesKeys = resources.keys();
                    while (resourcesKeys.hasNext()) {    // iterate over one res. at a time
                        String resourcesKey = resourcesKeys.next();
                        JSONObject resource = resources.getJSONObject(resourcesKey);
                        switch (key) {
                            case "lights": {
                                BridgeResource br = new BridgeResource(
                                        resourcesKey,
                                        key,
                                        "on",
                                        "on");
                                getLights().put(resourcesKey, br);
                                break;
                            }
                            case "groups": {
                                BridgeResource br = new BridgeResource(
                                        resourcesKey,
                                        key,
                                        "any_on",
                                        "on");
                                if (resource.getString("type").equals("Room"))
                                    getRooms().put(resourcesKey, br);
                                else if (resource.getString("type").equals("Zone"))
                                    getZones().put(resourcesKey, br);
                                break;
                            }
                            case "scenes":
                                Iterator<String> sceneKeys = resource.keys();
                                while (sceneKeys.hasNext()) {
                                    if (sceneKeys.next().equals("group")) {
                                        BridgeResource br = new BridgeResource(
                                                resourcesKey,
                                                key,
                                                "scene",
                                                "scene");
                                        getScenes().put(resourcesKey, br);
                                    }
                                }
                                break;
                        }
                    }
                } catch (JSONException e) {
                    Log.wtf(TAG, "Can't find lights in state!");
                    e.printStackTrace();
                }
            }
        }
    }

    public void setHueBrightness(Context context, BridgeResource br, int value) {
        String category = br.getCategory();
        if (category.equals("scenes")) {
            Log.e(TAG, "Trying to set brightness for scene");
            return;
        }
        String id = br.getId();
        String actionUrl = category.equals("lights") ? br.getStateUrl() : br.getActionUrl();
        String brightnessAction = br.getBrightnessAction();
        Log.d(TAG, "setHueColorHueState entered for: " + category + " " + id);
        setHueState(context, actionUrl, brightnessAction, value);
    }

    public void setHueColor(Context context, BridgeResource br, int value) {
        String category = br.getCategory();
        if (category.equals("scenes")) {
            Log.e(TAG, "Trying to set color for scene");
            return;
        }
        String id = br.getId();
        String actionUrl = category.equals("lights") ? br.getStateUrl() : br.getActionUrl();
        String colorAction = br.getColorAction();
        Log.d(TAG, "setHueColorHueState entered for: " + category + " " + id);
        setHueState(context, actionUrl, colorAction, value);
    }

    public void setHueSaturation(Context context, BridgeResource br, int value) {
        String category = br.getCategory();
        if (category.equals("scenes")) {
            Log.e(TAG, "Trying to set color for scene");
            return;
        }
        String id = br.getId();
        String actionUrl = category.equals("lights") ? br.getStateUrl() : br.getActionUrl();
        String saturationAction = br.getSaturationAction();
        Log.d(TAG, "setHueColorHueState entered for: " + category + " " + id);
        setHueState(context, actionUrl, saturationAction, value);
    }

    public void toggleHueState(Context context, BridgeResource br){
        String id = br.getId();
        String category = br.getCategory();
        String actionUrl = category.equals("lights") ? br.getStateUrl() : br.getActionUrl();
        String actionRead = br.getActionRead();
        String actionWrite = br.getActionWrite();
        Log.d(TAG, "toggleHueState entered for: " + category + " " + id);
        boolean lastState;
        if(category.equals("scenes")){
            setHueState(context, actionUrl, actionWrite, id);
        }
        else {
            try {
                lastState = getState().
                        getJSONObject(category).
                        getJSONObject(id).
                        getJSONObject("state").
                        getBoolean(actionRead);
            } catch (JSONException e) {
                Log.e(TAG, "Can't get lastState");
                e.printStackTrace();
                return;
            }
            setHueState(context, actionUrl, actionWrite, !lastState);
        }
    }

    //Set given pair to resourceUrl
    private void setHueState(Context context, final String resourceUrl, final String key, final Object value) {
        Log.d(TAG, "setHueState entered");
        JSONObject j = createJsonOnObject(key, value);
        assert j != null;
        JsonCustomRequest jcr = getJsonCustomRequest(context, j, resourceUrl);
        Log.d(TAG, "changeHueState putRequest created for this url\n" + resourceUrl);
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(context).addToRequestQueue(context, jcr);
    }

    //Construct intent for incoming state JsonObject
    private PendingIntent getStateIntent(Context context) {
        Intent stateIntent = new Intent(context, EdgeHueProvider.class);
        stateIntent.setAction(EdgeHueProvider.ACTION_RECEIVE_HUE_STATE);
        return PendingIntent.getBroadcast(context, 1, stateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Construct intent for incoming reply JsonArray
    private PendingIntent getReplyIntent(Context context) {
        Intent replyIntent = new Intent(context, EdgeHueProvider.class);
        replyIntent.setAction(EdgeHueProvider.ACTION_RECEIVE_HUE_REPLY);
        return PendingIntent.getBroadcast(context, 1, replyIntent,
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
    public void requestHueState(final Context ctx) {
        JsonObjectRequest jor = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        setState(response);
                        if (HueBridge.getInstance(ctx) == null){
                            Log.wtf(TAG, "HueBridge.getInstance() == null");
                        }
                        HueBridge bridge = HueBridge.getInstance(ctx);
                        assert bridge != null;
                        bridge.refreshAllHashMaps();
                        try {
                            bridge.getStateIntent(ctx).send();
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
        RequestQueueSingleton.getInstance(ctx).addToRequestQueue(ctx, jor);
        Log.d(TAG, "request sent to queue");
    }

    //PUT method
    private JsonCustomRequest getJsonCustomRequest(final Context ctx, final JSONObject jsonObject, final String resourceUrl){
        if(!jsonObject.keys().hasNext()){ // make sure we get an object that is not empty
            Log.wtf(TAG, "!jsonObject.keys().hasNext() Is this an empty request?");
            return null;
        }
        Log.d(TAG, "setHueState url " + url + resourceUrl); // this is the actual resource path
        return new JsonCustomRequest(Request.Method.PUT, url + resourceUrl, jsonObject,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "setHueState responds " + response.toString());
                        boolean success = false; // assume the worst
                        Iterator<String> responseKeys; // iterator for response JSONObject
                        String responseKey; // index for response JSONObject
                        Iterator<String> requestKeys; // iterator for arg JSONObject jsonObject
                        String requestKey; // index for arg JSONObject jsonObject

                        String responseState = null;
                        String requestedState = null;

                        HueBridge bridge = HueBridge.getInstance(ctx);
                        if (bridge == null){
                            Log.wtf(TAG, "HueBridge.getInstance() == null");
                        }

                        try {
                            JSONObject jsonResponse = response.getJSONObject(0);
                            responseKeys = jsonResponse.keys();
                            if(responseKeys.hasNext()) {
                                responseKey = responseKeys.next();
                                if(!responseKey.equals("success")){  //response key should be success
                                    Log.e(TAG, "Unsuccessful! Check reply");
                                    return;
                                }
                                requestKeys = jsonObject.keys();    // this can be "on" or "all_on" for example
                                if(requestKeys.hasNext()) {
                                    requestKey = requestKeys.next();
                                    requestedState = jsonObject.getString(requestKey);
                                    JSONObject responseValue = jsonResponse.getJSONObject(responseKey); // get key for success
                                    responseState = responseValue.getString(resourceUrl + "/" + requestKey); //get response for request
                                }
                            }
                            assert requestedState != null;
                            if (responseState != null) {
                                success = requestedState.equals(responseState);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (success ) {
                            Log.d(TAG, "changeHueState successful");
                            try {
                                assert bridge != null;
                                bridge.getReplyIntent(ctx).send();
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