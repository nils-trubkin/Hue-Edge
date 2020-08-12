package com.nilstrubkin.hueedge.activity;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.nilstrubkin.hueedge.R;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.look.Slook;

import java.util.Objects;

public class SetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.setup_activity);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(getResources().getColor(R.color.navigation_bar_color_setup, getTheme()));

        //bridgeDiscoveryResults = new ArrayList<>();

        // Setup the UI
        /*statusTextView = findViewById(R.id.status_text);
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
        symbolSwitch = findViewById(R.id.switch_symbols);
        symbolSwitch.setOnClickListener(this);
        hapticSwitch = findViewById(R.id.switch_haptic);
        hapticSwitch.setOnClickListener(this);*/

        Slook slook = new Slook();

        NavHostFragment navHostFragment = (NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fragment_container_setup));
        NavController navController = navHostFragment.getNavController();

        try {
            slook.initialize(this);
        } catch (SsdkUnsupportedException e){
            navController.navigate(R.id.action_welcomeFragment_to_notSupportedFragment);
            return;
        }

        // The device doesn't support Edge Single Mode, Edge Single Plus Mode, and Edge Feeds Mode.
        if (!slook.isFeatureEnabled(Slook.COCKTAIL_PANEL)) {
            navController.navigate(R.id.action_welcomeFragment_to_notSupportedFragment);
            return;
        }
    }



    public static class ProgressBarAnimation extends Animation {
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


    /*@Override
    public void onClick(View view) {
        if (view == bridgeDiscoveryButton) {
            startBridgeDiscovery();
        }*/
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
        /*else if (view == manualIp) {
            updateUI(UIState.ManualSetup);
        }
        else if (view == manualIpConfirm) {
            String ip = ipField.getText().toString();
            if (Patterns.IP_ADDRESS.matcher(ip).matches()) {
                Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
                discoveryEngine = new DiscoveryEngine(executor, mainThreadHandler);
                executorService = Executors.newFixedThreadPool(4);
                discoveryEngine.connectToBridge(ctx, executorService, bridgeAuthCallback, ip);
                updateUI(UIState.Auth);
            }
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
        }
        else if (view == quickButton) {
            try{
                HueBridge hueBridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
                if (hueBridge.getBridgeState().getLights().isEmpty()) {
                    String toastString = ctx.getString(R.string.toast_no_lights);
                    Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (NullPointerException e){
                Log.e(TAG, "Tried to perfrom quick setup but no HueBridge instance found");
                updateUI(UIState.Error);
            }
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
            aboutButton.setVisibility(View.GONE);
            aboutLayout.setVisibility(View.VISIBLE);
            helpLayout.setVisibility(View.GONE);
        }
        else if (view == aboutCloseButton){
            aboutLayout.setVisibility(View.GONE);
            aboutButton.setVisibility(View.VISIBLE);
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
        else if (view == symbolSwitch) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor e = settings.edit();
            e.putBoolean(ctx.getResources().getString(R.string.no_symbols_preference), symbolSwitch.isChecked());
            e.apply();
        }
        else if (view == hapticSwitch) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor e = settings.edit();
            e.putBoolean(ctx.getResources().getString(R.string.no_haptic_preference), hapticSwitch.isChecked());
            e.apply();
        }
    }*/

    /*private void updateUI(final UIState state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Status: " + state.toString());

                /*Animation anim;
                final int searchTimeout = 30 * 1000;
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
                aboutButton.setVisibility(View.VISIBLE);
                helpLayout.setVisibility(View.GONE);
                symbolSwitch.setVisibility(View.GONE);
                hapticSwitch.setVisibility(View.GONE);

                statusTextView.setPadding(0,0,0, (int) ctx.getResources().getDimension(R.dimen.setup_status_text_padding_bottom));

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
                        adapter = new BridgeDiscoveryResultAdapter(getApplicationContext(), bridgeDiscoveryResults);
                        bridgeDiscoveryListView.setAdapter(adapter);
                        statusTextView.setText(getResources().getString(R.string.fragment_search_label));
                        bridgeDiscoveryCancelButton.setVisibility(View.VISIBLE);
                        bridgeDiscoveryListView.setVisibility(View.VISIBLE);
                        progressBar.setProgress(0);
                        progressBar.setMax(10000);
                        anim = new SetupActivity.ProgressBarAnimation(progressBar, 0, progressBar.getMax());
                        anim.setDuration(searchTimeout);
                        progressBar.startAnimation(anim);
                        progressBar.setVisibility(View.VISIBLE);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(searchTimeout);
                                    if (bridgeDiscoveryRunning) {
                                        stopBridgeDiscovery();
                                        updateUI(UIState.Results);
                                        if (bridgeDiscoveryResults.isEmpty())
                                            updateUI(UIState.Error);
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        break;
                    case Results:
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                        bridgeDiscoveryListView.setVisibility(View.VISIBLE);
                        statusTextView.setText(getResources().getString(R.string.fragment_results_label));
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
                        manualIp.setVisibility(View.VISIBLE);
                        //cheatButton.setVisibility(View.VISIBLE);
                        break;
                    case Auth:
                        statusTextView.setText(ctx.getResources().getString(R.string.fragment_auth_label, DiscoveryEngine.REQUEST_AMOUNT));
                        pushlinkImage.setVisibility(View.VISIBLE);
                        bridgeDiscoveryCancelButton.setVisibility(View.VISIBLE);
                        progressBar.setProgress(0);
                        progressBar.setMax(10000);
                        anim = new SetupActivity.ProgressBarAnimation(progressBar, 0, progressBar.getMax());
                        anim.setDuration(1000 * DiscoveryEngine.REQUEST_AMOUNT);
                        progressBar.startAnimation(anim);
                        progressBar.setVisibility(View.VISIBLE);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                int i = DiscoveryEngine.REQUEST_AMOUNT;
                                while (i-- > 0) {
                                    long timeStamp = System.currentTimeMillis();
                                    while (System.currentTimeMillis() - timeStamp < 1000)
                                        if (executorService.isShutdown())
                                            return;
                                    if (executorService.isShutdown())
                                        return;
                                    final int finalI = i;
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            if (executorService.isShutdown())
                                                return;
                                            statusTextView.setText(ctx.getResources().getString(R.string.fragment_auth_label, finalI));
                                        }
                                    });
                                }
                            }
                        }).start();
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
                        symbolSwitch.setVisibility(View.VISIBLE);
                        hapticSwitch.setVisibility(View.VISIBLE);
                        statusTextView.setPadding(0,0,0, 0);
                        try {
                            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                            symbolSwitch.setChecked(settings.getBoolean(ctx.getResources().getString(R.string.no_symbols_preference), false));
                            hapticSwitch.setChecked(settings.getBoolean(ctx.getResources().getString(R.string.no_haptic_preference), false));
                        } catch (NullPointerException e){
                            Log.e(TAG, "Tried to display state for switch but no HueBridge instance present");
                        }
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
    }*/

    public boolean checkWifiOnAndConnected() {
        if(true)
            return true;
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr.isWifiEnabled())// Wi-Fi adapter is ON
            return wifiMgr.getConnectionInfo().getNetworkId() != -1;
        else
            return false; // Wi-Fi adapter is OFF
    }
}
