package com.nilstrubkin.hueedge.activity;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nilstrubkin.hueedge.DragEventListener;
import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.ResourceReference;
import com.nilstrubkin.hueedge.adapter.IconGalleryAdapter;
import com.nilstrubkin.hueedge.resources.BridgeResource;
import com.nilstrubkin.hueedge.resources.BridgeCatalogue;
import com.nilstrubkin.hueedge.R;
import com.nilstrubkin.hueedge.adapter.ResourceArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EditActivity extends AppCompatActivity {

    private static final String TAG = EditActivity.class.getSimpleName();
    private final Context ctx = this;

    private HueBridge bridge;
    private HueEdgeProvider.menuCategory currentCategory;
    private Map<HueEdgeProvider.menuCategory, Map<Integer, ResourceReference>> contents;
    private int currentIconBtn;


    private void setBridge(HueBridge bridge) {
        this.bridge = bridge;
    }

    private HueBridge getBridge() {
        return bridge;
    }

    private HueEdgeProvider.menuCategory getCurrentCategory() {
        return currentCategory;
    }

    private void setCurrentCategory(HueEdgeProvider.menuCategory currentCategory) {
        this.currentCategory = currentCategory;
    }

    private Map<HueEdgeProvider.menuCategory, Map<Integer, ResourceReference>> getContents() {
        return contents;
    }

    private void setContents(Map<HueEdgeProvider.menuCategory, Map<Integer, ResourceReference>> contents) {
        this.contents = contents;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started.");

        try {
            setBridge(Objects.requireNonNull(HueBridge.getInstance(ctx)));
        } catch (NullPointerException e){
            // If no bridge is found, start setup activity
            Log.e(TAG, "Entering edit activity but no HueBridge instance was found, starting setup activity");
            HueEdgeProvider.startSetupActivity(ctx);
            return;
        }

        setContentView(R.layout.edit_activity);

        //Set bottom lip color
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(ctx.getColor(R.color.navigation_bar_color_edit));

        //UI elements
        GridView gridViewResources = findViewById(R.id.gridView);
        RecyclerView iconGallery = findViewById(R.id.recycler_icon_gallery);
        Button btnSave = findViewById(R.id.btnSave);
        TextView hueStatus = findViewById(R.id.hueStatus);

        HueBridge br = getBridge();
        setCurrentCategory(br.getCurrentCategory(ctx));
        setContents(br.getContents());

        btnSave.setOnClickListener(v -> {
            if(HueBridge.getInstance(ctx) != null){
                HueBridge.saveAllConfiguration(ctx);
                String toastString = ctx.getString(R.string.toast_saved);
                Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show();
            }
            else
                Log.e(TAG, "Saving the settings but the HueBridge.getInstance() == null");
            finish();
        });

        String ip;
        try {
            ip = Objects.requireNonNull(HueBridge.getInstance(ctx)).getIp();
        }
        catch (NullPointerException e){
            Log.e(TAG, "Trying to enter edit activity but there is no instance of HueBridge");
            e.printStackTrace();
            return;
        }
        hueStatus.setText(ctx.getString(R.string.hue_status, ip));
        hueStatus.setOnClickListener(v -> {
            Intent setupIntent = new Intent(ctx, SetupActivity.class);
            setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(setupIntent);
        });

        panelUpdate();

        ArrayList<ResourceReference> resources = new ArrayList<>();
        Map<String, ? extends BridgeResource> map = null;
        BridgeCatalogue bridgeState = getBridge().getBridgeState();
        switch (getCurrentCategory()) {
            case QUICK_ACCESS:
                map = bridgeState.getLights();
                for (Map.Entry<String, ? extends BridgeResource> entry : map.entrySet()) {
                    resources.add(new ResourceReference(entry.getValue().getCategory(), entry.getValue().getId()));
                }
                map = bridgeState.getRooms();
                for (Map.Entry<String, ? extends BridgeResource> entry : map.entrySet()) {
                    resources.add(new ResourceReference(entry.getValue().getCategory(), entry.getValue().getId()));
                }
                map = bridgeState.getZones();
                for (Map.Entry<String, ? extends BridgeResource> entry : map.entrySet()) {
                    if(!entry.getKey().equals("0"))
                        resources.add(new ResourceReference(entry.getValue().getCategory(), entry.getValue().getId()));
                }
                map = bridgeState.getScenes();
                // The for loop that is supposed to be here can be found right after the switch cases
                break;
            case LIGHTS:
                map = bridgeState.getLights();
                break;
            case ROOMS:
                map = bridgeState.getRooms();
                break;
            case ZONES:
                map = bridgeState.getZones();
                break;
            case SCENES:
                map = bridgeState.getScenes();
                break;
            default:
                Log.e(TAG, "Unknown category!");
                break;
        }
        // Add the defined map to the resources, QUICK_ACCESS case uses this too as it's last step
        for (Map.Entry<String, ? extends BridgeResource> entry : map.entrySet()) {
            resources.add(new ResourceReference(entry.getValue().getCategory(), entry.getValue().getId()));
        }

        ResourceArrayAdapter adapter = new ResourceArrayAdapter(
                this, R.layout.edit_activity_adapter_view_layout, resources);
        adapter.sort((a, b) -> a.compareTo(ctx, b));
        gridViewResources.setAdapter(adapter);

        LinearLayoutManager layoutManager = new GridLayoutManager(ctx, 5);

        List<Integer> icons_res = new ArrayList<>();
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);
        icons_res.add(R.drawable.ic_001_bedside_table);
        icons_res.add(R.drawable.ic_002_alarm_clock);

        IconGalleryAdapter galleryAdapter = new IconGalleryAdapter(icons_res, this::setIcon);
        iconGallery.setAdapter(galleryAdapter);
        iconGallery.setHasFixedSize(true);
        iconGallery.setLayoutManager(layoutManager);

        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show(); //TODO some snackbar code
            }
        });*/
    }

    public void setIcon(View v){
        HueEdgeProvider.vibrate(ctx);
        findViewById(R.id.layout_icon_gallery).setVisibility(View.GONE);

        final Map<Integer, ResourceReference> currentCategoryContents;
        try {
            currentCategoryContents = Objects.requireNonNull(getContents().get(getCurrentCategory()));
        }
        catch (NullPointerException e){
            Log.e(TAG, "Failed to get contents of current category");
            e.printStackTrace();
            return;
        }
        ResourceReference resRef = currentCategoryContents.get(currentIconBtn);
        BridgeResource res;
        try {
            res = getBridge().getResource(Objects.requireNonNull(resRef));
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }

        int icon_res = (int) v.findViewById(R.id.button_icon).getTag();
        resRef.setIconRes(icon_res);
        HueBridge.saveAllConfiguration(ctx);

        final TextView btnTopText = findViewById(HueEdgeProvider.btnTopTextArr[currentIconBtn]);
        final ImageButton btn = findViewById(HueEdgeProvider.btnArr[currentIconBtn]);

        btn.setImageResource(icon_res);
        btn.setColorFilter(res.getBtnTextColor(ctx));
        btnTopText.setVisibility(View.GONE);
    }

    public void panelUpdate() {
        for (int i = 0; i < 10; i++) {
            panelUpdateIndex(i);
        }
    }

    public void panelUpdateIndex (int i){
        HueEdgeProvider.menuCategory cc = getCurrentCategory();
        Map<HueEdgeProvider.menuCategory, Map<Integer, ResourceReference>> contents = getContents();
        if (contents.containsKey(getCurrentCategory())) {
            Map<Integer, ResourceReference> currentCategoryContents;
            try {
                currentCategoryContents = Objects.requireNonNull(contents.get(cc));
            } catch (NullPointerException e) {
                Log.e(TAG, "Trying to enter edit activity panel but failed to get current category contents");
                e.printStackTrace();
                return;
            }
            boolean slotIsFilled = currentCategoryContents.containsKey(i);
            final TextView btnText = findViewById(HueEdgeProvider.btnTextArr[i]);
            final ImageButton btn = findViewById(HueEdgeProvider.btnArr[i]);
            final Button btnDelete = findViewById(HueEdgeProvider.btnDeleteArr[i]);
            final ImageButton btnIcon = findViewById(HueEdgeProvider.btnIconArr[i]);

            if (slotIsFilled) {
                final BridgeResource res;
                final ResourceReference resRef;
                try {
                    resRef = Objects.requireNonNull(currentCategoryContents.get(i));
                    res = getBridge().getResource(resRef);
                } catch (NullPointerException e) {
                    Log.e(TAG, "Failed to load filled slot");
                    e.printStackTrace();
                    return;
                }
                displaySlotAsFull(i, resRef);
                final int finalI = i;
                btn.setOnClickListener(v -> clearSlot(finalI));
                btnDelete.setOnClickListener(v -> clearSlot(finalI));
                btnDelete.setVisibility(View.VISIBLE);
                btnIcon.setOnClickListener(v -> handleIconBtn(finalI));
                btnIcon.setVisibility(View.VISIBLE);
                btn.setOnDragListener(null);
                btnText.setOnDragListener(null);
                btn.setOnLongClickListener(v -> {
                    HueEdgeProvider.vibrate(ctx);
                    ClipData.Item item = new ClipData.Item(String.valueOf(finalI));
                    ClipData dragData = new ClipData(
                            res.getName(),
                            new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN },
                            item);
                    View.DragShadowBuilder myShadow = new View.DragShadowBuilder(btn);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        return v.startDragAndDrop(dragData,  // the data to be dragged
                                myShadow,  // the drag shadow builder
                                resRef,      // pass resource ref
                                0          // flags (not currently used, set to 0)
                        );
                    else
                        //noinspection deprecation
                        return v.startDrag(dragData,  // the data to be dragged
                                myShadow,  // the drag shadow builder
                                resRef,      // pass resource ref
                                0          // flags (not currently used, set to 0)
                        );
                });
            } else {
                displaySlotAsEmpty(i);
            }
        }
    }

    public void clearSlot (int position) {
        HueEdgeProvider.vibrate(ctx);
        displaySlotAsEmpty(position);

        final Map<Integer, ResourceReference> currentCategoryContents;
        try {
            currentCategoryContents = Objects.requireNonNull(getContents().get(getCurrentCategory()));
        }
        catch (NullPointerException e){
            Log.e(TAG, "Failed to get contents of current category");
            e.printStackTrace();
            return;
        }
        currentCategoryContents.remove(position);
        HueBridge.saveAllConfiguration(ctx);
    }

    public void displaySlotAsEmpty (int position) {
        final ImageButton btn = findViewById(HueEdgeProvider.btnArr[position]);
        final TextView btnText = findViewById(HueEdgeProvider.btnTextArr[position]);
        final TextView btnTopText = findViewById(HueEdgeProvider.btnTopTextArr[position]);
        final Button btnDelete = findViewById(HueEdgeProvider.btnDeleteArr[position]);
        final ImageButton btnIcon = findViewById(HueEdgeProvider.btnIconArr[position]);

        btn.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.edit_add_button_background, getTheme()));
        btn.setImageResource(0);
        btnTopText.setText("");
        btnText.setText("");
        btnDelete.setVisibility(View.GONE);
        btnIcon.setVisibility(View.GONE);

        DragEventListener dragListen = new DragEventListener(ctx, position);
        btn.setOnDragListener(dragListen);
        btn.setOnLongClickListener(null);
        btnText.setOnDragListener(dragListen);
    }

    public void displaySlotAsFull (int position, ResourceReference resRef) {
        BridgeResource br;
        try {
            br = getBridge().getResource(resRef);
        } catch (NullPointerException e) {
            Log.e(TAG, "Failed to load filled slot");
            e.printStackTrace();
            return;
        }

        final ImageButton btn = findViewById(HueEdgeProvider.btnArr[position]);
        final TextView btnText = findViewById(HueEdgeProvider.btnTextArr[position]);
        final TextView btnTopText = findViewById(HueEdgeProvider.btnTopTextArr[position]);

        btn.setBackgroundResource(br.getBtnBackgroundResource());
        btnTopText.setText(br.getBtnText(ctx));
        btnText.setText(br.getUnderBtnText());
        btnTopText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                ctx.getResources().getDimensionPixelSize(br.getBtnTextSize(ctx)));
        btnTopText.setTextColor(br.getBtnTextColor(ctx));
        int icon_res = resRef.getIconRes();
        if (icon_res != 0) {
            btn.setImageResource(icon_res);
            btnTopText.setVisibility(View.GONE);
        } else {
            btnTopText.setVisibility(View.VISIBLE);
        }
    }

    public void handleIconBtn(int position){
        currentIconBtn = position;

        final Map<Integer, ResourceReference> currentCategoryContents;
        try {
            currentCategoryContents = Objects.requireNonNull(getContents().get(getCurrentCategory()));
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }
         ResourceReference resRef = currentCategoryContents.get(currentIconBtn);

        int presentIconRes = 0;
        try{
            presentIconRes = Objects.requireNonNull(resRef).getIconRes();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        if(presentIconRes == 0) {
            findViewById(R.id.layout_icon_gallery).setVisibility(View.VISIBLE);
        } else {
            final TextView btnTopText = findViewById(HueEdgeProvider.btnTopTextArr[currentIconBtn]);
            final ImageButton btn = findViewById(HueEdgeProvider.btnArr[currentIconBtn]);

            resRef.setIconRes(0);
            btn.setImageResource(0);
            btnTopText.setVisibility(View.VISIBLE);
        }
    }
}
