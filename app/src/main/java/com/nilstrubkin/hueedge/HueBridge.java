package com.nilstrubkin.hueedge;

import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.nilstrubkin.hueedge.resources.BridgeCatalogue;
import com.nilstrubkin.hueedge.resources.BridgeCatalogueAdapter;
import com.nilstrubkin.hueedge.resources.BridgeResource;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static android.content.Context.MODE_PRIVATE;
import static com.nilstrubkin.hueedge.HueEdgeProvider.*;

public class HueBridge implements Serializable {
    private transient static final String TAG = HueBridge.class.getSimpleName();
    private static final long serialVersionUID = -9055637954299678802L;

    private static HueBridge instance;
    private BridgeCatalogue bridgeState;

    private final String url;
    private final String ip;

    private transient JSONObject tempState;
    private transient JSONObject tempState0;

//    private menuCategory currentCategory = menuCategory.QUICK_ACCESS;
//    private HueEdgeProvider.slidersCategory currentSlidersCategory = HueEdgeProvider.slidersCategory.BRIGHTNESS;

    //Mapping of <category to <button id to resource reference>> used to keep all mappings
    private Map<menuCategory, Map<Integer, ResourceReference>> contents = new ConcurrentHashMap<>();

    //Constructor
    private HueBridge(Context ctx, String ip, String userName) {
        this.ip = ip;
        this.url = "http://" + ip + "/api/" + userName;
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor e = s.edit();
        e.putBoolean(ctx.getString(R.string.preference_bridge_configured), true);
        e.putString(ctx.getString(R.string.preference_ip), ip);
        e.putString(ctx.getString(R.string.preference_username), userName);
        e.apply();
        //Mappings of integers (representing R.id reference) to an instance of bridgeResource subclass
        for (menuCategory m : menuCategory.values()){
            getContents().put(m, new ConcurrentHashMap<>());
        }
    }

    //Get instance of instantiated HueBridge
    public static synchronized HueBridge getInstance(Context ctx) {
        if (instance == null) {
            Log.i(TAG, "HueBridge instance or state is null. Attempting to load config...");
            setInstance(ctx, loadAllConfiguration(ctx));
            if (instance == null)
                Log.w(TAG, "HueBridge instance is still null after loading config. Is this the first startup?");
            else requestHueState(ctx);
        }
        return instance;
    }

    //Constructor for instance, first time setup
    public static synchronized HueBridge getInstance(Context ctx, String ipAddress, String userName) {
        instance = new HueBridge(ctx, ipAddress, userName);
        requestHueState(ctx);
        return instance;
    }

    //Setting the instance for config loading
    public static synchronized void setInstance(Context ctx, HueBridge bridge) {
        instance = bridge;
        //HueEdgeProvider update
        setBridge(instance);
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor e = s.edit();
        e.putBoolean(ctx.getString(R.string.preference_bridge_configured), bridge != null);
        e.apply();
    }

    //Delete the instance
    public static synchronized void deleteInstance(Context ctx) {
        Log.i(TAG, "Deleting instance of HueBridge");
        instance = null;
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor e = s.edit();
        e.remove(ctx.getString(R.string.preference_bridge_configured));
        e.remove(ctx.getString(R.string.preference_ip));
        e.remove(ctx.getString(R.string.preference_username));
        e.apply();
        boolean deleted = deleteAllConfiguration(ctx);
        if (deleted) {
            String toastString = ctx.getString(R.string.toast_configuration_deleted);
            Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show();
        }
    }

    public String getIp() {
        return ip;
    }

    public String getUrl() {
        return url;
    }

    public BridgeResource getResource(ResourceReference ref){
        String id = ref.getId();
        switch (ref.getCategory()){
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

    public synchronized Map<menuCategory, Map<Integer, ResourceReference>> getContents() {
        return contents;
    }

    public synchronized void setContents(Map<menuCategory, Map<Integer, ResourceReference>> contents) {
        this.contents = contents;
    }

    @NonNull
    public menuCategory getCurrentCategory(Context ctx) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        int i = settings.getInt(ctx.getResources().getString(R.string.preference_current_category), 0);
        return menuCategory.values()[i];
    }

    public void setCurrentCategory(Context ctx, menuCategory currentCategory) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(ctx.getResources().getString(R.string.preference_current_category), currentCategory.ordinal());
        editor.apply();
    }

    public slidersCategory getCurrentSlidersCategory(Context ctx) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        int i = settings.getInt(ctx.getResources().getString(R.string.preference_current_category_sliders), 0);
        return slidersCategory.values()[i];
    }

    public void setCurrentSlidersCategory(Context ctx, slidersCategory currentSlidersCategory) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(ctx.getResources().getString(R.string.preference_current_category_sliders), currentSlidersCategory.ordinal());
        editor.apply();
    }

    public synchronized BridgeCatalogue getBridgeState(){
        return bridgeState;
    }

    public synchronized void setBridgeState(Context ctx, BridgeCatalogue bridgeState){
        this.bridgeState = bridgeState;
        saveAllConfiguration(ctx);
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

    public void addToCurrentCategory(Context ctx, ResourceReference resRef, int index){
        menuCategory category = getCurrentCategory(ctx);
        Log.d(TAG, "addToCurrentCategory()");
        if (getContents().containsKey(category)) {
            Map<Integer, ResourceReference> categoryContents = getContents().get(category);
            boolean slotIsEmpty = false;
            try {
                slotIsEmpty = !Objects.requireNonNull(categoryContents).containsKey(index);
            } catch (NullPointerException e) {
                Log.e(TAG, "Failed to get current category contents");
                e.printStackTrace();
            }
            if (slotIsEmpty) {
                categoryContents.put(index, resRef);
                Log.d(TAG, "addToCurrentCategory put at: " + index + " values is " + resRef.toString());
            }
        }
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
        if (!state0) {
            tempState = null;
            tempState0 = null;
        }

        Request request = new Request.Builder()
                .url(state0 ? url + "/groups/0" : url)
                .build();
        HueEdgeProvider.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (ResponseBody rb = Objects.requireNonNull(response.body())) {
                    String s = rb.string();
                    notifyState(ctx, new JSONObject(s), state0);
                } catch (JSONException | NullPointerException | IOException e) {
                    e.printStackTrace();
                    try {
                        if (!state0) getTimeoutIntent(ctx).send();
                    } catch (PendingIntent.CanceledException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                try {
                    if (!state0) getTimeoutIntent(ctx).send();
                } catch (PendingIntent.CanceledException ex) {
                    ex.printStackTrace();
                }
            }
        });
        if(!state0) requestHueState(ctx, true);
    }

    /**
     * Perform the quick setup for the buttons
     * @param ctx Context
     */
    public void quickSetup(Context ctx) {
        Log.d(TAG, "quickSetup entered");

        Map<String, ? extends BridgeResource> map;
        Map<Integer, ResourceReference> quickAccessContents;
        Map<Integer, ResourceReference> lightsContents;
        Map<Integer, ResourceReference> roomsContents;
        Map<Integer, ResourceReference> zonesContents;
        Map<Integer, ResourceReference> scenesContents;
        Map<menuCategory, Map<Integer, ResourceReference>> contents = getContents();
        try {
            quickAccessContents = Objects.requireNonNull(contents.get(menuCategory.QUICK_ACCESS));
            lightsContents = Objects.requireNonNull(contents.get(menuCategory.LIGHTS));
            roomsContents = Objects.requireNonNull(contents.get(menuCategory.ROOMS));
            zonesContents = Objects.requireNonNull(contents.get(menuCategory.ZONES));
            scenesContents = Objects.requireNonNull(contents.get(menuCategory.SCENES));
        } catch (NullPointerException e){
            Log.e(TAG, "Tried to perform quick setup but no instance of HueBridge was found");
            e.printStackTrace();
            return;
        }

        // new objects to avoid coupling bugs that change icons on multiple items that are essentially same item in java
        ResourceReference allResRefQ = BridgeCatalogue.getGroup0Ref();
        ResourceReference allResRefR = BridgeCatalogue.getGroup0Ref();
        ResourceReference allResRefZ = BridgeCatalogue.getGroup0Ref();

        int buttonIndex = 0;
        int qaButtonIndex = 0;

        quickAccessContents.put(qaButtonIndex++, allResRefQ);
        map = getBridgeState().getLights();
        Log.d(TAG, "quickSetup getLights() size: " + map.size());
        for (Map.Entry<String, ? extends BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for lights on id: " + entry.getKey());
            BridgeResource res = entry.getValue();
            ResourceReference resRef = new ResourceReference(res.getCategory(), res.getId());
            lightsContents.put(buttonIndex++, resRef);
            if(qaButtonIndex < 3) {
                ResourceReference resRefQ = new ResourceReference(res.getCategory(), res.getId());
                quickAccessContents.put(qaButtonIndex++, resRefQ);
            }
        }

        buttonIndex = 0;
        roomsContents.put(buttonIndex++, allResRefR);
        map = getBridgeState().getRooms();
        Log.d(TAG, "quickSetup getRooms() size: " + map.size());
        for (Map.Entry<String, ? extends BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for rooms on id: " + entry.getKey());
            if (!entry.getKey().equals("0")) {
                BridgeResource res = entry.getValue();
                ResourceReference resRef = new ResourceReference(res.getCategory(), res.getId());
                roomsContents.put(buttonIndex++, resRef);
                if (qaButtonIndex < 5) {
                    ResourceReference resRefQ = new ResourceReference(res.getCategory(), res.getId());
                    quickAccessContents.put(qaButtonIndex++, resRefQ);
                }
            }
        }

        buttonIndex = 0;
        zonesContents.put(buttonIndex++, allResRefZ);
        map = getBridgeState().getZones();
        Log.d(TAG, "quickSetup getZones() size: " + map.size());
        for (Map.Entry<String, ? extends BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for zones on id: " + entry.getKey());
            if (!entry.getKey().equals("0")) {
                BridgeResource res = entry.getValue();
                ResourceReference resRef = new ResourceReference(res.getCategory(), res.getId());
                zonesContents.put(buttonIndex++, resRef);
                if (qaButtonIndex < 7) {
                    ResourceReference resRefQ = new ResourceReference(res.getCategory(), res.getId());
                    quickAccessContents.put(qaButtonIndex++, resRefQ);
                }
            }
        }

        buttonIndex = 0;
        map = getBridgeState().getScenes();
        Log.d(TAG, "quickSetup getScenes() size: " + map.size());
        for (Map.Entry<String, ? extends BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for scenes on id: " + entry.getKey());
            BridgeResource res = entry.getValue();
            ResourceReference resRef = new ResourceReference(res.getCategory(), res.getId());
            scenesContents.put(buttonIndex++, resRef);
            if(qaButtonIndex < 9) {
                ResourceReference resRefQ = new ResourceReference(res.getCategory(), res.getId());
                quickAccessContents.put(qaButtonIndex++, resRefQ);
            }
        }
        saveAllConfiguration(ctx);
    }

    /**
     * Save and write the HueBridge instance to the memory
     * @param ctx Context
     */
    public static synchronized void saveAllConfiguration(Context ctx) {
        Log.d(TAG, "saveConfigurationToMemory()");

        HueBridge instanceToSave = getInstance(ctx);
        if (instanceToSave == null) {
            Log.e(TAG, "saveAllConfiguration() bridge is null");
            return;
        }
        File preferenceFile = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.preference_file_key));
        File recoveryFile = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.recovery_file_key));
        try {
            ObjectOutputStream preferenceOutputStream = new ObjectOutputStream(new FileOutputStream(preferenceFile));
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<HueBridge> jsonAdapter = moshi.adapter(HueBridge.class);
            String bridgeString = jsonAdapter.toJson(instanceToSave);
            preferenceOutputStream.writeObject(bridgeString);
            preferenceOutputStream.flush();
            preferenceOutputStream.close();

            // Recovery
            ObjectOutputStream recoveryOutputStream = new ObjectOutputStream(new FileOutputStream(recoveryFile));
            recoveryOutputStream.writeObject(instanceToSave.getContents());
            recoveryOutputStream.flush();
            recoveryOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save configuration");
            e.printStackTrace();
        }
    }

    /**
     * Read and set the HueBridge instance from the memory
     * @param ctx Context
     */
    public static synchronized HueBridge loadAllConfiguration(Context ctx) {
        Log.d(TAG, "loadConfigurationFromMemory()");
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean bridgeConfigured = s.getBoolean(ctx.getString(R.string.preference_bridge_configured), false);
        if (!bridgeConfigured)
            return null;

        File configFile = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.preference_file_key));
        ObjectInputStream configInputStream;

        // Load config file
        try {
            configInputStream = new ObjectInputStream(new FileInputStream(configFile));
        } catch (FileNotFoundException e){
            Log.e(TAG, "Config file not found");
            return null;
        } catch (IOException e) {
            Log.e(TAG, "IOException");
            e.printStackTrace();
            return null;
        }

        // Load instance of HueBridge
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<HueBridge> jsonAdapter = moshi.adapter(HueBridge.class);
        try {
            String bridgeString = Objects.requireNonNull(configInputStream).readObject().toString();
            return jsonAdapter.fromJson(bridgeString);
        } catch (NullPointerException e){
            Log.e(TAG, "Config file not found");
        }
        // Catch old version
        catch (InvalidClassException e){
            String toastString = ctx.getString(R.string.toast_old_version);
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            Log.e(TAG, toastString);
            File recoveryFile = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.recovery_file_key));

            // Open and apply recovery for HueBridge instance
            try {
                ObjectInputStream recoveryInputStream = new ObjectInputStream(new FileInputStream(recoveryFile));
                String ip = Objects.requireNonNull(s.getString(ctx.getString(R.string.preference_ip), ""));
                String userName = Objects.requireNonNull(s.getString(ctx.getString(R.string.preference_username), ""));
                if(ip.equals("") || userName.equals("")) {
                    Log.e(TAG, "Tried to load preferences, ip or username are not found, can not recover");
                    return null;
                }
                // Voodoo-area, proceed with caution
                @SuppressWarnings("unchecked")
                // Unsafe cast
                Map<menuCategory, Map<Integer, ResourceReference>> contents =
                        Objects.requireNonNull(
                                (HashMap<menuCategory, Map<Integer, ResourceReference>>)
                                        recoveryInputStream.readObject());
                // Empty body of for loop to fail-fast in case of any issue with members of the maps
                //noinspection StatementWithEmptyBody
                for (menuCategory m_ignored : contents.keySet());
                for (Map<Integer, ResourceReference> c : contents.values()) {
                    //noinspection StatementWithEmptyBody
                    for (Integer i_ignored : c.keySet());
                    //noinspection StatementWithEmptyBody
                    for (ResourceReference r_ignored : c.values());
                }
                // Voodoo-area ends here
                HueBridge newBridge = getInstance(ctx, ip, userName);
                newBridge.setContents(contents);
                Log.i(TAG,"Recovery successful");
                return newBridge;
            } catch (FileNotFoundException ex){
                Log.e(TAG, "Recovery file not found");
                deleteAllConfiguration(ctx);
            } catch (ClassCastException | ClassNotFoundException | IOException | NullPointerException ex){
                ex.printStackTrace();
                deleteAllConfiguration(ctx);
            }
            //toastString = "Recovery successful";
            //Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show();
        } catch (ClassNotFoundException | IOException e){
            e.printStackTrace();
        }
        //String toastString = "Loading successful";
        //Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show();
        return null;
    }

    public static boolean deleteAllConfiguration(Context ctx){
        File file;
        try {
            file = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.preference_file_key));
        }
        catch (NullPointerException e) {
            Log.e(TAG, "deleteAllConfig could not find configuration");
            return false;
        }
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor e = s.edit();
        e.remove(ctx.getString(R.string.preference_bridge_configured));
        e.remove(ctx.getString(R.string.preference_ip));
        e.remove(ctx.getString(R.string.preference_username));
        e.apply();
        return file.delete();
    }
}