package com.nilstrubkin.hueedge.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.badoualy.stepperindicator.StepperIndicator;
import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.R;

public class ManualFragment extends Fragment implements View.OnClickListener {
    private NavController navController = null;

    // UI elements
    private final int confirmButtonId = R.id.button_manual_confirm;
    private final int helpButtonId = R.id.button_manual_help;
    private final int closeButtonId = R.id.button_help_close;
    private ConstraintLayout helpLayout;
    private Button aboutButton;
    private ConstraintLayout tint;
    private EditText ipField;

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
        helpLayout = requireActivity().findViewById(R.id.layout_manual_help);
        aboutButton = requireActivity().findViewById(R.id.button_about);
        tint = requireActivity().findViewById(R.id.layout_tint);
        ipField = requireActivity().findViewById(R.id.text_manual_input_ip_field);

        ((StepperIndicator) requireActivity().findViewById(R.id.steps_wizard)).setCurrentStep(1);

        ipField.setOnKeyListener((v, keyCode, event) -> {
            Log.e("tag", "" + keyCode);
            if ((event.getAction() == KeyEvent.ACTION_DOWN)
                    && (keyCode == KeyEvent.KEYCODE_ENTER)){
                checkIfValidIp();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case confirmButtonId:
                checkIfValidIp();
                break;
            case helpButtonId:
                helpLayout.setVisibility(View.VISIBLE);
                aboutButton.setVisibility(View.GONE);
                tint.setVisibility(View.VISIBLE);
                break;
            case closeButtonId:
                helpLayout.setVisibility(View.GONE);
                aboutButton.setVisibility(View.VISIBLE);
                tint.setVisibility(View.GONE);
                break;
        }
    }

    private void checkIfValidIp(){
        if(HueEdgeProvider.checkWifiNotEnabled(requireContext())) {
            Toast.makeText(getContext(), requireContext().getString(R.string.toast_no_wifi), Toast.LENGTH_LONG).show();
            return;
        }
        String ip = ipField.getText().toString();
        if (Patterns.IP_ADDRESS.matcher(ip).matches()) {
            Bundle bundle = new Bundle();
            bundle.putString("ip", ip);
            navController.navigate(R.id.action_manualFragment_to_linkFragment, bundle);
        }
        else {
            Context ctx = requireContext();
            String toastString = ctx.getString(R.string.toast_ip_mistake);
            Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
        }
    }
}
