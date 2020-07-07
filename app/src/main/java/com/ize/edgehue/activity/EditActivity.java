package com.ize.edgehue.activity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ize.edgehue.EdgeHueProvider;
import com.ize.edgehue.HueBridge;
import com.ize.edgehue.R;
import com.ize.edgehue.resource.BridgeResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditActivity extends AppCompatActivity {

    private static final String TAG = EditActivity.class.getSimpleName();

    Context ctx = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        Log.d(TAG, "onCreate: Started.");
        GridView mListView = findViewById(R.id.gridView);
        Button btnSave = findViewById(R.id.btnSave);
        EdgeHueProvider.menuCategory currentCategory = EdgeHueProvider.getCurrentCategory();
        HashMap<EdgeHueProvider.menuCategory, HashMap<Integer, BridgeResource>> contents =
                EdgeHueProvider.getContents();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assert HueBridge.getInstance() != null;
                if(HueBridge.getInstance() != null){
                    HueBridge.getInstance().saveConfigurationToMemory(ctx);
                }
                else{
                    Log.e(TAG, "Saving the settings but the HueBridge.getInstance() == null");
                }
                finish();
            }
        });

        if(currentCategory != EdgeHueProvider.menuCategory.NO_BRIDGE) {
            for (int i = 0; i < 10; i++) {
                if (Objects.requireNonNull(contents.get(currentCategory)).containsKey(i)) {
                    BridgeResource resource = Objects.requireNonNull(contents.get(currentCategory)).get(i);
                    if(resource == null) {
                        Log.wtf(TAG, "resource == null");
                    }
                    assert resource != null;
                    TextView tw = findViewById(EdgeHueProvider.btnTextArr[i]);
                    tw.setText(resource.getName());
                    Button btn = findViewById(EdgeHueProvider.btnArr[i]);
                    btn.setText(resource.getBtnText());
                    btn.setTextColor(resource.getBtnTextColor());
                    btn.setBackgroundResource(resource.getBtnBackgroundResource());
                    if(resource.getCategory().equals("scenes")) {
                        btn.setTextSize(10);
                    }
                } else {/*
                    contentView.setTextViewText(btnTextArr[i], "");
                    contentView.setTextViewText(btnArr[i], "+");
                    contentView.setTextColor(btnArr[i], (ContextCompat.getColor(context, R.color.white)));
                    contentView.setInt(btnArr[i], "setBackgroundResource",
                            R.drawable.add_button_background);*/
                }
            }
        }

        ArrayList<BridgeResource> resourceList = new ArrayList<>();

        HueBridge bridge = HueBridge.getInstance();

        if (bridge == null){
            Log.e(TAG, "HueBridge.getInstance() == null. Probably missing config");
        }
        assert bridge != null;

        HashMap<String, BridgeResource> map = null;
        switch (EdgeHueProvider.getCurrentCategory()) {
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
        if(EdgeHueProvider.getCurrentCategory() == EdgeHueProvider.menuCategory.QUICK_ACCESS){
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
            for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
                resourceList.add(entry.getValue());
            }
        }
        ResourceArrayAdapter adapter = new ResourceArrayAdapter(this, R.layout.adapter_view_layout, resourceList);
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
}
