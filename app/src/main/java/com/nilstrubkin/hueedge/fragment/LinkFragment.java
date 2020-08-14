package com.nilstrubkin.hueedge.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.badoualy.stepperindicator.StepperIndicator;
import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.R;
import com.nilstrubkin.hueedge.activity.SetupActivity;
import com.nilstrubkin.hueedge.discovery.AuthEntry;
import com.nilstrubkin.hueedge.discovery.DiscoveryEngine;
import com.nilstrubkin.hueedge.discovery.Result;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class LinkFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = LinkFragment.class.getSimpleName();
    private NavController navController = null;
    private final int authTimeout = DiscoveryEngine.REQUEST_AMOUNT * 1000;
    private String ip;

    // UI elements
    private ProgressBar progressBar;

    // Link elements
    private ExecutorService executorService;
    private Executor executor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ip = requireArguments().getString("ip");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.link_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        progressBar = view.findViewById(R.id.progress_bar_setup_search);
        view.findViewById(R.id.button_setup_cancel).setOnClickListener(this);

        ((StepperIndicator) requireActivity().findViewById(R.id.steps_wizard)).setCurrentStep(2);
    }

    @Override
    public void onStart() {
        super.onStart();
        executor = r -> new Thread(r).start();
        progressBar.setMax(10000); // Smoother animation
        Animation progressBarAnim = new SetupActivity.ProgressBarAnimation(progressBar, 0, progressBar.getMax());
        progressBarAnim.setDuration(authTimeout);
        startBridgeLink();
        progressBar.startAnimation(progressBarAnim);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_setup_cancel) {
            stopBridgeLink();
            navController.popBackStack();
        }
    }

    public void startBridgeLink() {
        Log.i(TAG, "startBridgeLink()");
        Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
        executorService = Executors.newFixedThreadPool(Math.min(4, Runtime.getRuntime().availableProcessors()));
        DiscoveryEngine discoveryEngine = new DiscoveryEngine(executor, mainThreadHandler);
        discoveryEngine.connectToBridge(executorService, bridgeAuthCallback, ip);
        executorService.submit(() -> {
            try {
                Thread.sleep(authTimeout);
                if (!executorService.isShutdown()){
                    stopBridgeLink();
                    navController.navigate(R.id.action_linkFragment_to_errorFragment);
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Interrupted! Most likely successfully linked");
            }
        });
    }

    public void stopBridgeLink() {
        Log.i(TAG, "stopBridgeLink()");
        executorService.shutdownNow();
    }

    private final DiscoveryEngine.DiscoveryCallback<AuthEntry> bridgeAuthCallback = result -> {
        if (result instanceof Result.Success) {
            // Happy path
            stopBridgeLink();
            AuthEntry ae = ((Result.Success<AuthEntry>) result).data;
            HueBridge.getInstance(getContext(), ae.ip, ae.username);
            Log.i(TAG, "Bridge " + ae.ip +  " authorized successfully");
            navController.navigate(R.id.action_linkFragment_to_setupFragment);
        } else {
            // Error path
            Exception e = ((Result.Error<AuthEntry>) result).exception;
            Log.d(TAG, e.toString());
            if (e.getClass() == TimeoutException.class){
                navController.navigate(R.id.action_linkFragment_to_errorFragment);
            }
        }
    };
}
