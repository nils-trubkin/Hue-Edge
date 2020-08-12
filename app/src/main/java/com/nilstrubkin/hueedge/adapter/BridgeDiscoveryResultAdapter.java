package com.nilstrubkin.hueedge.adapter;

import android.content.Context;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nilstrubkin.hueedge.discovery.DiscoveryEntry;
import com.nilstrubkin.hueedge.R;

import java.util.List;

public class BridgeDiscoveryResultAdapter extends RecyclerView.Adapter<BridgeDiscoveryResultAdapter.BridgeDiscoveryViewHolder> {
    private List<DiscoveryEntry> list;
    private NavController navController;
    private View.OnClickListener listener;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class BridgeDiscoveryViewHolder extends RecyclerView.ViewHolder{
        public Context ctx;
        public ConstraintLayout discoveryItem;
        public TextView bridgeIp;
        public TextView bridgeId;
        public BridgeDiscoveryViewHolder(ConstraintLayout view){
            super(view);
            ctx = view.getContext();
            discoveryItem = view;
            bridgeIp = view.findViewById(R.id.bridge_ip);
            bridgeId = view.findViewById(R.id.bridge_id);
        }
    }

    public BridgeDiscoveryResultAdapter(List<DiscoveryEntry> list, NavController navController,
                                        View.OnClickListener listener) {
        this.list = list;
        this.navController = navController;
        this.listener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BridgeDiscoveryResultAdapter.BridgeDiscoveryViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        // create a new view
        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bridge_discovery_item, parent, false);
        return new BridgeDiscoveryViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(BridgeDiscoveryViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        DiscoveryEntry de = list.get(position);
        if(de.ip != null) {
            holder.bridgeIp.setText(de.friendlyName);
            holder.bridgeId.setText(holder.ctx.getResources().getString(R.string.list_view_id_label, de.serialNumber.toUpperCase()));
            Bundle bundle = new Bundle();
            bundle.putString("ip", de.ip);
            holder.itemView.setOnClickListener(view -> {
                listener.onClick(view);
                navController.navigate(R.id.action_discoveryFragment_to_linkFragment, bundle);
            });
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return list.size();
    }
}