package com.ize.edgehue;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.ize.edgehue.bridge_resource.BridgeResource;
import com.ize.edgehue.bridge_resource.LightResource;
import com.ize.edgehue.bridge_resource.RoomResource;
import com.ize.edgehue.bridge_resource.SceneResource;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class EdgeHueProvider extends SlookCocktailProvider {

    private static final String TAG = EdgeHueProvider.class.getSimpleName();

    private static final String ACTION_REMOTE_LONG_CLICK = "com.ize.edgehue.ACTION_REMOTE_LONGCLICK";
    private static final String ACTION_REMOTE_CLICK = "com.ize.edgehue.ACTION_REMOTE_CLICK";
    private static final String ACTION_PULL_TO_REFRESH = "com.ize.edgehue.ACTION_PULL_TO_REFRESH";
    private static final String ACTION_RECEIVE_HUE_STATE = "com.ize.edgehue.ACTION_RECEIVE_HUE_STATE";
    private static final String ACTION_RECEIVE_HUE_REPLY = "com.ize.edgehue.ACTION_RECEIVE_HUE_REPLY";

    //Array of references to buttons
    private static final int[] btnArr = {R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn10};
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

    //This method is called for every broadcast and before each of the other callback methods.
    //Samsung SDK
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (HueBridge.getInstance() == null) {
            currentCategory = menuCategory.NO_BRIDGE;
        }
        if (HueBridge.getInstance() != null) {
            //currentCategory = menuCategory.QUICK_ACCESS;
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
        Log.d(TAG, "onReceive: " + action);
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
                performPullToRefresh(context);
                break;
            case ACTION_RECEIVE_HUE_STATE:
                //just panel update
                break;
            case ACTION_RECEIVE_HUE_REPLY:
                HueBridge.requestHueState();
                break;
            default:
                break;
        }
        try {
            panelUpdate(context);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //This method is called when the Edge Single Plus Mode is created for the first time.
    //Samsung SDK
    @Override
    public void onEnabled(Context context) {
        // TODO Auto-generated method stub
        super.onEnabled(context);
        contents.put(menuCategory.QUICK_ACCESS, quickAccessContent);
        contents.put(menuCategory.LIGHTS, lightsContent);
        contents.put(menuCategory.ROOMS, roomsContent);
        contents.put(menuCategory.ZONES, zonesContent);
        contents.put(menuCategory.SCENES, scenesContent);
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

        //panelUpdate(context);

        /*Intent refreshIntent = new Intent(context, EdgeHueProvider.class);
        refreshIntent.setAction(ACTION_PULL_TO_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0xff, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        SlookCocktailManager.getInstance(context).setOnPullPendingIntent(cocktailIds[0], R.id.debugView, pendingIntent);*/
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
                            context, i++, currentCategory.ordinal()));
                }
                //SlookCocktailManager.getInstance(context).setOnLongClickPendingIntent(contentView, R.id.btn1, getLongClickIntent(context, R.id.btn1, 0));
                break;
            default:
                break;
        }
        return contentView;
    }

    //TODO LEGACY
    private RemoteViews createContentView2(Context context) {
        RemoteViews contentView = new RemoteViews(context.getPackageName(),
                R.layout.view_main2);
        SlookCocktailManager.getInstance(context).setOnLongClickPendingIntent(contentView, R.id.btn1, getLongClickIntent(context, R.id.btn1, 0));
        contentView.setOnClickPendingIntent(R.id.btn1, getClickIntent(context, R.id.btn1, 0));
        return contentView;
    }

    ////Create the help view, left panel. Used for categories.
    private RemoteViews createHelpView(Context context) {
        RemoteViews helpView = new RemoteViews(context.getPackageName(),
                R.layout.help_view);
        helpView.setOnClickPendingIntent(R.id.btnCategory1, getClickIntent(context, R.id.btnCategory1, 1));
        helpView.setOnClickPendingIntent(R.id.btnCategory2, getClickIntent(context, R.id.btnCategory2, 1));
        helpView.setOnClickPendingIntent(R.id.btnCategory3, getClickIntent(context, R.id.btnCategory3, 1));
        helpView.setOnClickPendingIntent(R.id.btnCategory4, getClickIntent(context, R.id.btnCategory4, 1));
        helpView.setOnClickPendingIntent(R.id.btnCategory5, getClickIntent(context, R.id.btnCategory5, 1));
        return helpView;
    }

    //Get the long click intent object to assign to a button
    private static PendingIntent getLongClickIntent(Context context, int id, int key) {
        Intent longClickIntent = new Intent(context, EdgeHueProvider.class);
        longClickIntent.setAction(ACTION_REMOTE_LONG_CLICK);
        longClickIntent.putExtra("id", id);
        longClickIntent.putExtra("key", key);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, id, longClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pIntent;
    }

    //Get the click intent object to assign to a button
    private static PendingIntent getClickIntent(Context context, int id, int key) {
        Intent clickIntent = new Intent(context, EdgeHueProvider.class);
        clickIntent.setAction(ACTION_REMOTE_CLICK);
        clickIntent.putExtra("id", id);
        clickIntent.putExtra("key", key);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, id, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pIntent;
    }

    //Button handler
    private void performRemoteClick(Context context, Intent intent) throws JSONException {
        int id = intent.getIntExtra("id", -1);
        int key = intent.getIntExtra("key", -1);
        if(key == 1) {
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
                default:
                    break;
            }
        }
        else{
            contents.get(currentCategory).get(id).activateResource();
            /*switch (currentCategory) {
                case QUICK_ACCESS:
                    Log.d(TAG, "Getting id: " + id);
                    quickAccessContent.get(id).activateResource();
                    break;
                case ROOMS:
                    Log.d(TAG, "Getting id: " + id);
                    roomsContent.get(id).activateResource();
                    break;
            }*/
        }
        panelUpdate(context);
    }

    //TODO Button handler for long clicks
    private void performRemoteLongClick(Context context, Intent intent) {
        StringBuffer debugString = new StringBuffer("ACTION_REMOTE_LONGCLICK");
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

    //TODO
    private void performPullToRefresh(Context context) {
        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, EdgeHueProvider.class));

        //cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.debugView);
    }

    //The initial setup of the buttons
    public static void quickSetup(Context context) throws JSONException {
        Log.d(TAG, "quickSetup entered");

        quickAccessContent.clear();
        lightsContent.clear();
        roomsContent.clear();
        zonesContent.clear();
        scenesContent.clear();

        currentCategory = menuCategory.QUICK_ACCESS;

        //TODO quickAccessContent

        JSONObject lights = HueBridge.getInstance().getState().getJSONObject("lights");
        Iterator<String> keys = lights.keys();
        int buttonIndex = 0;
        int qaButtonIndex = 0;
        while(keys.hasNext() && buttonIndex < 10) {
            String key = keys.next();
            JSONObject value = lights.getJSONObject(key);
            Log.d(TAG, "quickSetup for lights on id: " + Integer.valueOf(key));
            if (value instanceof JSONObject) {
                lightsContent.put(buttonIndex++, new LightResource(context, Integer.valueOf(key)));
                if(qaButtonIndex < 2) {
                    quickAccessContent.put(qaButtonIndex++, new LightResource(context, Integer.valueOf(key)));
                }
            }
        }

        JSONObject groups = HueBridge.getInstance().getState().getJSONObject("groups");
        keys = groups.keys();
        buttonIndex = 0;
        while(keys.hasNext() && buttonIndex < 10) {
            String key = keys.next();
            JSONObject value = groups.getJSONObject(key);
            Log.d(TAG, "quickSetup for rooms on id: " + Integer.valueOf(key));
            if (value instanceof JSONObject) {
                if (groups.getJSONObject(key).getString("type").equals("Room")) {
                    roomsContent.put(buttonIndex++, new RoomResource(context, Integer.valueOf(key)));
                    if(qaButtonIndex < 4) {
                        quickAccessContent.put(qaButtonIndex++, new RoomResource(context, Integer.valueOf(key)));
                    }
                }
            }
        }

        keys = groups.keys();
        buttonIndex = 0;
        while(keys.hasNext() && buttonIndex < 10) {
            String key = keys.next();
            JSONObject value = groups.getJSONObject(key);
            Log.d(TAG, "quickSetup for zones on id: " + Integer.valueOf(key));
            if (value instanceof JSONObject) {
                if (groups.getJSONObject(key).getString("type").equals("Zone")) {
                    zonesContent.put(buttonIndex++, new RoomResource(context, Integer.valueOf(key)));
                    if(qaButtonIndex < 6) {
                        quickAccessContent.put(qaButtonIndex++, new RoomResource(context, Integer.valueOf(key)));
                    }
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
    private void panelUpdate(Context context) throws JSONException {
        Log.d(TAG, "panelUpdate(): currentCategory is " + currentCategory);
        contentView = createContentView(context);
        helpView = createHelpView(context);
        Log.d(TAG, "currentCategory is " + currentCategory + ". Filling in buttons now");
        if(currentCategory != menuCategory.NO_BRIDGE) {
            for (int i = 0; i < 10; i++) {
                if (contents.get(currentCategory).containsKey(i)) {
                    BridgeResource resource = contents.get(currentCategory).get(i);
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
        /*if(currentCategory == menuCategory.QUICK_ACCESS) {
            Log.d(TAG, "currentCategory is " + currentCategory + ". Filling in buttons now");
            for (int i = 0; i < 10; i++) {
                if (quickAccessContent.containsKey(i)) {
                    BridgeResource resource = quickAccessContent.get(i);
                    contentView.setTextViewText(btnTextArr[i], resource.getName());
                    contentView.setTextViewText(btnArr[i], resource.getBtnText());
                    contentView.setTextColor(btnArr[i], resource.getBtnTextColor());
                    contentView.setInt(btnArr[i], "setBackgroundResource",
                            resource.getBtnBackgroundResource());
                }
                else{
                    contentView.setTextViewText(btnTextArr[i], "");
                    contentView.setTextViewText(btnArr[i], "+");
                    contentView.setTextColor(btnArr[i], (ContextCompat.getColor(context, R.color.white)));
                    contentView.setInt(btnArr[i], "setBackgroundResource",
                            R.drawable.add_button_background);
                }
            }
        }

        if(currentCategory == menuCategory.ROOMS) {
            Log.d(TAG, "currentCategory is " + currentCategory + ". Filling in buttons now");
            for (int i = 0; i < 10; i++) {
                if (roomsContent.containsKey(i)) {
                    BridgeResource resource = roomsContent.get(i);
                    contentView.setTextViewText(btnTextArr[i], resource.getName());
                    contentView.setTextViewText(btnArr[i], resource.getBtnText());
                    contentView.setTextColor(btnArr[i], resource.getBtnTextColor());
                    contentView.setInt(btnArr[i], "setBackgroundResource",
                            resource.getBtnBackgroundResource());
                }
                else{
                    contentView.setTextViewText(btnTextArr[i], "");
                    contentView.setTextViewText(btnArr[i], "+");
                    contentView.setTextColor(btnArr[i], (ContextCompat.getColor(context, R.color.white)));
                    contentView.setInt(btnArr[i], "setBackgroundResource",
                            R.drawable.add_button_background);
                }
            }
        }*/

        final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, EdgeHueProvider.class));
        cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
    }
}
