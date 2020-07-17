package com.ize.hueedge.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

    //UI elements
    private GridView mListView;
    private Button btnSave;
    private TextView hueStatus;

    private HueEdgeProvider.menuCategory currentCategory;
    private HashMap<HueEdgeProvider.menuCategory, HashMap<Integer, BridgeResource>> contents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started.");

        setContentView(R.layout.edit_activity);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(Color.rgb(30,30,30));

        mListView = findViewById(R.id.gridView);
        btnSave = findViewById(R.id.btnSave);
        hueStatus = findViewById(R.id.hueStatus);

        currentCategory = HueEdgeProvider.getCurrentCategory();
        contents = HueEdgeProvider.getContents(this);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assert HueBridge.getInstance(ctx) != null;
                if(HueBridge.getInstance(ctx) != null){
                    HueEdgeProvider.saveAllConfiguration(ctx);
                }
                else{
                    Log.e(TAG, "Saving the settings but the HueBridge.getInstance() == null");
                }
                finish();
            }
        });

        String ip = null;
        try {
            ip = Objects.requireNonNull(HueBridge.getInstance(ctx)).getIp();
        }
        catch (NullPointerException ex){
            Log.e(TAG, "Trying to enter edit activity but there is no instance of HueBridge");
            ex.printStackTrace();
        }
        hueStatus.setText(ip);

        if(HueEdgeProvider.isBridgeConfigured()) {
            for (int i = 0; i < 10; i++) {
                if (contents.containsKey(currentCategory)) {
                    final HashMap<Integer, BridgeResource> currentCategoryContents = contents.get(currentCategory);
                    boolean slotIsFilled = false;
                    try {
                        slotIsFilled = Objects.requireNonNull(currentCategoryContents).containsKey(i);
                    } catch (NullPointerException ex) {
                        Log.e(TAG, "Trying to enter edit activity panel but failed to get current category contents");
                        ex.printStackTrace();
                    }
                    if (slotIsFilled) {
                        BridgeResource resource;
                        try {
                            resource = Objects.requireNonNull(currentCategoryContents).get(i);
                        } catch (NullPointerException ex) {
                            Log.e(TAG, "Failed to load filled slot");
                            ex.printStackTrace();
                            break;
                        }
                        assert resource != null;
                        TextView tw = findViewById(HueEdgeProvider.btnTextArr[i]);
                        tw.setText(resource.getName(ctx));
                        Button btn = findViewById(HueEdgeProvider.btnArr[i]);
                        btn.setText(resource.getBtnText(ctx));
                        if (resource.getCategory().equals("scenes")) {
                            btn.setTextSize(10);
                        } else {
                            btn.setTextSize(14);
                        }
                        btn.setTextColor(resource.getBtnTextColor(ctx));
                        btn.setBackgroundResource(resource.getBtnBackgroundResource(ctx));
                        final int finalI = i;
                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                clearSlot(finalI);
                            }
                        });
                        Button btnDelete = findViewById(HueEdgeProvider.btnDeleteArr[i]);
                        btnDelete.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                clearSlot(finalI);
                            }
                        });
                        btnDelete.setVisibility(View.VISIBLE);
                        if (resource.getCategory().equals("scenes")) {
                            btn.setTextSize(10);
                        } else {
                            btn.setTextSize(14);
                        }
                    } else {
                        TextView tw = findViewById(HueEdgeProvider.btnTextArr[i]);
                        tw.setText("");
                        Button btn = findViewById(HueEdgeProvider.btnArr[i]);
                        btn.setText("");
                        btn.setBackground(getResources().getDrawable(R.drawable.edit_add_button_background, getTheme()));
                        Button btnDelete = findViewById(HueEdgeProvider.btnDeleteArr[i]);
                        btnDelete.setVisibility(View.GONE);
                    }
                }
            }
        }

        ArrayList<BridgeResource> resourceList = new ArrayList<>();

        HueBridge bridge = HueBridge.getInstance(ctx);

        if (bridge == null){
            Log.e(TAG, "HueBridge.getInstance() == null. Probably missing config");
        }
        assert bridge != null;

        HashMap<String, BridgeResource> map = null;
        switch (HueEdgeProvider.getCurrentCategory()) {
            case QUICK_ACCESS:
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
        if(HueEdgeProvider.getCurrentCategory() == HueEdgeProvider.menuCategory.QUICK_ACCESS){
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
        }
        else {
            assert map != null;
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
        TextView tw = findViewById(HueEdgeProvider.btnTextArr[position]);
        tw.setText("");
        Button btn = findViewById(HueEdgeProvider.btnArr[position]);
        btn.setText("");
        btn.setBackground(getResources().getDrawable(R.drawable.edit_add_button_background, getTheme()));
        Button btnDelete = findViewById(HueEdgeProvider.btnDeleteArr[position]);
        btnDelete.setVisibility(View.GONE);
    }
}
