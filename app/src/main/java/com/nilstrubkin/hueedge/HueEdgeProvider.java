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
import com.nilstrubkin.hueedge.resources.BridgeResource;
import com.nilstrubkin.hueedge.resources.BridgeResourceSliders;
import com.nilstrubkin.hueedge.resources.SceneResource;
import com.nilstrubkin.hueedge.service.*;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

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
            R.id.btnSlidersCategory3, R.id.btnSlidersCategory4};
    //Array of references to category buttons underlines
    public static final int[] btnSlidersCategoryLineArr = {R.id.btnSlidersCategoryLine1, R.id.btnSlidersCategoryLine2,
            R.id.btnSlidersCategoryLine3, R.id.btnSlidersCategoryLine4};
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
        SATURATION,
        TEMPERATURE
    }

    private static boolean slidersActive = false;

    private static HueBridge bridge;

    private static ResourceReference slidersResource;
    private static int slidersResHue;
    private static int slidersResSat;

    private static final List<Integer> currentlyClicked = new ArrayList<>();

    private static OkHttpClient client;

    private static HueBridge getBridge(Context ctx) {
        if (bridge == null)
            bridge = HueBridge.getInstance(ctx);
        return bridge;
    }

    public static void setBridge(HueBridge bridge) {
        HueEdgeProvider.bridge = bridge;
    }

    public static OkHttpClient getClient() {
        if (client == null)
            client = new OkHttpClient.Builder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build();
        return client;
    }

    private static boolean isBridgeNull(){
        return bridge == null;
    }

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

        String action = intent.getAction();

        if(action == null)
            return;
        Log.d(TAG, "onReceive: " + action);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean bridgeConfigured = settings.getBoolean(ctx.getResources().getString(R.string.preference_bridge_configured), false);

        if (!bridgeConfigured)
            bridge = null;
        else if (isBridgeNull()) {
            panelUpdate(ctx);
            getBridge(ctx);
            if (isBridgeNull()){
                Log.e(TAG, "preference_bridge_configured incorrectly set to true, no HueBridge found");
                HueBridge.deleteInstance(ctx);
            }
            panelUpdate(ctx);
        }

        switch (action) {
            case ACTION_REMOTE_LONG_CLICK:
                performRemoteLongClick(ctx, intent);
                break;
            case ACTION_REMOTE_CLICK:
                performRemoteClick(ctx, intent);
                break;
            case ACTION_PULL_TO_REFRESH:
                currentlyClicked.clear();
                panelUpdate(ctx);
                if (checkWifiNotEnabled(ctx)){
                    boolean noWifiErrMsg = settings.getBoolean(ctx.getResources().getString(R.string.preference_no_wifi_err_msg), false);
                    if(!noWifiErrMsg) Toast.makeText(ctx, ctx.getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
                    performPullToRefresh(ctx);
                }
                else HueBridge.requestHueState(ctx);
                break;
            case ACTION_RECEIVE_HUE_REPLY:
                HueBridge.requestHueState(ctx);
                break;
            case ACTION_TIMEOUT_HUE_REPLY:
                boolean noWifiErrMsg = settings.getBoolean(ctx.getResources().getString(R.string.preference_no_wifi_err_msg), false);
                if (!noWifiErrMsg) Toast.makeText(ctx, ctx.getString(R.string.toast_timeout_reply), Toast.LENGTH_LONG).show();
                currentlyClicked.clear();
                panelUpdate(ctx);
                break;
            case COCKTAIL_VISIBILITY_CHANGED:
                panelUpdate(ctx);
                break;
            case ACTION_RECEIVE_HUE_STATE:
                performPullToRefresh(ctx);
                currentlyClicked.clear();
                panelUpdate(ctx);
                HueBridge.saveAllConfiguration(ctx);
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
    }


    /**
     * Samsung SDK
     * This method is called when the instance of Edge Single Plus Mode is deleted from the enabled list
     * @param ctx Context
     */
    @Override
    public void onDisabled(Context ctx) {
        super.onDisabled(ctx);
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor e = s.edit();
        e.remove(ctx.getString(R.string.preference_tips_shown));
        e.apply();
    }

    /**
     * Samsung SDK
     * This method is called every now and then
     * @param ctx Context
     * @param cocktailManager ignored
     * @param cocktailIds ignored
     */
    @Override
    public void onUpdate(Context ctx, SlookCocktailManager cocktailManager, int[] cocktailIds) {
        super.onUpdate(ctx, cocktailManager, cocktailIds);
        Log.d(TAG, "onUpdate()");
        panelUpdate(ctx);
    }

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
        if(!isBridgeNull() && visibility == 1) {
            currentlyClicked.clear();
            panelUpdate(ctx);
            displayTips(ctx);
            if (checkWifiNotEnabled(ctx)) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                boolean noWifiErrMsg = settings.getBoolean(ctx.getResources().getString(R.string.preference_no_wifi_err_msg), false);
                if (!noWifiErrMsg) Toast.makeText(ctx, ctx.getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
            }
            else HueBridge.requestHueState(ctx);
        }
    }

    /**
     * Create the content view, right panel, used for buttons
     * @param ctx Context
     * @return RemoteViews
     */
    private RemoteViews createContentView(Context ctx) {
        RemoteViews contentView = new RemoteViews(ctx.getPackageName(), R.layout.main_view);

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
        menuCategory currentCategory = getBridge(ctx).getCurrentCategory(ctx);
        if(getBridge(ctx).getContents().containsKey(currentCategory)) {
            Map<Integer, ResourceReference> currentCategoryContents = getBridge(ctx).getContents().get(currentCategory);
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
        for(int button : btnCategoryArr){
            helpView.setOnClickPendingIntent(button, getClickIntent(ctx, button, 1));
            helpView.setTextColor(button, ctx.getColor(R.color.category_unselected_gray));
        }
        for (int line : btnCategoryLineArr){
            helpView.setViewVisibility(line, View.GONE);
        }
        if(!isBridgeNull()){
            int selectedNumber = getBridge(ctx).getCurrentCategory(ctx).ordinal();
            int currentButton = btnCategoryArr[selectedNumber];
            int currentLine = btnCategoryLineArr[selectedNumber];
            helpView.setTextColor(currentButton, ctx.getColor(R.color.category_selected_blue));
            helpView.setViewVisibility(currentLine, View.VISIBLE);
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

        ResourceReference resRef = getSlidersResource();
        BridgeResourceSliders res = (BridgeResourceSliders) getBridge(ctx).getResource(resRef);
        setSlidersResHue(res.getHue());
        setSlidersResSat(res.getSat());

        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));

        remoteListView.setViewVisibility(R.id.sliders_bri, View.GONE);
        remoteListView.setViewVisibility(R.id.sliders_hue, View.GONE);
        remoteListView.setViewVisibility(R.id.sliders_sat, View.GONE);
        remoteListView.setViewVisibility(R.id.sliders_ct, View.GONE);

        switch (getBridge(ctx).getCurrentSlidersCategory(ctx)) {
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
            case TEMPERATURE:
                Intent temperatureIntent = new Intent(ctx, LongClickCtSliderService.class);
                remoteListView.setRemoteAdapter(R.id.sliders_ct, temperatureIntent);
                cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.sliders_ct);
                remoteListView.setViewVisibility(R.id.sliders_ct, View.VISIBLE);
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
        remoteListView.setPendingIntentTemplate(R.id.sliders_ct, getClickIntent(ctx, R.id.sliders_ct, 2));
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

        for( int button : btnSlidersCategoryArr){
            helpView.setOnClickPendingIntent(button, getClickIntent(ctx, button, 1));
            helpView.setTextColor(button, ctx.getColor(R.color.category_unselected_gray));
        }
        for (int line : btnSlidersCategoryLineArr){
            helpView.setViewVisibility(line, View.GONE);
        }
        slidersCategory currentSlidersCategory = getBridge(ctx).getCurrentSlidersCategory(ctx);
        if(currentSlidersCategory != null){
            int currentButton = btnSlidersCategoryArr[currentSlidersCategory.ordinal()];
            int currentLine = btnSlidersCategoryLineArr[currentSlidersCategory.ordinal()];
            helpView.setTextColor(currentButton, ctx.getColor(R.color.category_selected_blue));
            helpView.setViewVisibility(currentLine, View.VISIBLE);
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

        if(isBridgeNull())
            return;

        switch (key) {
            // Key 0 for round buttons in content view
            case 0:
                boolean buttonIsMapped;
                menuCategory currentCategory = getBridge(ctx).getCurrentCategory(ctx);
                Map<Integer, ResourceReference> currentCategoryContents;
                try {
                    currentCategoryContents =
                            Objects.requireNonNull(getBridge(ctx).getContents().get(currentCategory));
                    buttonIsMapped = currentCategoryContents.containsKey(id);
                } catch (NullPointerException e) {
                    Log.e(TAG, "Received a button press but contents not set up. Did you clearAllContents at setup?");
                    e.printStackTrace();
                    return;
                }
                if (buttonIsMapped)
                    try {
                        vibrate(ctx);

                        if (checkWifiNotEnabled(ctx)) {
                            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                            boolean noWifiErrMsg = settings.getBoolean(ctx.getResources().getString(R.string.preference_no_wifi_err_msg), false);
                            if (!noWifiErrMsg) Toast.makeText(ctx, ctx.getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
                            return;
                        }

                        currentlyClicked.add(id);

                        setTipsDone(ctx, 1);
                        ResourceReference ref = Objects.requireNonNull(currentCategoryContents.get(id));
                        new Thread(() -> getBridge(ctx).getResource(ref).activateResource(ctx)).start();
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
                        getBridge(ctx).setCurrentCategory(ctx, menuCategory.QUICK_ACCESS);
                        break;
                    case R.id.btnCategory2:
                        getBridge(ctx).setCurrentCategory(ctx, menuCategory.LIGHTS);
                        break;
                    case R.id.btnCategory3:
                        getBridge(ctx).setCurrentCategory(ctx, menuCategory.ROOMS);
                        break;
                    case R.id.btnCategory4:
                        getBridge(ctx).setCurrentCategory(ctx, menuCategory.ZONES);
                        break;
                    case R.id.btnCategory5:
                        getBridge(ctx).setCurrentCategory(ctx, menuCategory.SCENES);
                        break;
                    case R.id.btnSlidersCategory1:
                        getBridge(ctx).setCurrentSlidersCategory(ctx, slidersCategory.BRIGHTNESS);
                        break;
                    case R.id.btnSlidersCategory2:
                        getBridge(ctx).setCurrentSlidersCategory(ctx, slidersCategory.COLOR);
                        break;
                    case R.id.btnSlidersCategory3:
                        getBridge(ctx).setCurrentSlidersCategory(ctx, slidersCategory.SATURATION);
                        break;
                    case R.id.btnSlidersCategory4:
                        getBridge(ctx).setCurrentSlidersCategory(ctx, slidersCategory.TEMPERATURE);
                        break;
                    case R.id.btnBack:
                        setSlidersActive(false);
                        break;
                    case R.id.btnEdit:
                        //HueBridge.loadAllConfiguration(ctx); // rebind for quick way to debug loadAllConfiguration()
                        startEditActivity(ctx);
                        break;
                    default:
                        break;
                }
                //HueBridge.saveAllConfiguration(ctx);
                if (!checkWifiNotEnabled(ctx))
                    new Thread(() -> HueBridge.requestHueState(ctx)).start();
                currentlyClicked.clear();
                break;
            case 2:
                vibrate(ctx);

                if (checkWifiNotEnabled(ctx)) {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                    boolean noWifiErrMsg = settings.getBoolean(ctx.getResources().getString(R.string.preference_no_wifi_err_msg), false);
                    if (!noWifiErrMsg) Toast.makeText(ctx, ctx.getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
                    return;
                }

                ResourceReference resRef = getSlidersResource();
                BridgeResourceSliders res = (BridgeResourceSliders) getBridge(ctx).getResource(resRef);
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
                    case R.id.sliders_ct:
                        value = intent.getIntExtra("ct", 0);
                        new Thread(() -> res.setCt(ctx, value)).start();
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
        Log.d(TAG, "performRemoteLongClick()");

        if (isBridgeNull())
            return;

        int id = intent.getIntExtra("id", -1);
        int key = intent.getIntExtra("key", -1);

        menuCategory currentCategory = getBridge(ctx).getCurrentCategory(ctx);
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
                vibrate(ctx);
                ResourceReference resRef;
                BridgeResource res;
                try {
                    resRef = Objects.requireNonNull(
                            Objects.requireNonNull(
                                    getBridge(ctx).
                                    getContents().
                                    get(currentCategory)).
                                    get(id));
                    res = getBridge(ctx).getResource(resRef);
                    // If a scene, get resRef for the attached group and activate scene
                    if (res.getCategory().equals("scenes")) {
                        if(checkWifiNotEnabled(ctx)) {
                            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                            boolean noWifiErrMsg = settings.getBoolean(ctx.getResources().getString(R.string.preference_no_wifi_err_msg), false);
                            if (!noWifiErrMsg) Toast.makeText(ctx, ctx.getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
                        }
                        else
                            new Thread(() -> res.activateResource(ctx)).start();
                        resRef = new ResourceReference("groups", ((SceneResource) res).getGroup());
                    }
                }
                catch (NullPointerException e){
                    Log.e(TAG, "Received a button press. The button is mapped. Failed to get the mapping for the button");
                    e.printStackTrace();
                    return;
                }
                setTipsDone(ctx, 2);
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
     * Finalize the spinning animation on the top after pull to refresh
     * @param ctx Context
     */
    private void performPullToRefresh(Context ctx) {
        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));
        cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.refreshArea);
        // If no wifi connected, show Toast
        if (checkWifiNotEnabled(ctx))
            // Update panel to attach a new pull to refresh intent
            panelUpdate(ctx);
    }

    /**
     * Update both help and content panels
     * @param ctx Context
     */
    private void panelUpdate(Context ctx){
        Log.d(TAG, "panelUpdate()");

        final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(ctx);
        final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(ctx, HueEdgeProvider.class));

        RemoteViews contentView;
        RemoteViews helpView;

        if (isBridgeNull()){
            helpView = createHelpView(ctx);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
            boolean bridgeConfigured = settings.getBoolean(ctx.getResources().getString(R.string.preference_bridge_configured), false);
            if (bridgeConfigured)
                contentView = new RemoteViews(ctx.getPackageName(),R.layout.main_view_loading);
            else {
                Log.d(TAG, "Creating content view, no bridge found, will display main_view_no_bridge");
                contentView = new RemoteViews(ctx.getPackageName(),
                        R.layout.main_view_no_bridge); // R.layout.main_view_demo); TODO demo
                contentView.setOnClickPendingIntent(R.id.configureButton,
                        getClickIntent(ctx, R.id.configureButton, 1));
            }
        } else if (isSlidersActive()){
            contentView = createSlidersContentView(ctx);
            helpView = createSlidersHelpView(ctx);
        } else {
            //One remoteView for left/help and one for right/content
            contentView = createContentView(ctx);
            helpView = createHelpView(ctx);

            //Set pull refresh
            PendingIntent pendingIntent = getRefreshIntent(ctx);
            SlookCocktailManager.getInstance(ctx).setOnPullPendingIntent(cocktailIds[0], R.id.refreshArea, pendingIntent);

            menuCategory currentCategory = getBridge(ctx).getCurrentCategory(ctx);
            for (int i = 0; i < 10; i++) {
                if (getBridge(ctx).getContents().containsKey(currentCategory)) {
                    Map<Integer, ResourceReference> currentCategoryContents;
                    try {
                        currentCategoryContents = Objects.requireNonNull(getBridge(ctx).getContents().get(currentCategory));
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
                        BridgeResource resource = getBridge(ctx).getResource(ref);
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

        cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
    }

    public static boolean checkWifiNotEnabled(Context ctx) {
        WifiManager wifiMgr = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return !wifiMgr.isWifiEnabled();
    }

    private void displayTips(Context ctx){
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(ctx);
        int tipsShown = s.getInt(ctx.getString(R.string.preference_tips_shown), 0);
        if (tipsShown < 6){
            SharedPreferences.Editor e = s.edit();
            if (tipsShown < 3)
                Toast.makeText(ctx, ctx.getString(R.string.toast_tips_toggle), Toast.LENGTH_LONG).show();
            else
                Toast.makeText(ctx, ctx.getString(R.string.toast_tips_long), Toast.LENGTH_LONG).show();
            e.putInt(ctx.getString(R.string.preference_tips_shown), ++tipsShown);
            e.apply();
        }
    }

    private void setTipsDone(Context ctx, int i){
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(ctx);
        int tipsShown = s.getInt(ctx.getString(R.string.preference_tips_shown), 0);
        if (tipsShown < 6) {
            SharedPreferences.Editor e = s.edit();
            switch (i) {
                case 1:
                    if (tipsShown < 3) {
                        tipsShown = 3;
                    }
                    break;
                case 2:
                    tipsShown = 6;
                    break;
            }
            e.putInt(ctx.getString(R.string.preference_tips_shown), tipsShown);
            e.apply();
        }
    }

    public static void vibrate(Context ctx){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean noHaptic = settings.getBoolean(ctx.getResources().getString(R.string.preference_no_haptic), false);
        if(!noHaptic) {
            Vibrator vibrator = (Vibrator) ctx.getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(1);
        }
    }
}
