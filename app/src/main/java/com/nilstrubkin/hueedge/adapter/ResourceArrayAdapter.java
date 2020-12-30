package com.nilstrubkin.hueedge.adapter;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.ResourceReference;
import com.nilstrubkin.hueedge.resources.BridgeResource;
import com.nilstrubkin.hueedge.R;

import java.util.ArrayList;
import java.util.Objects;

public class ResourceArrayAdapter extends ArrayAdapter<ResourceReference> {

    private static final String TAG = HueEdgeProvider.class.getSimpleName();
    private final Context ctx;
    private final int mResource;

    public ResourceArrayAdapter(Context context, int resource, ArrayList<ResourceReference> objects) {
        super(context, resource, objects);
        ctx = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent){

        LayoutInflater inflater = LayoutInflater.from(ctx);
        if(convertView == null)
            convertView = inflater.inflate(mResource, parent, false);

        final Button gridBtn = convertView.findViewById(R.id.gridBtn);
        final TextView gridBtnText = convertView.findViewById(R.id.gridBtnText);
        final TextView gridBtnTopText = convertView.findViewById(R.id.gridBtnTopText);

        final ResourceReference resRef;
        BridgeResource res;
        try {
            resRef = Objects.requireNonNull(getItem(position));
            res = HueBridge.getInstance(ctx).getResource(Objects.requireNonNull(resRef));
        }
        catch (NullPointerException e){
            Log.d(TAG, "Could not get item's position");
            e.printStackTrace();
            return convertView;
        }

        final String name = res.getName();
        final String underBtnText = res.getUnderBtnText();
        final String category = res.getCategory();
        final String btnText = res.getBtnText(ctx);
        final int btnTextSizeRes = res.getBtnTextSize(ctx);
        final int btnColor = res.getBtnTextColor(ctx);
        final int btnResource = res.getBtnBackgroundResource();
        gridBtnTopText.setText(btnText);
        gridBtnTopText.setTextColor(btnColor);
        gridBtnTopText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                ctx.getResources().getDimensionPixelSize(btnTextSizeRes));
        gridBtn.setBackgroundResource(btnResource);
        gridBtnText.setText(ctx.getString(R.string.detailed_under_btn_text, underBtnText, category));
        gridBtn.setOnLongClickListener(v -> {
            HueEdgeProvider.vibrate(ctx);
            ClipData.Item item = new ClipData.Item(String.valueOf(-1));
            ClipData dragData = new ClipData(
                    name,
                    new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                    item);
            View.DragShadowBuilder myShadow = new View.DragShadowBuilder(gridBtn);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                return v.startDragAndDrop(dragData,  // the data to be dragged
                        myShadow,  // the drag shadow builder
                        resRef,      // pass resource
                        0          // flags (not currently used, set to 0)
                );
            else
                //noinspection deprecation
                return v.startDrag(dragData,  // the data to be dragged
                        myShadow,  // the drag shadow builder
                        resRef,      // pass resource
                        0          // flags (not currently used, set to 0)
                );
        });
        return convertView;
    }
}
