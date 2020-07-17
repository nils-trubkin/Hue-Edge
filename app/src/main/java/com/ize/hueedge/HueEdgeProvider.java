package com.ize.hueedge;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.ize.hueedge.service.LongClickBrightnessSliderService;
import com.ize.hueedge.service.LongClickColorSliderService;
import com.ize.hueedge.service.LongClickSaturationSliderService;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

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

import static android.content.Context.MODE_PRIVATE;

public class HueEdgeProvider extends SlookCocktailProvider implements Serializable {

    private static final String TAG = HueEdgeProvider.class.getSimpleName();

    private static final String ACTION_REMOTE_LONG_CLICK = "com.ize.hueedge.ACTION_REMOTE_LONG_CLICK";
    private static final String ACTION_REMOTE_CLICK = "com.ize.hueedge.ACTION_REMOTE_CLICK";
    private static final String ACTION_PULL_TO_REFRESH = "com.ize.hueedge.ACTION_PULL_TO_REFRESH";
    protected static final String ACTION_RECEIVE_HUE_STATE = "com.ize.hueedge.ACTION_RECEIVE_HUE_STATE";
    protected static final String ACTION_RECEIVE_HUE_REPLY = "com.ize.hueedge.ACTION_RECEIVE_HUE_REPLY";
    private static final String COCKTAIL_VISIBILITY_CHANGED = "com.samsung.android.cocktail.action.COCKTAIL_VISIBILITY_CHANGED";

    private String LIGHTS;

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
    //Array of references to progress bars
    public static final int[] progressBarArr = {R.id.progress_bar1, R.id.progress_bar2, R.id.progress_bar3, R.id.progress_bar4,
            R.id.progress_bar5, R.id.progress_bar6, R.id.progress_bar7, R.id.progress_bar8, R.id.progress_bar9, R.id.progress_bar10};

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



    //Selected category initiated to none
    private static menuCategory currentCategory = menuCategory.QUICK_ACCESS;
    private static slidersCategory slidersCurrentCategory = slidersCategory.BRIGHTNESS;

    private static boolean slidersActive = false;
    private static boolean bridgeConfigured = false;

    private static BridgeResource slidersResource;

    private static int slidersResourceColor;
    private static int slidersResourceSaturation;

    private static int currentlyClicked = -1;

    public static BridgeResource getSlidersResource() {
        return slidersResource;
    }
    public static void setSlidersResource(BridgeResource slidersResource) {
        HueEdgeProvider.slidersResource = slidersResource;
    }

    public static int getSlidersResourceColor() {
        return slidersResourceColor;
    }
    public static void setSlidersResourceColor(int slidersResourceColor) {
        HueEdgeProvider.slidersResourceColor = slidersResourceColor;
    }

    public static int getSlidersResourceSaturation() {
        return slidersResourceSaturation;
    }
    public static void setSlidersResourceSaturation(int slidersResourceSaturation) {
        HueEdgeProvider.slidersResourceSaturation = slidersResourceSaturation;
    }

    public static boolean isSlidersActive() {
        return slidersActive;
    }
    public static void setSlidersActive(boolean slidersActive) {
        HueEdgeProvider.slidersActive = slidersActive;
    }

    //This method is called for every broadcast and before each of the other callback methods.
    //Samsung SDK
    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        Log.d(TAG, "onReceive()");

        Resources res = ctx.getResources();
        LIGHTS = res.getString(R.string.hue_api_lights);

        /*if (getContents().isEmpty()) {
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
        }*/

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
                int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));
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
        HueEdgeProvider.currentCategory = currentCategory;
    }

    public static slidersCategory getCurrentSlidersCategory() {
        if(slidersCurrentCategory == null){
            setCurrentSlidersCategory(slidersCategory.BRIGHTNESS);
        }
        return slidersCurrentCategory;
    }

    public static void setCurrentSlidersCategory(slidersCategory currentCategory) {
        HueEdgeProvider.slidersCurrentCategory = currentCategory;
    }

    public static int addToCurrentCategory(Context ctx, BridgeResource br){
        Log.d(TAG, "addToCurrentCategory()");
        if (getContents(ctx).containsKey(getCurrentCategory())) {
            HashMap<Integer, BridgeResource> currentCategoryContents = getContents(ctx).get(getCurrentCategory());
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

    public static HashMap<menuCategory, HashMap<Integer, BridgeResource>> getContents(Context ctx) {
        return HueBridge.getInstance(ctx).getContents();
    }

    public static void setContents(Context ctx, HashMap<menuCategory, HashMap<Integer, BridgeResource>> contents) {
        HueBridge.getInstance(ctx).setContents(contents);
    }

    public static boolean isBridgeConfigured() {
        return bridgeConfigured;
    }

    //Create the content view, right panel. Used for buttons
    private RemoteViews createContentView(Context ctx) {
        RemoteViews contentView;

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
        if(getContents(ctx).containsKey(getCurrentCategory())) {
            HashMap<Integer, BridgeResource> currentCategoryContents = getContents(ctx).get(getCurrentCategory());
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
        Log.d(TAG, "createSlidersContentView()");
        RemoteViews remoteListView = new RemoteViews(ctx.getPackageName(), R.layout.sliders_main_view);
        setSlidersResourceColor(getSlidersResource().getColor(ctx));
        setSlidersResourceSaturation(getSlidersResource().getSaturation(ctx));
        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));

        switch (getCurrentSlidersCategory()) {
            case BRIGHTNESS:
                Intent brightnessIntent = new Intent(ctx, LongClickBrightnessSliderService.class);
                remoteListView.setRemoteAdapter(R.id.sliders_brightness, brightnessIntent);
                remoteListView.setViewVisibility(R.id.sliders_brightness, View.VISIBLE);
                remoteListView.setViewVisibility(R.id.sliders_color, View.GONE);
                remoteListView.setViewVisibility(R.id.sliders_saturation, View.GONE);
                cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.sliders_brightness);
                break;
            case COLOR:
                Intent colorIntent = new Intent(ctx, LongClickColorSliderService.class);
                remoteListView.setRemoteAdapter(R.id.sliders_color, colorIntent);
                remoteListView.setViewVisibility(R.id.sliders_color, View.VISIBLE);
                remoteListView.setViewVisibility(R.id.sliders_brightness, View.GONE);
                remoteListView.setViewVisibility(R.id.sliders_saturation, View.GONE);
                cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.sliders_color);
                break;
            case SATURATION:
                Intent saturationIntent = new Intent(ctx, LongClickSaturationSliderService.class);
                remoteListView.setRemoteAdapter(R.id.sliders_saturation, saturationIntent);
                remoteListView.setViewVisibility(R.id.sliders_saturation, View.VISIBLE);
                remoteListView.setViewVisibility(R.id.sliders_brightness, View.GONE);
                remoteListView.setViewVisibility(R.id.sliders_color, View.GONE);
                cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.sliders_saturation);
                break;
            default:
                Log.e(TAG,"Unknown category!");
                break;
        }

        //SlookCocktailManager.getInstance(ctx).setOnLongClickPendingIntentTemplate(remoteListView, R.id.sliders_brightness, getLongClickIntent(ctx, R.id.sliders_brightness, 0)); // Long click of slider buttons
        remoteListView.setPendingIntentTemplate(R.id.sliders_brightness, getClickIntent(ctx, R.id.sliders_brightness, 2));
        remoteListView.setPendingIntentTemplate(R.id.sliders_color, getClickIntent(ctx, R.id.sliders_color, 2));
        remoteListView.setPendingIntentTemplate(R.id.sliders_saturation, getClickIntent(ctx, R.id.sliders_saturation, 2));
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
        Intent longClickIntent = new Intent(ctx, HueEdgeProvider.class);
        longClickIntent.setAction(ACTION_REMOTE_LONG_CLICK);
        longClickIntent.putExtra("id", id);
        longClickIntent.putExtra("key", key);
        return PendingIntent.getBroadcast(ctx, id, longClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Get the click intent object to assign to a button
    private static PendingIntent getClickIntent(Context ctx, int id, int key) {
        Intent clickIntent = new Intent(ctx, HueEdgeProvider.class);
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
                        Objects.requireNonNull(getContents(ctx).get(getCurrentCategory()));
                buttonIsMapped = currentCategoryContents.containsKey(id);
            }
            catch (NullPointerException ex){
                Log.e(TAG, "Received a button press but contents not set up. Did you clearAllContents at setup?");
                ex.printStackTrace();
            }
            if(buttonIsMapped){
                try{
                    currentlyClicked = id;
                    BridgeResource br = Objects.requireNonNull(getContents(ctx).get(getCurrentCategory())).get(id);
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
                    setSlidersActive(false);
                    break;
                case R.id.btnEdit:
                    loadAllConfiguration(ctx); // rebind for quick way to debug loadAllConfiguration()
                    //startEditActivity(ctx);
                    break;
                default:
                    break;
            }
            saveCurrentCategory(ctx);
        }
        else if(key == 2){
            HueBridge bridge = HueBridge.getInstance(ctx);
            assert bridge != null;
            BridgeResource br = getSlidersResource();
            String actionUrl = br.getCategory().equals(LIGHTS) ? br.getStateUrl() : br.getActionUrl();
            switch (id){
                case R.id.sliders_brightness:
                    bridge.setHueState(ctx, actionUrl, br.getActionWrite(), true);
                    bridge.setHueBrightness(ctx, br, intent.getIntExtra("brightness", 0));
                    break;
                case R.id.sliders_color:
                    bridge.setHueState(ctx, actionUrl, br.getActionWrite(), true);
                    bridge.setHueColor(ctx, br, intent.getIntExtra("color", 0));
                    break;
                case R.id.sliders_saturation:
                    bridge.setHueState(ctx, actionUrl, br.getActionWrite(), true);
                    bridge.setHueSaturation(ctx, br, intent.getIntExtra("saturation", 0));
                    break;
                default:
                    Log.e(TAG, "Unknown category!");
                    break;
            }
            //String toastString = String.format(ctx.getResources().getString(R.string.remote_list_item_clicked), itemId);
            //Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show(); // Debug toast for presses on the sliders buttons.
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
                        Objects.requireNonNull(getContents(ctx).get(getCurrentCategory()));
                buttonIsMapped = currentCategoryContents.containsKey(id);
            }
            catch (NullPointerException ex){
                Log.e(TAG, "Received a button press but contents not set up. Did you clearAllContents at setup?");
                ex.printStackTrace();
            }
            if(buttonIsMapped){
                BridgeResource br;
                boolean isNotAScene;
                try {
                    br = Objects.requireNonNull(getContents(ctx).get(getCurrentCategory())).get(id);
                    isNotAScene = !Objects.requireNonNull(br).getCategory().equals("scenes");
                }
                catch (NullPointerException ex){
                    Log.e(TAG, "Received a button press. The button is mapped. Failed to get the mapping for the button");
                    ex.printStackTrace();
                    return;
                }
                if (isNotAScene){
                    setSlidersActive(true);
                    setSlidersResource(br);
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
        final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));
        cocktailManager.setOnPullPendingIntent(cocktailIds[0], R.id.refreshArea, null);
        cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
    }

    //The initial setup of the buttons
    public static void quickSetup(Context ctx) {
        Log.d(TAG, "quickSetup entered");

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
        try {
            HashMap<menuCategory, HashMap<Integer, BridgeResource>> contents =
                    Objects.requireNonNull(HueBridge.getInstance(ctx)).getContents();
        } catch (NullPointerException ex){
            Log.e(TAG, "Tried to perform quick setup but no instance of HueBridge was found");
            ex.printStackTrace();
            return;
        }

        map = bridge.getLights();
        Log.d(TAG, "quickSetup getLights() size: " + map.size());
        for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for lights on id: " + entry.getKey());
            getContents(ctx).get(menuCategory.LIGHTS).put(buttonIndex++, entry.getValue());
            if(qaButtonIndex < 2) {
                getContents(ctx).get(menuCategory.QUICK_ACCESS).put(qaButtonIndex++, entry.getValue());
            }
        }

        buttonIndex = 0;

        map = bridge.getRooms();
        Log.d(TAG, "quickSetup getRooms() size: " + map.size());
        for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for rooms on id: " + entry.getKey());
            getContents(ctx).get(menuCategory.ROOMS).put(buttonIndex++, entry.getValue());
            if(qaButtonIndex < 4) {
                getContents(ctx).get(menuCategory.QUICK_ACCESS).put(qaButtonIndex++, entry.getValue());
            }
        }

        buttonIndex = 0;

        map = bridge.getZones();
        Log.d(TAG, "quickSetup getZones() size: " + map.size());
        for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for zones on id: " + entry.getKey());
            getContents(ctx).get(menuCategory.ZONES).put(buttonIndex++, entry.getValue());
            if(qaButtonIndex < 6) {
                getContents(ctx).get(menuCategory.QUICK_ACCESS).put(qaButtonIndex++, entry.getValue());
            }
        }

        buttonIndex = 0;

        map = bridge.getScenes();
        Log.d(TAG, "quickSetup getScenes() size: " + map.size());
        for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for scenes on id: " + entry.getKey());
            getContents(ctx).get(menuCategory.SCENES).put(buttonIndex++, entry.getValue());
            if(qaButtonIndex < 8) {
                getContents(ctx).get(menuCategory.QUICK_ACCESS).put(qaButtonIndex++, entry.getValue());
            }
        }

        saveAllConfiguration(ctx);
    }

    //Refresh both panels
    private void panelUpdate(Context ctx){
        Log.d(TAG, "panelUpdate()");

        if (isSlidersActive()) {
            contentView = createSlidersContentView(ctx);
            helpView = createSlidersHelpView(ctx);

            final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
            final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));
            cocktailManager.setOnPullPendingIntent(cocktailIds[0], R.id.refreshArea, null);
            cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
            return;
        }

        contentView = createContentView(ctx);
        helpView = createHelpView(ctx);

        if(bridgeConfigured) {
            for (int i = 0; i < 10; i++) {
                if (getContents(ctx).containsKey(getCurrentCategory())) {
                    HashMap<Integer, BridgeResource> currentCategoryContents = getContents(ctx).get(getCurrentCategory());
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
                        contentView.setViewVisibility(progressBarArr[i], View.GONE);
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
            if (currentlyClicked != -1){
                contentView.setViewVisibility(progressBarArr[currentlyClicked], View.VISIBLE);
                currentlyClicked = -1;
            }
        }

        final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
        final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));

        //Set pull refresh
        Intent refreshIntent = new Intent(ctx, HueEdgeProvider.class);
        refreshIntent.setAction(ACTION_PULL_TO_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 0xff, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        SlookCocktailManager.getInstance(ctx).setOnPullPendingIntent(cocktailIds[0], R.id.refreshArea, pendingIntent);

        cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
    }

    public static void saveCurrentCategory(Context ctx){
        Gson gson = new Gson();
        String currentCategory = gson.toJson(getCurrentCategory());
        String currentSlidersCategory = gson.toJson(getCurrentSlidersCategory());
        String slidersResource = gson.toJson(getSlidersResource());
        String slidersActive = gson.toJson(isSlidersActive());

        SharedPreferences sharedPref = ctx.getSharedPreferences(ctx.getResources().getString(R.string.preference_file_key), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(ctx.getResources().getString(R.string.current_category_config_file), currentCategory);
        editor.putString(ctx.getResources().getString(R.string.current_sliders_category_config_file), currentSlidersCategory);
        editor.putString(ctx.getResources().getString(R.string.sliders_resource_config_file), slidersResource);
        editor.putString(ctx.getResources().getString(R.string.sliders_active_config_file), slidersActive);
        editor.apply(); //TODO may use commit to write at once
    }

    public static void loadCurrentCategory(Context ctx) {
        Gson gson = new Gson();
        SharedPreferences sharedPref = ctx.getSharedPreferences(ctx.getResources().getString(R.string.preference_file_key), MODE_PRIVATE);
        String currentCategory = sharedPref.getString(ctx.getResources().getString(R.string.current_category_config_file), "");
        String currentSlidersCategory = sharedPref.getString(ctx.getResources().getString(R.string.current_sliders_category_config_file), "");
        String slidersResource = sharedPref.getString(ctx.getResources().getString(R.string.sliders_resource_config_file), "");
        String slidersActive = sharedPref.getString(ctx.getResources().getString(R.string.sliders_active_config_file), "");

        try {
            setCurrentCategory(gson.fromJson(currentCategory, menuCategory.class));
            setCurrentSlidersCategory(gson.fromJson(currentSlidersCategory, slidersCategory.class));
            setSlidersResource(gson.fromJson(slidersResource, BridgeResource.class));
            setSlidersActive(gson.fromJson(slidersActive, boolean.class));
        } catch (NullPointerException ex){
            String toastString = "Config file not found";
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            Log.e(TAG, toastString);
            //TODO remove toast
        }
    }

    public static void saveAllConfiguration(Context ctx) {
        saveCurrentCategory(ctx);

        File preferenceFile = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.preference_file_key));
        File recoveryFile = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.recovery_file_key));
        try {
            Log.d(TAG, "saveConfigurationToMemory()");
            HueBridge bridge;
            try {
                bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
            }
            catch (NullPointerException ex){
                Log.e(TAG, "Tried to save, no instance of HueBridge found");
                return;
            }
            //Log.d(TAG, "attempting to save state: " + HueBridge.getInstance(ctx).getState());

            ObjectOutputStream preferenceOutputStream = new ObjectOutputStream(new FileOutputStream(preferenceFile));
            preferenceOutputStream.writeObject(bridge);
            preferenceOutputStream.writeObject(getContents(ctx));
            preferenceOutputStream.flush();
            preferenceOutputStream.close();

            //recovery
            ObjectOutputStream recoveryOutputStream = new ObjectOutputStream(new FileOutputStream(recoveryFile));
            recoveryOutputStream.writeObject(bridge.getIp());
            recoveryOutputStream.writeObject(bridge.getUserName());
            recoveryOutputStream.flush();
            recoveryOutputStream.close();

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

    public static void loadAllConfiguration(Context ctx) {
        Log.d(TAG, "loadConfigurationFromMemory()");
        loadCurrentCategory(ctx);

        File configFile = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.preference_file_key));
        ObjectInputStream configInputStream = null;
        HashMap<menuCategory, HashMap<Integer, BridgeResource>> loadedContents;
        HueBridge bridge;

        // Load config file
        try {
            configInputStream = new ObjectInputStream(new FileInputStream(configFile));
        } catch (FileNotFoundException ex){
            String toastString = "Config file not found";
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            Log.e(TAG, toastString);
            //TODO remove toast
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Load instance of HueBridge
        try {
            bridge = (HueBridge) Objects.requireNonNull(configInputStream).readObject();
            HueBridge.setInstance(bridge);
        } catch (NullPointerException ex){
            String toastString = "Config file not found";
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            Log.e(TAG, toastString);
            //TODO remove toast
        }

        // Catch old version
        catch (InvalidClassException ex){
            String toastString = "Config file is old version, updating";
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            Log.e(TAG, toastString);

            File recoveryFile = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.recovery_file_key));

            // Open and apply recovery for HueBridge instance
            try {
                ObjectInputStream recoveryInputStream = new ObjectInputStream(new FileInputStream(recoveryFile));
                String ip = (String) recoveryInputStream.readObject();
                String userName = (String) recoveryInputStream.readObject();
                HueBridge.getInstance(ctx, ip, userName);
            } catch (FileNotFoundException ex2){
                toastString = "Recovery file not found";
                Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
                Log.e(TAG, toastString);
                //TODO remove toast
            } catch (ClassNotFoundException | IOException ex2){
                ex2.printStackTrace();
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to load configuration for other reason");
            ex.printStackTrace();
            String toastString = "Failed to load configuration for other reason";
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            //TODO remove toast
        }

        // Load contents;
        try {
            //Objects.requireNonNull(HueBridge.getInstance(ctx)).requestHueState(ctx); //TODO uncomment

            //loadedContents =
            //        (HashMap<menuCategory, HashMap<Integer, BridgeResource>>) Objects.requireNonNull(configInputStream).readObject();
            Object loadedContentsObject = Objects.requireNonNull(configInputStream).readObject();
            Log.e(TAG,loadedContentsObject.getClass().toString());
            //setContents(loadedContents);
        } catch (ClassCastException | ClassNotFoundException | IOException ex){
            Log.e(TAG, "Unchecked cast failed. Corrupt saved config or old version.");
            ex.printStackTrace();
        } catch (NullPointerException ex){
            Log.e(TAG, "Loading of settings failed. Is this the first start?");
            ex.printStackTrace();
        }

        String toastString = "Loading: " + getContents(ctx).size();
        Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
    }

    public static boolean deleteAllConfiguration(Context ctx){
        File file;
        try {
            file = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.preference_file_key));
        }
        catch (NullPointerException ex) {
            Log.e(TAG, "deleteAllConfig could not find configuration");
            ex.printStackTrace();
            String toastString = ex.toString();
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            return false;
            //TODO remove toast
        }
        return file.delete();
    }
}