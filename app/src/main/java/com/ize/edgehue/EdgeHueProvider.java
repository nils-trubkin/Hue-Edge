package com.ize.edgehue;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ize.edgehue.bridge_resource.BridgeResource;
import com.ize.edgehue.bridge_resource.LightResource;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ObjectInput;
import java.util.HashMap;
import java.util.Iterator;

public class EdgeHueProvider extends SlookCocktailProvider {

    private static final String TAG = EdgeHueProvider.class.getSimpleName();

    private static final String ACTION_REMOTE_LONG_CLICK = "com.ize.edgehue.ACTION_REMOTE_LONGCLICK";
    private static final String ACTION_REMOTE_CLICK = "com.ize.edgehue.ACTION_REMOTE_CLICK";
    private static final String ACTION_PULL_TO_REFRESH = "com.ize.edgehue.ACTION_PULL_TO_REFRESH";
    private static final String ACTION_RECEIVE_HUE_STATE = "com.ize.edgehue.ACTION_RECEIVE_HUE_STATE";
    private static final String ACTION_RECEIVE_HUE_REPLY = "com.ize.edgehue.ACTION_RECEIVE_HUE_REPLY";

    private static final int btnArr[] = {R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btn10};
    private static final int btnTextArr[] = {R.id.btn1text, R.id.btn2text, R.id.btn3text, R.id.btn4text, R.id.btn5text,
            R.id.btn6text, R.id.btn7text, R.id.btn8text, R.id.btn9text, R.id.btn10text};

    private enum menuCategory {
        NO_BRIDGE,
        QUICK_ACCESS,
        LIGHTS,
        ROOMS,
        ZONES,
        SCENES
    }

    private static RemoteViews contentView = null;
    private static RemoteViews helpView = null;

    private static HashMap<Integer, LightResource> quickAccessContent = new HashMap<>();
    private static HashMap<Integer,PendingIntent> lightsContent = new HashMap<>();
    private static HashMap<Integer,PendingIntent> roomsContent = new HashMap<>();
    private static HashMap<Integer,PendingIntent> zonesContent = new HashMap<>();
    private static HashMap<Integer,PendingIntent> scenesContent = new HashMap<>();

    private static menuCategory currentCategory = menuCategory.NO_BRIDGE;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (HueBridge.getInstance() == null) {
            currentCategory = menuCategory.NO_BRIDGE;
        }
        if (HueBridge.getInstance() != null) {
            currentCategory = menuCategory.QUICK_ACCESS;
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

    @Override
    public void onDisabled(Context context) {
        // TODO Auto-generated method stub
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        // TODO Auto-generated method stub
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, SlookCocktailManager cocktailManager, int[] cocktailIds) {

        //panelUpdate(context);

        /*Intent refreshIntent = new Intent(context, EdgeHueProvider.class);
        refreshIntent.setAction(ACTION_PULL_TO_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0xff, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        SlookCocktailManager.getInstance(context).setOnPullPendingIntent(cocktailIds[0], R.id.debugView, pendingIntent);*/
    }

    @Override
    public void onVisibilityChanged(Context context, int cocktailId, int visibility) {
        super.onVisibilityChanged(context, cocktailId, visibility);
    }

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

    private RemoteViews createContentView2(Context context) {
        RemoteViews contentView = new RemoteViews(context.getPackageName(),
                R.layout.view_main2);
        SlookCocktailManager.getInstance(context).setOnLongClickPendingIntent(contentView, R.id.btn1, getLongClickIntent(context, R.id.btn1, 0));
        contentView.setOnClickPendingIntent(R.id.btn1, getClickIntent(context, R.id.btn1, 0));
        return contentView;
    }

    private RemoteViews createHelpView(Context context) {
        RemoteViews helpView = new RemoteViews(context.getPackageName(),
                R.layout.help_view);
        helpView.setOnClickPendingIntent(R.id.btnCategory1, getClickIntent(context, R.id.btnCategory1, 0));
        helpView.setOnClickPendingIntent(R.id.btnCategory2, getClickIntent(context, R.id.btnCategory2, 0));
        helpView.setOnClickPendingIntent(R.id.btnCategory2, getClickIntent(context, R.id.btnCategory2, 0));
        helpView.setOnClickPendingIntent(R.id.btnCategory2, getClickIntent(context, R.id.btnCategory2, 0));
        helpView.setOnClickPendingIntent(R.id.btnCategory2, getClickIntent(context, R.id.btnCategory2, 0));
        return helpView;
    }

    private static PendingIntent getLongClickIntent(Context context, int id, int key) {
        Intent longClickIntent = new Intent(context, EdgeHueProvider.class);
        longClickIntent.setAction(ACTION_REMOTE_LONG_CLICK);
        longClickIntent.putExtra("id", id);
        longClickIntent.putExtra("key", key);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, id, longClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pIntent;
    }

    private static PendingIntent getClickIntent(Context context, int id, int key) {
        Intent clickIntent = new Intent(context, EdgeHueProvider.class);
        clickIntent.setAction(ACTION_REMOTE_CLICK);
        clickIntent.putExtra("id", id);
        clickIntent.putExtra("key", key);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, id, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pIntent;
    }

    private void performRemoteClick(Context context, Intent intent) throws JSONException {
        int id = intent.getIntExtra("id", -1);
        if(currentCategory == menuCategory.QUICK_ACCESS) {
            Log.d(TAG,"Getting id: " + id);
            quickAccessContent.get(id).activateResource();
        }
        /*case R.id.btnCategory1:
            contentView = createContentView(context);
            break;
        case R.id.btnCategory2:
            contentView = createContentView2(context);
            break;
        default:
            break;
    */
        panelUpdate(context);
    }

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

    private void performPullToRefresh(Context context) {
        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, EdgeHueProvider.class));

        //cocktailManager.notifyCocktailViewDataChanged(cocktailIds[0], R.id.debugView);
    }

    public static void quickSetup(Context context) throws JSONException {
        Log.d(TAG, "quickSetup entered");
        quickAccessContent.clear();
        JSONObject state = HueBridge.getInstance().getState().getJSONObject("lights");
        Iterator<String> keys = state.keys();
        int buttonIndex = 0;
        while(keys.hasNext() && buttonIndex < 10) {
            String key = keys.next();
            JSONObject value = state.getJSONObject(key);
            Log.d(TAG, "quickSetup on id: " + Integer.valueOf(key));
            if (value instanceof JSONObject) {
                quickAccessContent.put(buttonIndex++, new LightResource(context, Integer.valueOf(key)));
            }
        }
    }

    private void panelUpdate(Context context) throws JSONException {
        Log.d(TAG, "panelUpdate(): currentCategory is " + currentCategory);
        contentView = createContentView(context);
        helpView = createHelpView(context);
        if(currentCategory == menuCategory.QUICK_ACCESS) {
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
        final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, EdgeHueProvider.class));
        cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
    }
}
