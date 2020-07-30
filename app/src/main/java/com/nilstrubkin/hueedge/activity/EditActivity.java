package com.nilstrubkin.hueedge.activity;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nilstrubkin.hueedge.DragEventListener;
import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.R;
import com.nilstrubkin.hueedge.BridgeResource;
import com.nilstrubkin.hueedge.adapter.ResourceArrayAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditActivity extends AppCompatActivity {

    private static final String TAG = EditActivity.class.getSimpleName();
    private final Context ctx = this;

    private Vibrator vibrator;
    private HueEdgeProvider.menuCategory currentCategory;
    private HashMap<HueEdgeProvider.menuCategory, HashMap<Integer, BridgeResource>> contents;

    public HueEdgeProvider.menuCategory getCurrentCategory() {
        return currentCategory;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started.");

        if(HueBridge.getInstance(ctx) == null) {
            HueEdgeProvider.startSetupActivity(ctx);
            return;
        }

        setContentView(R.layout.edit_activity);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(ctx.getColor(R.color.navigation_bar_color_edit));

        //UI elements
        GridView mListView = findViewById(R.id.gridView);
        Button btnSave = findViewById(R.id.btnSave);
        TextView hueStatus = findViewById(R.id.hueStatus);

        final HueBridge bridge;
        try {
            bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
        } catch (NullPointerException ex){
            Log.e(TAG, "Entering edit activity but no HueBridge instance was found");
            ex.printStackTrace();
            return;
        }
        vibrator = (Vibrator) ctx.getSystemService(VIBRATOR_SERVICE);
        currentCategory = bridge.getCurrentCategory();
        contents = bridge.getContents();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(HueBridge.getInstance(ctx) != null){
                    HueEdgeProvider.saveAllConfiguration(ctx);
                    String toastString = ctx.getString(R.string.toast_saved);
                    Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
                }
                else
                    Log.e(TAG, "Saving the settings but the HueBridge.getInstance() == null");
                finish();
            }
        });

        String ip;
        try {
            ip = Objects.requireNonNull(HueBridge.getInstance(ctx)).getIp();
        }
        catch (NullPointerException ex){
            Log.e(TAG, "Trying to enter edit activity but there is no instance of HueBridge");
            ex.printStackTrace();
            return;
        }
        hueStatus.setText(ctx.getString(R.string.hue_status, ip));
        hueStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent setupIntent = new Intent(ctx, SetupActivity.class);
                setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(setupIntent);
            }
        });

        panelUpdate();

        ArrayList<BridgeResource> resources = new ArrayList<>();
        HashMap<String, BridgeResource> map = null;
        switch (currentCategory) {
            case QUICK_ACCESS:
                map = bridge.getLights();
                for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
                    resources.add(entry.getValue());
                }
                map = bridge.getRooms();
                for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
                    resources.add(entry.getValue());
                }
                map = bridge.getZones();
                for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
                    resources.add(entry.getValue());
                }
                map = bridge.getScenes();
                for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
                    resources.add(entry.getValue());
                }
                break;
            case LIGHTS:
                map = bridge.getLights();
                break;
            case ROOMS:
                map = bridge.getRooms();
                break;
            case ZONES:
                map = bridge.getZones();
                break;
            case SCENES:
                map = bridge.getScenes();
                break;
            default:
                Log.w(TAG, "Unknown category!");
                break;
        }
        if(!currentCategory.equals(HueEdgeProvider.menuCategory.QUICK_ACCESS)){
            for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
                resources.add(entry.getValue());
            }
        }
        if(!currentCategory.equals(HueEdgeProvider.menuCategory.SCENES)){
            resources.add(new BridgeResource("0", "All", "groups", "any_on","on"));
        }
        ResourceArrayAdapter adapter = new ResourceArrayAdapter(
                this, R.layout.edit_activity_adapter_view_layout, resources, vibrator);
        adapter.sort(new Comparator<BridgeResource>() {
            @Override
            public int compare(BridgeResource br1, BridgeResource br2) {
                return br1.compareTo(br2);
            }
        });
        mListView.setAdapter(adapter);

        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show(); //TODO snackbar
            }
        });*/
    }

    public void panelUpdate() {
        for (int i = 0; i < 10; i++) {
            panelUpdateIndex(i);
        }
    }

    public void panelUpdateIndex (int i){
        if (contents.containsKey(currentCategory)) {
            final HashMap<Integer, BridgeResource> currentCategoryContents = contents.get(currentCategory);
            boolean slotIsFilled = false;
            try {
                slotIsFilled = Objects.requireNonNull(currentCategoryContents).containsKey(i);
            } catch (NullPointerException ex) {
                Log.e(TAG, "Trying to enter edit activity panel but failed to get current category contents");
                ex.printStackTrace();
            }
            final TextView btnText = findViewById(HueEdgeProvider.btnTextArr[i]);
            final Button btn = findViewById(HueEdgeProvider.btnArr[i]);
            final TextView btnTopText = findViewById(HueEdgeProvider.btnTopTextArr[i]);
            final Button btnDelete = findViewById(HueEdgeProvider.btnDeleteArr[i]);
            final TextView btnDeleteTopText = findViewById(HueEdgeProvider.btnDeleteTopTextArr[i]);

            if (slotIsFilled) {
                final BridgeResource resource;
                try {
                    resource = Objects.requireNonNull(currentCategoryContents.get(i));
                } catch (NullPointerException ex) {
                    Log.e(TAG, "Failed to load filled slot");
                    ex.printStackTrace();
                    return;
                }
                displaySlotAsFull(i, resource);
                final int finalI = i;
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clearSlot(finalI);
                    }
                });
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clearSlot(finalI);
                    }
                });
                btnDelete.setVisibility(View.VISIBLE);
                btnDeleteTopText.setVisibility(View.VISIBLE);
                btn.setOnDragListener(null);
                btnText.setOnDragListener(null);
                btn.setOnLongClickListener(new View.OnLongClickListener(){
                    @Override
                    public boolean onLongClick(View v) {
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                        boolean noHaptic = settings.getBoolean(ctx.getResources().getString(R.string.no_haptic_preference), false);
                        if(!noHaptic)
                            vibrator.vibrate(1);
                        ClipData.Item item = new ClipData.Item(String.valueOf(finalI));
                        ClipData dragData = new ClipData(
                                resource.getName(),
                                new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN },
                                item);
                        View.DragShadowBuilder myShadow = new View.DragShadowBuilder(btn);
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                            return v.startDragAndDrop(dragData,  // the data to be dragged
                                    myShadow,  // the drag shadow builder
                                    resource,      // pass resource
                                    0          // flags (not currently used, set to 0)
                            );
                        else
                            //noinspection deprecation
                            return v.startDrag(dragData,  // the data to be dragged
                                    myShadow,  // the drag shadow builder
                                    resource,      // pass resource
                                    0          // flags (not currently used, set to 0)
                            );
                    }
                });
            } else {
                displaySlotAsEmpty(btn, btnTopText, btnDelete, btnText, btnDeleteTopText);
                DragEventListener dragListen = new DragEventListener(ctx, i);
                btn.setOnDragListener(dragListen);
                btnText.setOnDragListener(dragListen);
                btn.setOnLongClickListener(null);
            }
        }
    }

    public void clearSlot (int position) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean noHaptic = settings.getBoolean(ctx.getResources().getString(R.string.no_haptic_preference), false);
        if(!noHaptic)
            vibrator.vibrate(1);
        final HashMap<Integer, BridgeResource> currentCategoryContents;
        try {
            currentCategoryContents = Objects.requireNonNull(contents.get(currentCategory));
        }
        catch (NullPointerException ex){
            Log.e(TAG, "Failed to get contents of current category");
            ex.printStackTrace();
            return;
        }
        currentCategoryContents.remove(position);
        HueEdgeProvider.saveAllConfiguration(ctx);
        Button btn = findViewById(HueEdgeProvider.btnArr[position]);
        TextView btnText = findViewById(HueEdgeProvider.btnTextArr[position]);
        TextView btnTopText = findViewById(HueEdgeProvider.btnTopTextArr[position]);
        Button btnDelete = findViewById(HueEdgeProvider.btnDeleteArr[position]);
        TextView btnDeleteTopText = findViewById(HueEdgeProvider.btnDeleteTopTextArr[position]);
        displaySlotAsEmpty(btn, btnTopText, btnDelete, btnText, btnDeleteTopText);
        // Creates a new drag event listener
        DragEventListener dragListen = new DragEventListener(ctx, position);
        // Sets the drag event listener for the View
        btn.setOnDragListener(dragListen);
        btnText.setOnDragListener(dragListen);
        btn.setOnLongClickListener(null);
    }

    public void displaySlotAsEmpty (Button btn, TextView btnTopText, Button btnDelete, TextView btnText, TextView btnDeleteTopText) {
        btnTopText.setText("");
        btnText.setText("");
        btn.setBackground(getResources().getDrawable(R.drawable.edit_add_button_background, getTheme()));
        btnDelete.setVisibility(View.GONE);
        btnDeleteTopText.setVisibility(View.GONE);
    }

    public void displaySlotAsFull (int position, BridgeResource resource) {
        TextView tw = findViewById(HueEdgeProvider.btnTextArr[position]);
        tw.setText(resource.getName());
        final Button btn = findViewById(HueEdgeProvider.btnArr[position]);
        final TextView btnTopText = findViewById(HueEdgeProvider.btnTopTextArr[position]);
        btnTopText.setText(resource.getBtnText(ctx));
        btnTopText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                ctx.getResources().getDimensionPixelSize(resource.getBtnTextSize()));
        btnTopText.setTextColor(resource.getBtnTextColor(ctx));
        btn.setBackgroundResource(resource.getBtnBackgroundResource(ctx));
    }
}
