package com.nilstrubkin.hueedge.api;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class RequestQueueSingleton {
    private static RequestQueueSingleton instance;
    private RequestQueue requestQueue;

    private RequestQueueSingleton(Context ctx) {
        requestQueue = getRequestQueue(ctx);
    }

    public static synchronized RequestQueueSingleton getInstance(Context ctx) {
        if (instance == null) {
            instance = new RequestQueueSingleton(ctx);
        }
        return instance;
    }

    private RequestQueue getRequestQueue(Context ctx) {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Context ctx, Request<T> req) {
        getRequestQueue(ctx).add(req);
    }
}