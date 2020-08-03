package com.nilstrubkin.hueedge.adapter;

import android.content.Context;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nilstrubkin.hueedge.discovery.DiscoveryEntry;
import com.nilstrubkin.hueedge.R;

import java.util.List;
import java.util.Objects;

public class BridgeDiscoveryResultAdapter extends ArrayAdapter<DiscoveryEntry> {

    private static final String TAG = BridgeDiscoveryResultAdapter.class.getSimpleName();
    private final Context ctx;

    public BridgeDiscoveryResultAdapter(Context context, List<DiscoveryEntry> results) {
        super(context, 0, results);
        ctx = context;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        DiscoveryEntry result;
        try {
            result = Objects.requireNonNull(getItem(position));
        } catch (NullPointerException e){
            Log.e(TAG, "Could not fetch result");
            return convertView;
        }

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.bridge_discovery_item, parent, false);
        }

        TextView bridge_id = convertView.findViewById(R.id.bridge_id);
        TextView bridge_ip = convertView.findViewById(R.id.bridge_ip);

        bridge_ip.setText(result.friendlyName);
        bridge_id.setText(ctx.getResources().getString(R.string.list_view_id_label, result.serialNumber.toUpperCase()));

        return convertView;
    }
}