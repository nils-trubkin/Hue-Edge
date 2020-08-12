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

public class SetupFragment extends Fragment implements View.OnClickListener {
    private NavController navController;

    //UI elements
    private final int quickButton = R.id.button_setup_quick;
    private final int customButton = R.id.button_setup_custom;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.setup_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        view.findViewById(quickButton).setOnClickListener(this);
        view.findViewById(customButton).setOnClickListener(this);

        ((StepperIndicator) requireActivity().findViewById(R.id.steps_wizard)).setCurrentStep(3);
    }

    @Override
    public void onClick(View view) {
        SharedPreferences s;
        SharedPreferences.Editor e;
        switch (view.getId()){
            case quickButton:
                HueEdgeProvider.quickSetup(requireContext());
            case customButton:
                navController.navigate(R.id.action_setupFragment_to_finalFragment);
                break;
        }
    }
}
