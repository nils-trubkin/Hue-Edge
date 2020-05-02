package com.ize.edgehue.activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ize.edgehue.R;
import com.ize.edgehue.resource.BridgeResource;

import java.util.ArrayList;
import java.util.Objects;

public class ResourceArrayAdapter extends ArrayAdapter<BridgeResource> {

    private static final String TAG = ResourceArrayAdapter.class.getSimpleName();

    private Context ctx;
    int mResource;

    public ResourceArrayAdapter(Context context, int resource, ArrayList<BridgeResource> objects) {
        super(context, resource, objects);
        ctx = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        LayoutInflater inflater = LayoutInflater.from(ctx);
        convertView = inflater.inflate(mResource, parent, false);

        Button gridBtn = convertView.findViewById(R.id.gridBtn);
        TextView gridBtnText = convertView.findViewById(R.id.gridBtnText);

        String name = Objects.requireNonNull(getItem(position)).getName();
        String btnText = Objects.requireNonNull(getItem(position)).getBtnText();
        int btnColor = Objects.requireNonNull(getItem(position)).getBtnTextColor();
        int btnResource = Objects.requireNonNull(getItem(position)).getBtnBackgroundResource();
        gridBtn.setText(btnText);
        gridBtn.setTextColor(btnColor);
        gridBtn.setBackgroundResource(btnResource);
        gridBtnText.setText(name);

        return convertView;
    }
}
