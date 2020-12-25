package com.nilstrubkin.hueedge.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.nilstrubkin.hueedge.R;

public class HowtoFragment extends Fragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private NavController navController;

    //UI elements
    private final int removeButtonId = R.id.button_settings_remove;
    private final int backButtonId = R.id.button_settings_back;
    private final int symbolSwitchId = R.id.switch_symbols;
    private final int hapticSwitchId = R.id.switch_haptic;
    private final int briBarId = R.id.seek_bar_bri;
    private final int hueBarId = R.id.seek_bar_hue;
    private final int satBarId = R.id.seek_bar_sat;
    private SwitchCompat symbolSwitch;
    private SwitchCompat hapticSwitch;
    private TextView briStatus;
    private TextView hueStatus;
    private TextView satStatus;

    public static final int minProgress = 10;

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
        symbolSwitch.setChecked(s.getBoolean(getString(R.string.preference_no_symbols), false));
        hapticSwitch.setChecked(s.getBoolean(getString(R.string.preference_no_haptic), false));
        briStatus = view.findViewById(R.id.text_seek_bar_bri_status);
        hueStatus = view.findViewById(R.id.text_seek_bar_hue_status);
        satStatus = view.findViewById(R.id.text_seek_bar_sat_status);
        SeekBar briBar = view.findViewById(briBarId);
        SeekBar hueBar = view.findViewById(hueBarId);
        SeekBar satBar = view.findViewById(satBarId);
        briBar.setOnSeekBarChangeListener(this);
        hueBar.setOnSeekBarChangeListener(this);
        satBar.setOnSeekBarChangeListener(this);
        briBar.setProgress(s.getInt(getString(R.string.preference_bri_levels) , 5));
        hueBar.setProgress(s.getInt(getString(R.string.preference_hue_levels), 15));
        satBar.setProgress(s.getInt(getString(R.string.preference_sat_levels), 5));
        briStatus.setText(String.valueOf(briBar.getProgress() + minProgress));
        hueStatus.setText(String.valueOf(hueBar.getProgress() + minProgress));
        satStatus.setText(String.valueOf(satBar.getProgress() + minProgress));
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
                e.putBoolean(getResources().getString(R.string.preference_no_symbols), symbolSwitch.isChecked());
                e.apply();
                break;
            case hapticSwitchId:
                s = PreferenceManager.getDefaultSharedPreferences(requireContext());
                e = s.edit();
                e.putBoolean(getResources().getString(R.string.preference_no_haptic), hapticSwitch.isChecked());
                e.apply();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        String levelsString = String.valueOf(i + minProgress);
        switch (seekBar.getId()){
            case briBarId:
                briStatus.setText(levelsString);
                break;
            case hueBarId:
                hueStatus.setText(levelsString);
                break;
            case satBarId:
                satStatus.setText(levelsString);
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor e = s.edit();
        int levels = seekBar.getProgress();
        String levelsString = String.valueOf(levels + minProgress);
        switch (seekBar.getId()) {
            case briBarId:
                e.putInt(getResources().getString(R.string.preference_bri_levels), levels);
                e.apply();
                briStatus.setText(levelsString);
                break;
            case hueBarId:
                e.putInt(getResources().getString(R.string.preference_hue_levels), levels);
                e.apply();
                hueStatus.setText(levelsString);
                break;
            case satBarId:
                e.putInt(getResources().getString(R.string.preference_sat_levels), levels);
                e.apply();
                satStatus.setText(levelsString);
                break;
        }
    }
}
