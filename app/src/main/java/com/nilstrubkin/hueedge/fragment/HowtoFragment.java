package com.nilstrubkin.hueedge.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.nilstrubkin.hueedge.R;

public class HowtoFragment extends Fragment implements View.OnClickListener {
    private NavController navController;

    //UI elements
    private final int backButtonId = R.id.button_howto_back;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.howto_fragment, container, false);
        // gif image
        final int gifId = R.id.gif_edgeswipe;
        ImageView imageView = view.findViewById(gifId);
        Glide.with(this).load(R.drawable.edgeswipe).into(imageView);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        view.findViewById(backButtonId).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == backButtonId) {
            navController.popBackStack();
        }
    }
}
