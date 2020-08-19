package com.nilstrubkin.hueedge.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.nilstrubkin.hueedge.R;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.look.Slook;

import java.util.Objects;

public class SetupActivity extends AppCompatActivity implements View.OnClickListener {

    private final int aboutButtonId = R.id.button_about;
    private final int aboutCloseButtonId = R.id.button_about_close;
    private final int manualHelpCloseButtonId = R.id.button_help_close;
    private final int licenseButtonId = R.id.button_license;
    private final int contactMeButtonId = R.id.button_contact_me;
    private final int paypalButtonId = R.id.button_paypal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.setup_activity);
        findViewById(aboutButtonId).setOnClickListener(this);
        findViewById(aboutCloseButtonId).setOnClickListener(this);
        findViewById(manualHelpCloseButtonId).setOnClickListener(this);
        findViewById(licenseButtonId).setOnClickListener(this);
        findViewById(contactMeButtonId).setOnClickListener(this);
        findViewById(paypalButtonId).setOnClickListener(this);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(getResources().getColor(R.color.navigation_bar_color_setup, getTheme()));

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
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case aboutButtonId:
                showAbout();
                break;
            case aboutCloseButtonId:
                closeAbout();
                break;
            case manualHelpCloseButtonId:
                closeManualHelp();
                break;
            case licenseButtonId:
                showLicense();
                break;
            case contactMeButtonId:
                contactMe();
                break;
            case paypalButtonId:
                openPaypal();
                break;
        }
    }

    private void closeManualHelp(){
        findViewById(R.id.layout_manual_help).setVisibility(View.GONE);
    }

    private void showAbout(){
        findViewById(R.id.layout_about).setVisibility(View.VISIBLE);
        findViewById(R.id.button_about).setVisibility(View.GONE);
    }

    private void closeAbout(){
        findViewById(R.id.layout_about).setVisibility(View.GONE);
        findViewById(R.id.button_about).setVisibility(View.VISIBLE);
    }

    private void showLicense(){
        startActivity(new Intent(this, OssLicensesMenuActivity.class));
    }

    private void contactMe(){
        Intent email = new Intent(Intent.ACTION_SEND);
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.email)});
        //email.putExtra(Intent.EXTRA_SUBJECT, "");
        //email.putExtra(Intent.EXTRA_TEXT, "");

        //need this to prompts email client only
        email.setType("message/rfc822");
        startActivity(Intent.createChooser(email, "Choose an Email client :"));
    }

    private void openPaypal(){
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(getString(R.string.paypal_url)));
        startActivity(i);
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
}
