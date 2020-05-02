package com.ize.edgehue;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.ize.edgehue.resource.BridgeResource;
import com.ize.edgehue.resource.LightResource;
import com.ize.edgehue.resource.RoomResource;
import com.ize.edgehue.resource.SceneResource;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class EdgeHueProvider extends SlookCocktailProvider {

    private static final String TAG = EdgeHueProvider.class.getSimpleName();

    private static final String ACTION_REMOTE_LONG_CLICK = "com.ize.edgehue.ACTION_REMOTE_LONG_CLICK";
    private static final String ACTION_REMOTE_CLICK = "com.ize.edgehue.ACTION_REMOTE_CLICK";
    private static final String ACTION_PULL_TO_REFRESH = "com.ize.edgehue.ACTION_PULL_TO_REFRESH";
    private static final String ACTION_RECEIVE_HUE_STATE = "com.ize.edgehue.ACTION_RECEIVE_HUE_STATE";
    private static final String ACTION_RECEIVE_HUE_REPLY = "com.ize.edgehue.ACTION_RECEIVE_HUE_REPLY";

    //Array of references to buttons
    private static final int[] btnArr = {R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn10};
    //Array of references to category buttons
    private static final int[] btnCategoryArr = {R.id.btnCategory1, R.id.btnCategory2,
            R.id.btnCategory3, R.id.btnCategory4, R.id.btnCategory5};
    //Array of references to category buttons underlines
    private static final int[] btnCategoryLineArr = {R.id.btnCategoryLine1, R.id.btnCategoryLine2,
            R.id.btnCategoryLine3, R.id.btnCategoryLine4, R.id.btnCategoryLine5};
    //Array of references to button texts (text under the button itself)
    private static final int[] btnTextArr = {R.id.btn1text, R.id.btn2text, R.id.btn3text, R.id.btn4text, R.id.btn5text,
            R.id.btn6text, R.id.btn7text, R.id.btn8text, R.id.btn9text, R.id.btn10text};

    //Categories available in the left pane (helpContent)
    private enum menuCategory {
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
    private static HashMap<Integer, BridgeResource> quickAccessContent = new HashMap<>();
    private static HashMap<Integer, LightResource> lightsContent = new HashMap<>();
    private static HashMap<Integer, RoomResource> roomsContent = new HashMap<>();
    private static HashMap<Integer, RoomResource> zonesContent = new HashMap<>();
    private static HashMap<Integer, SceneResource> scenesContent = new HashMap<>();

    //Mapping of category to contents
    private static HashMap<menuCategory, HashMap<Integer, ? extends BridgeResource>> contents =
            new HashMap<>();

    //Selected category initiated to none
    private static menuCategory currentCategory = menuCategory.NO_BRIDGE;

    //The HueBridge instance
    private static HueBridge bridge;


    //This method is called for every broadcast and before each of the other callback methods.
    //Samsung SDK
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (HueBridge.getInstance() == null) {
            currentCategory = menuCategory.NO_BRIDGE;
        }
        if(contentView == null) {
            contentView = createContentView(context);
        }
        if(helpView == null) {
            helpView = createHelpView(context);
        }
        if (HueBridge.getInstance() == null) {
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
                performRemoteLongClick(context, intent);
                break;
            case ACTION_REMOTE_CLICK:
                try {
                    performRemoteClick(context, intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case ACTION_PULL_TO_REFRESH:
                SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
                int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, EdgeHueProvider.class));
                cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.refreshArea);
            case ACTION_RECEIVE_HUE_REPLY:
                bridge.requestHueState(context);
                break;
            case ACTION_RECEIVE_HUE_STATE:
                //just panel update
                break;
            default:
                break;
        }
        panelUpdate(context);
    }

    //This method is called when the Edge Single Plus Mode is created for the first time.
    //Samsung SDK
    @Override
    public void onEnabled(Context context) {
        // TODO Auto-generated method stub
        super.onEnabled(context);

    }

    //This method is called when the instance of
    //Edge Single Plus Mode is deleted from the enabled list
    //Samsung SDK
    @Override
    public void onDisabled(Context context) {
        // TODO Auto-generated method stub
        super.onDisabled(context);
    }

    //TODO
    @Override
    public void onUpdate(Context context, SlookCocktailManager cocktailManager, int[] cocktailIds) {

        Log.i(TAG, "onUpdate");
        panelUpdate(context);
    }

    //TODO
    @Override
    public void onVisibilityChanged(Context context, int cocktailId, int visibility) {
        super.onVisibilityChanged(context, cocktailId, visibility);
    }

    //Create the content view, right panel. Used for buttons
    private RemoteViews createContentView(Context context) {
        RemoteViews contentView = null;
        switch (currentCategory){
            case NO_BRIDGE:
                contentView = new RemoteViews(context.getPackageName(),
                        R.layout.content_view_no_bridge);
                break;
            case QUICK_ACCESS:
            case LIGHTS:
            case ROOMS:
            case ZONES:
            case SCENES:
                contentView = new RemoteViews(context.getPackageName(),
                        R.layout.view_main);

                int i = 0;
                for ( int button : btnArr ){
                    contentView.setOnClickPendingIntent(button, getClickIntent(
                            context, i++, 0));
                    SlookCocktailManager.getInstance(context).
                            setOnLongClickPendingIntent(contentView, button,
                                    getLongClickIntent(context, button, 0));
                }
                contentView.setOnClickPendingIntent(R.id.btnEdit,
                        getClickIntent(context, R.id.btnEdit, 1));
                break;
            default:
                break;
        }
        return contentView;
    }

    //Create the help view, left panel. Used for categories.
    private RemoteViews createHelpView(Context context) {
        RemoteViews helpView = new RemoteViews(context.getPackageName(),
                R.layout.view_help);
        for( int button : btnCategoryArr){
            helpView.setOnClickPendingIntent(button, getClickIntent(context, button, 1));
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
    private static PendingIntent getLongClickIntent(Context context, int id, int key) {
        Intent longClickIntent = new Intent(context, EdgeHueProvider.class);
        longClickIntent.setAction(ACTION_REMOTE_LONG_CLICK);
        longClickIntent.putExtra("id", id);
        longClickIntent.putExtra("key", key);
        return PendingIntent.getBroadcast(context, id, longClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Get the click intent object to assign to a button
    private static PendingIntent getClickIntent(Context context, int id, int key) {
        Intent clickIntent = new Intent(context, EdgeHueProvider.class);
        clickIntent.setAction(ACTION_REMOTE_CLICK);
        clickIntent.putExtra("id", id);
        clickIntent.putExtra("key", key);
        return PendingIntent.getBroadcast(context, id, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Button handler
    private void performRemoteClick(Context context, Intent intent) throws JSONException {
        int id = intent.getIntExtra("id", -1);
        int key = intent.getIntExtra("key", -1);
        if(key == 0){
            contents.get(currentCategory).get(id).activateResource(context);
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
                    Intent editIntent = new Intent(Intent.ACTION_EDIT);
                    editIntent.addCategory( Intent.CATEGORY_DEFAULT);
                    editIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(editIntent);
                    break;
                default:
                    break;
            }
        }
        panelUpdate(context);
    }

    //TODO Button handler for long clicks
    private void performRemoteLongClick(Context context, Intent intent) {
        StringBuilder debugString = new StringBuilder("ACTION_REMOTE_LONG_CLICK");
        int id = intent.getIntExtra("id", -1);
        debugString.append("id=").append(intent.getIntExtra("id", -1));
        Log.d(TAG, debugString.toString());
        switch (id) {
            case R.id.btn1:
                break;
            case R.id.btn2:
                break;
            default:
                break;
        }
    }

    //The initial setup of the buttons
    public static void quickSetup(Context context) throws JSONException {
        Log.d(TAG, "quickSetup entered");

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

        currentCategory = menuCategory.QUICK_ACCESS;

        bridge = HueBridge.getInstance();

        if (HueBridge.getInstance() == null){
            Log.e(TAG, "HueBridge.getInstance() == null. Probably missing config");
        }
        assert HueBridge.getInstance() != null;
        JSONObject lights = HueBridge.getInstance().getState().getJSONObject("lights");
        Iterator<String> keys = lights.keys();
        int buttonIndex = 0;
        int qaButtonIndex = 0;
        while(keys.hasNext() && buttonIndex < 10) {
            String key = keys.next();
            Log.d(TAG, "quickSetup for lights on id: " + Integer.valueOf(key));
            lightsContent.put(buttonIndex++, new LightResource(context, Integer.parseInt(key)));
            if(qaButtonIndex < 2) {
                quickAccessContent.put(qaButtonIndex++, new LightResource(context, Integer.parseInt(key)));
            }
        }

        JSONObject groups = HueBridge.getInstance().getState().getJSONObject("groups");
        keys = groups.keys();
        buttonIndex = 0;
        while(keys.hasNext() && buttonIndex < 10) {
            String key = keys.next();
            Log.d(TAG, "quickSetup for rooms on id: " + Integer.valueOf(key));
            if (groups.getJSONObject(key).getString("type").equals("Room")) {
                roomsContent.put(buttonIndex++, new RoomResource(context, Integer.parseInt(key)));
                if(qaButtonIndex < 4) {
                    quickAccessContent.put(qaButtonIndex++, new RoomResource(context, Integer.parseInt(key)));
                }
            }
        }

        keys = groups.keys();
        buttonIndex = 0;
        while(keys.hasNext() && buttonIndex < 10) {
            String key = keys.next();
            Log.d(TAG, "quickSetup for zones on id: " + Integer.valueOf(key));
            if (groups.getJSONObject(key).getString("type").equals("Zone")) {
                zonesContent.put(buttonIndex++, new RoomResource(context, Integer.parseInt(key)));
                if(qaButtonIndex < 6) {
                    quickAccessContent.put(qaButtonIndex++, new RoomResource(context, Integer.parseInt(key)));
                }
            }
        }

        /*JSONObject scenes = HueBridge.getInstance().getState().getJSONObject("scenes");
        keys = scenes.keys();
        buttonIndex = 0;
        while(keys.hasNext() && buttonIndex < 10) {
            String key = keys.next();
            JSONObject value = scenes.getJSONObject(key);
            Log.d(TAG, "quickSetup for scenes on id: " + Integer.valueOf(key));
            if (value instanceof JSONObject) {
                if (scenes.getJSONObject(key).getString("type").equals("GroupScene")) {
                    roomsContent.put(buttonIndex++, new SceneResource(context, Integer.valueOf(key)));
                    if(qaButtonIndex < 8) {
                        quickAccessContent.put(qaButtonIndex++, new RoomResource(context, Integer.valueOf(key)));
                    }
                }
            }
        }*/
    }

    //Refresh both panels
    private void panelUpdate(Context context){
        contentView = createContentView(context);
        helpView = createHelpView(context);
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
                    contentView.setInt(btnArr[i], "setBackgroundResource",
                            resource.getBtnBackgroundResource());
                } else {
                    contentView.setTextViewText(btnTextArr[i], "");
                    contentView.setTextViewText(btnArr[i], "+");
                    contentView.setTextColor(btnArr[i], (ContextCompat.getColor(context, R.color.white)));
                    contentView.setInt(btnArr[i], "setBackgroundResource",
                            R.drawable.add_button_background);
                }
            }
        }

        final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, EdgeHueProvider.class));

        //Set pull refresh
        Intent refreshIntent = new Intent(context, EdgeHueProvider.class);
        refreshIntent.setAction(ACTION_PULL_TO_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0xff, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        SlookCocktailManager.getInstance(context).setOnPullPendingIntent(cocktailIds[0], R.id.refreshArea, pendingIntent);

        cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
    }
}
