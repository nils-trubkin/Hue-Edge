package com.ize.hueedge;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.ize.hueedge.api.JsonCustomRequest;
import com.ize.hueedge.api.RequestQueueSingleton;

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
    private final String userName;
    private transient JSONObject stateJson;
    private String state;

    private HueEdgeProvider.menuCategory currentCategory = HueEdgeProvider.menuCategory.QUICK_ACCESS;
    private HueEdgeProvider.slidersCategory currentSlidersCategory = HueEdgeProvider.slidersCategory.BRIGHTNESS;

    private final HashMap<String, BridgeResource> lights = new HashMap<>();
    private final HashMap<String, BridgeResource> rooms = new HashMap<>();
    private final HashMap<String, BridgeResource> zones = new HashMap<>();
    private final HashMap<String, BridgeResource> scenes = new HashMap<>();

    //Mapping of category to contents
    private HashMap<HueEdgeProvider.menuCategory, HashMap<Integer, BridgeResource>> contents =
            new HashMap<>();

    final String LIGHTS;
    final String GROUPS;
    final String SCENES;
    final String ON;
    final String ANY_ON;
    final String TYPE;
    final String ROOM;
    final String ZONE;
    final String GROUP;
    final String SCENE;
    final String STATE;
    final String SUCCESS;

    //Default constructor with http header
    private HueBridge(Context ctx, String ip, String userName) {
        this(ctx, ip, userName, ctx.getString(R.string.http_header)); // String "http://"
    }

    //Custom constructor for future use TODO HTTPS
    private HueBridge(Context ctx, String ip, String userName, String urlHeader) {
        this.ip = ip;
        this.userName = Objects.requireNonNull(userName);
        this.url =
                Objects.requireNonNull(urlHeader) +
                        Objects.requireNonNull(ip) +
                        ctx.getString(R.string.api_path) + // String "/api/"
                        Objects.requireNonNull(userName);

        //Mappings of integers (representing R.id reference) to an instance of bridgeResource subclass
        getContents().
                put(HueEdgeProvider.menuCategory.QUICK_ACCESS, new HashMap<Integer, BridgeResource>());
        getContents().
                put(HueEdgeProvider.menuCategory.LIGHTS, new HashMap<Integer, BridgeResource>());
        getContents().
                put(HueEdgeProvider.menuCategory.ROOMS, new HashMap<Integer, BridgeResource>());
        getContents().
                put(HueEdgeProvider.menuCategory.ZONES, new HashMap<Integer, BridgeResource>());
        getContents().
                put(HueEdgeProvider.menuCategory.SCENES, new HashMap<Integer, BridgeResource>());

        Resources res = ctx.getResources();
        LIGHTS = res.getString(R.string.hue_api_lights);
        GROUPS = res.getString(R.string.hue_api_groups);
        SCENES = res.getString(R.string.hue_api_scenes);
        ON = res.getString(R.string.hue_api_on);
        ANY_ON = res.getString(R.string.hue_api_any_on);
        TYPE = res.getString(R.string.hue_api_type);
        ROOM = res.getString(R.string.hue_api_Room);
        ZONE = res.getString(R.string.hue_api_Zone);
        GROUP = res.getString(R.string.hue_api_group);
        SCENE = res.getString(R.string.hue_api_scene);
        STATE = res.getString(R.string.hue_api_state);
        SUCCESS = res.getString(R.string.hue_api_success);
    }

    //Delete the instance
    public static synchronized void deleteInstance(Context ctx) {
        Log.i(TAG, "Deleting instance of HueBridge");
        instance = null;
        boolean deleted = HueEdgeProvider.deleteAllConfiguration(ctx);
        if (deleted) {
            String toastString = ctx.getString(R.string.toast_configuration_deleted);
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
        }
    }

    //Get instance of instantiated HueBridge
    public static synchronized HueBridge getInstance(Context ctx) {
        if (instance == null) {
            Log.i(TAG, "HueBridge instance or state is null. Attempting to load config...");
            HueEdgeProvider.loadAllConfiguration(ctx);
            if (instance == null) {
                Log.w(TAG, "HueBridge instance is still null after loading config. Is this the first startup?");
                return null;
            }
            else instance.requestHueState(ctx);
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

    public String getUserName() {
        return userName;
    }

    public JSONObject getState() {
        if (stateJson != null)
            return stateJson;
        else {
            try {
                setStateJson(new JSONObject(state));
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

    public void setStateJson(JSONObject stateJson) {
        this.stateJson = stateJson;
    }

    public HashMap<HueEdgeProvider.menuCategory, HashMap<Integer, BridgeResource>> getContents() {
        return contents;
    }

    public void setContents(HashMap<HueEdgeProvider.menuCategory, HashMap<Integer, BridgeResource>> contents) {
        this.contents = contents;
    }

    public HueEdgeProvider.menuCategory getCurrentCategory() {
        return currentCategory;
    }

    public void setCurrentCategory(HueEdgeProvider.menuCategory currentCategory) {
        this.currentCategory = currentCategory;
    }

    public HueEdgeProvider.slidersCategory getCurrentSlidersCategory() {
        return currentSlidersCategory;
    }

    public void setCurrentSlidersCategory(HueEdgeProvider.slidersCategory currentSlidersCategory) {
        this.currentSlidersCategory = currentSlidersCategory;
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
                    getJSONObject(SCENES).
                    getJSONObject(br.getId()).
                    getString(GROUP);
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
            if (key.equals(LIGHTS) ||
                    key.equals(GROUPS) ||
                    key.equals(SCENES)) {
                try {
                    JSONObject resources = getState().getJSONObject(key); // get all res. in category
                    Iterator<String> resourcesKeys = resources.keys();
                    while (resourcesKeys.hasNext()) {    // iterate over one res. at a time
                        String resourcesKey = resourcesKeys.next();
                        JSONObject resource = resources.getJSONObject(resourcesKey);
                        if (key.equals(LIGHTS)) {
                            BridgeResource br = new BridgeResource(
                                    resourcesKey,
                                    key,
                                    ON,
                                    ON);
                            getLights().put(resourcesKey, br);
                        } else if (key.equals(GROUPS)) {
                            BridgeResource br = new BridgeResource(
                                    resourcesKey,
                                    key,
                                    ANY_ON,
                                    ON);
                            if (resource.getString(TYPE).equals(ROOM))
                                getRooms().put(resourcesKey, br);
                            else if (resource.getString(TYPE).equals(ZONE))
                                getZones().put(resourcesKey, br);
                        } else { // key.equals(SCENES)
                            Iterator<String> sceneKeys = resource.keys();
                            while (sceneKeys.hasNext()) {
                                if (sceneKeys.next().equals(GROUP)) {
                                    BridgeResource br = new BridgeResource(
                                            resourcesKey,
                                            key,
                                            SCENE,
                                            SCENE);
                                    getScenes().put(resourcesKey, br);
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Can't find lights in state!");
                    e.printStackTrace();
                }
            }
        }
    }

    public int addToCategory(Context ctx, HueEdgeProvider.menuCategory category, BridgeResource br){
        Log.d(TAG, "addToCurrentCategory()");
        HueBridge bridge;
        try{
            bridge = Objects.requireNonNull(getInstance(ctx));
        } catch (NullPointerException ex){
            Log.e(TAG, "Tried to add to current category but no instance of HueBridge was found");
            ex.printStackTrace();
            return -1;
        }
        if (bridge.getContents().containsKey(category)) {
            HashMap<Integer, BridgeResource> categoryContents = bridge.getContents().get(category);
            for (int i = 0; i < 10; i++) {
                boolean slotIsEmpty = false;
                try {
                    slotIsEmpty = !Objects.requireNonNull(categoryContents).containsKey(i);
                } catch (NullPointerException ex) {
                    Log.e(TAG, "Failed to get current category contents");
                    ex.printStackTrace();
                }
                if (slotIsEmpty) {
                    categoryContents.put(i, br);
                    Log.d(TAG, "addToCurrentCategory put at: " + i + " values is " + br.toString());
                    return i;
                }
            }
        }
        return -1;
    }

    public void addToCategory(Context ctx, HueEdgeProvider.menuCategory category, BridgeResource br, int index){
        Log.d(TAG, "addToCurrentCategory()");
        HueBridge bridge;
        try{
            bridge = Objects.requireNonNull(getInstance(ctx));
        } catch (NullPointerException ex){
            Log.e(TAG, "Tried to add to current category but no instance of HueBridge was found");
            ex.printStackTrace();
            return;
        }
        if (bridge.getContents().containsKey(category)) {
            HashMap<Integer, BridgeResource> categoryContents = bridge.getContents().get(category);
            boolean slotIsEmpty = false;
            try {
                slotIsEmpty = !Objects.requireNonNull(categoryContents).containsKey(index);
            } catch (NullPointerException ex) {
                Log.e(TAG, "Failed to get current category contents");
                ex.printStackTrace();
            }
            if (slotIsEmpty) {
                categoryContents.put(index, br);
                Log.d(TAG, "addToCurrentCategory put at: " + index + " values is " + br.toString());
            }
        }
    }

    public void setHueBrightness(Context context, BridgeResource br, int value) {
        String category = br.getCategory();
        if (category.equals(SCENES)) {
            Log.e(TAG, "Can't set brightness for scene");
            return;
        }
        String id = br.getId();
        Log.d(TAG, "setHueBrightness entered for: " + category + " " + id);
        String actionUrl = category.equals(LIGHTS) ? br.getStateUrl() : br.getActionUrl();
        String brightnessAction = br.getBrightnessAction();
        setHueState(context, actionUrl, brightnessAction, value);
    }

    public void setHueColor(Context context, BridgeResource br, int value) {
        String category = br.getCategory();
        if (category.equals(SCENES)) {
            Log.e(TAG, "Can't set color for scene");
            return;
        }
        String id = br.getId();
        Log.d(TAG, "setHueColor() for: " + category + " " + id);
        String actionUrl = category.equals(LIGHTS) ? br.getStateUrl() : br.getActionUrl();
        String colorAction = br.getColorAction();
        setHueState(context, actionUrl, colorAction, value);
    }

    public void setHueSaturation(Context context, BridgeResource br, int value) {
        String category = br.getCategory();
        if (category.equals(SCENES)) {
            Log.e(TAG, "Can't set color for scene");
            return;
        }
        String id = br.getId();
        Log.d(TAG, "setHueSaturation() for: " + category + " " + id);
        String actionUrl = category.equals(LIGHTS) ? br.getStateUrl() : br.getActionUrl();
        String saturationAction = br.getSaturationAction();
        setHueState(context, actionUrl, saturationAction, value);
    }

    public void toggleHueState(Context context, BridgeResource br){
        String id = br.getId();
        String category = br.getCategory();
        Log.d(TAG, "toggleHueState() for: " + category + " " + id);
        String actionUrl = category.equals(LIGHTS) ? br.getStateUrl() : br.getActionUrl();
        String actionRead = br.getActionRead();
        String actionWrite = br.getActionWrite();
        boolean lastState;
        if(category.equals(SCENES)){
            setHueState(context, actionUrl, actionWrite, id);
        }
        else {
            try {
                lastState = getState().
                        getJSONObject(category).
                        getJSONObject(id).
                        getJSONObject(STATE).
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
    public void setHueState(Context context, final String resourceUrl, final String key, final Object value) {
        Log.d(TAG, "setHueState()");
        JSONObject j = createJsonOnObject(key, value);
        assert j != null;
        JsonCustomRequest jcr = getJsonCustomRequest(context, j, resourceUrl);
        Log.d(TAG, "changeHueState putRequest created for this url\n" + resourceUrl);
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(context).addToRequestQueue(context, jcr);
    }

    //Construct intent for incoming state JsonObject
    private PendingIntent getStateIntent(Context context) {
        Intent stateIntent = new Intent(context, HueEdgeProvider.class);
        stateIntent.setAction(HueEdgeProvider.ACTION_RECEIVE_HUE_STATE);
        return PendingIntent.getBroadcast(context, 1, stateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Construct intent for incoming reply JsonArray
    private PendingIntent getReplyIntent(Context context) {
        Intent replyIntent = new Intent(context, HueEdgeProvider.class);
        replyIntent.setAction(HueEdgeProvider.ACTION_RECEIVE_HUE_REPLY);
        return PendingIntent.getBroadcast(context, 1, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Create a JsonObject to send to Hue Bridge
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
                        HueBridge bridge;
                        try {
                            bridge = HueBridge.getInstance(ctx);
                        }
                        catch (NullPointerException ex){
                            Log.e(TAG, "HueBridge.getInstance() == null");
                            ex.printStackTrace();
                            return;
                        }
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
        Log.d(TAG, "Request for Hue state sent to queue");
    }

    //PUT method
    private JsonCustomRequest getJsonCustomRequest(final Context ctx, final JSONObject jsonObject, final String resourceUrl){
        if(!jsonObject.keys().hasNext()){ // make sure we get an object that is not empty
            Log.e(TAG, "!jsonObject.keys().hasNext() Is this an empty request?");
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
                                if(!responseKey.equals(SUCCESS)){  //response key should be success
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
                        if (success) {
                            Log.d(TAG, "changeHueState successful");
                            try {
                                assert bridge != null;
                                bridge.getReplyIntent(ctx).send();
                            } catch (PendingIntent.CanceledException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e(TAG, "changeHueState unsuccessful");
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