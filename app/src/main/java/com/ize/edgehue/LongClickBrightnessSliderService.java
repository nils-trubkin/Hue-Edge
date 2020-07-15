package com.ize.edgehue;

import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class LongClickBrightnessSliderService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent arg0) {
        return new SampleRemoveViewFactory();
    }

    private class SampleRemoveViewFactory implements RemoteViewsService.RemoteViewsFactory {

        private static final int MAX_CHILD = 25;
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
            itemView.setTextViewText(R.id.item_text1, getResources().getString(R.string.remote_list_item_title) + itemId);
            float h = 360f;
            float s = 1f;
            float v = 1f - id / (float) MAX_CHILD;
            int bgColor = Color.HSVToColor(new float[]{h, s, v});
            itemView.setInt(R.id.item_text1, "setBackgroundColor", bgColor);

            // set fill in intent
            Intent intent = new Intent();
            intent.putExtra("item_id", itemId);
            intent.putExtra("bg_color", bgColor);
            intent.putExtra("brightness", Math.round(v * 254));
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
