package com.nilstrubkin.hueedge.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.badoualy.stepperindicator.StepperIndicator;
import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class WelcomeFragment extends Fragment implements View.OnClickListener {
    private NavController navController;

    //UI elements
    private final int searchButtonId = R.id.button_welcome_search;
    private final int manualButtonId = R.id.button_welcome_manual;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.welcome_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        // Fix for wonky google stuff, if it breaks again
        //NavHostFragment navHostFragment = (NavHostFragment) Objects.requireNonNull(requireActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_container_setup));
        //navController = navHostFragment.getNavController();
        view.findViewById(searchButtonId).setOnClickListener(this);
        view.findViewById(manualButtonId).setOnClickListener(this);

        ((StepperIndicator) requireActivity().findViewById(R.id.steps_wizard)).setCurrentStep(0);

        if (HueBridge.getInstance(requireContext()) == null)
            requireActivity().findViewById(R.id.steps_wizard).setVisibility(View.VISIBLE);
        else
            navController.navigate(R.id.action_welcomeFragment_to_finalFragment);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(HueEdgeProvider.checkWifiNotEnabled(requireContext()))
            Toast.makeText(getContext(), requireContext().getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case searchButtonId:
                navController.navigate(R.id.action_welcomeFragment_to_discoveryFragment);
                if(HueEdgeProvider.checkWifiNotEnabled(requireContext()))
                    Toast.makeText(getContext(), requireContext().getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
                break;
            case manualButtonId:
                navController.navigate(R.id.action_welcomeFragment_to_manualFragment);
                if(HueEdgeProvider.checkWifiNotEnabled(requireContext()))
                    Toast.makeText(getContext(), requireContext().getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
                break;
        }
    }
}
