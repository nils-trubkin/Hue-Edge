package com.ize.edgehue.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.ize.edgehue.HueBridge;
import com.ize.edgehue.R;
import com.philips.lighting.hue.sdk.wrapper.HueLog;
import com.philips.lighting.hue.sdk.wrapper.Persistence;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryImpl;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;
import com.philips.lighting.hue.sdk.wrapper.utilities.InitSdk;

import org.w3c.dom.Text;

import java.util.List;

public class EdgeSetup extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener  {

    static {
        System.loadLibrary("huesdk");
    }

    private static final String TAG = EdgeSetup.class.getSimpleName();

    private BridgeDiscovery bridgeDiscovery;
    private List<BridgeDiscoveryResult> bridgeDiscoveryResults;

    // UI elements
    private ListView bridgeDiscoveryListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        TextView bbridgeDiscoveryListView = findViewById(R.id.textView);
        //bridgeDiscoveryListView.setOnItemClickListener(this);
        bbridgeDiscoveryListView.setVisibility(View.GONE);


        //bridgeDiscoveryRecyclerView = (RecyclerView)findViewById(R.id.bridge_discovery_result_list);

        InitSdk.setApplicationContext(getApplicationContext());
        Persistence.setStorageLocation(getFilesDir().getAbsolutePath(), "EdgeHue");
        HueLog.setConsoleLogLevel(HueLog.LogLevel.DEBUG);
    }

    /**
     * Start the bridge discovery search
     * Read the documentation on meethue for an explanation of the bridge discovery options
     */
    public void startBridgeDiscovery() {
        Log.i(TAG, "startBridgeDiscovery()");
        HueBridge.deleteInstance();

        bridgeDiscovery = new BridgeDiscoveryImpl();
        // ALL Include [UPNP, IPSCAN, NUPNP, MDNS] but in some nets UPNP, NUPNP and MDNS is not working properly
        bridgeDiscovery.search(bridgeDiscoveryCallback);
    }

    /**
     * Stops the bridge discovery if it is still running
     */
    public void stopBridgeDiscovery() {
        Log.i(TAG, "stopBridgeDiscovery()");
        if (bridgeDiscovery != null) {
            bridgeDiscovery.stop();
            bridgeDiscovery = null;
        }
    }

    private BridgeDiscovery.Callback bridgeDiscoveryCallback = new BridgeDiscovery.Callback() {
        @Override
        public void onFinished(final List<BridgeDiscoveryResult> results, final BridgeDiscovery.ReturnCode returnCode) {
            // Set to null to prevent stopBridgeDiscovery from stopping it
            bridgeDiscovery = null;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (returnCode == BridgeDiscovery.ReturnCode.SUCCESS) {
                        bridgeDiscoveryListView.setAdapter(new BridgeDiscoveryResultAdapter(getApplicationContext(), results));
                        bridgeDiscoveryResults = results;
                        Log.i(TAG, "Bridge discovery found " + results.size() + " bridge(s) in the network.");
                        //updateUI(UIState.BridgeDiscoveryResults, "Found " + results.size() + " bridge(s) in the network.");

                        /*Fragment newFragment = new ResultFragment();
                        getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, newFragment)
                        .setPrimaryNavigationFragment(newFragment)
                        .commit();*/
                        findViewById(R.id.bridge_discovery_result_list).setVisibility(View.VISIBLE);
                        findViewById(R.id.fragment_search_textview).setVisibility(View.GONE);
                        findViewById(R.id.fragment_search_button).setVisibility(View.GONE);
                    } else if (returnCode == BridgeDiscovery.ReturnCode.STOPPED) {
                        Log.i(TAG, "Bridge discovery stopped.");
                    } else {
                        Log.i(TAG, "Bridge discovery error.");
                        //updateUI(UIState.Idle, "Error doing bridge discovery: " + returnCode);
                    }
                }
            });
        }
    };

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String bridgeIp = bridgeDiscoveryResults.get(i).getIp();
        Log.i(TAG, "!!!!!!!Selected Bridge " + bridgeIp);
        //connectToBridge(bridgeIp);
    }

    @Override
    public void onClick(View view) {

    }
}
