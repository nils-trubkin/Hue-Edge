package com.ize.edgehue.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ize.edgehue.EdgeHueProvider;
import com.ize.edgehue.HueBridge;
import com.ize.edgehue.R;
import com.ize.edgehue.api.JsonCustomRequest;
import com.ize.edgehue.api.RequestQueueSingleton;
import com.philips.lighting.hue.sdk.wrapper.HueLog;
import com.philips.lighting.hue.sdk.wrapper.Persistence;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryImpl;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;
import com.philips.lighting.hue.sdk.wrapper.utilities.InitSdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public class EdgeSetup extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, Serializable {

    /*

    Log.d(TAG, "Instantiating HueBridge singleton");
                HueBridge.getInstance(
                        "192.168.69.166",
                        "aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD")
                        .requestHueState(getActivity());
                Log.d(TAG, "getInstance() returns (!= null):" + HueBridge.getInstance());

     */

    static {
        System.loadLibrary("huesdk");
    }

    private transient static final String TAG = EdgeSetup.class.getSimpleName();
    private transient final Context ctx = this;

    private transient BridgeDiscovery bridgeDiscovery;

    private transient List<BridgeDiscoveryResult> bridgeDiscoveryResults;

    private transient int requestAmount;

    // UI elements
    private transient TextView statusTextView;
    private transient ListView bridgeDiscoveryListView;
    private transient TextView bridgeIpTextView;
    private transient View pushlinkImage;
    private transient Button bridgeDiscoveryButton;
    private transient Button cheatButton;
    private transient Button bridgeDiscoveryCancelButton;
    private transient Button quickButton;
    private transient Button customButton;
    private transient Button finishButton;
    private transient Button removeButton;
    private transient Button yesButton;
    private transient Button noButton;

    enum UIState {
        Welcome,
        Search,
        Results,
        Connecting,
        Error,
        Auth,
        Settings,
        Final,
        Confirmation
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(Color.rgb(53,53,53));

        // Setup the UI
        statusTextView = findViewById(R.id.status_text);
        bridgeDiscoveryListView = findViewById(R.id.bridge_discovery_result_list);
        bridgeDiscoveryListView.setOnItemClickListener(this);
        bridgeIpTextView = findViewById(R.id.bridge_ip_text);
        pushlinkImage = findViewById(R.id.pushlink_image);
        bridgeDiscoveryButton = findViewById(R.id.bridge_discovery_button);
        bridgeDiscoveryButton.setOnClickListener(this);
        cheatButton = findViewById(R.id.cheat_button);
        cheatButton.setOnClickListener(this);
        bridgeDiscoveryCancelButton = findViewById(R.id.bridge_discovery_cancel_button);
        bridgeDiscoveryCancelButton.setOnClickListener(this);
        quickButton = findViewById(R.id.quick_setup_button);
        quickButton.setOnClickListener(this);
        customButton = findViewById(R.id.custom_setup_button);
        customButton.setOnClickListener(this);
        finishButton = findViewById(R.id.finish_button);
        finishButton.setOnClickListener(this);
        removeButton = findViewById(R.id.remove_button);
        removeButton.setOnClickListener(this);
        yesButton = findViewById(R.id.yes_button);
        yesButton.setOnClickListener(this);
        noButton = findViewById(R.id.no_button);
        noButton.setOnClickListener(this);
        InitSdk.setApplicationContext(getApplicationContext());
        Persistence.setStorageLocation(getFilesDir().getAbsolutePath(), "EdgeHue");
        HueLog.setConsoleLogLevel(HueLog.LogLevel.DEBUG);

        if (HueBridge.getInstance(ctx) == null){
            updateUI(UIState.Welcome);
        }
        else{
            updateUI(UIState.Final);
        }
    }

    /**
     * Start the bridge discovery search
     * Read the documentation on meethue for an explanation of the bridge discovery options
     */
    public void startBridgeDiscovery() {
        Log.i(TAG, "startBridgeDiscovery()");
        HueBridge.deleteInstance();
        stopBridgeDiscovery();
        bridgeDiscovery = new BridgeDiscoveryImpl();
        // ALL Include [UPNP, IPSCAN, NUPNP, MDNS] but in some nets UPNP, NUPNP and MDNS is not working properly
        bridgeDiscovery.search(bridgeDiscoveryCallback);

        updateUI(UIState.Search);
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

        updateUI(UIState.Welcome);
    }

    private final BridgeDiscovery.Callback bridgeDiscoveryCallback = new BridgeDiscovery.Callback() {
        @Override
        public void onFinished(@NonNull final List<BridgeDiscoveryResult> results, @NonNull final BridgeDiscovery.ReturnCode returnCode) {
            // Set to null to prevent stopBridgeDiscovery from stopping it
            bridgeDiscovery = null;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (returnCode == BridgeDiscovery.ReturnCode.SUCCESS) {
                        bridgeDiscoveryListView.setAdapter(new BridgeDiscoveryResultAdapter(getApplicationContext(), results));
                        bridgeDiscoveryResults = results;
                        Log.i(TAG, "Bridge discovery found " + results.size() + " bridge(s) in the network.");

                        updateUI(UIState.Results);

                    } else if (returnCode == BridgeDiscovery.ReturnCode.STOPPED) {
                        Log.i(TAG, "Bridge discovery stopped.");
                        updateUI(UIState.Welcome);
                    } else {
                        Log.i(TAG, "Bridge discovery error.");
                        updateUI(UIState.Error);
                    }
                }
            });
        }
    };

    private void sendAuthRequest(int i, JSONObject j, String bridgeIp){
        if(i == 0){
            updateUI(UIState.Error);
            return;
        }
        else if(i == -1){
            updateUI(UIState.Results);
            return;
        }
        statusTextView.setText(getResources().getString(R.string.fragment_auth_label, requestAmount));
        Log.d(TAG, "requestAmount = " + requestAmount);
        requestAmount = i - 1;
        JsonCustomRequest jcr = getJsonCustomRequest(j, bridgeIp);
        Log.d(TAG, "changeHueState postRequest created for this ip " + bridgeIp);
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(this).addToRequestQueue(this, jcr);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        String bridgeIp = bridgeDiscoveryResults.get(i).getIp();
        Log.i(TAG, "Selected Bridge " + bridgeIp);
        //connectToBridge(bridgeIp);
        Log.d(TAG, "Sending request for this devicetype: " + "EdgeHUE#" + android.os.Build.MODEL);
        JSONObject j = HueBridge.createJsonOnObject("devicetype", "EdgeHUE#" + android.os.Build.MODEL);
        assert j != null;
        updateUI(UIState.Auth);
        requestAmount = 100; //Requests to send
        sendAuthRequest(requestAmount, j, bridgeIp);
        /*JsonCustomRequest jcr = getJsonCustomRequest(j, bridgeIp);
        Log.d(TAG, "changeHueState postRequest created for this ip " + bridgeIp);
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(this).addToRequestQueue(jcr);*/

    }

    @Override
    public void onClick(View view) {
        if (view == bridgeDiscoveryButton) {
            EdgeHueProvider.clearAllContents();
            startBridgeDiscovery();
        }
        else if (view == cheatButton) {
            Log.d(TAG, "Instantiating HueBridge singleton");
            EdgeHueProvider.clearAllContents();
            HueBridge.getInstance(
                    ctx,
                    "192.168.69.166",
                    "aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD")
                    .requestHueState(this);
            Log.d(TAG, "getInstance() returns cheat bridge:" + HueBridge.getInstance(ctx));
            updateUI(UIState.Settings);
        }
        else if (view == bridgeDiscoveryCancelButton) {
            stopBridgeDiscovery();
            requestAmount = -1;
        }
        else if (view == quickButton) {
            EdgeHueProvider.quickSetup(this);
            updateUI(UIState.Final);
        }
        else if (view == customButton) {
            //TODO Set the category to something that is not NO_BRIDGE
            updateUI(UIState.Final);
        }
        else if (view == finishButton) {
            this.finish();
        }
        else if (view == removeButton) {
            updateUI(UIState.Confirmation);
        }
        else if (view == yesButton) {
            HueBridge.deleteInstance();
            updateUI(UIState.Welcome);
        }
        else if (view == noButton) {
            updateUI(UIState.Final);
        }
    }

    private void updateUI(final UIState state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Status: " + state.toString());

                bridgeDiscoveryListView.setVisibility(View.GONE);
                bridgeIpTextView.setVisibility(View.GONE);
                statusTextView.setVisibility(View.VISIBLE);
                pushlinkImage.setVisibility(View.GONE);
                bridgeDiscoveryButton.setVisibility(View.GONE);
                cheatButton.setVisibility(View.GONE);
                bridgeDiscoveryCancelButton.setVisibility(View.GONE);
                quickButton.setVisibility(View.GONE);
                customButton.setVisibility(View.GONE);
                finishButton.setVisibility(View.GONE);
                removeButton.setVisibility(View.GONE);
                yesButton.setVisibility(View.GONE);
                noButton.setVisibility(View.GONE);

                switch (state) {
                    case Welcome:
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setText(getResources().getText(R.string.fragment_welcome_button));
                        statusTextView.setText(getResources().getString(R.string.fragment_welcome_label));
                        cheatButton.setVisibility(View.VISIBLE);
                        break;
                    case Search:
                        bridgeDiscoveryCancelButton.setVisibility(View.VISIBLE);
                        statusTextView.setText(getResources().getString(R.string.fragment_search_label));
                        break;
                    case Results:
                        statusTextView.setVisibility(View.GONE);
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setText(getResources().getText(R.string.fragment_result_button));
                        bridgeDiscoveryListView.setVisibility(View.VISIBLE);
                        break;
                    case Connecting:
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        statusTextView.setText(getResources().getString(R.string.fragment_connect_label));
                        bridgeDiscoveryButton.setText(getResources().getText(R.string.fragment_result_button));
                        break;
                    case Error:
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setText(getResources().getText(R.string.fragment_error_button));
                        statusTextView.setText(getResources().getString(R.string.fragment_error_label));
                        cheatButton.setVisibility(View.VISIBLE);
                        break;
                    case Auth:
                        statusTextView.setText(getResources().getString(R.string.fragment_auth_label));
                        pushlinkImage.setVisibility(View.VISIBLE);
                        bridgeDiscoveryCancelButton.setVisibility(View.VISIBLE);
                        break;
                    case Settings:
                        statusTextView.setText(getResources().getString(R.string.fragment_settings_label));
                        quickButton.setVisibility(View.VISIBLE);
                        customButton.setVisibility(View.VISIBLE);
                        break;
                    case Final:
                        statusTextView.setText(getResources().getString(R.string.fragment_final_label));
                        finishButton.setVisibility(View.VISIBLE);
                        removeButton.setVisibility(View.VISIBLE);
                        break;
                    case Confirmation:
                        statusTextView.setText(getResources().getString(R.string.fragment_confirmation_label));
                        yesButton.setVisibility(View.VISIBLE);
                        noButton.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });
    }

    private JsonCustomRequest getJsonCustomRequest(final JSONObject jsonObject, final String ip){
        if(!jsonObject.keys().hasNext()){ // make sure we get an object that is not empty
            Log.wtf(TAG, "!jsonObject.keys().hasNext() Is this an empty request?");
            return null;
        }
        Log.d(TAG, "setHueState url " + ip); // this is the actual resource path
        return new JsonCustomRequest(Request.Method.POST, "http://" + ip + "/api", jsonObject,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "Auth request responds " + response.toString());
                        Iterator<String> responseKeys; // iterator for response JSONObject
                        String responseKey; // index for response JSONObject
                        try {
                            JSONObject jsonResponse = response.getJSONObject(0);
                            responseKeys = jsonResponse.keys();
                            if(responseKeys.hasNext()) {
                                responseKey = responseKeys.next();
                                if(!responseKey.equals("success")){  //response key should be success
                                    Log.e(TAG, "Unsuccessful! Check reply");
                                    sendAuthRequest(requestAmount, jsonObject, ip);
                                    return;
                                }
                                JSONObject usernameContainer = (JSONObject) jsonResponse.get("success");    // this should be JSONObject with username field
                                if(!usernameContainer.keys().next().equals("username")) {
                                    Log.w(TAG, "Unsuccessful! Check reply");            //  really weird if it fails here. API for HUE might have been changed recently
                                    sendAuthRequest(requestAmount, jsonObject, ip);
                                    return;
                                }
                                String username = usernameContainer.getString("username");
                                Log.d(TAG, "Auth successful: " + username.substring(0,5) + "*****");
                                EdgeHueProvider.clearAllContents();
                                HueBridge.getInstance(ctx, ip, username)
                                        .requestHueState(ctx);

                                updateUI(UIState.Settings);
                            }
                        } catch (JSONException ex) {
                            ex.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.toString());
                    }
                }
        );
    }

}
