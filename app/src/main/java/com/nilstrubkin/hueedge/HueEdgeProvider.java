package com.nilstrubkin.hueedge;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Vibrator;
import androidx.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
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
    //private static long ts; // TODO remove timestamp

    private static final String ACTION_REMOTE_LONG_CLICK = "com.nilstrubkin.hueedge.ACTION_REMOTE_LONG_CLICK";
    private static final String ACTION_REMOTE_CLICK = "com.nilstrubkin.hueedge.ACTION_REMOTE_CLICK";
    private static final String ACTION_PULL_TO_REFRESH = "com.nilstrubkin.hueedge.ACTION_PULL_TO_REFRESH";
    protected static final String ACTION_RECEIVE_HUE_STATE = "com.nilstrubkin.hueedge.ACTION_RECEIVE_HUE_STATE";
    protected static final String ACTION_TIMEOUT_HUE_REPLY = "com.nilstrubkin.hueedge.ACTION_TIMEOUT_HUE_REPLY";
    private static final String COCKTAIL_VISIBILITY_CHANGED = "com.samsung.android.cocktail.action.COCKTAIL_VISIBILITY_CHANGED";

    //Array of references to buttons
    public static final int[] btnArr = {R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn10};
    //Array of references to button texts (text on the button itself)
    public static final int[] btnTopTextArr = {R.id.btn1topText, R.id.btn2topText, R.id.btn3topText, R.id.btn4topText, R.id.btn5topText,
            R.id.btn6topText, R.id.btn7topText, R.id.btn8topText, R.id.btn9topText, R.id.btn10topText};
    //Array of references to button texts (text under the button itself)
    public static final int[] btnTextArr = {R.id.btn1text, R.id.btn2text, R.id.btn3text, R.id.btn4text, R.id.btn5text,
            R.id.btn6text, R.id.btn7text, R.id.btn8text, R.id.btn9text, R.id.btn10text};
    //Array of references to category buttons
    public static final int[] btnCategoryArr = {R.id.btnCategory1, R.id.btnCategory2,
            R.id.btnCategory3, R.id.btnCategory4, R.id.btnCategory5};
    //Array of references to category buttons underlines
    public static final int[] btnCategoryLineArr = {R.id.btnCategoryLine1, R.id.btnCategoryLine2,
            R.id.btnCategoryLine3, R.id.btnCategoryLine4, R.id.btnCategoryLine5};
    //Array of references to delete buttons in Edit activity
    public static final int[] btnDeleteArr = {R.id.btn1delete, R.id.btn2delete, R.id.btn3delete, R.id.btn4delete, R.id.btn5delete,
            R.id.btn6delete, R.id.btn7delete, R.id.btn8delete, R.id.btn9delete, R.id.btn10delete};
    //Array of references to icon buttons in Edit activity
    public static final int[] btnIconArr = {R.id.btn1icon, R.id.btn2icon, R.id.btn3icon, R.id.btn4icon, R.id.btn5icon,
            R.id.btn6icon, R.id.btn7icon, R.id.btn8icon, R.id.btn9icon, R.id.btn10icon};
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

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build();

    private static HueBridge getBridge(Context ctx) {
        if (isBridgeNull()) bridge = HueBridge.getInstance(ctx);
        return bridge;
    }

    public static void setBridge(HueBridge bridge) {
        HueEdgeProvider.bridge = bridge;
    }

    public static OkHttpClient getClient() {
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
        String action = intent.getAction();

        if(action == null) return;
        Log.d(TAG, "onReceive: " + action);
        //long tn = System.currentTimeMillis(); //TODO remove
        //Toast.makeText(ctx, action.substring(24) + ":" + (tn - ts), Toast.LENGTH_SHORT).show(); //TODO remove
        //ts = tn; //TODO remove

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean bridgeConfigured = settings.getBoolean(ctx.getResources().getString(R.string.preference_bridge_configured), false);

        if (!bridgeConfigured) bridge = null;
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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean legacyHelpView = settings.getBoolean(ctx.getResources().getString(R.string.preference_legacy_help_view), false);
        RemoteViews helpView = new RemoteViews(ctx.getPackageName(),
                !legacyHelpView ? R.layout.help_view : R.layout.help_view_legacy);
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
            if(!legacyHelpView) {
                helpView.setTextColor(currentButton, ctx.getColor(R.color.category_selected_white));
                // a bit of voodoo magic to make text bold
                String text = null;
                switch (selectedNumber){
                    case 0:
                        text = ctx.getString(R.string.button_help_1_text);
                        break;
                    case 1:
                        text = ctx.getString(R.string.button_help_2_text);
                        break;
                    case 2:
                        text = ctx.getString(R.string.button_help_3_text);
                        break;
                    case 3:
                        text = ctx.getString(R.string.button_help_4_text);
                        break;
                    case 4:
                        text = ctx.getString(R.string.button_help_5_text);
                        break;
                }
                SpannableString mspInt = new SpannableString(text);
                mspInt.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                helpView.setTextViewText(currentButton, mspInt);
            } else {
                helpView.setTextColor(currentButton, ctx.getColor(R.color.category_selected_blue_legacy));
            }
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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean legacyHelpView = settings.getBoolean(ctx.getResources().getString(R.string.preference_legacy_help_view), false);
        RemoteViews helpView = new RemoteViews(ctx.getPackageName(),
                !legacyHelpView ? R.layout.sliders_help_view : R.layout.sliders_help_view_legacy);
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
            int selectedNumber = getBridge(ctx).getCurrentSlidersCategory(ctx).ordinal();
            int currentButton = btnSlidersCategoryArr[selectedNumber];
            int currentLine = btnSlidersCategoryLineArr[selectedNumber];
            if(!legacyHelpView) {
                helpView.setTextColor(currentButton, ctx.getColor(R.color.category_selected_white));
                // a bit of voodoo magic to make text bold
                String text = null;
                switch (selectedNumber){
                    case 0:
                        text = ctx.getString(R.string.button_sliders_help_bri);
                        break;
                    case 1:
                        text = ctx.getString(R.string.button_sliders_help_hue);
                        break;
                    case 2:
                        text = ctx.getString(R.string.button_sliders_help_sat);
                        break;
                    case 3:
                        text = ctx.getString(R.string.button_sliders_help_ct);
                        break;
                }
                SpannableString mspInt = new SpannableString(text);
                mspInt.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                helpView.setTextViewText(currentButton, mspInt);
            } else {
                helpView.setTextColor(currentButton, ctx.getColor(R.color.category_selected_blue_legacy));
            }
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
                if (id == R.id.btnCategory1) {
                    getBridge(ctx).setCurrentCategory(ctx, menuCategory.QUICK_ACCESS);
                } else if (id == R.id.btnCategory2) {
                    getBridge(ctx).setCurrentCategory(ctx, menuCategory.LIGHTS);
                } else if (id == R.id.btnCategory3) {
                    getBridge(ctx).setCurrentCategory(ctx, menuCategory.ROOMS);
                } else if (id == R.id.btnCategory4) {
                    getBridge(ctx).setCurrentCategory(ctx, menuCategory.ZONES);
                } else if (id == R.id.btnCategory5) {
                    getBridge(ctx).setCurrentCategory(ctx, menuCategory.SCENES);
                } else if (id == R.id.btnSlidersCategory1) {
                    getBridge(ctx).setCurrentSlidersCategory(ctx, slidersCategory.BRIGHTNESS);
                } else if (id == R.id.btnSlidersCategory2) {
                    getBridge(ctx).setCurrentSlidersCategory(ctx, slidersCategory.COLOR);
                } else if (id == R.id.btnSlidersCategory3) {
                    getBridge(ctx).setCurrentSlidersCategory(ctx, slidersCategory.SATURATION);
                } else if (id == R.id.btnSlidersCategory4) {
                    getBridge(ctx).setCurrentSlidersCategory(ctx, slidersCategory.TEMPERATURE);
                } else if (id == R.id.btnBack) {
                    setSlidersActive(false);
                } else if (id == R.id.btnEdit) {//HueBridge.loadAllConfiguration(ctx); // rebind for quick way to debug loadAllConfiguration()
                    startEditActivity(ctx);
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
                if (id == R.id.sliders_bri) {
                    value = intent.getIntExtra("bri", 0);
                    new Thread(() -> res.setBri(ctx, value)).start();
                } else if (id == R.id.sliders_hue) {
                    value = intent.getIntExtra("hue", 0);
                    new Thread(() -> res.setHue(ctx, value)).start();
                } else if (id == R.id.sliders_sat) {
                    value = intent.getIntExtra("sat", 0);
                    new Thread(() -> res.setSat(ctx, value)).start();
                } else if (id == R.id.sliders_ct) {
                    value = intent.getIntExtra("ct", 0);
                    new Thread(() -> res.setCt(ctx, value)).start();
                } else {
                    Log.e(TAG, "Unknown category!");
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
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
            boolean bridgeConfigured = settings.getBoolean(ctx.getResources().getString(R.string.preference_bridge_configured), false);
            if (!bridgeConfigured) {
//                 contentView = new RemoteViews(ctx.getPackageName(),R.layout.main_view_loading);
                Log.d(TAG, "Creating content view, no bridge found, will display main_view_no_bridge");
                contentView = new RemoteViews(ctx.getPackageName(),
                        R.layout.main_view_no_bridge); // R.layout.main_view_demo); TODO demo
                contentView.setOnClickPendingIntent(R.id.configureButton,
                        getClickIntent(ctx, R.id.configureButton, 1));
                helpView = createHelpView(ctx);
                cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
                return;
            }
        }
        if (isSlidersActive()){
            contentView = createSlidersContentView(ctx);
            helpView = createSlidersHelpView(ctx);
        } else {
            //One remoteView for left/help and one for right/content
            contentView = createContentView(ctx);
            helpView = createHelpView(ctx);

            //Set pull refresh
            PendingIntent pendingIntent = getRefreshIntent(ctx);
            SlookCocktailManager.getInstance(ctx).setOnPullPendingIntent(cocktailIds[0], R.id.refreshArea, pendingIntent);

            menuCategory currentCategory;
            Map<menuCategory, Map<Integer, ResourceReference>> contents;
            try {
                currentCategory = Objects.requireNonNull(getBridge(ctx).getCurrentCategory(ctx));
                contents = Objects.requireNonNull(getBridge(ctx).getContents());
            } catch (NullPointerException e){
                Log.e(TAG, "Trying to update panel but failed to get current category contents");
                e.printStackTrace();
                return;
            }
            for (int i = 0; i < 10; i++) {
                if (contents.containsKey(currentCategory)) {
                    Map<Integer, ResourceReference> currentCategoryContents;
                    try {
                        currentCategoryContents = Objects.requireNonNull(contents.get(currentCategory));
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

                            int icon_res = ref.getIconRes();
                            contentView.setImageViewResource(btnArr[i], icon_res);
                            if (icon_res != 0){
                                contentView.setViewVisibility(btnTopTextArr[i], View.GONE);
                            } else {
                                contentView.setViewVisibility(btnTopTextArr[i], View.VISIBLE);
                            }
                            int customColor = ref.getIconColor();
                            if(customColor == 0) {
                                int defaultColor = resource.getBtnTextColor(ctx);
                                contentView.setInt(btnArr[i], "setColorFilter", defaultColor);
                                contentView.setTextColor(btnTopTextArr[i], defaultColor);
                            } else {
                                contentView.setInt(btnArr[i], "setColorFilter", customColor);
                                contentView.setTextColor(btnTopTextArr[i], customColor);
                            }
                        }
                    } else {
                        contentView.setTextViewText(btnTextArr[i], "");
                        contentView.setTextViewText(btnTopTextArr[i], ctx.getResources().getString(R.string.plus_symbol));
                        contentView.setViewVisibility(btnTopTextArr[i], View.VISIBLE);
                        //contentView.setFloat(btnTopTextArr[i], "setTextSize", 8);
                        contentView.setTextViewTextSize(btnTopTextArr[i], TypedValue.COMPLEX_UNIT_PX, ctx.getResources().getDimension(R.dimen.resource_btn_text_size_symbol));
                        contentView.setTextColor(btnTopTextArr[i], (ContextCompat.getColor(ctx, R.color.white)));
                        contentView.setInt(btnArr[i], "setBackgroundResource",
                                R.drawable.add_button_background);
                        contentView.setInt(btnArr[i], "setColorFilter", 0);
                        contentView.setImageViewResource(btnArr[i], 0);
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
