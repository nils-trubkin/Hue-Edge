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
import android.widget.Toast;

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

    private HueEdgeProvider.menuCategory currentCategory;
    private HashMap<HueEdgeProvider.menuCategory, HashMap<Integer, BridgeResource>> contents;

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

        String ip = null;
        try {
            ip = Objects.requireNonNull(HueBridge.getInstance(ctx)).getIp();
        }
        catch (NullPointerException ex){
            Log.e(TAG, "Trying to enter edit activity but there is no instance of HueBridge");
            ex.printStackTrace();
        }
        hueStatus.setText(ip);

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
                        btn.setTextSize(ctx.getResources().getDimension(R.dimen.resource_btn_text_size_scene));
                    } else {
                        btn.setTextSize(ctx.getResources().getDimension(R.dimen.resource_btn_text_size_symbol));
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
                        btn.setTextSize(ctx.getResources().getDimension(R.dimen.resource_btn_text_size_scene));
                    } else {
                        btn.setTextSize(ctx.getResources().getDimension(R.dimen.resource_btn_text_size_symbol));
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

        ArrayList<BridgeResource> resourceList = new ArrayList<>();
        HashMap<String, BridgeResource> map = null;
        switch (bridge.getCurrentCategory()) {
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
        if(!bridge.getCurrentCategory().equals(HueEdgeProvider.menuCategory.QUICK_ACCESS)){
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
