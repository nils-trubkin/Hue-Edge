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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.R;
import com.nilstrubkin.hueedge.ResourceReference;
import com.nilstrubkin.hueedge.resources.BridgeResource;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class CatalogueAdapter extends RecyclerView.Adapter<CatalogueAdapter.ResourceViewHolder> {
    private final List<ResourceReference> list;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ResourceViewHolder extends RecyclerView.ViewHolder{
        public final Context ctx;
        public final Button gridBtn;
        public final TextView gridBtnText;

        public ResourceViewHolder(ConstraintLayout view){
            super(view);
            ctx = view.getContext();
            gridBtn = view.findViewById(R.id.gridBtn);
            gridBtnText = view.findViewById(R.id.gridBtnText);
        }
    }

    public CatalogueAdapter(List<ResourceReference> list) {
        this.list = list;
    }

    // Create new views (invoked by the layout manager)
    @NotNull
    @Override
    public ResourceViewHolder onCreateViewHolder(ViewGroup parent,
                                                 int viewType) {
        // create a new view
        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.catalogue_item, parent, false);
        return new ResourceViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NotNull ResourceViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Context ctx = holder.ctx;
        ResourceReference resRef;
        BridgeResource res;
        resRef = list.get(position);
        res = HueBridge.getInstance(ctx).getResource(resRef);

        final String name = res.getName();
        final String underBtnText = res.getUnderBtnText();
        final String category = res.getCategory();
        final String btnText = res.getBtnText(ctx);
        final int btnTextSizeRes = res.getBtnTextSize(ctx);
        final int btnColor = res.getBtnTextColor(ctx);
        final int btnResource = res.getBtnBackgroundResource();
        holder.gridBtn.setText(btnText);
        holder.gridBtn.setTextColor(btnColor);
        holder.gridBtn.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                ctx.getResources().getDimensionPixelSize(btnTextSizeRes));
        holder.gridBtn.setBackgroundResource(btnResource);
        holder.gridBtnText.setText(ctx.getString(R.string.detailed_under_btn_text, underBtnText, category.substring(0, category.length() - 1)));
        holder.gridBtn.setOnLongClickListener(v -> {
            HueEdgeProvider.vibrate(ctx);
            ClipData.Item item = new ClipData.Item(String.valueOf(-1));
            ClipData dragData = new ClipData(
                    name,
                    new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                    item);
            View.DragShadowBuilder myShadow = new View.DragShadowBuilder(holder.gridBtn);
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
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return list.size();
    }
}