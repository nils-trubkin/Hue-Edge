package com.nilstrubkin.hueedge.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nilstrubkin.hueedge.R;
import com.nilstrubkin.hueedge.activity.SetupActivity;
import com.nilstrubkin.hueedge.adapter.BridgeDiscoveryResultAdapter;
import com.nilstrubkin.hueedge.discovery.DiscoveryEngine;
import com.nilstrubkin.hueedge.discovery.DiscoveryEntry;
import com.nilstrubkin.hueedge.discovery.Result;
import com.badoualy.stepperindicator.StepperIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscoveryFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = DiscoveryFragment.class.getSimpleName();
    private NavController navController = null;
    private final int searchTimeout = 30 * 1000;

    //UI elements
    private TextView statusText;
    private ProgressBar progressBar;
    private Button cancelButton;

    // Discovery elements
    private final List<DiscoveryEntry> results = new ArrayList<>();
    private ExecutorService executorService;
    private Executor executor;
    private BridgeDiscoveryResultAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.discovery_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        statusText = view.findViewById(R.id.text_setup_status);
        RecyclerView resultsList = view.findViewById(R.id.list_setup_results);
        progressBar = view.findViewById(R.id.progress_bar_setup_search);
        cancelButton = view.findViewById(R.id.button_setup_cancel);

        cancelButton.setOnClickListener(this);

        navController = Navigation.findNavController(view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

        adapter = new BridgeDiscoveryResultAdapter(results, navController, ignored -> stopBridgeDiscovery());
        resultsList.setAdapter(adapter);
        resultsList.setHasFixedSize(true);
        resultsList.setLayoutManager(layoutManager);

        ((StepperIndicator) requireActivity().findViewById(R.id.steps_wizard)).setCurrentStep(1);
    }

    @Override
    public void onStart() {
        super.onStart();
        executor = r -> new Thread(r).start();
        progressBar.setMax(10000); // Smoother animation
        Animation progressBarAnim = new SetupActivity.ProgressBarAnimation(progressBar, 0, progressBar.getMax());
        progressBarAnim.setDuration(searchTimeout);
        startBridgeDiscovery();
        progressBar.startAnimation(progressBarAnim);
    }

    @Override
    public void onResume() {
        super.onResume();
        results.clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_setup_cancel) {
            stopBridgeDiscovery();
            navController.popBackStack();
        }
    }

    public void startBridgeDiscovery() {
        Log.i(TAG, "startBridgeDiscovery()");
        Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
        executorService = Executors.newFixedThreadPool(Math.min(4, Runtime.getRuntime().availableProcessors()));
        DiscoveryEngine discoveryEngine = new DiscoveryEngine(executor, mainThreadHandler);
        discoveryEngine.initializeFullDiscovery(getActivity(), executorService, bridgeDiscoveryCallback);
        executorService.submit(() -> {
            try {
                Thread.sleep(searchTimeout);
                if (!executorService.isShutdown()){
                    stopBridgeDiscovery();
                    if (results.isEmpty())
                        navController.navigate(R.id.errorFragment);
                    else {
                        statusText.setText(getResources().getString(R.string.fragment_results_label));
                        cancelButton.setText(getResources().getString(R.string.button_back_text));
                    }
                }
            } catch (InterruptedException e) {
                stopBridgeDiscovery();
            }
        });
    }

    public void stopBridgeDiscovery() {
        Log.i(TAG, "stopBridgeDiscovery()");
        executorService.shutdownNow();
    }

    private final DiscoveryEngine.DiscoveryCallback<DiscoveryEntry> bridgeDiscoveryCallback = result -> {
        if (result instanceof Result.Success) {
            // Happy path
            DiscoveryEntry de = ((Result.Success<DiscoveryEntry>) result).data;
            Log.i(TAG, "Result: " + de.friendlyName);
            for (DiscoveryEntry deInList : results)
                if(deInList.ip.equals(de.ip))
                    return;
            results.add(de);
            adapter.notifyItemInserted(adapter.getItemCount() - 1);
        } else {
            // Error path
            Exception e = ((Result.Error<DiscoveryEntry>) result).exception;
            Log.e(TAG, "Error: " + e.toString());
        }
    };
}
