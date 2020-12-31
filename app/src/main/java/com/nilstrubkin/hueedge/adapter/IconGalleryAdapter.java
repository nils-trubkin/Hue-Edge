package com.nilstrubkin.hueedge.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.nilstrubkin.hueedge.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IconGalleryAdapter extends RecyclerView.Adapter<IconGalleryAdapter.IconViewHolder> {
    private final List<Integer> list;
    private final View.OnClickListener listener;
    private int selectedPos = RecyclerView.NO_POSITION;
    private int selectedColor = 0x252525;

    public int getSelectedPos() {
        return selectedPos;
    }

    public void setSelectedPos(int selectedPos) {
        this.selectedPos = selectedPos;
    }

    public int getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(int selectedColor) {
        this.selectedColor = selectedColor;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class IconViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public final ImageView icon;

        public IconViewHolder(ConstraintLayout view){
            super(view);
            icon = view.findViewById(R.id.icon);
            icon.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            notifyItemChanged(selectedPos);
            selectedPos = getLayoutPosition();
            notifyItemChanged(selectedPos);
            listener.onClick(v);
        }
    }

    public IconGalleryAdapter(List<Integer> list,
                              View.OnClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    // Create new views (invoked by the layout manager)
    @NotNull
    @Override
    public IconGalleryAdapter.IconViewHolder onCreateViewHolder(ViewGroup parent,
                                                                           int viewType) {
        // create a new view
        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.icon_item, parent, false);
        return new IconViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NotNull IconViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        int icon_res = list.get(position);
        holder.icon.setImageResource(icon_res);
        holder.icon.setColorFilter(selectedColor);
        holder.icon.setTag(icon_res);
        holder.icon.setSelected(selectedPos == position);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return list.size();
    }


}