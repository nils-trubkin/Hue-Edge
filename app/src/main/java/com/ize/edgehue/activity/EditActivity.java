package com.ize.edgehue.activity;

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

import com.ize.edgehue.EdgeHueProvider;
import com.ize.edgehue.HueBridge;
import com.ize.edgehue.R;
import com.ize.edgehue.BridgeResource;

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

    private EdgeHueProvider.menuCategory currentCategory;
    private HashMap<EdgeHueProvider.menuCategory, HashMap<Integer, BridgeResource>> contents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started.");

        setContentView(R.layout.activity_edit);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(Color.rgb(53,53,53));

        mListView = findViewById(R.id.gridView);
        btnSave = findViewById(R.id.btnSave);
        hueStatus = findViewById(R.id.hueStatus);

        currentCategory = EdgeHueProvider.getCurrentCategory();
        contents = EdgeHueProvider.getContents();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assert HueBridge.getInstance(ctx) != null;
                if(HueBridge.getInstance(ctx) != null){
                    EdgeHueProvider.saveAllConfiguration(ctx);
                }
                else{
                    Log.e(TAG, "Saving the settings but the HueBridge.getInstance() == null");
                }
                finish();
            }
        });

        hueStatus.setText(HueBridge.getInstance(ctx).getIp());

        if(currentCategory != EdgeHueProvider.menuCategory.NO_BRIDGE) {
            for (int i = 0; i < 10; i++) {
                if (contents.containsKey(currentCategory) && contents.get(currentCategory).containsKey(i)) {
                    BridgeResource resource = Objects.requireNonNull(contents.get(currentCategory)).get(i);
                    if(resource == null) {
                        Log.wtf(TAG, "resource == null");
                    }
                    assert resource != null;
                    TextView tw = findViewById(EdgeHueProvider.btnTextArr[i]);
                    tw.setText(resource.getName(ctx));
                    Button btn = findViewById(EdgeHueProvider.btnArr[i]);
                    btn.setText(resource.getBtnText(ctx));
                    btn.setTextColor(resource.getBtnTextColor(ctx));
                    btn.setBackgroundResource(resource.getBtnBackgroundResource(ctx));
                    Button btnDelete = findViewById(EdgeHueProvider.btnDeleteArr[i]);
                    final int finalI = i;
                    btnDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            contents.get(currentCategory).remove(finalI);
                            EdgeHueProvider.saveAllConfiguration(ctx);
                            TextView tw = findViewById(EdgeHueProvider.btnTextArr[finalI]);
                            tw.setText("");
                            Button btn = findViewById(EdgeHueProvider.btnArr[finalI]);
                            btn.setText("");
                            btn.setBackground(getResources().getDrawable(R.drawable.edit_add_button_background, getTheme()));
                            Button btnDelete = findViewById(EdgeHueProvider.btnDeleteArr[finalI]);
                            btnDelete.setVisibility(View.GONE);
                        }
                    });
                    btnDelete.setVisibility(View.VISIBLE);
                    if(resource.getCategory().equals("scenes")) {
                        btn.setTextSize(10);
                    }
                    else{
                        btn.setTextSize(14);
                    }
                } else {
                    TextView tw = findViewById(EdgeHueProvider.btnTextArr[i]);
                    tw.setText("");
                    Button btn = findViewById(EdgeHueProvider.btnArr[i]);
                    btn.setText("");
                    btn.setBackground(getResources().getDrawable(R.drawable.edit_add_button_background, getTheme()));
                    Button btnDelete = findViewById(EdgeHueProvider.btnDeleteArr[i]);
                    btnDelete.setVisibility(View.GONE);
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
            assert map != null;
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
