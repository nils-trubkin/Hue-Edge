package com.ize.edgehue.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.appcompat.app.AppCompatActivity;

import com.ize.edgehue.HueBridge;
import com.ize.edgehue.R;
import com.ize.edgehue.activity.EdgeSetup;
import com.ize.edgehue.activity.MainActivity;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryImpl;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;

import java.util.List;

public class WelcomeFragment extends Fragment {

    private static final String TAG = WelcomeFragment.class.getSimpleName();

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.fragment_welcome_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((EdgeSetup)getActivity()).startBridgeDiscovery();

                NavHostFragment.findNavController(WelcomeFragment.this)
                        .navigate(R.id.action_WelcomeFragment_to_SearchFragment);
            }
        });

        view.findViewById(R.id.fragment_welcome_button_cheat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(WelcomeFragment.this)
                        .navigate(R.id.action_WelcomeFragment_to_FinalFragment);
                Log.d(TAG, "Instantiating HueBridge singleton");
                HueBridge.getInstance(
                        "192.168.69.166",
                        "aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD")
                        .requestHueState(getActivity());
                Log.d(TAG, "getInstance() returns (!= null):" + HueBridge.getInstance());
            }
        });
    }
}
