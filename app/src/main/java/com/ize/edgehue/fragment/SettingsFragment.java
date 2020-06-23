package com.ize.edgehue.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.ize.edgehue.EdgeHueProvider;
import com.ize.edgehue.HueBridge;
import com.ize.edgehue.R;

import org.json.JSONException;

public class SettingsFragment extends Fragment {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.fragment_settings_button_quick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    EdgeHueProvider.quickSetup(getActivity());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                NavHostFragment.findNavController(SettingsFragment.this)
                        .navigate(R.id.action_SettingsFragment_to_FinalFragment);
            }
        });

        view.findViewById(R.id.fragment_settings_button_quick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO custom button
                NavHostFragment.findNavController(SettingsFragment.this)
                        .navigate(R.id.action_SettingsFragment_to_FinalFragment);
            }
        });
    }
}
