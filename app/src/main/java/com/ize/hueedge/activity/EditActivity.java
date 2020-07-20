package com.ize.hueedge.activity;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ize.hueedge.DragEventListener;
import com.ize.hueedge.HueEdgeProvider;
import com.ize.hueedge.HueBridge;
import com.ize.hueedge.R;
import com.ize.hueedge.BridgeResource;
import com.ize.hueedge.adapter.ResourceArrayAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditActivity extends AppCompatActivity {

    private static final String TAG = EditActivity.class.getSimpleName();
    private final Context ctx = this;

    private HueEdgeProvider.menuCategory currentCategory;
    private HashMap<HueEdgeProvider.menuCategory, HashMap<Integer, BridgeResource>> contents;

    public HueEdgeProvider.menuCategory getCurrentCategory() {
        return currentCategory;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started.");

        setContentView(R.layout.edit_activity);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(ctx.getColor(R.color.navigation_bar_color_edit));

        //UI elements
        GridView mListView = findViewById(R.id.gridView);
        Button btnSave = findViewById(R.id.btnSave);
        TextView hueStatus = findViewById(R.id.hueStatus);

        HueBridge bridge;
        try {
            bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
        } catch (NullPointerException ex){
            Log.e(TAG, "Entering edit activity but no HueBridge instance was found");
            ex.printStackTrace();
            return;
        }

        currentCategory = bridge.getCurrentCategory();
        contents = bridge.getContents();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assert HueBridge.getInstance(ctx) != null;
                if(HueBridge.getInstance(ctx) != null){
                    HueEdgeProvider.saveAllConfiguration(ctx);
                    String toastString = ctx.getString(R.string.toast_saved);
                    Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
                }
                else{
                    Log.e(TAG, "Saving the settings but the HueBridge.getInstance() == null");
                }
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

        ArrayList<BridgeResource> resourceList = new ArrayList<>();
        HashMap<String, BridgeResource> map = null;
        switch (currentCategory) {
            case QUICK_ACCESS:
                map = bridge.getLights();
                for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
                    resourceList.add(entry.getValue());
                }
                map = bridge.getRooms();
                for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
                    resourceList.add(entry.getValue());
                }
                map = bridge.getZones();
                for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
                    resourceList.add(entry.getValue());
                }
                map = bridge.getScenes();
                for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
                    resourceList.add(entry.getValue());
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
                resourceList.add(entry.getValue());
            }
        }
        ResourceArrayAdapter adapter = new ResourceArrayAdapter(this, R.layout.edit_activity_adapter_view_layout, resourceList);
        mListView.setAdapter(adapter);

        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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
            TextView tw = findViewById(HueEdgeProvider.btnTextArr[i]);
            final Button btn = findViewById(HueEdgeProvider.btnArr[i]);
            Button btnDelete = findViewById(HueEdgeProvider.btnDeleteArr[i]);

            if (slotIsFilled) {
                final BridgeResource resource;
                try {
                    resource = Objects.requireNonNull(currentCategoryContents).get(i);
                } catch (NullPointerException ex) {
                    Log.e(TAG, "Failed to load filled slot");
                    ex.printStackTrace();
                    return;
                }
                assert resource != null;
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
                if (resource.getCategory().equals("scenes")) {
                    btn.setTextSize(ctx.getResources().getDimension(R.dimen.resource_btn_text_size_scene));
                } else {
                    btn.setTextSize(ctx.getResources().getDimension(R.dimen.resource_btn_text_size_symbol));
                }
                btn.setOnDragListener(null);
                tw.setOnDragListener(null);
                // Sets a long click listener for the ImageView using an anonymous listener object that
                // implements the OnLongClickListener interface
                /*btn.setOnLongClickListener(new View.OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {

                        // Create a new ClipData.
                        // This is done in two steps to provide clarity. The convenience method
                        // ClipData.newPlainText() can create a plain text ClipData in one step.
                        ClipData.Item item = new ClipData.Item(String.valueOf(finalI));
                        // Create a new ClipData using the tag as a label, the plain text MIME type, and
                        // the already-created item. This will create a new ClipDescription object within the
                        // ClipData, and set its MIME type entry to "text/plain"
                        ClipData dragData = new ClipData(
                                resource.getName(ctx),
                                new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN },
                                item);

                        // Instantiates the drag shadow builder.
                        View.DragShadowBuilder myShadow = new View.DragShadowBuilder(btn);
                        // Starts the drag
                        return v.startDragAndDrop(dragData,  // the data to be dragged
                                myShadow,  // the drag shadow builder
                                resource,      // pass resource
                                0          // flags (not currently used, set to 0)
                        );
                    }
                });*/
                btn.setOnTouchListener(new View.OnTouchListener(){
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            ClipData.Item item = new ClipData.Item(String.valueOf(finalI));
                            ClipData dragData = new ClipData(
                                    resource.getName(ctx),
                                    new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN },
                                    item);
                            View.DragShadowBuilder myShadow = new View.DragShadowBuilder(btn);
                            return v.startDragAndDrop(dragData,  // the data to be dragged
                                    myShadow,  // the drag shadow builder
                                    resource,      // pass resource
                                    0          // flags (not currently used, set to 0)
                            );
                        }
                        else
                            return false;
                    }
                });
            } else {
                displaySlotAsEmpty(btn, btnDelete, tw);
                DragEventListener dragListen = new DragEventListener(ctx, i);
                btn.setOnDragListener(dragListen);
                tw.setOnDragListener(dragListen);
                btn.setOnTouchListener(null);
            }
        }
    }

    public void clearSlot (int position) {
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
        TextView tw = findViewById(HueEdgeProvider.btnTextArr[position]);
        Button btnDelete = findViewById(HueEdgeProvider.btnDeleteArr[position]);
        displaySlotAsEmpty(btn, btnDelete, tw);
        // Creates a new drag event listener
        DragEventListener dragListen = new DragEventListener(ctx, position);
        // Sets the drag event listener for the View
        btn.setOnDragListener(dragListen);

        tw.setOnDragListener(dragListen);
        btn.setOnTouchListener(null);
    }

    public void displaySlotAsEmpty (Button btn, Button btnDelete, TextView tw) {
        tw.setText("-----------------------");
        btn.setText("");
        btn.setBackground(getResources().getDrawable(R.drawable.edit_add_button_background, getTheme()));
        btnDelete.setVisibility(View.GONE);
    }

    public void displaySlotAsFull (int position, BridgeResource resource) {
        TextView tw = findViewById(HueEdgeProvider.btnTextArr[position]);
        tw.setText(resource.getName(ctx));
        final Button btn = findViewById(HueEdgeProvider.btnArr[position]);
        btn.setText(resource.getBtnText(ctx));
        if (resource.getCategory().equals("scenes")) {
            btn.setTextSize(ctx.getResources().getDimension(R.dimen.resource_btn_text_size_scene));
        } else {
            btn.setTextSize(ctx.getResources().getDimension(R.dimen.resource_btn_text_size_symbol));
        }
        btn.setTextColor(resource.getBtnTextColor(ctx));
        btn.setBackgroundResource(resource.getBtnBackgroundResource(ctx));
    }
}
