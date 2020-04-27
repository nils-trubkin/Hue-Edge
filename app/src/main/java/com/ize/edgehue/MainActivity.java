package com.ize.edgehue;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.samsung.android.sdk.look.Slook;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Slook slook = new Slook();
        //RequestQueue queue = Volley.newRequestQueue(this);

        try {
            slook.initialize(this);
        } catch (Exception e) {
            return;
        }

        if (slook.isFeatureEnabled(Slook.COCKTAIL_PANEL)) {
            // The Device supports Edge Single Mode, Edge Single Plus Mode, and Edge Feeds Mode.
            setContentView(R.layout.view_main);
        }


    }

}
