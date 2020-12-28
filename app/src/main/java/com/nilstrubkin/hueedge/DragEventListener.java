package com.nilstrubkin.hueedge;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;

import com.nilstrubkin.hueedge.activity.EditActivity;
import com.nilstrubkin.hueedge.resources.BridgeResource;

import java.util.Objects;

public class DragEventListener implements View.OnDragListener {

    private final transient static  String TAG = DragEventListener.class.getSimpleName();
    private final transient Context ctx;
    private final transient int index;

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
        ResourceReference resRef;
        // Handles each of the expected events
        switch(action) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Determines if this View can accept the dragged data
                // returns true to indicate that the View can accept the dragged data.
                return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                // If it returns false, during the current drag and drop operation, this View will
                // not receive events again until ACTION_DRAG_ENDED is sent.

            case DragEvent.ACTION_DRAG_ENTERED:
                resRef = (ResourceReference) event.getLocalState();
                instance.displaySlotAsFull(index, resRef);
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                // Ignore the event
                return true;

            case DragEvent.ACTION_DRAG_EXITED:
                instance.panelUpdateIndex(index);
                return true;

            case DragEvent.ACTION_DROP:
                // Gets the item containing the dragged data
                ClipData.Item item = event.getClipData().getItemAt(0);
                // Gets the text data from the item.
                int dragData = Integer.parseInt((String) item.getText());
                if (dragData >= 0){
                    instance.clearSlot(dragData);
                }
                resRef = (ResourceReference) event.getLocalState();
                HueBridge bridge;
                try {
                    bridge = Objects.requireNonNull(HueBridge.getInstance(ctx));
                } catch (NullPointerException e){
                    Log.e(TAG, "Tried to drag and drop but no HueBridge instance was found");
                    e.printStackTrace();
                    return false;
                }
                BridgeResource br = bridge.getResource(resRef);
                bridge.addToCurrentCategory(ctx, br, index);
                instance.panelUpdateIndex(index); 
                // Returns true. DragEvent.getResult() will return true.
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                // Does a getResult(), and displays what happened.
                if (event.getResult()) {
                    Log.d(TAG,"The drop was handled.");
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
}