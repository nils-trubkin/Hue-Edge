package com.nilstrubkin.hueedge.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nilstrubkin.hueedge.R;
import com.rakshakhegde.stepperindicator.StepperIndicator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class WelcomeFragment extends Fragment implements View.OnClickListener {

    private NavController navController;

    //UI elements
    private final int searchButtonId = R.id.button_setup_search;
    private final int manualButtonId = R.id.button_setup_manual;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.welcome_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        //NavHostFragment navHostFragment = (NavHostFragment) Objects.requireNonNull(requireActivity().getSupportFragmentManager().findFragmentById(R.id.fragmentContainerSetup));
        //navController = navHostFragment.getNavController();
        view.findViewById(searchButtonId).setOnClickListener(this);
        view.findViewById(manualButtonId).setOnClickListener(this);

        ((StepperIndicator) requireActivity().findViewById(R.id.steps_wizard)).setCurrentStep(0);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case searchButtonId:
                navController.navigate(R.id.action_welcomeFragment_to_discoveryFragment);
                break;
            case manualButtonId:
                navController.navigate(R.id.action_welcomeFragment_to_manualFragment);
                break;
        }
    }
}
