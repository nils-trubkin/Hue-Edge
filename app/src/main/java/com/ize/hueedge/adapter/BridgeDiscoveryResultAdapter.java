package com.ize.hueedge.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.ize.hueedge.R;

import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;

import java.util.List;

public class BridgeDiscoveryResultAdapter extends ArrayAdapter<BridgeDiscoveryResult> {

    private final Context ctx;

    public BridgeDiscoveryResultAdapter(Context context, List<BridgeDiscoveryResult> results) {
        super(context, 0, results);
        ctx = context;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        BridgeDiscoveryResult result = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.bridge_discovery_item, parent, false);
        }

        TextView bridge_id = convertView.findViewById(R.id.bridge_id);
        TextView bridge_ip = convertView.findViewById(R.id.bridge_ip);

        assert result != null;
        bridge_ip.setText(ctx.getResources().getString(R.string.list_view_ip_label, result.getIp()));
        bridge_id.setText(ctx.getResources().getString(R.string.list_view_id_label, result.getUniqueId()));

        return convertView;
    }
}