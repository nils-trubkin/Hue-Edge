package com.ize.edgehue.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

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
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.entertainment.Entertainment;
import com.philips.lighting.hue.sdk.wrapper.utilities.InitSdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Iterator;
import java.util.List;

public class EdgeSetup extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener  {

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

    private static final String TAG = EdgeSetup.class.getSimpleName();

    private BridgeDiscovery bridgeDiscovery;

    private List<BridgeDiscoveryResult> bridgeDiscoveryResults;

    private int requestAmount;

    // UI elements
    private TextView statusTextView;
    private ListView bridgeDiscoveryListView;
    private TextView bridgeIpTextView;
    private View pushlinkImage;
    private Button bridgeDiscoveryButton;
    private Button cheatButton;
    private Button bridgeDiscoveryCancelButton;
    private Button quickButton;
    private Button customButton;
    private Button finishButton;
    private Button removeButton;
    private Button yesButton;
    private Button noButton;


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

        // Setup the UI
        statusTextView = (TextView)findViewById(R.id.status_text);
        bridgeDiscoveryListView = (ListView)findViewById(R.id.bridge_discovery_result_list);
        bridgeDiscoveryListView.setOnItemClickListener(this);
        bridgeIpTextView = (TextView)findViewById(R.id.bridge_ip_text);
        pushlinkImage = findViewById(R.id.pushlink_image);
        bridgeDiscoveryButton = (Button)findViewById(R.id.bridge_discovery_button);
        bridgeDiscoveryButton.setOnClickListener(this);
        cheatButton = (Button)findViewById(R.id.cheat_button);
        cheatButton.setOnClickListener(this);
        bridgeDiscoveryCancelButton = (Button)findViewById(R.id.bridge_discovery_cancel_button);
        bridgeDiscoveryCancelButton.setOnClickListener(this);
        quickButton = (Button)findViewById(R.id.quick_setup_button);
        quickButton.setOnClickListener(this);
        customButton = (Button)findViewById(R.id.custom_setup_button);
        customButton.setOnClickListener(this);
        finishButton = (Button)findViewById(R.id.finish_button);
        finishButton.setOnClickListener(this);
        removeButton = (Button)findViewById(R.id.remove_button);
        removeButton.setOnClickListener(this);
        yesButton = (Button)findViewById(R.id.yes_button);
        yesButton.setOnClickListener(this);
        noButton = (Button)findViewById(R.id.no_button);
        noButton.setOnClickListener(this);
        InitSdk.setApplicationContext(getApplicationContext());
        Persistence.setStorageLocation(getFilesDir().getAbsolutePath(), "EdgeHue");
        HueLog.setConsoleLogLevel(HueLog.LogLevel.DEBUG);

        updateUI(UIState.Welcome);
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
        statusTextView.setText(new StringBuilder().append(getResources().getString(R.string.fragment_connect_label)).append("\n\n").append(requestAmount).toString());
        Log.d(TAG, "requestAmount = " + requestAmount);
        requestAmount = i - 1;
        JsonCustomRequest jcr = getJsonCustomRequest(j, bridgeIp);
        Log.d(TAG, "changeHueState postRequest created for this ip " + bridgeIp);
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(this).addToRequestQueue(jcr);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        String bridgeIp = bridgeDiscoveryResults.get(i).getIp();
        Log.i(TAG, "Selected Bridge " + bridgeIp);
        //connectToBridge(bridgeIp);
        Log.d(TAG, "Sending request for this devicetype: " + "EdgeHUE#" + android.os.Build.MODEL);
        JSONObject j = HueBridge.createJsonOnObject("devicetype", "EdgeHUE#" + android.os.Build.MODEL);
        assert j != null;
        updateUI(UIState.Connecting);
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
            startBridgeDiscovery();
        }
        else if (view == cheatButton) {
            Log.d(TAG, "Instantiating HueBridge singleton");
            HueBridge.getInstance(
                    "192.168.69.166",
                    "aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD")
                    .requestHueState(this);
            Log.d(TAG, "getInstance() returns (!= null):" + HueBridge.getInstance());
            updateUI(UIState.Settings);
        }
        else if (view == bridgeDiscoveryCancelButton) {
            stopBridgeDiscovery();
        }
        else if (view == quickButton) {
            try {
                EdgeHueProvider.quickSetup(this);
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setText(getResources().getText(R.string.fragment_result_button));
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
        final Context ctx = this;
        assert jsonObject.keys().hasNext();
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
                                    Log.e(TAG, "Unsuccesfull! Check reply");
                                    sendAuthRequest(requestAmount, jsonObject, ip);
                                    return;
                                }
                                JSONObject usernameContainer = (JSONObject) jsonResponse.get("success");    // this should be JSONObject with username field
                                if(!usernameContainer.keys().next().equals("username")) {
                                    Log.w(TAG, "Unsuccesfull! Check reply");            //  really weird if it fails here. API for HUE might have been changed recently
                                    sendAuthRequest(requestAmount, jsonObject, ip);
                                    return;
                                }
                                String username = usernameContainer.getString("username");
                                Log.d(TAG, "Auth successful: " + username.substring(0,5) + "*****");

                                HueBridge.getInstance(ip, username)
                                        .requestHueState(ctx);

                                updateUI(UIState.Settings);
                                }
                            else
                                return;
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
