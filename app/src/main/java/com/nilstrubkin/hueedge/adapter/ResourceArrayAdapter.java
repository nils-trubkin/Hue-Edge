package com.nilstrubkin.hueedge.adapter;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.resources.BridgeResource;
import com.nilstrubkin.hueedge.R;

import java.util.ArrayList;
import java.util.Objects;

public class ResourceArrayAdapter extends ArrayAdapter<BridgeResource> {

    private static final String TAG = HueEdgeProvider.class.getSimpleName();
    private final Context ctx;
    private final int mResource;
    private final Vibrator vibrator;

    public ResourceArrayAdapter(Context context, int resource, ArrayList<BridgeResource> objects, Vibrator vib) {
        super(context, resource, objects);
        ctx = context;
        mResource = resource;
        vibrator = vib;
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

        final BridgeResource resource;
        try {
            resource = Objects.requireNonNull(getItem(position));
        }
        catch (NullPointerException e){
            Log.d(TAG, "Could not get item's position");
            e.printStackTrace();
            return convertView;
        }

        final String name = resource.getName();
        final String btnText = resource.getBtnText(ctx);
        final int btnTextSizeRes = resource.getBtnTextSize(ctx);
        final int btnColor = resource.getBtnTextColor(ctx);
        final int btnResource = resource.getBtnBackgroundResource();
        gridBtnTopText.setText(btnText);
        gridBtnTopText.setTextColor(btnColor);
        gridBtnTopText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                ctx.getResources().getDimensionPixelSize(btnTextSizeRes));
        gridBtn.setBackgroundResource(btnResource);
        gridBtnText.setText(name);
        /*gridBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BridgeResource br;
                try {
                    br = Objects.requireNonNull(getItem(position));
                }
                catch (NullPointerException e){
                    Log.e(TAG, "Failed to get item in grid adapter");
                    e.printStackTrace();
                    return;
                }
                HueBridge bridge;
                try {
                    bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
                } catch (NullPointerException e){
                    Log.e(TAG,"Failed to get HueBridge instance");
                    e.printStackTrace();
                    return;
                }
                final EditActivity instance = (EditActivity) ctx;
                HueEdgeProvider.menuCategory currentCategory = instance.getCurrentCategory();
                final int position = bridge.addToCategory(ctx, currentCategory, br);
                if (position == -1){
                    String toastString = ctx.getString(R.string.toast_add_over_ten_buttons);
                    Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
                }
                else {
                    HueEdgeProvider.saveAllConfiguration(ctx);
                    TextView tw = instance.findViewById(HueEdgeProvider.btnTextArr[position]);
                    tw.setText(br.getName());
                    Button btn = instance.findViewById(HueEdgeProvider.btnArr[position]);
                    btn.setText(br.getBtnText(ctx));
                    if(br.getCategory().equals("scenes")){
                        btn.setTextSize(ctx.getResources().getDimension(R.dimen.resource_btn_text_size_scene));
                    }
                    else {
                        btn.setTextSize(ctx.getResources().getDimension(R.dimen.resource_btn_text_size_symbol));
                    }
                    btn.setTextColor(br.getBtnTextColor(ctx));
                    btn.setBackgroundResource(br.getBtnBackgroundResource(ctx));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            instance.clearSlot(position);
                        }
                    });
                    Button btnDelete = instance.findViewById(HueEdgeProvider.btnDeleteArr[position]);
                    btnDelete.setVisibility(View.VISIBLE);
                    btnDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            instance.clearSlot(position);
                        }
                    });
                    String toastString = ctx.getString(R.string.toast_adding, br.getName());
                    Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
                }
            }
        });*/
            gridBtn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
                    boolean noHaptic = settings.getBoolean(ctx.getResources().getString(R.string.no_haptic_preference), false);
                    if(!noHaptic)
                        vibrator.vibrate(1);
                    ClipData.Item item = new ClipData.Item(String.valueOf(-1));
                    ClipData dragData = new ClipData(
                            name,
                            new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                            item);
                    View.DragShadowBuilder myShadow = new View.DragShadowBuilder(gridBtn);
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        return v.startDragAndDrop(dragData,  // the data to be dragged
                                myShadow,  // the drag shadow builder
                                resource,      // pass resource
                                0          // flags (not currently used, set to 0)
                        );
                    else
                        //noinspection deprecation
                        return v.startDrag(dragData,  // the data to be dragged
                                myShadow,  // the drag shadow builder
                                resource,      // pass resource
                                0          // flags (not currently used, set to 0)
                        );
                }
            });
        return convertView;
    }
}
