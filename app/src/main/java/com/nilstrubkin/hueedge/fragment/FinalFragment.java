package com.nilstrubkin.hueedge.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.badoualy.stepperindicator.StepperIndicator;
import com.nilstrubkin.hueedge.R;

public class FinalFragment extends Fragment implements View.OnClickListener {
    private NavController navController;

    //UI elements
    private final int settingsButtonId = R.id.button_final_settings;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.final_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        view.findViewById(settingsButtonId).setOnClickListener(this);

        ((StepperIndicator) requireActivity().findViewById(R.id.steps_wizard)).setCurrentStep(4);
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == settingsButtonId) {
            navController.navigate(R.id.action_finalFragment_to_settingsFragment);
            requireActivity().findViewById(R.id.steps_wizard).setVisibility(View.GONE);
        }
    }
}
