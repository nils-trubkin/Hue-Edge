package com.nilstrubkin.hueedge.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.nilstrubkin.hueedge.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ColorGalleryAdapter extends RecyclerView.Adapter<ColorGalleryAdapter.ColorViewHolder> {
    private final List<Integer> list;
    private final View.OnClickListener listener;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ColorViewHolder extends RecyclerView.ViewHolder{
        public final Context ctx;
        public final ImageButton button;

        public ColorViewHolder(ConstraintLayout view){
            super(view);
            ctx = view.getContext();
            button = view.findViewById(R.id.button_icon);
        }
    }

    public ColorGalleryAdapter(List<Integer> list,
                               View.OnClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    // Create new views (invoked by the layout manager)
    @NotNull
    @Override
    public ColorGalleryAdapter.ColorViewHolder onCreateViewHolder(ViewGroup parent,
                                                                            int viewType) {
        // create a new view
        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.color_item, parent, false);
        return new ColorViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NotNull ColorViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        int color = list.get(position);
        holder.button.setImageResource(R.drawable.ic_053_circle_svg);
        holder.button.setColorFilter(color);
        holder.button.setTag(color);
        holder.button.setOnClickListener(listener);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return list.size();
    }
}