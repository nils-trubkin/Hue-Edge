package com.nilstrubkin.hueedge.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.badoualy.stepperindicator.StepperIndicator;
import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.R;

import java.util.Objects;

public class SetupFragment extends Fragment implements View.OnClickListener {
    private transient static final String TAG = SetupFragment.class.getSimpleName();
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
        switch (view.getId()){
            case quickButton:
                try {
                    HueBridge br = Objects.requireNonNull(HueBridge.getInstance(requireContext()));
                    if (br.getBridgeState() == null || br.getBridgeState().getLights().isEmpty()){
                        Toast.makeText(requireContext(), getString(R.string.toast_no_lights), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    br.quickSetup(requireContext());
                } catch (NullPointerException e){
                    Log.e(TAG, "Tried to perform quickSetup but no bridge was found");
                    e.printStackTrace();
                }
            case customButton:
                navController.navigate(R.id.action_setupFragment_to_finalFragment);
                break;
        }
    }
}
