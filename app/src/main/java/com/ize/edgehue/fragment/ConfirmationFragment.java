package com.ize.edgehue.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.ize.edgehue.EdgeHueProvider;
import com.ize.edgehue.HueBridge;
import com.ize.edgehue.R;
import com.ize.edgehue.resource.BridgeResource;

import org.json.JSONException;

public class ConfirmationFragment extends Fragment {

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_confirmation, container, false);
    }

    public void onViewCreated(@NonNull View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.fragment_confirmation_button_yes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HueBridge.getInstance().deleteInstance();
                NavHostFragment.findNavController(ConfirmationFragment.this)
                        .navigate(R.id.action_ConfirmationFragment_to_WelcomeFragment);
            }
        });

        view.findViewById(R.id.fragment_confirmation_button_no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(ConfirmationFragment.this)
                        .navigate(R.id.action_ConfirmationFragment_to_FinalFragment);

            }
        });
    }
}
