package com.nilstrubkin.hueedge;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.nilstrubkin.hueedge.activity.SetupActivity;
import com.nilstrubkin.hueedge.resources.BridgeCatalogue;
import com.nilstrubkin.hueedge.resources.BridgeResource;
import com.nilstrubkin.hueedge.resources.BridgeResourceSliders;
import com.nilstrubkin.hueedge.resources.SceneResource;
import com.nilstrubkin.hueedge.service.LongClickBrightnessSliderService;
import com.nilstrubkin.hueedge.service.LongClickColorSliderService;
import com.nilstrubkin.hueedge.service.LongClickSaturationSliderService;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.VIBRATOR_SERVICE;

public class HueEdgeProvider extends SlookCocktailProvider {

    private static final String TAG = HueEdgeProvider.class.getSimpleName();

    private static final String ACTION_REMOTE_LONG_CLICK = "com.nilstrubkin.hueedge.ACTION_REMOTE_LONG_CLICK";
    private static final String ACTION_REMOTE_CLICK = "com.nilstrubkin.hueedge.ACTION_REMOTE_CLICK";
    private static final String ACTION_PULL_TO_REFRESH = "com.nilstrubkin.hueedge.ACTION_PULL_TO_REFRESH";
    protected static final String ACTION_RECEIVE_HUE_STATE = "com.nilstrubkin.hueedge.ACTION_RECEIVE_HUE_STATE";
    protected static final String ACTION_RECEIVE_HUE_REPLY = "com.nilstrubkin.hueedge.ACTION_RECEIVE_HUE_REPLY";
    protected static final String ACTION_TIMEOUT_HUE_REPLY = "com.nilstrubkin.hueedge.ACTION_TIMEOUT_HUE_REPLY";
    private static final String COCKTAIL_VISIBILITY_CHANGED = "com.samsung.android.cocktail.action.COCKTAIL_VISIBILITY_CHANGED";

    //Array of references to buttons
    public static final int[] btnArr = {R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn10};
    //Array of references to category buttons
    public static final int[] btnCategoryArr = {R.id.btnCategory1, R.id.btnCategory2,
            R.id.btnCategory3, R.id.btnCategory4, R.id.btnCategory5};
    //Array of references to category buttons underlines
    public static final int[] btnCategoryLineArr = {R.id.btnCategoryLine1, R.id.btnCategoryLine2,
            R.id.btnCategoryLine3, R.id.btnCategoryLine4, R.id.btnCategoryLine5};
    //Array of references to button texts (text on the button itself)
    public static final int[] btnTopTextArr = {R.id.btn1topText, R.id.btn2topText, R.id.btn3topText, R.id.btn4topText, R.id.btn5topText,
            R.id.btn6topText, R.id.btn7topText, R.id.btn8topText, R.id.btn9topText, R.id.btn10topText};
    //Array of references to button texts (text under the button itself)
    public static final int[] btnTextArr = {R.id.btn1text, R.id.btn2text, R.id.btn3text, R.id.btn4text, R.id.btn5text,
            R.id.btn6text, R.id.btn7text, R.id.btn8text, R.id.btn9text, R.id.btn10text};
    //Array of references to delete buttons in Edit activity
    public static final int[] btnDeleteArr = {R.id.btn1delete, R.id.btn2delete, R.id.btn3delete, R.id.btn4delete, R.id.btn5delete,
            R.id.btn6delete, R.id.btn7delete, R.id.btn8delete, R.id.btn9delete, R.id.btn10delete};
    //Array of references to delete buttons top texts in Edit activity
    public static final int[] btnDeleteTopTextArr = {R.id.btn1deleteTopText, R.id.btn2deleteTopText, R.id.btn3deleteTopText, R.id.btn4deleteTopText, R.id.btn5deleteTopText,
            R.id.btn6deleteTopText, R.id.btn7deleteTopText, R.id.btn8deleteTopText, R.id.btn9deleteTopText, R.id.btn10deleteTopText};
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
    public enum menuCategory {
        QUICK_ACCESS,
        LIGHTS,
        ROOMS,
        ZONES,
        SCENES
    }

    //Categories available in the left pane (helpContent)
    public enum slidersCategory {
        BRIGHTNESS,
        COLOR,
        SATURATION
    }

    private static boolean slidersActive = false;
    private static boolean bridgeConfigured = false;

    private static ResourceReference slidersResource;
    private static int slidersResHue;
    private static int slidersResSat;

    private static final List<Integer> currentlyClicked = new ArrayList<>();

    public static ResourceReference getSlidersResource() {
        return slidersResource;
    }
    public static void setSlidersResource(ResourceReference slidersResource) {
        HueEdgeProvider.slidersResource = slidersResource;
    }

    public static int getSlidersResHue() {
        return slidersResHue;
    }
    public static void setSlidersResHue(int slidersResHue) {
        HueEdgeProvider.slidersResHue = slidersResHue;
    }

    public static int getSlidersResSat() {
        return slidersResSat;
    }
    public static void setSlidersResSat(int slidersResSat) {
        HueEdgeProvider.slidersResSat = slidersResSat;
    }

    public static boolean isSlidersActive() {
        return slidersActive;
    }
    public static void setSlidersActive(boolean slidersActive) {
        HueEdgeProvider.slidersActive = slidersActive;
    }

    /**
     * Samsung SDK
     * This method is called for every broadcast and before each of the other callback methods
     * @param ctx Context
     * @param intent Intent
     */
    @Override
    public void onReceive(final Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        Log.d(TAG, "onReceive()");

        if (HueBridge.getInstance(ctx) == null) {
            bridgeConfigured = false;
            panelUpdate(ctx);
        }
        else
            bridgeConfigured = true;

        String action;
        try {
            action = Objects.requireNonNull(intent.getAction());
        }
        catch (NullPointerException e){
            Log.e(TAG, "Received action is null");
            e.printStackTrace();
            return;
        }
        Log.d(TAG, "onReceive: " + action);
        switch (action) {
            case ACTION_REMOTE_LONG_CLICK:
                performRemoteLongClick(ctx, intent);
                break;
            case ACTION_REMOTE_CLICK:
                performRemoteClick(ctx, intent);
                break;
            case ACTION_PULL_TO_REFRESH:
                currentlyClicked.clear();
                performPullToRefresh(ctx);
                if (checkWifiNotConnected(ctx))
                    break;
                //else skip and simulate ACTION_RECEIVE_HUE_REPLY which requests a new state
            case ACTION_RECEIVE_HUE_REPLY:
                HueBridge.requestHueState(ctx);
                currentlyClicked.clear();
                panelUpdate(ctx);
                break;
            case ACTION_TIMEOUT_HUE_REPLY:
                Toast.makeText(ctx, ctx.getString(R.string.toast_timeout_reply), Toast.LENGTH_LONG).show();
                currentlyClicked.clear();
                panelUpdate(ctx);
                break;
            case COCKTAIL_VISIBILITY_CHANGED:
                panelUpdate(ctx);
                break;
            case ACTION_RECEIVE_HUE_STATE:
                panelUpdate(ctx);
                saveAllConfiguration(ctx); //TODO smaller
                break;
            default:
                break;
        }
    }

    /**
     * Samsung SDK
     * This method is called when the Edge Single Plus Mode is created for the first time
     * @param ctx Context
     */
    @Override
    public void onEnabled(Context ctx) {
        super.onEnabled(ctx);
        startSetupActivity(ctx);
    }

    //This method is called when the instance of
    //Edge Single Plus Mode is deleted from the enabled list
    //Samsung SDK
    @Override
    public void onDisabled(Context ctx) {
        Log.d(TAG, "onDisabled()");
        super.onDisabled(ctx);
    }

    /*
     * Samsung SDK
     * This method is called every now and then
     * @param ctx Context
     * @param cocktailManager ignored
     * @param cocktailIds ignored
     */
    /*@Override
    public void onUpdate(Context ctx, SlookCocktailManager cocktailManager, int[] cocktailIds) {
        super.onUpdate(ctx, cocktailManager, cocktailIds);
        Log.d(TAG, "onUpdate()");
        panelUpdate(ctx);
    }*/

    /**
     * Samsung SDK
     * This method is called when the user opens or closes the panel
     * @param ctx Context
     * @param cocktailId ignored
     * @param visibility is 1 when visible
     */
    @Override
    public void onVisibilityChanged(Context ctx, int cocktailId, int visibility) {
        super.onVisibilityChanged(ctx, cocktailId, visibility);
        Log.d(TAG, "onVisibilityChanged(): " + visibility);
        if(bridgeConfigured && visibility == 1) {
            performPullToRefresh(ctx);
        }
    }

    /**
     * Create the content view, right panel, used for buttons
     * @param ctx Context
     * @return RemoteViews
     */
    private RemoteViews createContentView(Context ctx) {
        RemoteViews contentView;
        HueBridge bridge;
        try {
            bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
        } catch (NullPointerException e){
            Log.d(TAG, "Creating content view, no bridge found, will display main_view_no_bridge");
            contentView = new RemoteViews(ctx.getPackageName(),
                    R.layout.main_view_no_bridge); // R.layout.main_view_demo); TODO demo
            contentView.setOnClickPendingIntent(R.id.configureButton,
                    getClickIntent(ctx, R.id.configureButton, 1));
            return contentView;
        }

        contentView = new RemoteViews(ctx.getPackageName(), R.layout.main_view);

        int i = 0;
        for ( int button : btnArr ){
            contentView.setOnClickPendingIntent(button, getClickIntent(
                    ctx, i, 0));
            SlookCocktailManager.getInstance(ctx).
                    setOnLongClickPendingIntent(contentView, button,
                            getLongClickIntent(ctx, i++));
        }
        contentView.setOnClickPendingIntent(R.id.btnEdit,
                getClickIntent(ctx, R.id.btnEdit, 1));

        //Hide empty columns but at least show mainColumn if both are empty
        menuCategory currentCategory = bridge.getCurrentCategory();
        if(bridge.getContents().containsKey(currentCategory)) {
            Map<Integer, ResourceReference> currentCategoryContents = bridge.getContents().get(currentCategory);
            boolean mainColumnEmpty = true;
            boolean extraColumnEmpty = true;

            for (i = 0; i < 10; i++) {
                boolean slotIsFilled = false;
                try{
                    slotIsFilled = Objects.requireNonNull(currentCategoryContents).containsKey(i);
                } catch (NullPointerException e) {
                    Log.e(TAG, "Failed to get current category contents");
                    e.printStackTrace();
                }
                if (slotIsFilled) {
                    if(i < 5){
                        mainColumnEmpty = false;
                        i = 4;  // Skip to the next column if first is not empty
                    }
                    else {
                        extraColumnEmpty = false;
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

    /**
     * Create the help view, left panel, used for categories
     * @param ctx Context
     * @return RemoteViews
     */
    private RemoteViews createHelpView(Context ctx) {
        RemoteViews helpView = new RemoteViews(ctx.getPackageName(),
                R.layout.help_view);
        for( int button : btnCategoryArr){
            helpView.setOnClickPendingIntent(button, getClickIntent(ctx, button, 1));
            helpView.setTextColor(button, ctx.getColor(R.color.category_unselected_gray));
        }
        for (int line : btnCategoryLineArr){
            helpView.setInt(line, "setBackgroundResource", 0);
        }
        if(bridgeConfigured){
            int selectedNumber;
            try {
                HueBridge bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
                selectedNumber = bridge.getCurrentCategory().ordinal();
            } catch (NullPointerException e){
                Log.e(TAG, "Creating help view but no HueBridge instance is found");
                e.printStackTrace();
                return helpView;
            }
            int currentButton = btnCategoryArr[selectedNumber];
            int currentLine = btnCategoryLineArr[selectedNumber];
            helpView.setTextColor(currentButton, ctx.getColor(R.color.category_selected_blue));
            helpView.setInt(currentLine, "setBackgroundResource", R.drawable.dotted);
        }
        return helpView;
    }

    /**
     * Create the content view, right panel, used for sliders
     * @param ctx Context
     * @return RemoteViews
     */
    private RemoteViews createSlidersContentView(Context ctx) {
        Log.d(TAG, "createSlidersContentView()");
        RemoteViews remoteListView = new RemoteViews(ctx.getPackageName(), R.layout.sliders_main_view);
        HueBridge bridge;
        try {
            bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
        } catch (NullPointerException e){
            Log.e(TAG, "Creating sliders content view but no HueBridge instance is found");
            e.printStackTrace();
            return remoteListView;
        }
        ResourceReference resRef = getSlidersResource();
        BridgeResourceSliders res = (BridgeResourceSliders) bridge.getResource(resRef);
        setSlidersResHue(res.getHue());
        setSlidersResSat(res.getSat());

        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));

        remoteListView.setViewVisibility(R.id.sliders_bri, View.GONE);
        remoteListView.setViewVisibility(R.id.sliders_hue, View.GONE);
        remoteListView.setViewVisibility(R.id.sliders_sat, View.GONE);

        switch (bridge.getCurrentSlidersCategory()) {
            case BRIGHTNESS:
                Intent brightnessIntent = new Intent(ctx, LongClickBrightnessSliderService.class);
                remoteListView.setRemoteAdapter(R.id.sliders_bri, brightnessIntent);
                cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.sliders_bri);
                remoteListView.setViewVisibility(R.id.sliders_bri, View.VISIBLE);
                break;
            case COLOR:
                Intent colorIntent = new Intent(ctx, LongClickColorSliderService.class);
                remoteListView.setRemoteAdapter(R.id.sliders_hue, colorIntent);
                cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.sliders_hue);
                remoteListView.setViewVisibility(R.id.sliders_hue, View.VISIBLE);
                break;
            case SATURATION:
                Intent saturationIntent = new Intent(ctx, LongClickSaturationSliderService.class);
                remoteListView.setRemoteAdapter(R.id.sliders_sat, saturationIntent);
                cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.sliders_sat);
                remoteListView.setViewVisibility(R.id.sliders_sat, View.VISIBLE);
                break;
            default:
                Log.e(TAG,"Unknown category!");
                break;
        }

        // Long click of slider buttons for future use
        //SlookCocktailManager.getInstance(ctx).setOnLongClickPendingIntentTemplate(remoteListView, R.id.sliders_brightness, getLongClickIntent(ctx, R.id.sliders_brightness, 0));
        remoteListView.setPendingIntentTemplate(R.id.sliders_bri, getClickIntent(ctx, R.id.sliders_bri, 2));
        remoteListView.setPendingIntentTemplate(R.id.sliders_hue, getClickIntent(ctx, R.id.sliders_hue, 2));
        remoteListView.setPendingIntentTemplate(R.id.sliders_sat, getClickIntent(ctx, R.id.sliders_sat, 2));
        return remoteListView;
    }

    /**
     * Create the help view, left panel, used for categories  of sliders
     * @param ctx Context
     * @return RemoteViews
     */
    private RemoteViews createSlidersHelpView(Context ctx) {
        RemoteViews helpView = new RemoteViews(ctx.getPackageName(), R.layout.sliders_help_view);
        helpView.setOnClickPendingIntent(R.id.btnBack, getClickIntent(ctx, R.id.btnBack, 1));
        HueBridge bridge;
        try {
            bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
        } catch (NullPointerException e){
            Log.e(TAG, "Creating sliders help view but no HueBridge instance is found");
            e.printStackTrace();
            return helpView;
        }
        for( int button : btnSlidersCategoryArr){
            helpView.setOnClickPendingIntent(button, getClickIntent(ctx, button, 1));
            helpView.setTextColor(button, ctx.getColor(R.color.category_unselected_gray));
        }
        for (int line : btnSlidersCategoryLineArr){
            helpView.setInt(line, "setBackgroundResource", 0);
        }
        slidersCategory currentSlidersCategory = bridge.getCurrentSlidersCategory();
        if(currentSlidersCategory != null){
            int currentButton = btnSlidersCategoryArr[currentSlidersCategory.ordinal()];
            int currentLine = btnSlidersCategoryLineArr[currentSlidersCategory.ordinal()];
            helpView.setTextColor(currentButton, ctx.getColor(R.color.category_selected_blue));
            helpView.setInt(currentLine, "setBackgroundResource", R.drawable.dotted);
        }
        return helpView;
    }

    /**
     * Get the long click intent object for a button
     * @param ctx Context
     * @param id id
     * @return Intent
     */
    private static PendingIntent getLongClickIntent(Context ctx, int id) {
        Intent longClickIntent = new Intent(ctx, HueEdgeProvider.class);
        longClickIntent.setAction(ACTION_REMOTE_LONG_CLICK);
        longClickIntent.putExtra("id", id);
        longClickIntent.putExtra("key", 0);
        return PendingIntent.getBroadcast(ctx, id, longClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Get the click intent object for a button
     * @param ctx Context
     * @param id id
     * @param key key
     * @return Intent
     */
    private static PendingIntent getClickIntent(Context ctx, int id, int key) {
        Intent clickIntent = new Intent(ctx, HueEdgeProvider.class);
        clickIntent.setAction(ACTION_REMOTE_CLICK);
        clickIntent.putExtra("id", id);
        clickIntent.putExtra("key", key);
        return PendingIntent.getBroadcast(ctx, id, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Get the intent for refreshing with pull-down
     * @param ctx Context
     * @return Intent
     */
    private static PendingIntent getRefreshIntent(Context ctx){
        Intent refreshIntent = new Intent(ctx, HueEdgeProvider.class);
        refreshIntent.setAction(ACTION_PULL_TO_REFRESH);
        return PendingIntent.getBroadcast(ctx, 0xff,
                refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Get the intent for incoming state JsonObject
     * @param ctx Context
     * @return Intent
     */
    public static PendingIntent getStateIntent(Context ctx) {
        Intent stateIntent = new Intent(ctx, HueEdgeProvider.class);
        stateIntent.setAction(HueEdgeProvider.ACTION_RECEIVE_HUE_STATE);
        return PendingIntent.getBroadcast(ctx, 1, stateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Get the intent for incoming reply JsonArray
     * @param ctx Context
     * @return Intent
     */
    public static PendingIntent getReplyIntent(Context ctx) {
        Intent replyIntent = new Intent(ctx, HueEdgeProvider.class);
        replyIntent.setAction(HueEdgeProvider.ACTION_RECEIVE_HUE_REPLY);
        return PendingIntent.getBroadcast(ctx, 1, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Get the intent for timeout reply
     * @param ctx Context
     * @return Intent
     */
    public static PendingIntent getTimeoutIntent(Context ctx) {
        Intent replyIntent = new Intent(ctx, HueEdgeProvider.class);
        replyIntent.setAction(HueEdgeProvider.ACTION_TIMEOUT_HUE_REPLY);
        return PendingIntent.getBroadcast(ctx, 1, replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Enter the edit activity to customize buttons
     * @param ctx Context
     */
    private static void startEditActivity(Context ctx){
        Intent editIntent = new Intent(Intent.ACTION_EDIT);
        editIntent.addCategory(Intent.CATEGORY_DEFAULT);
        editIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(editIntent);
    }

    /**
     * Start setup activity to link to a bridge
     * @param ctx Context
     */
    public static void startSetupActivity(Context ctx){
        Intent setupIntent = new Intent(ctx, SetupActivity.class);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(setupIntent);
    }

    /**
     * Perform click (not long click)
     * @param ctx Context
     * @param intent Intent
     */
    private void performRemoteClick(Context ctx, Intent intent) {
        int id = intent.getIntExtra("id", -1);
        int key = intent.getIntExtra("key", -1);

        // When no bridge found user gets this button in content view to get to setup
        if (id == R.id.configureButton)
            startSetupActivity(ctx);

        if(!bridgeConfigured)
            return;

        HueBridge bridge;
        try {
            bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
        } catch (NullPointerException e){
            Log.e(TAG, "Performing click on but no HueBridge instance is found");
            e.printStackTrace();
            return;
        }

        switch (key) {
            // Key 0 for round buttons in content view
            case 0:
                boolean buttonIsMapped;
                menuCategory currentCategory = bridge.getCurrentCategory();
                Map<Integer, ResourceReference> currentCategoryContents;
                try {
                    currentCategoryContents =
                            Objects.requireNonNull(bridge.getContents().get(currentCategory));
                    buttonIsMapped = currentCategoryContents.containsKey(id);
                } catch (NullPointerException e) {
                    Log.e(TAG, "Received a button press but contents not set up. Did you clearAllContents at setup?");
                    e.printStackTrace();
                    return;
                }
                if (buttonIsMapped)
                    try {
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                        boolean noHaptic = settings.getBoolean(ctx.getResources().getString(R.string.preference_no_haptic), false);
                        if (!noHaptic) {
                            Vibrator vibrator = (Vibrator) ctx.getSystemService(VIBRATOR_SERVICE);
                            vibrator.vibrate(1);
                        }

                        if (checkWifiNotConnected(ctx)) {
                            Toast.makeText(ctx, ctx.getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
                            return;
                        }

                        currentlyClicked.add(id);

                        ResourceReference ref = Objects.requireNonNull(currentCategoryContents.get(id));
                        new Thread(() -> bridge.getResource(ref).activateResource(ctx)).start();
                    } catch (NullPointerException e) {
                        Log.e(TAG, "Received a button press. The button is mapped. But, there is no instance of HueBridge or failed to get the mapping for the button");
                        e.printStackTrace();
                    }
                else {
                    startEditActivity(ctx);
                    return;
                }
                break;
            case 1:
                switch (id) {
                    case R.id.btnCategory1:
                        bridge.setCurrentCategory(menuCategory.QUICK_ACCESS);
                        break;
                    case R.id.btnCategory2:
                        bridge.setCurrentCategory(menuCategory.LIGHTS);
                        break;
                    case R.id.btnCategory3:
                        bridge.setCurrentCategory(menuCategory.ROOMS);
                        break;
                    case R.id.btnCategory4:
                        bridge.setCurrentCategory(menuCategory.ZONES);
                        break;
                    case R.id.btnCategory5:
                        bridge.setCurrentCategory(menuCategory.SCENES);
                        break;
                    case R.id.btnSlidersCategory1:
                        bridge.setCurrentSlidersCategory(slidersCategory.BRIGHTNESS);
                        break;
                    case R.id.btnSlidersCategory2:
                        bridge.setCurrentSlidersCategory(slidersCategory.COLOR);
                        break;
                    case R.id.btnSlidersCategory3:
                        bridge.setCurrentSlidersCategory(slidersCategory.SATURATION);
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
                saveAllConfiguration(ctx);
                break;
            case 2:
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                boolean noHaptic = settings.getBoolean(ctx.getResources().getString(R.string.preference_no_haptic), false);
                if (!noHaptic) {
                    Vibrator vibrator = (Vibrator) ctx.getSystemService(VIBRATOR_SERVICE);
                    vibrator.vibrate(1);
                }

                if (checkWifiNotConnected(ctx)) {
                    Toast.makeText(ctx, ctx.getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
                    return;
                }

                ResourceReference resRef = getSlidersResource();
                BridgeResourceSliders res = (BridgeResourceSliders) bridge.getResource(resRef);
                int value;
                switch (id) {
                    case R.id.sliders_bri:
                        value = intent.getIntExtra("bri", 0);
                        new Thread(() -> res.setBri(ctx, value)).start();
                        break;
                    case R.id.sliders_hue:
                        value = intent.getIntExtra("hue", 0);
                        new Thread(() -> res.setHue(ctx, value)).start();
                        break;
                    case R.id.sliders_sat:
                        value = intent.getIntExtra("sat", 0);
                        new Thread(() -> res.setSat(ctx, value)).start();
                        break;
                    default:
                        Log.e(TAG, "Unknown category!");
                        break;
                }
                //String toastString = String.format(ctx.getResources().getString(R.string.remote_list_item_clicked), itemId);
                //Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show(); // Debug toast for presses on the sliders buttons.
            break;
        }
        panelUpdate(ctx);
    }

    /**
     * Perform long click
     * @param ctx Context
     * @param intent Intent
     */
    private void performRemoteLongClick(Context ctx, Intent intent) {
        if(!bridgeConfigured)
            return;
        Log.d(TAG, "performRemoteLongClick()");

        int id = intent.getIntExtra("id", -1);
        int key = intent.getIntExtra("key", -1);

        HueBridge bridge;
        try {
            bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
        } catch (NullPointerException e){
            Log.e(TAG, "Performing long click on but no HueBridge instance is found");
            e.printStackTrace();
            return;
        }
        menuCategory currentCategory = bridge.getCurrentCategory();
        if(key == 0){
            boolean buttonIsMapped = false;
            try{
                Map<Integer, ResourceReference> currentCategoryContents =
                        Objects.requireNonNull(
                                Objects.requireNonNull(
                                        HueBridge.getInstance(ctx)).
                                        getContents().
                                        get(currentCategory));
                buttonIsMapped = currentCategoryContents.containsKey(id);
            }
            catch (NullPointerException e){
                Log.e(TAG, "Received a button press but contents not set up. Did you clearAllContents at setup?");
                e.printStackTrace();
            }
            if(buttonIsMapped){
                ResourceReference resRef;
                BridgeResource res;
                try {
                    resRef = Objects.requireNonNull(
                            Objects.requireNonNull(
                                    bridge.
                                    getContents().
                                    get(currentCategory)).
                                    get(id));
                    res = bridge.getResource(resRef);
                    // If a scene, get resRef for the attached group and activate scene
                    if (res.getCategory().equals("scenes")) {
                        new Thread(() -> res.activateResource(ctx)).start();
                        resRef = new ResourceReference("groups", ((SceneResource) res).getGroup());
                    }
                }
                catch (NullPointerException e){
                    Log.e(TAG, "Received a button press. The button is mapped. Failed to get the mapping for the button");
                    e.printStackTrace();
                    return;
                }

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                boolean noHaptic = settings.getBoolean(ctx.getResources().getString(R.string.preference_no_haptic), false);
                if(!noHaptic) {
                    Vibrator vibrator = (Vibrator) ctx.getSystemService(VIBRATOR_SERVICE);
                    vibrator.vibrate(1);
                }

                if(checkWifiNotConnected(ctx)) {
                    Toast.makeText(ctx, ctx.getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
                    return;
                }

                setSlidersResource(resRef);
                setSlidersActive(true);
            }
            else {
                startEditActivity(ctx);
                return;
            }
        }
        panelUpdate(ctx);
    }

    /**
     * Perform pull to refresh action
     * @param ctx Context
     */
    private void performPullToRefresh(Context ctx) {
        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));
        cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.refreshArea);
        // If no wifi connected, show Toast
        if (checkWifiNotConnected(ctx)) {
            Toast.makeText(ctx, ctx.getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
            // Update panel to attach a new pull to refresh intent
            panelUpdate(ctx);
        }
    }

    /**
     * Perform the quick setup for the buttons
     * @param ctx Context
     */
    public static void quickSetup(Context ctx) {
        Log.d(TAG, "quickSetup entered");

        //The HueBridge instance
        HueBridge bridge;
        try {
            bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
        } catch (NullPointerException e){
            Log.e(TAG, "Tried to perform quick setup but no instance of HueBridge was found");
            e.printStackTrace();
            return;
        }
        Map<String, ? extends BridgeResource> map;
        Map<Integer, ResourceReference> quickAccessContents;
        Map<Integer, ResourceReference> lightsContents;
        Map<Integer, ResourceReference> roomsContents;
        Map<Integer, ResourceReference> zonesContents;
        Map<Integer, ResourceReference> scenesContents;
        Map<menuCategory, Map<Integer, ResourceReference>> contents = bridge.getContents();
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

        ResourceReference allResRef = BridgeCatalogue.getGroup0Ref();

        int buttonIndex = 0;
        int qaButtonIndex = 0;

        quickAccessContents.put(qaButtonIndex++, allResRef);
        map = bridge.getBridgeState().getLights();
        Log.d(TAG, "quickSetup getLights() size: " + map.size());
        for (Map.Entry<String, ? extends BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for lights on id: " + entry.getKey());
            BridgeResource res = entry.getValue();
            ResourceReference resRef = new ResourceReference(res.getCategory(), res.getId());
            lightsContents.put(buttonIndex++, resRef);
            if(qaButtonIndex < 3) {
                quickAccessContents.put(qaButtonIndex++, resRef);
            }
        }

        buttonIndex = 0;
        roomsContents.put(buttonIndex++, allResRef);
        map = bridge.getBridgeState().getRooms();
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
                    quickAccessContents.put(qaButtonIndex++, resRef);
                }
            }
        }

        buttonIndex = 0;
        zonesContents.put(buttonIndex++, allResRef);
        map = bridge.getBridgeState().getZones();
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
                    quickAccessContents.put(qaButtonIndex++, resRef);
                }
            }
        }

        buttonIndex = 0;
        map = bridge.getBridgeState().getScenes();
        Log.d(TAG, "quickSetup getScenes() size: " + map.size());
        for (Map.Entry<String, ? extends BridgeResource> entry : map.entrySet()) {
            if(buttonIndex >= 10)
                break;
            Log.d(TAG, "quickSetup for scenes on id: " + entry.getKey());
            BridgeResource res = entry.getValue();
            ResourceReference resRef = new ResourceReference(res.getCategory(), res.getId());
            scenesContents.put(buttonIndex++, resRef);
            if(qaButtonIndex < 9) {
                quickAccessContents.put(qaButtonIndex++, resRef);
            }
        }
        saveAllConfiguration(ctx);
    }

    /**
     * Update both help and content panels
     * @param ctx Context
     */
    private void panelUpdate(Context ctx){
        Log.d(TAG, "panelUpdate()");

        //One remoteView for left/help and one for right/content
        RemoteViews contentView;
        RemoteViews helpView;

        //
        if (isSlidersActive()) {
            contentView = createSlidersContentView(ctx);
            helpView = createSlidersHelpView(ctx);

            final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
            final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));
            cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
            return;
        }

        contentView = createContentView(ctx);
        helpView = createHelpView(ctx);

        if(bridgeConfigured) {
            HueBridge bridge;
            try {
                bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
            } catch (NullPointerException e){
                Log.e(TAG, "Updating panel. bridgeConfigured is true but no HueBridge instance is found");
                e.printStackTrace();
                return;
            }
            menuCategory currentCategory = bridge.getCurrentCategory();
            for (int i = 0; i < 10; i++) {
                if (bridge.getContents().containsKey(currentCategory)) {
                    Map<Integer, ResourceReference> currentCategoryContents;
                    try {
                        currentCategoryContents = Objects.requireNonNull(bridge.getContents().get(currentCategory));
                    } catch (NullPointerException e){
                        Log.e(TAG, "Trying to update panel but failed to get current category contents");
                        e.printStackTrace();
                        return;
                    }
                    boolean slotIsFilled = currentCategoryContents.containsKey(i);
                    contentView.setViewVisibility(progressBarArr[i], View.GONE);
                    if (slotIsFilled) {
                        ResourceReference ref;
                        try {
                            ref = Objects.requireNonNull(currentCategoryContents.get(i));
                        } catch (NullPointerException e){
                            e.printStackTrace();
                            return;
                        }
                        BridgeResource resource = bridge.getResource(ref);
                        if (resource == null) {
                            Log.e(TAG, "resource == null");
                            currentCategoryContents.remove(i--);
                        } else {
                            contentView.setTextViewText(btnTextArr[i], resource.getUnderBtnText());
                            contentView.setTextViewText(btnTopTextArr[i], resource.getBtnText(ctx));
                            contentView.setTextColor(btnTopTextArr[i], resource.getBtnTextColor(ctx));
                            contentView.setInt(btnArr[i], "setBackgroundResource",
                                    resource.getBtnBackgroundResource());
                            //contentView.setFloat(btnTopTextArr[i], "setTextSize", 8);
                            contentView.setTextViewTextSize(btnTopTextArr[i], TypedValue.COMPLEX_UNIT_PX, ctx.getResources().getDimensionPixelSize(resource.getBtnTextSize(ctx)));
                        }
                    } else {
                        contentView.setTextViewText(btnTextArr[i], "");
                        contentView.setTextViewText(btnTopTextArr[i], ctx.getResources().getString(R.string.plus_symbol));
                        //contentView.setFloat(btnTopTextArr[i], "setTextSize", 8);
                        contentView.setTextViewTextSize(btnTopTextArr[i], TypedValue.COMPLEX_UNIT_PX, ctx.getResources().getDimension(R.dimen.resource_btn_text_size_symbol));
                        contentView.setTextColor(btnTopTextArr[i], (ContextCompat.getColor(ctx, R.color.white)));
                        contentView.setInt(btnArr[i], "setBackgroundResource",
                                R.drawable.add_button_background);
                    }
                }
            }
            if (!currentlyClicked.isEmpty()){
                for(int id : currentlyClicked)
                    contentView.setViewVisibility(progressBarArr[id], View.VISIBLE);
            }
        }

        final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
        final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));

        //Set pull refresh
        PendingIntent pendingIntent = getRefreshIntent(ctx);
        SlookCocktailManager.getInstance(ctx).setOnPullPendingIntent(cocktailIds[0], R.id.refreshArea, pendingIntent);

        cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
    }

    /*public static void saveCurrentCategory(Context ctx){
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
        editor.apply();
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
        } catch (NullPointerException e){
            String toastString = "Config file not found";
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            Log.e(TAG, toastString);
        }
    }*/

    /**
     * Save and write the HueBridge instance to the memory
     * @param ctx Context
     */
    public static void saveAllConfiguration(Context ctx) {
        File preferenceFile = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.preference_file_key));
        File recoveryFile = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.recovery_file_key));
        try {
            Log.d(TAG, "saveConfigurationToMemory()");
            HueBridge bridge;
            try {
                bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
            }
            catch (NullPointerException e){
                Log.e(TAG, "Tried to save, no instance of HueBridge found");
                return;
            }

            ObjectOutputStream preferenceOutputStream = new ObjectOutputStream(new FileOutputStream(preferenceFile));
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<HueBridge> jsonAdapter = moshi.adapter(HueBridge.class);
            String bridgeString = jsonAdapter.toJson(bridge);
            preferenceOutputStream.writeObject(bridgeString);
            preferenceOutputStream.flush();
            preferenceOutputStream.close();

            // Recovery
            ObjectOutputStream recoveryOutputStream = new ObjectOutputStream(new FileOutputStream(recoveryFile));
            recoveryOutputStream.writeObject(bridge.getContents());
            recoveryOutputStream.flush();
            recoveryOutputStream.close();
        } catch (Exception e) {
            Log.e(TAG,"Failed to save configuration");
            e.printStackTrace();
        }
    }

    /**
     * Read and set the HueBridge instance from the memory
     * @param ctx Context
     */
    public static void loadAllConfiguration(Context ctx) {
        Log.d(TAG, "loadConfigurationFromMemory()");
        //loadCurrentCategory(ctx);
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean bridgeConfigured = s.getBoolean(ctx.getString(R.string.preference_bridge_configured), false);
        if (!bridgeConfigured)
            return;

        File configFile = new File(ctx.getDir("data", MODE_PRIVATE), ctx.getResources().getString(R.string.preference_file_key));
        ObjectInputStream configInputStream;

        // Load config file
        try {
            configInputStream = new ObjectInputStream(new FileInputStream(configFile));
        } catch (FileNotFoundException e){
            Log.e(TAG, "Config file not found");
            return;
        } catch (IOException e) {
            Log.e(TAG, "IOException");
            e.printStackTrace();
            return;
        }

        // Load instance of HueBridge
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<HueBridge> jsonAdapter = moshi.adapter(HueBridge.class);
        HueBridge bridge;
        try {
            String bridgeString = Objects.requireNonNull(configInputStream).readObject().toString();
            bridge = jsonAdapter.fromJson(bridgeString);
            HueBridge.setInstance(ctx, bridge);
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
                if(ip.equals("") || userName.equals(""))
                    return;
                Map<menuCategory, Map<Integer, ResourceReference>> contents =
                        Objects.requireNonNull(
                            (HashMap<menuCategory, Map<Integer, ResourceReference>>)
                                    recoveryInputStream.readObject());
                HueBridge.getInstance(ctx, ip, userName);
                bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
                bridge.setContents(contents);
                HueBridge.requestHueState(ctx);
                Log.i(TAG,"Recovery successful");
            } catch (FileNotFoundException ex){
                Log.e(TAG, "Recovery file not found");
                deleteAllConfiguration(ctx);
            } catch (ClassCastException | ClassNotFoundException | IOException ex){
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
        return file.delete();
    }

    public boolean checkWifiNotConnected(Context ctx) {
        WifiManager wifiMgr = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr.isWifiEnabled())// Wi-Fi adapter is ON
            return wifiMgr.getConnectionInfo().getNetworkId() == -1;
        else
            return true; // Wi-Fi adapter is OFF
    }
}
