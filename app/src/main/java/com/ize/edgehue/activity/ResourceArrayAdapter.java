package com.ize.edgehue.activity;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ize.edgehue.EdgeHueProvider;
import com.ize.edgehue.R;
import com.ize.edgehue.BridgeResource;

import java.util.ArrayList;
import java.util.Objects;

public class ResourceArrayAdapter extends ArrayAdapter<BridgeResource> {

    private static final String TAG = EdgeHueProvider.class.getSimpleName();
    private final Context ctx;
    private final int mResource;

    public ResourceArrayAdapter(Context context, int resource, ArrayList<BridgeResource> objects) {
        super(context, resource, objects);
        ctx = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent){

        LayoutInflater inflater = LayoutInflater.from(ctx);
        convertView = inflater.inflate(mResource, parent, false);

        Button gridBtn = convertView.findViewById(R.id.gridBtn);
        TextView gridBtnText = convertView.findViewById(R.id.gridBtnText);

        BridgeResource resource;
        try {
            resource = Objects.requireNonNull(getItem(position));
        }
        catch (NullPointerException ex){
            Log.d(TAG, "Could not get item's position");
            ex.printStackTrace();
            return convertView;
        }

        String name = resource.getName(ctx);
        String btnText = resource.getBtnText(ctx);
        int btnColor = resource.getBtnTextColor(ctx);
        int btnResource = resource.getBtnBackgroundResource(ctx);
        gridBtn.setText(btnText);
        gridBtn.setTextColor(btnColor);
        gridBtn.setBackgroundResource(btnResource);
        if(resource.getCategory().equals("scenes")){
            gridBtn.setTextSize(10);
        }
        else {
            gridBtn.setTextSize(14);
        }
        gridBtn.setBackgroundResource(btnResource);
        gridBtnText.setText(name);
        gridBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BridgeResource br;
                try {
                    br = Objects.requireNonNull(getItem(position));
                }
                catch (NullPointerException ex){
                    Log.e(TAG, "Failed to get item in grid adapter");
                    ex.printStackTrace();
                    return;
                }
                final int position = EdgeHueProvider.addToCurrentCategory(br);
                if (position == -1){
                    String toastString = "Can't add more than 10 buttons";
                    Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
                }
                else {
                    EdgeHueProvider.saveAllConfiguration(ctx);
                    final EditActivity instance = (EditActivity) ctx;
                    TextView tw = instance.findViewById(EdgeHueProvider.btnTextArr[position]);
                    tw.setText(br.getName(ctx));
                    Button btn = instance.findViewById(EdgeHueProvider.btnArr[position]);
                    btn.setText(br.getBtnText(ctx));
                    btn.setTextColor(br.getBtnTextColor(ctx));
                    btn.setBackgroundResource(br.getBtnBackgroundResource(ctx));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            instance.clearSlot(position);
                        }
                    });
                    Button btnDelete = instance.findViewById(EdgeHueProvider.btnDeleteArr[position]);
                    btnDelete.setVisibility(View.VISIBLE);
                    btnDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            instance.clearSlot(position);
                        }
                    });
                    String toastString = "Adding \"" + br.getName(ctx) + "\"";
                    Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
                }
            }
        });

        return convertView;
    }
}
