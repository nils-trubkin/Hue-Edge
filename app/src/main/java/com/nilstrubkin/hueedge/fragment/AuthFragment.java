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

import com.nilstrubkin.hueedge.R;

public class AuthFragment extends Fragment implements View.OnClickListener {
    private NavController navController = null;
    private final int searchButton = R.id.button_setup_search;
    private final int manualButton = R.id.button_setup_manual;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.welcome_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        view.findViewById(searchButton).setOnClickListener(this);
        view.findViewById(manualButton).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case searchButton:
                navController.navigate(R.id.action_welcomeFragment_to_discoveryFragment);
                break;
            case manualButton:
                navController.navigate(R.id.action_welcomeFragment_to_manualFragment);
                break;
        }
    }
}
