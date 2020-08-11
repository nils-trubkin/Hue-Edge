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

import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.R;
import com.nilstrubkin.hueedge.activity.SetupActivity;
import com.nilstrubkin.hueedge.adapter.BridgeDiscoveryResultAdapter;
import com.nilstrubkin.hueedge.discovery.AuthEntry;
import com.nilstrubkin.hueedge.discovery.DiscoveryEngine;
import com.nilstrubkin.hueedge.discovery.DiscoveryEntry;
import com.nilstrubkin.hueedge.discovery.Result;
import com.rakshakhegde.stepperindicator.StepperIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class DiscoveryFragment extends Fragment implements View.OnClickListener {
    private transient static final String TAG = SetupActivity.class.getSimpleName();
    private NavController navController = null;
    private final int searchTimeout = 5 * 1000;
    //UI elements
    private TextView statusText;
    private RecyclerView resultsList;
    private ProgressBar progressBar;
    private Button cancelButton;
    private Animation progressBarAnim;

    private final List<DiscoveryEntry> results = new ArrayList<>();
    private transient ExecutorService executorService;
    private transient Executor executor;
    private transient DiscoveryEngine discoveryEngine;
    private transient BridgeDiscoveryResultAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.discovery_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        statusText = view.findViewById(R.id.text_setup_status);
        resultsList = view.findViewById(R.id.list_setup_results);
        progressBar = view.findViewById(R.id.progress_bar_setup_search);
        cancelButton = view.findViewById(R.id.button_setup_cancel);
        progressBar.setMax(10000); // Smoother animation
        progressBarAnim = new SetupActivity.ProgressBarAnimation(progressBar, 0, progressBar.getMax());
        progressBarAnim.setDuration(searchTimeout);
        progressBar.startAnimation(progressBarAnim);
        cancelButton.setOnClickListener(this);
        adapter = new BridgeDiscoveryResultAdapter(results);
        resultsList.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        resultsList.setLayoutManager(layoutManager);
        resultsList.setAdapter(adapter);
        navController = Navigation.findNavController(view);
        executor = r -> new Thread(r).start();

        ((StepperIndicator) requireActivity().findViewById(R.id.steps_wizard)).setCurrentStep(1);
    }


    @Override
    public void onStart() {
        super.onStart();
        startBridgeDiscovery();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_setup_cancel) {
            executorService.shutdown();
            navController.popBackStack();
        }
    }

    public void startBridgeDiscovery() {
        Log.i(TAG, "startBridgeDiscovery()");
        Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
        executorService = Executors.newFixedThreadPool(4);
        discoveryEngine = new DiscoveryEngine(executor, mainThreadHandler);
        discoveryEngine.initializeFullDiscovery(getActivity(), executorService, bridgeDiscoveryCallback);
        executorService.submit(() -> {
            try {
                Thread.sleep(searchTimeout);
                if (!executorService.isShutdown()){
                    statusText.setText(getResources().getString(R.string.fragment_results_label));
                    cancelButton.setText(getResources().getString(R.string.button_back_text));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        //progressBar.startAnimation(progressBarAnim);
    }

    public void stopBridgeDiscovery() {
        Log.i(TAG, "stopBridgeDiscovery()");
        progressBar.clearAnimation(); //TODO check this
        executorService.shutdownNow();
    }

    private final DiscoveryEngine.DiscoveryCallback<DiscoveryEntry> bridgeDiscoveryCallback = result -> {
        if (result instanceof Result.Success) {
            // Happy path
            DiscoveryEntry de = ((Result.Success<DiscoveryEntry>) result).data;
            Log.i(TAG, "Result: " + de.friendlyName);
//            for (DiscoveryEntry deInList : results)
//                if(deInList.ip.equals(de.ip))
//                    return;
            results.add(de);
            adapter.notifyItemInserted(adapter.getItemCount() - 1);
        } else {
            // Show error in UI
            Exception e = ((Result.Error<DiscoveryEntry>) result).exception;
            Log.e(TAG, "Error: " + e.toString());
        }
    };

    private final DiscoveryEngine.DiscoveryCallback<AuthEntry> bridgeAuthCallback = new DiscoveryEngine.DiscoveryCallback<AuthEntry>(){
        @Override
        public void onComplete(Result<AuthEntry> result) {
            if (result instanceof Result.Success) {
                // Happy path
                stopBridgeDiscovery();
                AuthEntry ae = ((Result.Success<AuthEntry>) result).data;
                HueBridge.getInstance(getContext(), ae.ip, ae.username);
                Log.i(TAG, "Bridge " + ae.ip +  " authorized successfully");
                //navController.navigate(R.id.settings); TODO
            } else {
                // Show error in UI
                stopBridgeDiscovery();
                Exception e = ((Result.Error<AuthEntry>) result).exception;
                Log.d(TAG, e.toString());
                if (e.getClass() == TimeoutException.class){
                    //navController.navigate(R.id.error); TODO
                }
            }
        }
    };
}
