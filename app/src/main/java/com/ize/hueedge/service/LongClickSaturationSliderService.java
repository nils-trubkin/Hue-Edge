package com.ize.hueedge.service;

import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.ize.hueedge.HueEdgeProvider;
import com.ize.hueedge.R;

public class LongClickSaturationSliderService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent arg0) {
        return new SampleRemoveViewFactory();
    }

    private class SampleRemoveViewFactory implements RemoteViewsFactory {

        private static final int MAX_CHILD = 15;
        private static final int MAX_LEVEL = 10;
        private final String TAG = SampleRemoveViewFactory.class.getSimpleName();
        private float mIdOffset = -1;

        @Override
        public int getCount() {
            return MAX_CHILD;
        }

        @Override
        public long getItemId(int id) {
            return id;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public RemoteViews getViewAt(int id) {
            // create list item
            RemoteViews itemView = new RemoteViews(getPackageName(), R.layout.sliders_list_item);
            int itemId = (int) (id + (mIdOffset * MAX_CHILD));
            //itemView.setTextViewText(R.id.item_text1, getResources().getString(R.string.remote_list_item_title3) + itemId);
            int slidersResourceColor = HueEdgeProvider.getSlidersResourceColor();
            float h = slidersResourceColor * 360f / 65536f;
            float s = id / (float) MAX_CHILD;
            float v = 1f;
            int bgColor = Color.HSVToColor(new float[]{h, s, v});
            itemView.setInt(R.id.item_text1, "setBackgroundColor", bgColor);

            // set fill in intent
            Intent intent = new Intent();
            intent.putExtra("item_id", itemId);
            intent.putExtra("bg_color", bgColor);
            intent.putExtra("saturation", Math.round(s * 254));
            // should be set fillInIntent to root of item layout
            itemView.setOnClickFillInIntent(R.id.item_root, intent);

            return itemView;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public void onCreate() {
        }

        @Override
        public void onDataSetChanged() {
            mIdOffset = (mIdOffset + 1) % MAX_LEVEL;
            Log.d(TAG, "onDataSetChanged: " + mIdOffset);
        }

        @Override
        public void onDestroy() {
        }

    }
}
