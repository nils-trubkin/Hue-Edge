package com.ize.hueedge;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;

import com.ize.hueedge.activity.EditActivity;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Objects;

public class DragEventListener implements View.OnDragListener {

    private transient static final String TAG = DragEventListener.class.getSimpleName();
    private transient Context ctx;
    private transient final int index;

    public DragEventListener (Context ctx, int index){
        super();
        this.ctx = ctx;
        this.index = index;
    }

    // This is the method that the system calls when it dispatches a drag event to the
    // listener.
    public boolean onDrag(View v, DragEvent event) {

        // Defines a variable to store the action type for the incoming event
        final int action = event.getAction();
        EditActivity instance = (EditActivity) ctx;
        BridgeResource br;
        // Handles each of the expected events
        switch(action) {

            case DragEvent.ACTION_DRAG_STARTED:

                // Determines if this View can accept the dragged data
                if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {

                    // Invalidate the view to force a redraw in the new tint
                    v.invalidate();


                    // returns true to indicate that the View can accept the dragged data.
                    return true;

                }

                // Returns false. During the current drag and drop operation, this View will
                // not receive events again until ACTION_DRAG_ENDED is sent.
                return false;

            case DragEvent.ACTION_DRAG_ENTERED:

                br = (BridgeResource) event.getLocalState();
                instance.displaySlotAsFull(index, br);
                v.invalidate();

                return true;

            case DragEvent.ACTION_DRAG_LOCATION:

                // Ignore the event
                return true;

            case DragEvent.ACTION_DRAG_EXITED:

                // Invalidate the view to force a redraw in the new tint
                instance.panelUpdateIndex(index);
                v.invalidate();

                return true;

            case DragEvent.ACTION_DROP:

                // Gets the item containing the dragged data
                // Gets the item containing the dragged data
                ClipData.Item item = event.getClipData().getItemAt(0);

                // Gets the text data from the item.
                int dragData = Integer.parseInt((String) item.getText());

                if (dragData >= 0){
                    instance.clearSlot(dragData);
                }
                br = (BridgeResource) event.getLocalState();
                HueBridge bridge;
                try {
                    bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
                } catch (NullPointerException ex){
                    Log.e(TAG, "Tried to drag and drop but no HueBridge instance was found");
                    ex.printStackTrace();
                    return false;
                }

                bridge.addToCategory(ctx, instance.getCurrentCategory(), br, index);
                instance.panelUpdateIndex(index);
                // Invalidates the view to force a redraw
                v.invalidate();

                // Returns true. DragEvent.getResult() will return true.
                return true;

            case DragEvent.ACTION_DRAG_ENDED:


                // Invalidates the view to force a redraw
                v.invalidate();

                // Does a getResult(), and displays what happened.
                if (event.getResult()) {
                    Log.e(TAG,"The drop was handled.");
                } else {
                    Log.e(TAG,"The drop didn't work.");
                }

                // returns true; the value is ignored.
                return true;

            // An unknown action type was received.
            default:
                Log.e(TAG,"Unknown action type received by OnDragListener.");
                break;
        }

        return false;
    }
};