package com.nilstrubkin.hueedge;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.nilstrubkin.hueedge.HueEdgeProvider.menuCategory;
import com.nilstrubkin.hueedge.resources.BridgeCatalogue;
import com.nilstrubkin.hueedge.resources.BridgeCatalogueAdapter;
import com.nilstrubkin.hueedge.resources.BridgeResource;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HueBridge implements Serializable {
    private transient static final String TAG = HueBridge.class.getSimpleName();

    private static HueBridge instance;
    private BridgeCatalogue bridgeState;

    private final String url;
    private final String ip;
    private final String userName;

    private transient JSONObject tempState;
    private transient JSONObject tempState0;

    private menuCategory currentCategory = menuCategory.QUICK_ACCESS;
    private HueEdgeProvider.slidersCategory currentSlidersCategory = HueEdgeProvider.slidersCategory.BRIGHTNESS;

    //Mapping of <category to <button id to resource reference>> used to keep all mappings
    private final Map<menuCategory, Map<Integer, ResourceReference>> contents = new HashMap<>();

    //Default constructor with http header
    private HueBridge(Context ctx, String ip, String userName) {
        this(ctx, ip, userName, ctx.getString(R.string.http_header)); // String "http://"
    }

    //Custom constructor for future use
    private HueBridge(Context ctx, String ip, String userName, String urlHeader) {
        this.ip = ip;
        this.userName = Objects.requireNonNull(userName);
        this.url =
                Objects.requireNonNull(urlHeader) +
                        Objects.requireNonNull(ip) +
                        ctx.getString(R.string.api_path) + // String "/api/"
                        Objects.requireNonNull(userName);

        //Mappings of integers (representing R.id reference) to an instance of bridgeResource subclass
        for (menuCategory m : menuCategory.values()){
            getContents().put(m, new HashMap<Integer, ResourceReference>());
        }
    }

    //Delete the instance
    public static synchronized void deleteInstance(Context ctx) {
        Log.i(TAG, "Deleting instance of HueBridge");
        instance = null;
        boolean deleted = HueEdgeProvider.deleteAllConfiguration(ctx);
        if (deleted) {
            String toastString = ctx.getString(R.string.toast_configuration_deleted);
            Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show();
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
            else requestHueState(ctx);
        }
        return instance;
    }

    //Constructor for instance, first time setup
    public static synchronized void getInstance(Context ctx, String ipAddress, String userName) {
        instance = new HueBridge(ctx, ipAddress, userName);
        requestHueState(ctx);
    }

    //Setting the instance for config loading
    public static synchronized void setInstance(HueBridge bridge) {
        instance = bridge;
    }

    public String getIp() {
        return ip;
    }

    public String getUrl() {
        return url;
    }

    public String getUserName() {
        return userName;
    }

    public BridgeResource getResource(ResourceReference ref){
        String id = ref.id;
        switch (ref.category){
            case "lights":
                return getBridgeState().getLights().get(id);
            case "groups":
                return getBridgeState().getGroups().get(id);
            case "scenes":
                return getBridgeState().getScenes().get(id);
            default:
                Log.e(TAG, "Unknown category");
                return null;
        }
    }

    private void notifyState(Context ctx, JSONObject state, boolean state0){
        if(state0)
            tempState0 = state;
        else
            tempState = state;
        mergeState(ctx);
    }

    private void mergeState(Context ctx){
        if(tempState != null && tempState0 != null)
            try {
                JSONObject currentGroups = tempState.getJSONObject("groups");
                currentGroups.put("0", tempState0);
                JSONObject completeState = tempState.put("groups", currentGroups);
                refreshAllHashMaps(ctx, completeState);
            } catch (JSONException e) {
                Log.e(TAG, "Could not merge states. No groups in stateJson.");
                e.printStackTrace();
            }
    }

    public Map<menuCategory, Map<Integer, ResourceReference>> getContents() {
        return contents;
    }

    public menuCategory getCurrentCategory() {
        return currentCategory;
    }

    public void setCurrentCategory(menuCategory currentCategory) {
        this.currentCategory = currentCategory;
    }

    public HueEdgeProvider.slidersCategory getCurrentSlidersCategory() {
        return currentSlidersCategory;
    }

    public void setCurrentSlidersCategory(HueEdgeProvider.slidersCategory currentSlidersCategory) {
        this.currentSlidersCategory = currentSlidersCategory;
    }

    public BridgeCatalogue getBridgeState(){
        return bridgeState;
    }

    public void setBridgeState(Context ctx, BridgeCatalogue bridgeState){
        this.bridgeState = bridgeState;
        try {
            getStateIntent(ctx).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    private void refreshAllHashMaps(Context ctx, JSONObject catalogue) {
        Moshi moshi = new Moshi.Builder().add(new BridgeCatalogueAdapter()).build();
        JsonAdapter<BridgeCatalogue> jsonAdapter = moshi.adapter(BridgeCatalogue.class);
        try {
            BridgeCatalogue bridgeState = jsonAdapter.fromJson(catalogue.toString());
            setBridgeState(ctx, bridgeState);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addToCategory(Context ctx, menuCategory category, BridgeResource br, int index){
        Log.d(TAG, "addToCurrentCategory()");
        HueBridge bridge;
        try{
            bridge = Objects.requireNonNull(getInstance(ctx));
        } catch (NullPointerException e){
            Log.e(TAG, "Tried to add to current category but no instance of HueBridge was found");
            e.printStackTrace();
            return;
        }
        if (bridge.getContents().containsKey(category)) {
            Map<Integer, ResourceReference> categoryContents = bridge.getContents().get(category);
            boolean slotIsEmpty = false;
            try {
                slotIsEmpty = !Objects.requireNonNull(categoryContents).containsKey(index);
            } catch (NullPointerException e) {
                Log.e(TAG, "Failed to get current category contents");
                e.printStackTrace();
            }
            if (slotIsEmpty) {
                String resCat = br.getCategory();
                String resId = br.getId();
                ResourceReference resRef = new ResourceReference(resCat, resId);
                categoryContents.put(index, resRef);
                Log.d(TAG, "addToCurrentCategory put at: " + index + " values is " + br.toString());
            }
        }
    }

    //Construct intent for incoming state JsonObject
    private PendingIntent getStateIntent(Context context) {
        Intent stateIntent = new Intent(context, HueEdgeProvider.class);
        stateIntent.setAction(HueEdgeProvider.ACTION_RECEIVE_HUE_STATE);
        return PendingIntent.getBroadcast(context, 1, stateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Construct intent for incoming reply JsonArray
    public PendingIntent getReplyIntent(Context context) {
        Intent replyIntent = new Intent(context, HueEdgeProvider.class);
        replyIntent.setAction(HueEdgeProvider.ACTION_RECEIVE_HUE_REPLY);
        return PendingIntent.getBroadcast(context, 1, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void requestHueState(Context ctx){
        try {
            Objects.requireNonNull(HueBridge.getInstance(ctx)).requestHueState(ctx, false);
        } catch (NullPointerException e){
            e.printStackTrace();
            Log.e(TAG, "Tried to requestHueState() but there is no instance of HueBridge present");
        }
    }

    //GET method
    public void requestHueState(final Context ctx, final boolean state0) {
        if(!state0) {
            tempState = null;
            tempState0 = null;
        }

        ExecutorService pool = Executors.newFixedThreadPool(1);
        Callable<String> callable = new Callable<String>() {
            @Override
            public String call() {
                Request request = new Request.Builder()
                        .url(state0 ? url + "/groups/0" : url)
                        .build();
                final OkHttpClient client = new OkHttpClient();
                try (Response response = client.newCall(request).execute()) {
                    return Objects.requireNonNull(response.body()).string();
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        Future<String> future = pool.submit(callable);
        try {
            notifyState(ctx, new JSONObject(future.get()), state0);
        } catch (ExecutionException | InterruptedException | JSONException e) {
            e.printStackTrace();
        }
        if(!state0)
            requestHueState(ctx, true);
    }
}