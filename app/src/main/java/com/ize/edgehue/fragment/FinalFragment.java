package com.ize.edgehue.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.ize.edgehue.EdgeHueProvider;
import com.ize.edgehue.R;

import org.json.JSONException;

public class FinalFragment extends Fragment {

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_final, container, false);
    }

    public void onViewCreated(@NonNull View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.fragment_final_button_finish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    EdgeHueProvider.quickSetup(getActivity());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                requireActivity().finish();
            }
        });

        view.findViewById(R.id.fragment_final_button_remove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FinalFragment.this)
                        .navigate(R.id.action_FinalFragment_to_ConfirmationFragment);
            }
        });
    }
}
