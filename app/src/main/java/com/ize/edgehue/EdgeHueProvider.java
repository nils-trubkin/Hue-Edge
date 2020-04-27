package com.ize.edgehue;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;


import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import org.json.JSONException;
import org.json.JSONObject;

public class EdgeHueProvider extends SlookCocktailProvider {

    private static final String TAG = EdgeHueProvider.class.getSimpleName();

    private static final String ACTION_REMOTE_LONG_CLICK = "com.ize.edgehue.ACTION_REMOTE_LONGCLICK";
    private static final String ACTION_REMOTE_CLICK = "com.ize.edgehue.ACTION_REMOTE_CLICK";
    private static final String ACTION_PULL_TO_REFRESH = "com.ize.edgehue.ACTION_PULL_TO_REFRESH";
    private static final String ACTION_RECEIVE_HUE_STATE = "com.ize.edgehue.ACTION_RECEIVE_HUE_STATE";
    private static final String ACTION_RECEIVE_HUE_REPLY = "com.ize.edgehue.ACTION_RECEIVE_HUE_REPLY";

    private static RemoteViews contentView = null;
    private static RemoteViews helpView = null;

    private static PendingIntent jsonInt;

    @Override
    public void onReceive(Context context, Intent intent) {

        if(contentView == null) {
            contentView = createContentView(context);
        }
        if(helpView == null) {
            helpView = createHelpView(context);
        }

        if (HueBridge.getInstance() == null){
            HueBridge.getInstance(context,
                    "192.168.69.166",
                    "aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD");
            HueBridge.getInstance().requestHueState();
        }

        super.onReceive(context, intent);
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);
        switch(action) {
            case ACTION_REMOTE_LONG_CLICK:
                performRemoteLongClick(context, intent);
                break;
            case ACTION_REMOTE_CLICK:
                performRemoteClick(context, intent);
                break;
            case ACTION_PULL_TO_REFRESH:
                performPullToRefresh(context);
                break;
            case ACTION_RECEIVE_HUE_STATE:
                JSONObject state = HueBridge.getInstance().getState();
                try {
                    contentView.setTextViewText(R.id.btn5, (state.getJSONObject("lights").getJSONObject("10").getJSONObject("state").get("on")).equals(false) ? "0" : "1");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                panelUpdate(context);
                break;
            case ACTION_RECEIVE_HUE_REPLY:
                //handleHueReply(context, intent);
                HueBridge.getInstance().requestHueState();
                break;
            default:
                break;
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
        if (HueBridge.getInstance() == null){
            HueBridge.getInstance(context,
                    "192.168.69.166",
                    "aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD");
            HueBridge.getInstance().requestHueState();
        }
    }

    @Override
    public void onUpdate(Context context, SlookCocktailManager cocktailManager, int[] cocktailIds) {
        if(contentView == null) {
            contentView = createContentView(context);
        }
        if(helpView == null) {
            helpView = createHelpView(context);
        }
        cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);

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
        RemoteViews contentView = new RemoteViews(context.getPackageName(),
                R.layout.view_main);
        SlookCocktailManager.getInstance(context).setOnLongClickPendingIntent(contentView, R.id.btn1, getLongClickIntent(context, R.id.btn1, 0));
        contentView.setOnClickPendingIntent(R.id.btn1, getClickIntent(context, R.id.btn1, 0));
        contentView.setOnClickPendingIntent(R.id.btn2, getClickIntent(context, R.id.btn2, 0));
        contentView.setOnClickPendingIntent(R.id.btn3, getClickIntent(context, R.id.btn3, 0));
        contentView.setOnClickPendingIntent(R.id.btn4, getClickIntent(context, R.id.btn4, 0));
        contentView.setOnClickPendingIntent(R.id.btn5, getClickIntent(context, R.id.btn5, 0));
        return contentView;
    }

    private RemoteViews createContentView2(Context context) {
        RemoteViews contentView = new RemoteViews(context.getPackageName(),
                R.layout.view_main2);
        SlookCocktailManager.getInstance(context).setOnLongClickPendingIntent(contentView, R.id.btn1, getLongClickIntent(context, R.id.btn1, 0));
        contentView.setOnClickPendingIntent(R.id.btn1, getClickIntent(context, R.id.btn1, 0));
        contentView.setOnClickPendingIntent(R.id.btn2, getClickIntent(context, R.id.btn2, 0));
        contentView.setOnClickPendingIntent(R.id.btn3, getClickIntent(context, R.id.btn3, 0));
        contentView.setOnClickPendingIntent(R.id.btn4, getClickIntent(context, R.id.btn4, 0));
        contentView.setOnClickPendingIntent(R.id.btn5, getClickIntent(context, R.id.btn5, 0));
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

    private PendingIntent getLongClickIntent(Context context, int id, int key) {
        Intent longClickIntent = new Intent(context, EdgeHueProvider.class);
        longClickIntent.setAction(ACTION_REMOTE_LONG_CLICK);
        longClickIntent.putExtra("id", id);
        longClickIntent.putExtra("key", key);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, id, longClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pIntent;
    }

    private PendingIntent getClickIntent(Context context, int id, int key) {
        Intent clickIntent = new Intent(context, EdgeHueProvider.class);
        clickIntent.setAction(ACTION_REMOTE_CLICK);
        clickIntent.putExtra("id", id);
        clickIntent.putExtra("key", key);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, id, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pIntent;
    }

    private void performRemoteClick(Context context, Intent intent) {
        int id = intent.getIntExtra("id", -1);
        switch (id) {
            case R.id.btn1:
                HueBridge.changeHueState(10, true);
                break;
            case R.id.btn2:
                HueBridge.changeHueState(10, false);
                break;
            case R.id.btn4:
                HueBridge.getInstance().requestHueState();
                contentView.setTextViewText(R.id.btn4, "Req");
                break;
            case R.id.btn5:
                boolean currentState = false;
                try {
                    currentState = HueBridge.getInstance().getState()
                            .getJSONObject("lights")
                            .getJSONObject("10")
                            .getJSONObject("state")
                            .getBoolean("on");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "currentState is: " + currentState);
                HueBridge.changeHueState(10, !currentState);
                break;
            case R.id.btnCategory1:
                contentView = createContentView(context);
                break;
            case R.id.btnCategory2:
                contentView = createContentView2(context);
                break;
            default:
                break;
        }
        panelUpdate(context);
    }

    private void performRemoteLongClick(Context context, Intent intent) {
        StringBuffer debugString = new StringBuffer("ACTION_REMOTE_LONGCLICK");
        int id = intent.getIntExtra("id", -1);
        debugString.append("id=").append(intent.getIntExtra("id", -1));
        Log.d(TAG, debugString.toString());
        switch (id) {
            case R.id.btn1:
                btnHandler(context, 1);
                break;
            case R.id.btn2:
                btnHandler(context, 2);
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

    private void btnHandler(final Context context, int id) {
        if(contentView == null) {
            contentView = createContentView(context);
        }
        if(helpView == null) {
            helpView = createHelpView(context);
        }

        if(id == 1) {
                HueBridge.changeHueState(10, true);
        }
        if(id == 2) {
                HueBridge.changeHueState(10, false);
        }
        if(id == 3) {
        }

        if(id == 4) {

        }

        if(id == 5) {

        }
        if (id == 11){

        }
        if (id == 22){

        }
        panelUpdate(context);
    }

    private void handleHueReply(Context context, Intent intent){
        StringBuffer debugString = new StringBuffer("ACTION_REMOTE_LONGCLICK");
        int id = intent.getIntExtra("id", -1);
        debugString.append("id=").append(intent.getIntExtra("id", -1));
        Log.d(TAG, debugString.toString());
        switch (id) {
            case 10:
                btnHandler(context, 1);
                break;
            default:
                break;
        }
    }

    private static void panelUpdate(Context context) {
        final SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        final int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, EdgeHueProvider.class));
        cocktailManager.updateCocktail(cocktailIds[0], contentView, helpView);
    }

    /*public static void panelUpdate(Context context, SlookCocktailManager manager, int[] cocktailIds, String data) {
        int mainLayoutId = R.layout.view_main;
        int helpLayoutId = R.layout.help_view;
        RemoteViews mainLayoutRv = new RemoteViews(context.getPackageName(), mainLayoutId);
        RemoteViews helpLayoutRv = new RemoteViews(context.getPackageName(), helpLayoutId);
        helpView.setTextViewText(R.id.debugView, "Response is: " + data);
        //mainLayoutRv.setViewVisibility(R.id.main_background, View.VISIBLE);
        //rv.setImageViewResource(R.id.main_background, R.drawable.apps_edge);
        if (cocktailIds != null) {
            for (int id : cocktailIds) {
                manager.updateCocktail(id, mainLayoutRv, helpLayoutRv);
            }
        }
    }*/

}
