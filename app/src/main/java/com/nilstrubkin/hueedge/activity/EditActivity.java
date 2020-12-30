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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nilstrubkin.hueedge.DragEventListener;
import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.ResourceReference;
import com.nilstrubkin.hueedge.adapter.CatalogueAdapter;
import com.nilstrubkin.hueedge.adapter.ColorGalleryAdapter;
import com.nilstrubkin.hueedge.adapter.IconGalleryAdapter;
import com.nilstrubkin.hueedge.resources.BridgeResource;
import com.nilstrubkin.hueedge.resources.BridgeCatalogue;
import com.nilstrubkin.hueedge.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EditActivity extends AppCompatActivity {

    private static final String TAG = EditActivity.class.getSimpleName();
    private final Context ctx = this;

    private HueBridge bridge;
    private HueEdgeProvider.menuCategory currentCategory;
    private Map<Integer, ResourceReference> currentCategoryContents;
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

    private Map<Integer, ResourceReference> getCurrentCategoryContents() {
        return currentCategoryContents;
    }

    private void setCurrentCategoryContents(Map<Integer, ResourceReference> currentCategoryContents) {
        this.currentCategoryContents = currentCategoryContents;
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
        RecyclerView catalogueView = findViewById(R.id.recycler_resources_catalogue);
        RecyclerView iconGallery = findViewById(R.id.recycler_icons_gallery);
        RecyclerView colorsGallery = findViewById(R.id.recycler_colors_gallery);
        Button btnSave = findViewById(R.id.btnSave);
        TextView hueStatus = findViewById(R.id.hueStatus);
        ImageButton galleryClose = findViewById(R.id.btn_gallery_close);

        HueBridge br = getBridge();
        setCurrentCategory(br.getCurrentCategory(ctx));
        try {
            Map<Integer, ResourceReference> cc = Objects.requireNonNull(br.getContents().get(getCurrentCategory()));
            setCurrentCategoryContents(cc);
        }
        catch (NullPointerException e){
            Log.e(TAG, "Failed to get contents of current category");
            e.printStackTrace();
            return;
        }

        btnSave.setOnClickListener(v -> {
            if(getBridge() != null){
                HueBridge.saveAllConfiguration(ctx);
                String toastString = ctx.getString(R.string.toast_saved);
                Toast.makeText(ctx, toastString, Toast.LENGTH_SHORT).show();
            }
            else
                Log.e(TAG, "Saving the settings but the getBridge() == null");
            finish();
        });

        String ip = getBridge().getIp();
        hueStatus.setText(ctx.getString(R.string.hue_status, ip));
        hueStatus.setOnClickListener(v -> {
            Intent setupIntent = new Intent(ctx, SetupActivity.class);
            setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(setupIntent);
        });
        galleryClose.setOnClickListener(ignored -> findViewById(R.id.layout_icon_gallery).setVisibility(View.GONE));

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

        /*ResourceArrayAdapter adapter = new ResourceArrayAdapter(
                this, R.layout.catalogue_item, resources);
        adapter.sort((a, b) -> a.compareTo(ctx, b));
        gridViewResources.setAdapter(adapter);*/
        RecyclerView.LayoutManager catalogueLayoutMgr = new GridLayoutManager(ctx, 2);
        CatalogueAdapter catalogueAdapter = new CatalogueAdapter(resources);
        catalogueView.setAdapter(catalogueAdapter);
        catalogueView.setHasFixedSize(true);
        catalogueView.setLayoutManager(catalogueLayoutMgr);

        RecyclerView.LayoutManager iconsLayoutMgr = new GridLayoutManager(ctx, 5);
        List<Integer> icons_res = new ArrayList<>();
        icons_res.add(R.drawable.filled_circle);
        icons_res.add(R.drawable.ic_001_musical_note_svg);
        icons_res.add(R.drawable.ic_002_lamp_svg);
        icons_res.add(R.drawable.ic_003_bed_svg);
        icons_res.add(R.drawable.ic_004_desk_svg);
        icons_res.add(R.drawable.ic_005_headphones_svg);
        icons_res.add(R.drawable.ic_006_light_svg);
        icons_res.add(R.drawable.ic_007_gamepad_svg);
        icons_res.add(R.drawable.ic_008_bath_svg);
        icons_res.add(R.drawable.ic_009_couch_svg);
        icons_res.add(R.drawable.ic_010_divan_svg);
        icons_res.add(R.drawable.ic_011_hanger_svg);
        icons_res.add(R.drawable.ic_012_television_svg);
        icons_res.add(R.drawable.ic_013_chair_svg);
        icons_res.add(R.drawable.ic_014_ceiling_fan_svg);
        icons_res.add(R.drawable.ic_015_floor_lamp_svg);
        icons_res.add(R.drawable.ic_016_computer_svg);
        icons_res.add(R.drawable.ic_017_shelving_svg);
        icons_res.add(R.drawable.ic_018_studying_svg);
        icons_res.add(R.drawable.ic_019_picture_svg);

        IconGalleryAdapter galleryAdapter = new IconGalleryAdapter(icons_res, this::setIcon);
        iconGallery.setAdapter(galleryAdapter);
        iconGallery.setHasFixedSize(true);
        iconGallery.setLayoutManager(iconsLayoutMgr);

        List<Integer> colors_res = new ArrayList<>();
        colors_res.add(getColor(R.color.black));
        colors_res.add(getColor(R.color.red));
        colors_res.add(getColor(R.color.orange));
        colors_res.add(getColor(R.color.yellow));
        colors_res.add(getColor(R.color.green));
        colors_res.add(getColor(R.color.cyan));
        colors_res.add(getColor(R.color.blue));
        colors_res.add(getColor(R.color.purple));

        ColorGalleryAdapter colorsAdapter = new ColorGalleryAdapter(colors_res, this::setColor);
        RecyclerView.LayoutManager colorsLayoutMgr = new GridLayoutManager(ctx, colors_res.size());
        colorsGallery.setAdapter(colorsAdapter);
        colorsGallery.setHasFixedSize(true);
        colorsGallery.setLayoutManager(colorsLayoutMgr);


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

        ResourceReference resRef;
        try {
            resRef = Objects.requireNonNull(getCurrentCategoryContents().get(currentIconBtn));
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }

        int iconRes = (int) v.findViewById(R.id.button_icon).getTag();
        if (iconRes == R.drawable.filled_circle) iconRes = 0;
        resRef.setIconRes(iconRes);
        HueBridge.saveAllConfiguration(ctx);

        final TextView btnTopText = findViewById(HueEdgeProvider.btnTopTextArr[currentIconBtn]);
        final ImageButton btn = findViewById(HueEdgeProvider.btnArr[currentIconBtn]);

        btn.setImageResource(iconRes);
        btnTopText.setVisibility(iconRes == 0 ? View.VISIBLE : View.GONE);
    }

    public void setColor(View v){
        HueEdgeProvider.vibrate(ctx);
        findViewById(R.id.layout_icon_gallery).setVisibility(View.GONE);

        ResourceReference resRef = getCurrentCategoryContents().get(currentIconBtn);
        BridgeResource res;
        try {
            res = getBridge().getResource(Objects.requireNonNull(resRef));
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }

        int iconColor = (int) v.findViewById(R.id.button_icon).getTag();
        if (iconColor == getColor(R.color.black)) resRef.setIconColor(0);
        else resRef.setIconColor(iconColor);
        HueBridge.saveAllConfiguration(ctx);

        final TextView btnTopText = findViewById(HueEdgeProvider.btnTopTextArr[currentIconBtn]);
        final ImageButton btn = findViewById(HueEdgeProvider.btnArr[currentIconBtn]);

        int customColor = resRef.getIconColor();
        if(customColor == 0) {
            int defaultColor = res.getBtnTextColor(ctx);
            btn.setColorFilter(defaultColor);
            btnTopText.setTextColor(defaultColor);
        } else {
            btn.setColorFilter(customColor);
            btnTopText.setTextColor(customColor);
        }
    }

    public void panelUpdate() {
        for (int i = 0; i < 10; i++) {
            panelUpdateIndex(i);
        }
    }

    public void panelUpdateIndex (int i){
        boolean slotIsFilled = getCurrentCategoryContents().containsKey(i);
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

    public void clearSlot (int position) {
        HueEdgeProvider.vibrate(ctx);
        displaySlotAsEmpty(position);

        getCurrentCategoryContents().remove(position);
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
        btn.setColorFilter(ctx.getColor(R.color.black));
        btnTopText.setTextColor(ctx.getColor(R.color.black));
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

        int iconRes = resRef.getIconRes();
        Log.e(TAG, "Pos " + position + " icon " + iconRes);
        if (iconRes != 0) {
            btn.setImageResource(iconRes);
            btnTopText.setVisibility(View.GONE);
        } else {
            btnTopText.setVisibility(View.VISIBLE);
        }

        int customColor = resRef.getIconColor();
        Log.e(TAG, "Pos " + position + " col " + customColor);
        if(customColor == 0) {
            int defaultColor = br.getBtnTextColor(ctx);
            btn.setColorFilter(defaultColor);
            btnTopText.setTextColor(defaultColor);
        } else {
            btn.setColorFilter(customColor);
            btnTopText.setTextColor(customColor);
        }
    }

    public void handleIconBtn(int position){
        currentIconBtn = position;
        findViewById(R.id.layout_icon_gallery).setVisibility(View.VISIBLE);
    }
}
