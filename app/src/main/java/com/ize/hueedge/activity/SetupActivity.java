package com.ize.hueedge.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.ize.hueedge.HueEdgeProvider;
import com.ize.hueedge.HueBridge;
import com.ize.hueedge.R;
import com.ize.hueedge.adapter.BridgeDiscoveryResultAdapter;
import com.ize.hueedge.api.JsonCustomRequest;
import com.ize.hueedge.api.RequestQueueSingleton;
import com.philips.lighting.hue.sdk.wrapper.HueLog;
import com.philips.lighting.hue.sdk.wrapper.Persistence;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryImpl;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;
import com.philips.lighting.hue.sdk.wrapper.utilities.InitSdk;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.look.Slook;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SetupActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, Serializable {

    private transient static final int REQUEST_AMOUNT = 60;

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

    private transient static final String TAG = SetupActivity.class.getSimpleName();
    private transient final Context ctx = this;

    private transient BridgeDiscovery bridgeDiscovery;

    private transient List<BridgeDiscoveryResult> bridgeDiscoveryResults;

    private transient int requestAmount;
    private transient Timer timer;
    private transient sendAuthRequestTask<Object> backgroundAuthRequestTask;

    // UI elements
    private transient TextView statusTextView;
    private transient ListView bridgeDiscoveryListView;
    private transient View pushlinkImage;
    private transient ProgressBar progressBar;
    private transient Button bridgeDiscoveryButton;
    private transient Button cheatButton;
    private transient Button manualIp;
    private transient Button manualIpConfirm;
    private transient Button manualIpHelp;
    private transient Button manualIpBack;
    private transient LinearLayout helpLayout;
    private transient Button helpCloseButton;
    private transient EditText ipField;
    private transient Button bridgeDiscoveryCancelButton;
    private transient Button quickButton;
    private transient Button customButton;
    private transient Button finishButton;
    private transient Button removeButton;
    private transient Button yesButton;
    private transient Button noButton;
    private transient LinearLayout aboutLayout;
    private transient Button aboutButton;
    private transient Button aboutCloseButton;
    private transient Button contactMe;
    private transient Button support;

    enum UIState {
        Welcome,
        ManualSetup,
        Search,
        Results,
        Connecting,
        Error,
        Auth,
        Settings,
        Final,
        Confirmation,
        Not_supported
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.setup_activity);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(getResources().getColor(R.color.navigation_bar_color_setup, getTheme()));

        // Setup the UI
        statusTextView = findViewById(R.id.status_text);
        bridgeDiscoveryListView = findViewById(R.id.bridge_discovery_result_list);
        bridgeDiscoveryListView.setOnItemClickListener(this);
        pushlinkImage = findViewById(R.id.pushlink_image);
        progressBar = findViewById(R.id.progress_bar);
        bridgeDiscoveryButton = findViewById(R.id.bridge_discovery_button);
        bridgeDiscoveryButton.setOnClickListener(this);
        cheatButton = findViewById(R.id.cheat_button);
        cheatButton.setOnClickListener(this);
        manualIp = findViewById(R.id.manual_ip);
        manualIp.setOnClickListener(this);
        manualIpConfirm = findViewById(R.id.manual_ip_confirm);
        manualIpConfirm.setOnClickListener(this);
        manualIpHelp = findViewById(R.id.manual_ip_help);
        manualIpHelp.setOnClickListener(this);
        manualIpBack = findViewById(R.id.manual_ip_back);
        manualIpBack.setOnClickListener(this);
        helpLayout = findViewById(R.id.help_layout);
        helpCloseButton = findViewById(R.id.close_help);
        helpCloseButton.setOnClickListener(this);
        ipField = findViewById(R.id.ip_field);
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
        aboutLayout = findViewById(R.id.about_layout);
        aboutButton = findViewById(R.id.about_button);
        aboutButton.setOnClickListener(this);
        aboutCloseButton = findViewById(R.id.about_close_button);
        aboutCloseButton.setOnClickListener(this);
        contactMe = findViewById(R.id.contact_me);
        contactMe.setOnClickListener(this);
        support = findViewById(R.id.support);
        support.setOnClickListener(this);
        InitSdk.setApplicationContext(getApplicationContext());
        Persistence.setStorageLocation(getFilesDir().getAbsolutePath(), Build.ID);
        HueLog.setConsoleLogLevel(HueLog.LogLevel.DEBUG); //TODO remove debug

        Slook slook = new Slook();

        try {
            slook.initialize(this);
        } catch (SsdkUnsupportedException e){
            updateUI(UIState.Not_supported);
            return;
        }

        // The device doesn't support Edge Single Mode, Edge Single Plus Mode, and Edge Feeds Mode.
        if (!slook.isFeatureEnabled(Slook.COCKTAIL_PANEL)) {
            updateUI(UIState.Not_supported);
            return;
        }

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
                        Log.i(TAG, "Bridge discovery found " + results.size() + " bridge(s) in the network");
                        if(results.size() == 0)
                            updateUI(UIState.Error);
                        else
                            updateUI(UIState.Results);
                    } else if (returnCode == BridgeDiscovery.ReturnCode.STOPPED) {
                        Log.i(TAG, "Bridge discovery stopped");
                        updateUI(UIState.Welcome);
                    } else {
                        Log.e(TAG, "Bridge discovery error");
                        updateUI(UIState.Error);
                    }
                }
            });
        }
    };

    static private void sendAuthRequest(SetupActivity ins, JSONObject job, String bridgeIp){
        if(ins.requestAmount == 0){
            ins.timer.cancel();
            ins.progressBar.clearAnimation();
            ins.updateUI(UIState.Error);
            return;
        }
        else if(ins.requestAmount == -1){
            ins.timer.cancel();
            ins.progressBar.clearAnimation();
            ins.updateUI(UIState.Results);
            return;
        }
        ins.statusTextView.setText(ins.getResources().getString(R.string.fragment_auth_label, ins.requestAmount));
        //ins.progressBar.incrementProgressBy(ins.progressBar.getMax() / REQUEST_AMOUNT);
        Log.d(TAG, "requestAmount = " + ins.requestAmount);
        ins.requestAmount--;
        JsonCustomRequest jcr = getJsonCustomRequest(ins, job, bridgeIp);
        Log.d(TAG, "changeHueState postRequest created for this ip " + bridgeIp);
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(ins).addToRequestQueue(ins, jcr);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final String bridgeIp = bridgeDiscoveryResults.get(i).getIp();
        Log.i(TAG, "Selected Bridge " + bridgeIp);
        connectToBridge(bridgeIp);
    }

    private void connectToBridge(final String bridgeIp){
        Log.d(TAG, "Sending request for this devicetype: " + "HueEdge#" + android.os.Build.MODEL);
        final JSONObject job = HueBridge.createJsonOnObject("devicetype", "HueEdge#" + android.os.Build.MODEL);
        assert job != null;
        updateUI(UIState.Auth);
        requestAmount = REQUEST_AMOUNT; //Requests to send
        final Handler handler = new Handler();
        timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            backgroundAuthRequestTask = new sendAuthRequestTask<>((SetupActivity) ctx);
                            // PerformBackgroundTask this class is the class that extends AsyncTask
                            backgroundAuthRequestTask.execute(job, bridgeIp);
                        } catch (Exception ex) {
                            Log.e(TAG, "Could not start background auth request async task");
                            ex.printStackTrace();
                        }
                    }
                });
            }
        };
        progressBar.setMax(10000);
        ProgressBarAnimation anim = new ProgressBarAnimation(progressBar, progressBar.getMin(), progressBar.getMax());
        anim.setDuration(1000 * REQUEST_AMOUNT);
        progressBar.startAnimation(anim);
        timer.schedule(doAsynchronousTask, 0, 1000); //execute every second
    }

    private static class sendAuthRequestTask<T> extends AsyncTask<T, T, T> {
        private transient final WeakReference<SetupActivity> activityReference;
        sendAuthRequestTask(SetupActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected T doInBackground(T[] objects) {
            sendAuthRequest(activityReference.get(), (JSONObject) objects[0], (String) objects[1]);
            return null;
        }

        public void success() {
            activityReference.get().timer.cancel();
            activityReference.get().progressBar.clearAnimation();
        }
    }

    private static class ProgressBarAnimation extends Animation {
        private final ProgressBar progressBar;
        private final float from;
        private final float to;

        public ProgressBarAnimation(ProgressBar progressBar, float from, float to) {
            super();
            this.progressBar = progressBar;
            this.from = from;
            this.to = to;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            float value = from + (to - from) * interpolatedTime;
            progressBar.setProgress((int) value);
        }

    }

    @Override
    public void onClick(View view) {
        if (view == bridgeDiscoveryButton) {
            startBridgeDiscovery();
        }
        /*else if (view == cheatButton) {
            Log.d(TAG, "Instantiating HueBridge singleton");
            HueBridge.getInstance(
                    ctx,
                    "192.168.69.166",
                    "aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD")
                    .requestHueState(this);
            Log.d(TAG, "getInstance() returns cheat bridge:" + HueBridge.getInstance(ctx));
            updateUI(UIState.Settings);
        }*/
        else if (view == manualIp) {
            updateUI(UIState.ManualSetup);
        }
        else if (view == manualIpConfirm) {
            String ip = ipField.getText().toString();
            if (Patterns.IP_ADDRESS.matcher(ip).matches())
                connectToBridge(ip);
            else {
                String toastString = ctx.getString(R.string.toast_ip_mistake);
                Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            }
        }
        else if (view == manualIpHelp) {
            helpLayout.setVisibility(View.VISIBLE);
            aboutLayout.setVisibility(View.GONE);
        }
        else if (view == manualIpBack) {
            updateUI(UIState.Welcome);
        }
        else if (view == helpCloseButton) {
            helpLayout.setVisibility(View.GONE);
        }
        else if (view == bridgeDiscoveryCancelButton) {
            stopBridgeDiscovery();
            requestAmount = -1;
        }
        else if (view == quickButton) {
            HueEdgeProvider.quickSetup(this);
            updateUI(UIState.Final);
        }
        else if (view == customButton) {
            HueEdgeProvider.saveAllConfiguration(ctx);
            updateUI(UIState.Final);
        }
        else if (view == finishButton) {
            this.finish();
        }
        else if (view == removeButton) {
            updateUI(UIState.Confirmation);
        }
        else if (view == yesButton) {
            HueBridge.deleteInstance(this);
            updateUI(UIState.Welcome);
        }
        else if (view == noButton) {
            updateUI(UIState.Final);
        }
        else if (view == aboutButton){
            aboutLayout.setVisibility(View.VISIBLE);
            helpLayout.setVisibility(View.GONE);
        }
        else if (view == aboutCloseButton){
            aboutLayout.setVisibility(View.GONE);
        }
        else if (view == contactMe){
            Intent email = new Intent(Intent.ACTION_SEND);
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{ctx.getString(R.string.email)});
            //email.putExtra(Intent.EXTRA_SUBJECT, "");
            //email.putExtra(Intent.EXTRA_TEXT, "");

            //need this to prompts email client only
            email.setType("message/rfc822");
            startActivity(Intent.createChooser(email, "Choose an Email client :"));
        }
        else if (view == support){
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(ctx.getString(R.string.paypal_url)));
            startActivity(i);
        }
    }

    private void updateUI(final UIState state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Status: " + state.toString());

                bridgeDiscoveryListView.setVisibility(View.GONE);
                statusTextView.setVisibility(View.VISIBLE);
                pushlinkImage.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                bridgeDiscoveryButton.setVisibility(View.GONE);
                cheatButton.setVisibility(View.GONE);
                manualIp.setVisibility(View.GONE);
                manualIpConfirm.setVisibility(View.GONE);
                manualIpHelp.setVisibility(View.GONE);
                manualIpBack.setVisibility(View.GONE);
                ipField.setVisibility(View.GONE);
                bridgeDiscoveryCancelButton.setVisibility(View.GONE);
                quickButton.setVisibility(View.GONE);
                customButton.setVisibility(View.GONE);
                finishButton.setVisibility(View.GONE);
                removeButton.setVisibility(View.GONE);
                yesButton.setVisibility(View.GONE);
                noButton.setVisibility(View.GONE);
                aboutLayout.setVisibility(View.GONE);
                aboutButton.setText(ctx.getString(R.string.about_button_text));
                helpLayout.setVisibility(View.GONE);

                switch (state) {
                    case Welcome:
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setText(getResources().getText(R.string.fragment_welcome_button));
                        statusTextView.setText(getResources().getString(R.string.fragment_welcome_label));
                        //cheatButton.setVisibility(View.VISIBLE);
                        manualIp.setVisibility(View.VISIBLE);
                        break;
                    case ManualSetup:
                        statusTextView.setText(getResources().getString(R.string.fragment_manual_label));
                        manualIpConfirm.setVisibility(View.VISIBLE);
                        manualIpHelp.setVisibility(View.VISIBLE);
                        manualIpBack.setVisibility(View.VISIBLE);
                        ipField.setVisibility(View.VISIBLE);
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
                        //cheatButton.setVisibility(View.VISIBLE);
                        break;
                    case Auth:
                        statusTextView.setText(getResources().getString(R.string.fragment_auth_label));
                        pushlinkImage.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(0);
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
                    case Not_supported:
                        statusTextView.setText(getResources().getString(R.string.fragment_not_supported_label));
                        break;
                    default:
                        Log.e(TAG, "Unknown UI fragment");
                }
            }
        });
    }

    static private JsonCustomRequest getJsonCustomRequest(Context ctx, final JSONObject jsonObject, final String ip){
        final SetupActivity ins = (SetupActivity) ctx;
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
                                    Log.i(TAG, "Unsuccessful! Check reply");
                                    return;
                                }
                                JSONObject usernameContainer = (JSONObject) jsonResponse.get("success");    // this should be JSONObject with username field
                                if(!usernameContainer.keys().next().equals("username")) {
                                    Log.e(TAG, "Unsuccessful! Check reply");            //  really weird if it fails here. API for HUE might have been changed recently
                                    return;
                                }
                                ins.backgroundAuthRequestTask.success();
                                String username = usernameContainer.getString("username");
                                Log.d(TAG, "Auth successful: " + username.substring(0,5) + "*****");
                                HueBridge.getInstance(ins, ip, username)
                                        .requestHueState(ins);

                                ins.updateUI(UIState.Settings);
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
