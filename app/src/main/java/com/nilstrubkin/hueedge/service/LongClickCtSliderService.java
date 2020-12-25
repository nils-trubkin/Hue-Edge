package com.nilstrubkin.hueedge.service;

import android.content.Intent;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.R;
import com.nilstrubkin.hueedge.fragment.SettingsFragment;

public class LongClickCtSliderService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent arg0) {
        return new SampleRemoveViewFactory();
    }

    private class SampleRemoveViewFactory implements RemoteViewsFactory {

        private int MAX_CHILD = 15;
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
            MAX_CHILD = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .getInt(getString(R.string.preference_ct_levels), 5) + SettingsFragment.minProgress;
            // create list item
            RemoteViews itemView = new RemoteViews(getPackageName(), R.layout.sliders_list_item);
            //int itemId = (int) (id + (mIdOffset * MAX_CHILD));
            //itemView.setTextViewText(R.id.item_text1, getResources().getString(R.string.remote_list_item_title) + itemId);
            //int slidersResourceColor = HueEdgeProvider.getSlidersResHue();
            float ct = (500f - 153f) * id / (float) MAX_CHILD + 153f;
            float kelvin = 1_000_000f / ct;
            //float h = 1f;
            //float s = 1f;
            //float v = 1f;
            int bgColor = colorTemperatureToColor(kelvin);
            //Log.e(TAG, "ct " + ct + " kelvin " + kelvin + " bgColor " + String.format("0x%08X", bgColor));

            itemView.setInt(R.id.item_text1, "setBackgroundColor", bgColor);

            // set fill in intent
            Intent intent = new Intent();
            //intent.putExtra("item_id", itemId);
            //intent.putExtra("bg_color", bgColor);
            //intent.putExtra("bri", Math.round(v * 254));
            intent.putExtra("ct", Math.round(ct));
            // should be set fillInIntent to root of item layout
            itemView.setOnClickFillInIntent(R.id.item_root, intent);

            return itemView;
        }

        private int colorTemperatureToColor(float kelvin){

            // adjusted from 100 to 50 to shift the spectrum into cold
            float temp = kelvin / 50;
            float red, green, blue;

            if( temp <= 66 ){
                red = 255;

                green = temp;
                green = 99.4708025861f * (float) Math.log(green) - 161.1195681661f;

                if( temp <= 19){
                    blue = 0;
                } else {
                    blue = temp-10;
                    blue = 138.5177312231f * (float) Math.log(blue) - 305.0447927307f;
                }
            } else {
                red = temp - 60;
                red = 329.698727446f * (float) Math.pow(red, -0.1332047592f);

                green = temp - 60;
                green = 288.1221695283f * (float) Math.pow(green, -0.0755148492f);

                blue = 255;
            }

            // adjusted to have higher saturation
            float[] hsv = new float[3];
            Color.RGBToHSV((int) clamp(red), (int) clamp(green), (int) clamp(blue), hsv);
            hsv[1] *= 2;
            return Color.HSVToColor(hsv);
        }

        public float clamp(float val) {
            return Math.max(0f, Math.min(255f, val));
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
