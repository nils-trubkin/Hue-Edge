package com.ize.edgehue.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.ize.edgehue.EdgeHueProvider;
import com.ize.edgehue.HueBridge;
import com.ize.edgehue.R;
import com.ize.edgehue.resource.BridgeResource;
import com.ize.edgehue.resource.LightResource;
import com.ize.edgehue.resource.RoomResource;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

        Iterator<String> keys;
        switch (EdgeHueProvider.getCurrentCategory()) {
            case LIGHTS:
                JSONObject lights = null;
                try {
                    lights = bridge.getState().getJSONObject("lights");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                keys = lights.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    resourceList.add(new LightResource(this, Integer.parseInt(key)));
                }
                break;

            case ROOMS:
                JSONObject groups = null;
                try {
                    groups = HueBridge.getInstance().getState().getJSONObject("groups");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                keys = groups.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        if (groups.getJSONObject(key).getString("type").equals("Room")) {
                            resourceList.add(new RoomResource(this, Integer.parseInt(key)));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            case ZONES:
                JSONObject zones = null;
                try {
                    zones = HueBridge.getInstance().getState().getJSONObject("groups");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                keys = zones.keys();
                while (keys.hasNext() ) {
                    String key = keys.next();
                    try {
                        if (zones.getJSONObject(key).getString("type").equals("Zone")) {
                            resourceList.add(new RoomResource(this, Integer.parseInt(key)));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            /*JSONObject scenes = HueBridge.getInstance().getState().getJSONObject("scenes");
            keys = scenes.keys();
            buttonIndex = 0;
            while(keys.hasNext() && buttonIndex < 10) {
                String key = keys.next();
                JSONObject value = scenes.getJSONObject(key);
                Log.d(TAG, "quickSetup for scenes on id: " + Integer.valueOf(key));
                if (value instanceof JSONObject) {
                    if (scenes.getJSONObject(key).getString("type").equals("GroupScene")) {
                        roomsContent.put(buttonIndex++, new SceneResource(context, Integer.valueOf(key)));
                        if(qaButtonIndex < 8) {
                            quickAccessContent.put(qaButtonIndex++, new RoomResource(context, Integer.valueOf(key)));
                        }
                    }
                }
            }*/
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
