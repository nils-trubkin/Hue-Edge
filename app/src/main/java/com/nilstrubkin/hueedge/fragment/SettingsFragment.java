package com.nilstrubkin.hueedge.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.R;
import com.rakshakhegde.stepperindicator.StepperIndicator;

public class SettingsFragment extends Fragment implements View.OnClickListener {
    private NavController navController;

    //UI elements
    private final int removeButtonId = R.id.button_settings_remove;
    private final int backButtonId = R.id.button_settings_back;
    private final int symbolSwitchId = R.id.switch_symbols;
    private final int hapticSwitchId = R.id.switch_haptic;
    private SwitchCompat symbolSwitch;
    private SwitchCompat hapticSwitch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        view.findViewById(removeButtonId).setOnClickListener(this);
        view.findViewById(backButtonId).setOnClickListener(this);
        symbolSwitch = view.findViewById(symbolSwitchId);
        hapticSwitch = view.findViewById(hapticSwitchId);
        symbolSwitch.setOnClickListener(this);
        hapticSwitch.setOnClickListener(this);
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(requireContext());
        symbolSwitch.setChecked(s.getBoolean(getString(R.string.no_symbols_preference), false));
        hapticSwitch.setChecked(s.getBoolean(getString(R.string.no_haptic_preference), false));
    }

    @Override
    public void onClick(View view) {
        SharedPreferences s;
        SharedPreferences.Editor e;
        switch (view.getId()){
            case removeButtonId:
                navController.navigate(R.id.action_settingsFragment_to_confirmationFragment);
                break;
            case backButtonId:
                navController.popBackStack();
                break;
            case symbolSwitchId:
                s = PreferenceManager.getDefaultSharedPreferences(requireContext());
                e = s.edit();
                e.putBoolean(getResources().getString(R.string.no_symbols_preference), symbolSwitch.isChecked());
                e.apply();
                break;
            case hapticSwitchId:
                s = PreferenceManager.getDefaultSharedPreferences(requireContext());
                e = s.edit();
                e.putBoolean(getResources().getString(R.string.no_haptic_preference), hapticSwitch.isChecked());
                e.apply();
                break;
        }
    }
}
