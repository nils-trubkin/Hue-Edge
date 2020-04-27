package com.ize.edgehue;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class RequestQueueSingleton {
    private static RequestQueueSingleton instance;
    private static Context ctx;
    private static String url = "http://192.168.69.166/api/aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD/lights/10/state";
    private RequestQueue requestQueue;

    private RequestQueueSingleton(Context context) {
        ctx = context;
        requestQueue = getRequestQueue();
    }

    public static String getUrl() {
        return url;
    }

    public static synchronized RequestQueueSingleton getInstance(Context context) {
        if (instance == null) {
            instance = new RequestQueueSingleton(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}