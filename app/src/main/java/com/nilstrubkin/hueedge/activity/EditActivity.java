package com.nilstrubkin.hueedge.activity;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import androidx.constraintlayout.widget.ConstraintLayout;
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

    private IconGalleryAdapter iconAdapter;
    private ColorGalleryAdapter colorAdapter;
    private List<Integer> iconsRes;
    private List<Integer> colorsRes;

    // Samsung review api
    private boolean hasAuthority;
    private String deeplinkUri;
    private int currentScore;

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
        ConstraintLayout galleryTint = findViewById(R.id.layout_tint);
        Button btnReviewNever = findViewById(R.id.button_review_never);
        Button btnReviewLater = findViewById(R.id.button_review_later);
        Button btnReviewNow = findViewById(R.id.button_review_now);

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
            SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            int userSavedTimes = s.getInt(getString(R.string.review_user_saved_times), 0);
            if (userSavedTimes >= 3) finish();
            else {
                SharedPreferences.Editor e = s.edit();
                e.putInt(getString(R.string.review_user_saved_times), ++userSavedTimes);
                e.apply();
                if (userSavedTimes == 3 && hasAuthority){
                    if (deeplinkUri == null) {
                        reviewLater(); // review later if we didn't have time to get the link reply
                        //Toast.makeText(ctx, "reviewLater cuz null", Toast.LENGTH_SHORT).show(); //TODO debugging only
                        finish();
                    }
                    else showReviewDialogue();
                }
                else finish();
            }
        });

        String ip = getBridge().getIp();
        hueStatus.setText(ctx.getString(R.string.hue_status, ip));
        hueStatus.setOnClickListener(v -> {
            Intent setupIntent = new Intent(ctx, SetupActivity.class);
            setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(setupIntent);
        });
        galleryClose.setOnClickListener(ignored -> closeGallery());
        galleryTint.setOnClickListener(ignored -> closeGallery());

        btnReviewNever.setOnClickListener(ignored -> closeGallery());
        btnReviewLater.setOnClickListener(ignored -> reviewLater());
        btnReviewNow.setOnClickListener(ignored -> requestReview());

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.sort((a, b) -> a.compareTo(ctx, b));
        }
        RecyclerView.LayoutManager catalogueLayoutMgr = new GridLayoutManager(ctx, 2);
        CatalogueAdapter catalogueAdapter = new CatalogueAdapter(resources);
        catalogueView.setAdapter(catalogueAdapter);
        catalogueView.setHasFixedSize(true);
        catalogueView.setLayoutManager(catalogueLayoutMgr);

        iconsRes = new ArrayList<>();
        iconsRes.add(R.drawable.ic_000_empty);
        iconsRes.add(R.drawable.ic_003_lamp_1_svg);
        iconsRes.add(R.drawable.ic_004_lamp_svg);
        iconsRes.add(R.drawable.ic_005_floor_lamp_svg);
        iconsRes.add(R.drawable.ic_006_ceiling_fan_svg);
        iconsRes.add(R.drawable.ic_007_weight_svg);
        iconsRes.add(R.drawable.ic_008_sofa_svg);
        iconsRes.add(R.drawable.ic_009_television_svg);
        iconsRes.add(R.drawable.ic_010_computer_svg);
        iconsRes.add(R.drawable.ic_011_ping_pong_svg);
        iconsRes.add(R.drawable.ic_012_gamepad_svg);
        iconsRes.add(R.drawable.ic_013_headphones_svg);
        iconsRes.add(R.drawable.ic_014_open_book_svg);
        iconsRes.add(R.drawable.ic_015_canvas_svg);
        iconsRes.add(R.drawable.ic_016_couch_svg);
        iconsRes.add(R.drawable.ic_017_pot_svg);
        iconsRes.add(R.drawable.ic_018_cutlery_svg);
        iconsRes.add(R.drawable.ic_019_bed_svg);
        iconsRes.add(R.drawable.ic_020_teddy_bear_svg);
        iconsRes.add(R.drawable.ic_021_bathtub_svg);
        iconsRes.add(R.drawable.ic_022_rocking_horse_svg);
        iconsRes.add(R.drawable.ic_023_desk_svg);
        iconsRes.add(R.drawable.ic_024_chair_svg);
        iconsRes.add(R.drawable.ic_025_wc_svg);
        iconsRes.add(R.drawable.ic_026_stairs_svg);
        iconsRes.add(R.drawable.ic_027_upstairs_svg);
        iconsRes.add(R.drawable.ic_028_downstairs_svg);
        iconsRes.add(R.drawable.ic_029_hanger_svg);
        iconsRes.add(R.drawable.ic_030_washing_machine_svg);
        iconsRes.add(R.drawable.ic_031_warehouse_svg);
        iconsRes.add(R.drawable.ic_032_wardrobe_svg);
        iconsRes.add(R.drawable.ic_033_home_svg);
        iconsRes.add(R.drawable.ic_034_support_svg);
        iconsRes.add(R.drawable.ic_035_door_svg);
        iconsRes.add(R.drawable.ic_036_tree_svg);
        iconsRes.add(R.drawable.ic_037_terrace_svg);
        iconsRes.add(R.drawable.ic_038_balcony_svg);
        iconsRes.add(R.drawable.ic_039_car_svg);
        iconsRes.add(R.drawable.ic_040_garage_svg);
        iconsRes.add(R.drawable.ic_041_door_1_svg);
        iconsRes.add(R.drawable.ic_042_rocking_chair_svg);
        iconsRes.add(R.drawable.ic_043_bench_svg);
        iconsRes.add(R.drawable.ic_044_grill_svg);
        iconsRes.add(R.drawable.ic_045_swimming_pool_svg);
        iconsRes.add(R.drawable.ic_046_lightbulb_6_svg);
        iconsRes.add(R.drawable.ic_047_lightbulb_2_svg);
        iconsRes.add(R.drawable.ic_048_lightbulb_4_svg);
        iconsRes.add(R.drawable.ic_049_lightbulb_3_svg);
        iconsRes.add(R.drawable.ic_050_lightbulb_svg);
        iconsRes.add(R.drawable.ic_051_lightbulb_1_svg);
        iconsRes.add(R.drawable.ic_052_lightbulb_5_svg);
        iconsRes.add(R.drawable.ic_053_circle_svg);
        iconsRes.add(R.drawable.ic_054_square_svg);
        iconsRes.add(R.drawable.ic_055_triangle_svg);
        iconsRes.add(R.drawable.ic_056_up_arrow_svg);
        iconsRes.add(R.drawable.ic_057_star_svg);
        iconsRes.add(R.drawable.ic_058_heart_svg);
        iconsRes.add(R.drawable.ic_059_moon_svg);
        iconsRes.add(R.drawable.ic_060_bolt_svg);
        iconsRes.add(R.drawable.ic_061_cube_svg);
        iconsRes.add(R.drawable.ic_062_diamond_svg);
        iconsRes.add(R.drawable.ic_063_sun_svg);
        iconsRes.add(R.drawable.ic_064_fire_svg);

        iconAdapter = new IconGalleryAdapter(iconsRes, this::setIcon);
        RecyclerView.LayoutManager iconsLayoutMgr = new GridLayoutManager(ctx, 5);
        iconGallery.setAdapter(iconAdapter);
        iconGallery.setHasFixedSize(true);
        iconGallery.setLayoutManager(iconsLayoutMgr);

        colorsRes = new ArrayList<>();
        colorsRes.add(getColor(R.color.black));
        colorsRes.add(getColor(R.color.red));
        colorsRes.add(getColor(R.color.orange));
        colorsRes.add(getColor(R.color.yellow));
        colorsRes.add(getColor(R.color.green));
        colorsRes.add(getColor(R.color.cyan));
        colorsRes.add(getColor(R.color.blue));
        colorsRes.add(getColor(R.color.purple));

        colorAdapter = new ColorGalleryAdapter(colorsRes, this::setColor);
        RecyclerView.LayoutManager colorsLayoutMgr = new GridLayoutManager(ctx, colorsRes.size());
        colorsGallery.setAdapter(colorAdapter);
        colorsGallery.setHasFixedSize(true);
        colorsGallery.setLayoutManager(colorsLayoutMgr);

        if(galaxyStoreVerChk()) checkReviewAuth();
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
        //closeGallery();

        ResourceReference resRef;
        try {
            resRef = Objects.requireNonNull(getCurrentCategoryContents().get(currentIconBtn));
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }

        int iconRes = (int) v.findViewById(R.id.icon).getTag();
        if (iconRes == R.drawable.ic_000_empty) iconRes = 0;
        resRef.setIconRes(iconRes);
        HueBridge.saveAllConfiguration(ctx);

        final TextView btnTopText = findViewById(HueEdgeProvider.btnTopTextArr[currentIconBtn]);
        final ImageButton btn = findViewById(HueEdgeProvider.btnArr[currentIconBtn]);

        btn.setImageResource(iconRes);
        btnTopText.setVisibility(iconRes == 0 ? View.VISIBLE : View.GONE);
    }

    public void setColor(View v){
        HueEdgeProvider.vibrate(ctx);
        //closeGallery();

        ResourceReference resRef = getCurrentCategoryContents().get(currentIconBtn);
        BridgeResource res;
        try {
            res = getBridge().getResource(Objects.requireNonNull(resRef));
        } catch (NullPointerException e) {
            e.printStackTrace();
            return;
        }

        int iconColor = (int) v.findViewById(R.id.icon).getTag();
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
            iconAdapter.setSelectedColor(0x252525);
            iconAdapter.notifyDataSetChanged();
        } else {
            btn.setColorFilter(customColor);
            btnTopText.setTextColor(customColor);
            iconAdapter.setSelectedColor(customColor);
            iconAdapter.notifyDataSetChanged();
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
            btnIcon.setOnClickListener(v -> openGallery(finalI, resRef.getIconRes(), resRef.getIconColor()));
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
        if (iconRes != 0) {
            btn.setImageResource(iconRes);
            btnTopText.setVisibility(View.GONE);
        } else {
            btnTopText.setVisibility(View.VISIBLE);
        }

        int customColor = resRef.getIconColor();
        if (customColor == 0) {
            int defaultColor = br.getBtnTextColor(ctx);
            btn.setColorFilter(defaultColor);
            btnTopText.setTextColor(defaultColor);
        } else {
            btn.setColorFilter(customColor);
            btnTopText.setTextColor(customColor);
        }
    }

    public void openGallery(int position, int presentIconRes, int presentColor){
        currentIconBtn = position;
        int presentIconIndex;
        int presentColorIndex;
        if (presentIconRes == 0) presentIconIndex = 0;
        else presentIconIndex = iconsRes.indexOf(presentIconRes);
        if (presentColor == 0) presentColorIndex = 0;
        else presentColorIndex = colorsRes.indexOf(presentColor);
        iconAdapter.setSelectedPos(presentIconIndex);
        iconAdapter.setSelectedColor(colorsRes.get(presentColorIndex));
        iconAdapter.notifyDataSetChanged();
        colorAdapter.setSelectedPos(presentColorIndex);
        colorAdapter.notifyItemChanged(presentColorIndex);
        findViewById(R.id.layout_icon_gallery).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_tint).setVisibility(View.VISIBLE);
        RecyclerView iconGallery = findViewById(R.id.recycler_icons_gallery);
        iconGallery.smoothScrollToPosition(presentIconIndex);
    }

    public void closeGallery(){
        findViewById(R.id.layout_icon_gallery).setVisibility(View.GONE);
        findViewById(R.id.layout_tint).setVisibility(View.GONE);
        findViewById(R.id.layout_review).setVisibility(View.GONE);
        iconAdapter.notifyItemChanged(iconAdapter.getSelectedPos());
        iconAdapter.setSelectedPos(RecyclerView.NO_POSITION);
        iconAdapter.setSelectedColor(0x252525);
        colorAdapter.notifyItemChanged(colorAdapter.getSelectedPos());
        colorAdapter.setSelectedPos(RecyclerView.NO_POSITION);
    }

    private boolean galaxyStoreVerChk(){
        ApplicationInfo ai;
        try {
            ai = getPackageManager().getApplicationInfo(
                    "com.sec.android.app.samsungapps", PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        int inappReviewVersion =
                ai.metaData.getInt("com.sec.android.app.samsungapps.review.inappReview", 0);
        return inappReviewVersion > 0;
    }

    private void checkReviewAuth(){
        Intent intent = new Intent("com.sec.android.app.samsungapps.REQUEST_INAPP_REVIEW_AUTHORITY");
        intent.setPackage("com.sec.android.app.samsungapps");
        intent.putExtra("callerPackage", getPackageName());
        sendBroadcast(intent);

        // set up response fetch
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.sec.android.app.samsungapps.RESPONSE_INAPP_REVIEW_AUTHORITY");
        BroadcastReceiver authorityReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                hasAuthority = intent.getBooleanExtra("hasAuthority", false);
                deeplinkUri = intent.getStringExtra("deeplinkUri");
                currentScore = intent.getIntExtra("currentScore", -1);
                if (currentScore == 10) hasAuthority = false;
                //hasAuthority = true; //TODO debugging only
            }
        };
        registerReceiver(authorityReciever, filter);
    }

    private void requestReview(){
        closeGallery();
        Intent reviewIntent = new Intent();
        reviewIntent.setData(Uri.parse(deeplinkUri));
        reviewIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        startActivity(reviewIntent);
    }

    private void showReviewDialogue(){
        findViewById(R.id.layout_review).setVisibility(View.VISIBLE);
        findViewById(R.id.layout_tint).setVisibility(View.VISIBLE);
    }

    private void reviewLater(){
        closeGallery();
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor e = s.edit();
        e.putInt(getString(R.string.review_user_saved_times), 0);
        e.apply();
    }
}
