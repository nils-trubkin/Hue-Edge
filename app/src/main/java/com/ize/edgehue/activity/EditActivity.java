package com.ize.edgehue.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;

import androidx.appcompat.app.AppCompatActivity;

import com.ize.edgehue.EdgeHueProvider;
import com.ize.edgehue.HueBridge;
import com.ize.edgehue.R;
import com.ize.edgehue.resource.BridgeResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditActivity extends AppCompatActivity {

    private static final String TAG = EditActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        Log.d(TAG, "onCreate: Started.");
        GridView mListView = findViewById(R.id.gridView);

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ArrayList<BridgeResource> resourceList = new ArrayList<>();

        HueBridge bridge = HueBridge.getInstance();

        if (bridge == null){
            Log.e(TAG, "HueBridge.getInstance() == null. Probably missing config");
        }
        assert bridge != null;

        HashMap<String, BridgeResource> map = null;
        switch (EdgeHueProvider.getCurrentCategory()) {
            case LIGHTS:
                map = HueBridge.getInstance().getLights();
                break;
            case ROOMS:
                map = HueBridge.getInstance().getRooms();
                break;
            case ZONES:
                map = HueBridge.getInstance().getZones();
                break;
            case SCENES:
                map = HueBridge.getInstance().getScenes();
                break;
            default:
                Log.w(TAG, "Unknown category!");
                break;
        }
        for (Map.Entry<String, BridgeResource> entry : map.entrySet()) {
            resourceList.add(entry.getValue());
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
