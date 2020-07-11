package com.ize.edgehue;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EdgeHueProvider extends SlookCocktailProvider {

    private static final String TAG = EdgeHueProvider.class.getSimpleName();

    private static final String ACTION_REMOTE_LONG_CLICK = "com.ize.edgehue.ACTION_REMOTE_LONG_CLICK";
    private static final String ACTION_REMOTE_CLICK = "com.ize.edgehue.ACTION_REMOTE_CLICK";
    private static final String ACTION_PULL_TO_REFRESH = "com.ize.edgehue.ACTION_PULL_TO_REFRESH";
    protected static final String ACTION_RECEIVE_HUE_STATE = "com.ize.edgehue.ACTION_RECEIVE_HUE_STATE";
    protected static final String ACTION_RECEIVE_HUE_REPLY = "com.ize.edgehue.ACTION_RECEIVE_HUE_REPLY";

    //Array of references to buttons
    public static final int[] btnArr = {R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn10};
    //Array of references to category buttons
    public static final int[] btnCategoryArr = {R.id.btnCategory1, R.id.btnCategory2,
            R.id.btnCategory3, R.id.btnCategory4, R.id.btnCategory5};
    //Array of references to category buttons underlines
    public static final int[] btnCategoryLineArr = {R.id.btnCategoryLine1, R.id.btnCategoryLine2,
            R.id.btnCategoryLine3, R.id.btnCategoryLine4, R.id.btnCategoryLine5};
    //Array of references to button texts (text under the button itself)
    public static final int[] btnTextArr = {R.id.btn1text, R.id.btn2text, R.id.btn3text, R.id.btn4text, R.id.btn5text,
            R.id.btn6text, R.id.btn7text, R.id.btn8text, R.id.btn9text, R.id.btn10text};

    //Categories available in the left pane (helpContent)
    public enum menuCategory {
        NO_BRIDGE,
        QUICK_ACCESS,
        LIGHTS,
        ROOMS,
        ZONES,
        SCENES
    }

    //One remoteView for left/help and one for right/content
    private static RemoteViews contentView = null;
    private static RemoteViews helpView = null;

    //Mappings of integers (representing R.id reference) to an instance of bridgeResource subclass
    private static final HashMap<Integer, BridgeResource> quickAccessContent = new HashMap<>();
    private static final HashMap<Integer, BridgeResource> lightsContent = new HashMap<>();
    private static final HashMap<Integer, BridgeResource> roomsContent = new HashMap<>();
    private static final HashMap<Integer, BridgeResource> zonesContent = new HashMap<>();
    private static final HashMap<Integer, BridgeResource> scenesContent = new HashMap<>();

    //Mapping of category to contents
    private static final HashMap<menuCategory, HashMap<Integer, BridgeResource>> contents =
            new HashMap<>();

    //Selected category initiated to none
    private static menuCategory currentCategory = menuCategory.NO_BRIDGE;

    //The HueBridge instance
    private static HueBridge bridge;


    //This method is called for every broadcast and before each of the other callback methods.
    //Samsung SDK
    @Override
    public void onReceive(Context ctx, Intent intent) {
        Log.d(TAG, "onRecieve()");
        super.onReceive(ctx, intent);
        if (HueBridge.getInstance(ctx) == null) {
            currentCategory = menuCategory.NO_BRIDGE;
        }
        if(contentView == null) {
            contentView = createContentView(ctx);
        }
        if(helpView == null) {
            helpView = createHelpView(ctx);
        }
        if (HueBridge.getInstance(ctx) == null) {
            return;
        }
        String action = intent.getAction();
        if(action == null) {
            Log.wtf(TAG, "action == null");
        }
        assert action != null;
        Log.i(TAG, "onReceive: " + action);
        switch (action) {
            case ACTION_REMOTE_LONG_CLICK:
                performRemoteLongClick(ctx, intent);
                break;
            case ACTION_REMOTE_CLICK:
                performRemoteClick(ctx, intent);
                break;
            case ACTION_PULL_TO_REFRESH:
                SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
                int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, EdgeHueProvider.class));
                cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.refreshArea);
            case ACTION_RECEIVE_HUE_REPLY:
                bridge.requestHueState(ctx);
                break;
            case ACTION_RECEIVE_HUE_STATE:
                //just panel update
                break;
            default:
                break;
        }
        panelUpdate(ctx);
    }

    //This method is called when the Edge Single Plus Mode is created for the first time.
    //Samsung SDK
    @Override
    public void onEnabled(Context ctx) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onEnabled()");
        super.onEnabled(ctx);

    }

    //This method is called when the instance of
    //Edge Single Plus Mode is deleted from the enabled list
    //Samsung SDK
    @Override
    public void onDisabled(Context ctx) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onDisabled()");
        super.onDisabled(ctx);
    }

    //TODO
    @Override
    public void onUpdate(Context ctx, SlookCocktailManager cocktailManager, int[] cocktailIds) {

        Log.d(TAG, "onUpdate()");
        panelUpdate(ctx);
    }

    //TODO
    @Override
    public void onVisibilityChanged(Context ctx, int cocktailId, int visibility) {
        Log.d(TAG, "onVisibilityChanged()");
        super.onVisibilityChanged(ctx, cocktailId, visibility);
    }

    public static menuCategory getCurrentCategory() {
        return currentCategory;
    }

    public static void setCurrentCategory(menuCategory currentCategory) {
        EdgeHueProvider.currentCategory = currentCategory;
    }

    public static void addToCurrentCategory(BridgeResource br){
        Log.d(TAG, "addToCurrentCategory()");
        HashMap<Integer, BridgeResource> cc =
                contents.get(getCurrentCategory());
        for (int i : btnArr){
            if(!Objects.requireNonNull(cc).containsKey(i)){
                cc.put(i, br);
                Objects.requireNonNull(contents.get(getCurrentCategory())).put(i, br);
                Log.d(TAG, "addToCurrentCategory put at: " + i + " values is " + br.toString());
                return;
            }
        }

    }

    public static HashMap<menuCategory, HashMap<Integer, BridgeResource>> getContents() {
        return contents;
    }

    //Create the content view, right panel. Used for buttons
    private RemoteViews createContentView(Context ctx) {
        RemoteViews contentView = null;
        switch (currentCategory){
            case NO_BRIDGE:
                contentView = new RemoteViews(ctx.getPackageName(),
                        R.layout.content_view_no_bridge);
                break;
            case QUICK_ACCESS:
            case LIGHTS:
            case ROOMS:
            case ZONES:
            case SCENES:
                contentView = new RemoteViews(ctx.getPackageName(),
                        R.layout.view_main);

                int i = 0;
                for ( int button : btnArr ){
                    contentView.setOnClickPendingIntent(button, getClickIntent(
                            ctx, i++, 0));
                    SlookCocktailManager.getInstance(ctx).
                            setOnLongClickPendingIntent(contentView, button,
                                    getLongClickIntent(ctx, button, 0));
                }
                contentView.setOnClickPendingIntent(R.id.btnEdit,
                        getClickIntent(ctx, R.id.btnEdit, 1));
                break;
            default:
                break;
        }

        //Hide empty columns but at least show mainColumn if both are empty
        if(contents.get(currentCategory) != null) {
            boolean mainColumnEmpty = true;
            boolean extraColumnEmpty = true;
            contentView.setViewVisibility(R.id.mainColumn, View.GONE);
            contentView.setViewVisibility(R.id.extraColumn, View.GONE);
            for (int i = 0; i < 5; i++) {
                if (Objects.requireNonNull(contents.get(currentCategory)).containsKey(i)) {
                    mainColumnEmpty = false;
                    break;
                }
            }
            for (int i = 5; i < 10; i++) {
                if (Objects.requireNonNull(contents.get(currentCategory)).containsKey(i)) {
                    extraColumnEmpty = false;
                    break;
                }
            }
            if (mainColumnEmpty && extraColumnEmpty)
                mainColumnEmpty = false;

            contentView.setViewVisibility(R.id.mainColumn, mainColumnEmpty ? View.GONE : View.VISIBLE);
            contentView.setViewVisibility(R.id.extraColumn, extraColumnEmpty ? View.GONE : View.VISIBLE);
        }
        return contentView;
    }

    //Create the help view, left panel. Used for categories.
    private RemoteViews createHelpView(Context ctx) {
        RemoteViews helpView = new RemoteViews(ctx.getPackageName(),
                R.layout.view_help);
        for( int button : btnCategoryArr){
            helpView.setOnClickPendingIntent(button, getClickIntent(ctx, button, 1));
            helpView.setTextColor(button, Color.parseColor("#99FAFAFA"));
        }
        for (int line : btnCategoryLineArr){
            helpView.setInt(line, "setBackgroundResource", 0);
        }
        if(currentCategory != menuCategory.NO_BRIDGE){
            int currentButton = btnCategoryArr[currentCategory.ordinal() - 1];
            int currentLine = btnCategoryLineArr[currentCategory.ordinal() - 1];
            helpView.setTextColor(currentButton, Color.parseColor("#2187F3"));
            helpView.setInt(currentLine, "setBackgroundResource", R.drawable.dotted);
        }
        return helpView;
    }

    //Get the long click intent object to assign to a button
    private static PendingIntent getLongClickIntent(Context ctx, int id, int key) {
        Intent longClickIntent = new Intent(ctx, EdgeHueProvider.class);
        longClickIntent.setAction(ACTION_REMOTE_LONG_CLICK);
        longClickIntent.putExtra("id", id);
        longClickIntent.putExtra("key", key);
        return PendingIntent.getBroadcast(ctx, id, longClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Get the click intent object to assign to a button
    private static PendingIntent getClickIntent(Context ctx, int id, int key) {
        Intent clickIntent = new Intent(ctx, EdgeHueProvider.class);
        clickIntent.setAction(ACTION_REMOTE_CLICK);
        clickIntent.putExtra("id", id);
        clickIntent.putExtra("key", key);
        return PendingIntent.getBroadcast(ctx, id, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Enter the edit activity to customize buttons
    private void startEditActivity(Context ctx){
        Intent editIntent = new Intent(Intent.ACTION_EDIT);
        editIntent.addCategory( Intent.CATEGORY_DEFAULT);
        editIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(editIntent);
    }

    /*private void startSetupActivity(Context ctx){
        Intent setupIntent = new Intent(Intent.ACTION_MAIN);
        setupIntent.addCategory( Intent.CATEGORY_LAUNCHER);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(setupIntent); //TODO
    }*/

    //Button handler
    private void performRemoteClick(Context ctx, Intent intent) {
        int id = intent.getIntExtra("id", -1);
        int key = intent.getIntExtra("key", -1);
        if(currentCategory == menuCategory.NO_BRIDGE){
            //startSetupActivity(ctx);
            return;
        }
        if(key == 0){
            if(Objects.requireNonNull(contents.get(currentCategory)).containsKey(id)){
                BridgeResource br = Objects.requireNonNull(contents.get(currentCategory)).get(id);
                assert br != null;
                Objects.requireNonNull(HueBridge.getInstance(ctx)).toggleHueState(ctx, br);
            }
            else
                startEditActivity(ctx);
        }
        else if(key == 1) {
            switch (id) {
                case R.id.btnCategory1:
                    currentCategory = menuCategory.QUICK_ACCESS;
                    break;
                case R.id.btnCategory2:
                    currentCategory = menuCategory.LIGHTS;
                    break;
                case R.id.btnCategory3:
                    currentCategory = menuCategory.ROOMS;
                    break;
                case R.id.btnCategory4:
                    currentCategory = menuCategory.ZONES;
                    break;
                case R.id.btnCategory5:
                    currentCategory = menuCategory.SCENES;
                    break;
                case R.id.btnEdit:
                    startEditActivity(ctx);
                    break;
                default:
                    break;
            }
        }
        panelUpdate(ctx);
    }

    private void performRemoteLongClick(Context ctx, Intent intent) {
        Log.d(TAG, "ACTION_REMOTE_LONG_CLICK" + "id=" + intent.getIntExtra("id", -1));
        startEditActivity(ctx);
    }

    public static void clearAllContents(){
        Log.d(TAG, "clearAllContents entered");
        quickAccessContent.clear();
        lightsContent.clear();
        roomsContent.clear();
        zonesContent.clear();
        scenesContent.clear();
        contents.clear();

        contents.put(menuCategory.QUICK_ACCESS, quickAccessContent);
        contents.put(menuCategory.LIGHTS, lightsContent);
        contents.put(menuCategory.ROOMS, roomsContent);
        contents.put(menuCategory.ZONES, zonesContent);
        contents.put(menuCategory.SCENES, scenesContent);
        Log.d(TAG, "clearAllContents done");
    }

    //The initial setup of the buttons
    public static void quickSetup(Context ctx) {
        Log.d(TAG, "quickSetup entered");

        clearAllContents();

        currentCategory = menuCategory.QUICK_ACCESS;

        bridge = HueBridge.getInstance(ctx);

        if (HueBridge.getInstance(ctx) == null){
            Log.e(TAG, "HueBridge.getInstance() == null. Probably missing config");
        }
        assert HueBridge.getInstance(ctx) != null;
        int buttonIndex = 0;
        int qaButtonIndex = 0;
        HashMap<String, BridgeResource> map;

        map = bridge.getLights();
        Log.d(TAG, "quickSetup getLights() size: " + map.size());
        for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for lights on id: " + entry.getKey());
            lightsContent.put(buttonIndex++, entry.getValue());
            if(qaButtonIndex < 2) {
                quickAccessContent.put(qaButtonIndex++, entry.getValue());
            }
        }

        buttonIndex = 0;

        map = bridge.getRooms();
        Log.d(TAG, "quickSetup getRooms() size: " + map.size());
        for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for rooms on id: " + entry.getKey());
            roomsContent.put(buttonIndex++, entry.getValue());
            if(qaButtonIndex < 4) {
                quickAccessContent.put(qaButtonIndex++, entry.getValue());
            }
        }

        buttonIndex = 0;

        map = bridge.getZones();
        Log.d(TAG, "quickSetup getZones() size: " + map.size());
        for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for zones on id: " + entry.getKey());
            zonesContent.put(buttonIndex++, entry.getValue());
            if(qaButtonIndex < 6) {
                quickAccessContent.put(qaButtonIndex++, entry.getValue());
            }
        }

        buttonIndex = 0;

        map = bridge.getScenes();
        Log.d(TAG, "quickSetup getScenes() size: " + map.size());
        for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for scenes on id: " + entry.getKey());
            scenesContent.put(buttonIndex++, entry.getValue());
            if(qaButtonIndex < 8) {
                quickAccessContent.put(qaButtonIndex++, entry.getValue());
            }
        }

        saveConfigurationToMemory(ctx);
    }

    //Refresh both panels
    private void panelUpdate(Context ctx){
        contentView = createContentView(ctx);
        helpView = createHelpView(ctx);
        Log.i(TAG, "Doing panelUpdate currentCategory is " + currentCategory + ". Filling in buttons now");
        if(currentCategory != menuCategory.NO_BRIDGE) {
            for (int i = 0; i < 10; i++) {
                if (Objects.requireNonNull(contents.get(currentCategory)).containsKey(i)) {
                    BridgeResource resource = Objects.requireNonNull(contents.get(currentCategory)).get(i);
                    if(resource == null) {
                        Log.wtf(TAG, "resource == null");
                    }
                    assert resource != null;
                    contentView.setTextViewText(btnTextArr[i], resource.getName());
                    contentView.setTextViewText(btnArr[i], resource.getBtnText());
                    contentView.setTextColor(btnArr[i], resource.getBtnTextColor());
                    contentView.setFloat(btnArr[i], "setTextSize", 14);
                    contentView.setInt(btnArr[i], "setBackgroundResource",
                            resource.getBtnBackgroundResource());
                    if(resource.getCategory().equals("scenes")){
                        contentView.setFloat(btnArr[i], "setTextSize", 10);
                    }
                } else {
                    contentView.setTextViewText(btnTextArr[i], "");
                    contentView.setTextViewText(btnArr[i], "+");
                    contentView.setTextColor(btnArr[i], (ContextCompat.getColor(ctx, R.color.white)));
                    contentView.setInt(btnArr[i], "setBackgroundResource",
                            R.drawable.add_button_background);
                }
            }
        }

        final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
        final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, EdgeHueProvider.class));

        //Set pull refresh
        Intent refreshIntent = new Intent(ctx, EdgeHueProvider.class);
        refreshIntent.setAction(ACTION_PULL_TO_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0xff, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        SlookCocktailManager.getInstance(ctx).setOnPullPendingIntent(cocktailIds[0], R.id.refreshArea, pendingIntent);

        cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
    }

    public static void saveConfigurationToMemory(Context ctx) {
        try {
            Log.d(TAG, "saveConfigurationToMemory()");
            SharedPreferences sharedPref = ctx.getSharedPreferences(ctx.getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            Gson gson = new Gson();
            String json = gson.toJson(Objects.requireNonNull(HueBridge.getInstance(ctx)));
            String json2 = gson.toJson(Objects.requireNonNull(getCurrentCategory()));
            editor.putString(ctx.getResources().getString(R.string.hue_bridge_config_file), json);
            editor.putString(ctx.getResources().getString(R.string.current_category_config_file), json2);
            editor.apply(); //TODO may use commit to write at once
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void loadConfigurationFromMemory(Context ctx){
        try {
            Log.d(TAG, "loadConfigurationFromMemory()");
            SharedPreferences sharedPref = ctx.getSharedPreferences(ctx.getResources().getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            Gson gson = new Gson();
            String json = sharedPref.getString(ctx.getResources().getString(R.string.hue_bridge_config_file), "");
            String json2 = sharedPref.getString(ctx.getResources().getString(R.string.current_category_config_file), "");
            HueBridge.setInstance(Objects.requireNonNull(gson.fromJson(json, HueBridge.class)));
            setCurrentCategory(Objects.requireNonNull(gson.fromJson(json2, menuCategory.class)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
