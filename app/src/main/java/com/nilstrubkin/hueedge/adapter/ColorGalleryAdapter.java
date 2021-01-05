package com.nilstrubkin.hueedge.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.nilstrubkin.hueedge.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ColorGalleryAdapter extends RecyclerView.Adapter<ColorGalleryAdapter.ColorViewHolder> {
    private final List<Integer> list;
    private final View.OnClickListener listener;
    private int selectedPos = RecyclerView.NO_POSITION;

    public int getSelectedPos() {
        return selectedPos;
    }

    public void setSelectedPos(int selectedPos) {
        this.selectedPos = selectedPos;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ColorViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public final ImageView icon;

        public ColorViewHolder(ConstraintLayout view){
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
        holder.icon.setImageResource(R.drawable.ic_053_circle_svg);
        holder.icon.setColorFilter(color);
        holder.icon.setTag(color);
        holder.icon.setSelected(selectedPos == position);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return list.size();
    }
}