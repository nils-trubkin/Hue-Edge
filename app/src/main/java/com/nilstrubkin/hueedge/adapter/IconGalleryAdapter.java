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

public class IconGalleryAdapter extends RecyclerView.Adapter<IconGalleryAdapter.IconViewHolder> {
    private final List<Integer> list;
    private final View.OnClickListener listener;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class IconViewHolder extends RecyclerView.ViewHolder{
        public final Context ctx;
        public final ImageButton button;

        public IconViewHolder(ConstraintLayout view){
            super(view);
            ctx = view.getContext();
            button = view.findViewById(R.id.button_icon);
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
        holder.button.setImageResource(icon_res);
        holder.button.setTag(icon_res);
        holder.button.setOnClickListener(listener);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return list.size();
    }
}