package com.nilstrubkin.hueedge.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.R;

public class ErrorFragment extends Fragment implements View.OnClickListener {
    private NavController navController;

    //UI elements
    private final int searchButtonId = R.id.button_welcome_search;
    private final int manualButtonId = R.id.button_welcome_manual;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.error_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        view.findViewById(searchButtonId).setOnClickListener(this);
        view.findViewById(manualButtonId).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(HueEdgeProvider.checkWifiNotEnabled(requireContext())) {
            Toast.makeText(getContext(), requireContext().getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
            return;
        }
        switch (view.getId()){
            case searchButtonId:
                navController.navigate(R.id.action_errorFragment_to_discoveryFragment);
                break;
            case manualButtonId:
                navController.navigate(R.id.action_errorFragment_to_manualFragment);
                break;
        }
    }
}
