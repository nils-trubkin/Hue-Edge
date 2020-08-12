package com.nilstrubkin.hueedge.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.R;

public class ConfirmationFragment extends Fragment implements View.OnClickListener {
    private NavController navController;

    //UI elements
    private final int yesButton = R.id.button_confirmation_yes;
    private final int noButton = R.id.button_confirmation_no;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.confirmation_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        view.findViewById(yesButton).setOnClickListener(this);
        view.findViewById(noButton).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case yesButton:
                HueBridge.deleteInstance(requireContext());
                navController.navigate(R.id.action_confirmationFragment_to_welcomeFragment);
                break;
            case noButton:
                navController.popBackStack();
                break;
        }
    }
}
