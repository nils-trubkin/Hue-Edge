package com.nilstrubkin.hueedge.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.nilstrubkin.hueedge.R;

public class ManualFragment extends Fragment implements View.OnClickListener {
    private NavController navController = null;

    private final int confirmButtonId = R.id.button_manual_confirm;
    private final int helpButtonId = R.id.button_manual_help;
    private final int closeButtonId = R.id.button_help_close;
    private ConstraintLayout helpLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.manual_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        view.findViewById(confirmButtonId).setOnClickListener(this);
        view.findViewById(helpButtonId).setOnClickListener(this);
        requireActivity().findViewById(closeButtonId).setOnClickListener(this);
        helpLayout = requireActivity().findViewById(R.id.help_layout);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case confirmButtonId:
                navController.navigate(R.id.action_welcomeFragment_to_manualFragment);
                break;
            case helpButtonId:
                helpLayout.setVisibility(View.VISIBLE);
                break;
            case closeButtonId:
                helpLayout.setVisibility(View.GONE);
                break;
        }
    }
}
