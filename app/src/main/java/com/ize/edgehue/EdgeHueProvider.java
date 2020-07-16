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
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class EdgeHueProvider extends SlookCocktailProvider implements Serializable {

    private static final String TAG = EdgeHueProvider.class.getSimpleName();

    private static final String ACTION_REMOTE_LONG_CLICK = "com.ize.edgehue.ACTION_REMOTE_LONG_CLICK";
    private static final String ACTION_REMOTE_CLICK = "com.ize.edgehue.ACTION_REMOTE_CLICK";
    private static final String ACTION_PULL_TO_REFRESH = "com.ize.edgehue.ACTION_PULL_TO_REFRESH";
    protected static final String ACTION_RECEIVE_HUE_STATE = "com.ize.edgehue.ACTION_RECEIVE_HUE_STATE";
    protected static final String ACTION_RECEIVE_HUE_REPLY = "com.ize.edgehue.ACTION_RECEIVE_HUE_REPLY";
    protected static final String COCKTAIL_VISIBILITY_CHANGED = "com.samsung.android.cocktail.action.COCKTAIL_VISIBILITY_CHANGED";

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
    //Array of references to delete buttons in Edit activity
    public static final int[] btnDeleteArr = {R.id.btn1delete, R.id.btn2delete, R.id.btn3delete, R.id.btn4delete, R.id.btn5delete,
            R.id.btn6delete, R.id.btn7delete, R.id.btn8delete, R.id.btn9delete, R.id.btn10delete};
    //Array of references to category buttons
    public static final int[] btnSlidersCategoryArr = {R.id.btnSlidersCategory1, R.id.btnSlidersCategory2,
            R.id.btnSlidersCategory3};
    //Array of references to category buttons underlines
    public static final int[] btnSlidersCategoryLineArr = {R.id.btnSlidersCategoryLine1, R.id.btnSlidersCategoryLine2,
            R.id.btnSlidersCategoryLine3};

    //Categories available in the left pane (helpContent)
    public enum menuCategory implements Serializable {
        QUICK_ACCESS,
        LIGHTS,
        ROOMS,
        ZONES,
        SCENES
    }

    //Categories available in the left pane (helpContent)
    public enum slidersCategory implements Serializable {
        BRIGHTNESS,
        COLOR,
        SATURATION
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
    private static HashMap<menuCategory, HashMap<Integer, BridgeResource>> contents =
            new HashMap<>();

    //Selected category initiated to none
    private static menuCategory currentCategory = menuCategory.QUICK_ACCESS;
    private static slidersCategory slidersCurrentCategory = slidersCategory.BRIGHTNESS;

    private static boolean slidersActive = false;
    private static boolean bridgeConfigured = false;

    private static BridgeResource slidersResource;
    private static Intent brightnessIntent;
    private static Intent colorIntent;
    private static Intent saturationIntent;

    //This method is called for every broadcast and before each of the other callback methods.
    //Samsung SDK
    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        Log.d(TAG, "onReceive()");

        if (getContents().isEmpty()) {
            String toastString = "getContents is empty, loading";
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            loadAllConfiguration(ctx);
        }
        if (getCurrentCategory() == null){
            String toastString = "getCurrentCategory is empty, loading";
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            loadAllConfiguration(ctx);
        }
        if (getCurrentSlidersCategory() == null){
            String toastString = "getCurrentSlidersCategory is empty, loading";
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            loadAllConfiguration(ctx);
        }

        if (HueBridge.getInstance(ctx) == null) {
            bridgeConfigured = false;
            panelUpdate(ctx);
            return;
        }
        else
            bridgeConfigured = true;
        /*if(contentView == null) {
            contentView = createContentView(ctx);
        }
        if(helpView == null) {
            helpView = createHelpView(ctx);
        }*/

        String action;

        try{
            action = Objects.requireNonNull(intent.getAction());
        }
        catch (NullPointerException ex){
            Log.e(TAG, "Recieved action is null");
            ex.printStackTrace();
            return;
        }
        Log.d(TAG, "onReceive: " + action);

        //String toastString = "onReceive: " + action;
        //Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show(); // TODO delete
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
                String toastString = "Refreshing";
                Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            case ACTION_RECEIVE_HUE_REPLY:
                try {
                    Objects.requireNonNull(HueBridge.getInstance(ctx)).requestHueState(ctx);
                }
                catch (NullPointerException ex){
                    Log.e(TAG, "Received a reply, tried to request new state but there is no instance of HueBridge present");
                    ex.printStackTrace();
                    return;
                }
                break;
            case COCKTAIL_VISIBILITY_CHANGED:
            case ACTION_RECEIVE_HUE_STATE:
                panelUpdate(ctx);
                break;
            default:
                break;
        }
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

    public static slidersCategory getCurrentSlidersCategory() {
        if(slidersCurrentCategory == null){
            setCurrentSlidersCategory(slidersCategory.BRIGHTNESS);
        }
        return slidersCurrentCategory;
    }

    public static void setCurrentSlidersCategory(slidersCategory currentCategory) {
        EdgeHueProvider.slidersCurrentCategory = currentCategory;
    }

    public static int addToCurrentCategory(BridgeResource br){
        Log.d(TAG, "addToCurrentCategory()");
        if (getContents().containsKey(getCurrentCategory())) {
            HashMap<Integer, BridgeResource> currentCategoryContents = getContents().get(getCurrentCategory());
            for (int i = 0; i < 10; i++) {
                boolean slotIsEmpty = false;
                try {
                    slotIsEmpty = !Objects.requireNonNull(currentCategoryContents).containsKey(i);
                } catch (NullPointerException ex) {
                    Log.e(TAG, "Failed to get current category contents");
                    ex.printStackTrace();
                }
                if (slotIsEmpty) {
                    currentCategoryContents.put(i, br);
                    Log.d(TAG, "addToCurrentCategory put at: " + i + " values is " + br.toString());
                    return i;
                }
            }
        }
        return -1;
    }

    public static HashMap<menuCategory, HashMap<Integer, BridgeResource>> getContents() {
        return contents;
    }

    public static void setContents(HashMap<menuCategory, HashMap<Integer, BridgeResource>> contents) {
        EdgeHueProvider.contents = contents;
    }

    public static boolean isBridgeConfigured() {
        return bridgeConfigured;
    }

    //Create the content view, right panel. Used for buttons
    private RemoteViews createContentView(Context ctx) {
        RemoteViews contentView = null;

        /*if(contents.isEmpty()){
            loadConfigurationFromMemory(ctx);
        }*/

        if(!bridgeConfigured){
            contentView = new RemoteViews(ctx.getPackageName(),
                    R.layout.main_view_no_bridge);
            return contentView;
        }

        contentView = new RemoteViews(ctx.getPackageName(),
                R.layout.main_view);

        int i = 0;
        for ( int button : btnArr ){
            contentView.setOnClickPendingIntent(button, getClickIntent(
                    ctx, i, 0));
            SlookCocktailManager.getInstance(ctx).
                    setOnLongClickPendingIntent(contentView, button,
                            getLongClickIntent(ctx, i++, 0));
        }
        contentView.setOnClickPendingIntent(R.id.btnEdit,
                getClickIntent(ctx, R.id.btnEdit, 1));

        //Hide empty columns but at least show mainColumn if both are empty
        if(getContents().containsKey(getCurrentCategory())) {
            HashMap<Integer, BridgeResource> currentCategoryContents = getContents().get(getCurrentCategory());
            boolean mainColumnEmpty = true;
            boolean extraColumnEmpty = true;

            for (i = 0; i < 10; i++) {
                boolean slotIsFilled = false;
                try{
                    slotIsFilled = Objects.requireNonNull(currentCategoryContents).containsKey(i);
                } catch (NullPointerException ex) {
                    Log.e(TAG, "Failed to get current category contents");
                    ex.printStackTrace();
                }
                if (slotIsFilled) {
                    if(i < 5){
                        mainColumnEmpty = false;
                        Log.d(TAG, "mainColumnEmpty = false");
                        i = 4;  // Skip to the next column if first is not empty
                    }
                    else {
                        extraColumnEmpty = false;
                        Log.d(TAG, "extraColumnEmpty = false;");
                        break;
                    }
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
                R.layout.help_view);
        for( int button : btnCategoryArr){
            helpView.setOnClickPendingIntent(button, getClickIntent(ctx, button, 1));
            helpView.setTextColor(button, Color.parseColor("#99FAFAFA"));
        }
        for (int line : btnCategoryLineArr){
            helpView.setInt(line, "setBackgroundResource", 0);
        }
        if(bridgeConfigured){
            int currentButton = btnCategoryArr[currentCategory.ordinal()];
            int currentLine = btnCategoryLineArr[currentCategory.ordinal()];
            helpView.setTextColor(currentButton, Color.parseColor("#2187F3"));
            helpView.setInt(currentLine, "setBackgroundResource", R.drawable.dotted);
        }
        return helpView;
    }

    private RemoteViews createSlidersContentView(Context ctx) {
        Log.d(TAG, "createRemoteListView()");
        RemoteViews remoteListView = new RemoteViews(ctx.getPackageName(), R.layout.sliders_main_view);
        switch (getCurrentSlidersCategory()) {
            case BRIGHTNESS:
                if(brightnessIntent == null)
                    brightnessIntent = new Intent(ctx, LongClickBrightnessSliderService.class);
                remoteListView.setRemoteAdapter(R.id.remote_list, brightnessIntent);
                break;
            case COLOR:
                if(colorIntent == null)
                    colorIntent = new Intent(ctx, LongClickColorSliderService.class);
                remoteListView.setRemoteAdapter(R.id.remote_list, colorIntent);
                break;
            case SATURATION:
                if(saturationIntent == null)
                    saturationIntent = new Intent(ctx, LongClickSaturationSliderService.class);
                remoteListView.setRemoteAdapter(R.id.remote_list, saturationIntent);
                break;
            default:
                Log.e(TAG,"Unknown category!");
                break;
        }
        SlookCocktailManager.getInstance(ctx).setOnLongClickPendingIntentTemplate(remoteListView, R.id.remote_list, getLongClickIntent(ctx, R.id.remote_list, 0));
        remoteListView.setPendingIntentTemplate(R.id.remote_list, getClickIntent(ctx, R.id.remote_list, 2));
        return remoteListView;
    }

    //Create the help view, left panel. Used for categories.
    private RemoteViews createSlidersHelpView(Context ctx) {
        RemoteViews helpView = new RemoteViews(ctx.getPackageName(), R.layout.sliders_help_view);
        helpView.setOnClickPendingIntent(R.id.btnBack, getClickIntent(ctx, R.id.btnBack, 1));
        for( int button : btnSlidersCategoryArr){
            helpView.setOnClickPendingIntent(button, getClickIntent(ctx, button, 1));
            helpView.setTextColor(button, Color.parseColor("#99FAFAFA"));
        }
        for (int line : btnSlidersCategoryLineArr){
            helpView.setInt(line, "setBackgroundResource", 0);
        }
        if(getCurrentSlidersCategory() != null){
            int currentButton = btnSlidersCategoryArr[getCurrentSlidersCategory().ordinal()];
            int currentLine = btnSlidersCategoryLineArr[getCurrentSlidersCategory().ordinal()];
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
        if(!bridgeConfigured)
            return;

        int id = intent.getIntExtra("id", -1);
        int key = intent.getIntExtra("key", -1);
        //String toastString = "Clicked id " + id + ", key " + key;
        //Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();

        if(key == 0){
            boolean buttonIsMapped = false;
            try{
                HashMap<Integer, BridgeResource> currentCategoryContents =
                        Objects.requireNonNull(getContents().get(getCurrentCategory()));
                buttonIsMapped = currentCategoryContents.containsKey(id);
            }
            catch (NullPointerException ex){
                Log.e(TAG, "Received a button press but contents not set up. Did you clearAllContents at setup?");
                ex.printStackTrace();
            }
            if(buttonIsMapped){
                try{
                    BridgeResource br = Objects.requireNonNull(getContents().get(getCurrentCategory())).get(id);
                    Objects.requireNonNull(HueBridge.getInstance(ctx)).toggleHueState(ctx, Objects.requireNonNull(br));
                }
                catch (NullPointerException ex){
                    Log.e(TAG, "Received a button press. The button is mapped. But, there is no instance of HueBridge or failed to get the mapping for the button");
                    ex.printStackTrace();
                }
            }
            else {
                startEditActivity(ctx);
                return;
            }
        }
        else if(key == 1) {
            switch (id) {
                case R.id.btnCategory1:
                    setCurrentCategory(menuCategory.QUICK_ACCESS);
                    break;
                case R.id.btnCategory2:
                    setCurrentCategory(menuCategory.LIGHTS);
                    break;
                case R.id.btnCategory3:
                    setCurrentCategory(menuCategory.ROOMS);
                    break;
                case R.id.btnCategory4:
                    setCurrentCategory(menuCategory.ZONES);
                    break;
                case R.id.btnCategory5:
                    setCurrentCategory(menuCategory.SCENES);
                    break;
                case R.id.btnSlidersCategory1:
                    setCurrentSlidersCategory(slidersCategory.BRIGHTNESS);
                    break;
                case R.id.btnSlidersCategory2:
                    setCurrentSlidersCategory(slidersCategory.COLOR);
                    break;
                case R.id.btnSlidersCategory3:
                    setCurrentSlidersCategory(slidersCategory.SATURATION);
                    break;
                case R.id.btnBack:
                    slidersActive = false;
                    break;
                case R.id.btnEdit:
                    loadAllConfiguration(ctx); // rebind for quick to debug loadAllConfiguration() TODO delete
                    //startEditActivity(ctx);
                    break;
                default:
                    break;
            }
            saveCurrentCategory(ctx);
        }
        else if(key == 2){
            switch (id) {
                case R.id.remote_list:
                    int itemId = intent.getIntExtra("item_id", -1);
                    int itemBgColor = intent.getIntExtra("bg_color", -1);
                    HueBridge bridge = HueBridge.getInstance(ctx);
                    assert bridge != null;
                    switch (getCurrentSlidersCategory()) {
                        case BRIGHTNESS:
                            bridge.setHueBrightness(ctx, slidersResource, intent.getIntExtra("brightness", 0));
                            break;
                        case COLOR:
                            bridge.setHueColor(ctx, slidersResource, intent.getIntExtra("color", 0));
                            break;
                        case SATURATION:
                            bridge.setHueSaturation(ctx, slidersResource, intent.getIntExtra("saturation", 0));
                            break;
                        default:
                            Log.e(TAG,"Unknown category!");
                            break;
                    }
                    String toastString = String.format(ctx.getResources().getString(R.string.remote_list_item_clicked), itemId);
                    Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
                    break;
            }
        }
        panelUpdate(ctx);
    }

    private void performRemoteLongClick(Context ctx, Intent intent) {
        if(!bridgeConfigured)
            return;
        Log.d(TAG, "performRemoteLongClick()");

        int id = intent.getIntExtra("id", -1);
        int key = intent.getIntExtra("key", -1);

        if(key == 0){
            boolean buttonIsMapped = false;
            try{
                HashMap<Integer, BridgeResource> currentCategoryContents =
                        Objects.requireNonNull(getContents().get(getCurrentCategory()));
                buttonIsMapped = currentCategoryContents.containsKey(id);
            }
            catch (NullPointerException ex){
                Log.e(TAG, "Received a button press but contents not set up. Did you clearAllContents at setup?");
                ex.printStackTrace();
            }
            if(buttonIsMapped){
                BridgeResource br;
                try {
                    br = Objects.requireNonNull(getContents().get(getCurrentCategory())).get(id);
                }
                catch (NullPointerException ex){
                    Log.e(TAG, "Received a button press. The button is mapped. Failed to get the mapping for the button");
                    ex.printStackTrace();
                    return;
                }
                if (!br.getCategory().equals("scenes")){
                    slidersActive = true;
                    slidersResource = br;
                }
                else {
                    String toastString = "Sliders not available for scenes";
                    Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
                    return;
                }
            }
            else {
                startEditActivity(ctx);
                return;
            }
        }

        contentView = createSlidersContentView(ctx);
        helpView = createSlidersHelpView(ctx);

        final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
        final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, EdgeHueProvider.class));
        cocktailManager.setOnPullPendingIntent(cocktailIds[0], R.id.refreshArea, null);
        cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
    }

    public static void clearAllContents(){
        Log.d(TAG, "clearAllContents()");
        quickAccessContent.clear();
        lightsContent.clear();
        roomsContent.clear();
        zonesContent.clear();
        scenesContent.clear();
        getContents().clear();

        getContents().put(menuCategory.QUICK_ACCESS, quickAccessContent);
        getContents().put(menuCategory.LIGHTS, lightsContent);
        getContents().put(menuCategory.ROOMS, roomsContent);
        getContents().put(menuCategory.ZONES, zonesContent);
        getContents().put(menuCategory.SCENES, scenesContent);
        Log.d(TAG, "clearAllContents done");
    }

    //The initial setup of the buttons
    public static void quickSetup(Context ctx) {
        Log.d(TAG, "quickSetup entered");

        clearAllContents();

        setCurrentCategory(menuCategory.QUICK_ACCESS);

        //The HueBridge instance
        HueBridge bridge = HueBridge.getInstance(ctx);

        if (bridge == null){
            Log.e(TAG, "Quick setup failed. HueBridge instance is null");
            return;
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

        saveAllConfiguration(ctx);
    }

    //Refresh both panels
    private void panelUpdate(Context ctx){
        Log.d(TAG, "panelUpdate()");

        if (slidersActive) {
            contentView = createSlidersContentView(ctx);
            helpView = createSlidersHelpView(ctx);

            final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
            final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, EdgeHueProvider.class));
            cocktailManager.setOnPullPendingIntent(cocktailIds[0], R.id.refreshArea, null);
            cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
            return;
        }

        contentView = createContentView(ctx);
        helpView = createHelpView(ctx);

        if(bridgeConfigured) {
            for (int i = 0; i < 10; i++) {
                if (getContents().containsKey(getCurrentCategory())) {
                    HashMap<Integer, BridgeResource> currentCategoryContents = getContents().get(getCurrentCategory());
                    boolean slotIsFilled = false;
                    try {
                        slotIsFilled = Objects.requireNonNull(currentCategoryContents).containsKey(i);
                    } catch (Exception ex) {
                        Log.e(TAG, "Trying to update panel but failed to get current category contents");
                        ex.printStackTrace();
                    }
                    if (slotIsFilled) {
                        BridgeResource resource = currentCategoryContents.get(i);
                        if (resource == null) {
                            Log.wtf(TAG, "resource == null");
                        }
                        assert resource != null;
                        contentView.setTextViewText(btnTextArr[i], resource.getName(ctx));
                        contentView.setTextViewText(btnArr[i], resource.getBtnText(ctx));
                        contentView.setTextColor(btnArr[i], resource.getBtnTextColor(ctx));
                        contentView.setInt(btnArr[i], "setBackgroundResource",
                                resource.getBtnBackgroundResource(ctx));
                        if (resource.getCategory().equals("scenes")) {
                            contentView.setFloat(btnArr[i], "setTextSize", 10);
                        } else {
                            contentView.setFloat(btnArr[i], "setTextSize", 14);
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

    public static void saveCurrentCategory(Context ctx){
        Gson gson = new Gson();
        String currentCategory = gson.toJson(getCurrentCategory());
        String currentSlidersCategory = gson.toJson(getCurrentSlidersCategory());

        SharedPreferences sharedPref = ctx.getSharedPreferences(ctx.getResources().getString(R.string.preference_file_key), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(ctx.getResources().getString(R.string.current_category_config_file), currentCategory);
        editor.putString(ctx.getResources().getString(R.string.current_sliders_category_config_file), currentSlidersCategory);
        editor.apply(); //TODO may use commit to write at once
    }

    public static void loadCurrentCategory(Context ctx) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(ctx.getResources().getString(R.string.preference_file_key), MODE_PRIVATE);

        Gson gson = new Gson();
        String currentCategory = sharedPref.getString(ctx.getResources().getString(R.string.current_category_config_file), "");
        String currentSlidersCategory = sharedPref.getString(ctx.getResources().getString(R.string.current_sliders_category_config_file), "");
        setCurrentCategory(gson.fromJson(currentCategory, menuCategory.class));
        setCurrentSlidersCategory(gson.fromJson(currentSlidersCategory, slidersCategory.class));
    }

    public static void saveAllConfiguration(Context ctx) {
        try {
            Log.d(TAG, "saveConfigurationToMemory()");

            saveCurrentCategory(ctx);

            //Log.d(TAG, "attempting to save state: " + HueBridge.getInstance(ctx).getState());
            File file = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.preference_file_key));
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(HueBridge.getInstance(ctx));
            outputStream.writeObject(getContents());
            outputStream.flush();
            outputStream.close();

            String toastString = "Saved";
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            //TODO maybe remove toast
        } catch (Exception ex) {
            ex.printStackTrace();
            String toastString = ex.toString();
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            //TODO remove toast
        }
    }

    public static void loadAllConfiguration(Context ctx){
        Log.d(TAG, "loadConfigurationFromMemory()");

        loadCurrentCategory(ctx);

        try {
            File file = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.preference_file_key));
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
            HueBridge bridge = (HueBridge) inputStream.readObject();

            HashMap<menuCategory, HashMap<Integer, BridgeResource>> loadedContents = null;
            try {
                //noinspection unchecked
                loadedContents =
                        (HashMap<menuCategory, HashMap<Integer, BridgeResource>>) inputStream.readObject();
            }
            catch (ClassCastException ex){
                Log.e(TAG, "Unchecked cast failed. Corrupt saved config or old version.");
                ex.printStackTrace();
            }

            HueBridge.setInstance(bridge);
            try {
                Objects.requireNonNull(HueBridge.getInstance(ctx)).requestHueState(ctx);
            }
            catch (NullPointerException ex){
                Log.e(TAG, "Loading of settings failed. Is this the first start?");
                ex.printStackTrace();
            }
            setContents(loadedContents);

//            Log.d(TAG, "attempting to load state: " + bridgeInstance);
//            Log.d(TAG, "attempting to load state: " + bridgeInstance.getState());
//            Log.d(TAG, "attempting to load state: " + bridgeInstance.getIp());

            String toastString = "Loading: " + contents.size();
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();

        }
        catch (FileNotFoundException ex){
            Log.e(TAG, "Config file not found");
            ex.printStackTrace();
            String toastString = ex.toString();
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            //TODO remove toast
        }
        catch (InvalidClassException ex){
            Log.e(TAG, "Config file is old version");
            ex.printStackTrace();
            String toastString = "Old version of config file";
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            //TODO remove toast, fix deletition or something.
        }
        catch (Exception ex) {
            Log.e(TAG, "Failed to load configuration");
            ex.printStackTrace();
            String toastString = ex.toString();
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            //TODO remove toast
        }
    }

    public static boolean deleteAllConfiguration(Context ctx){
        File file = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.preference_file_key));
        return file.delete();
    }
}
