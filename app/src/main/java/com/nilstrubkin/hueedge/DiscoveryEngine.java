package com.nilstrubkin.hueedge;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;
import android.util.Xml;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.nilstrubkin.hueedge.activity.SetupActivity;
import com.nilstrubkin.hueedge.api.JsonCustomRequest;
import com.nilstrubkin.hueedge.api.RequestQueueSingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.concurrent.Executor;

import static android.content.Context.NSD_SERVICE;

public class DiscoveryEngine {

    private transient static final String TAG = DiscoveryEngine.class.getSimpleName();
    private final Executor executor;
    private final Handler resultHandler;
    //parser TODO

    public DiscoveryEngine(Executor executor, Handler resultHandler){
        this.executor = executor;
        this.resultHandler = resultHandler;
    }

    public void initializeSynchronousDnsSDDiscovery(
            final Context ctx,
            final DiscoveryCallback<DiscoveryEntry> callback){
        Log.d(TAG, "Initialize mDNS discovery...");
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final String SERVICE_TYPE = "_hue._tcp.";
                final NsdManager nsdManager = (NsdManager) ctx.getSystemService(NSD_SERVICE);
                NsdManager.DiscoveryListener discoveryListener =
                        initializeDnsSDDiscoveryListener(nsdManager, SERVICE_TYPE, callback);
                nsdManager.discoverServices(
                        SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            }
        });
    }

    private NsdManager.DiscoveryListener initializeDnsSDDiscoveryListener(
            final NsdManager nsdManager,
            final String SERVICE_TYPE,
            final DiscoveryCallback<DiscoveryEntry> callback) {
        // Instantiate a new DiscoveryListener
        return new NsdManager.DiscoveryListener() {
            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }
            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().contains("Hue")) {
                    //nsdManager.resolveService(service, resolveListener);
                    Log.d(TAG, "Found hue: " + service.getServiceName());
                    NsdManager.ResolveListener resolveListener =
                            initializeDnsSDResolveListener(callback);
                    nsdManager.resolveService(service, resolveListener);
                }
            }
            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "Service lost: " + service);
            }
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }
        };
    }

    private NsdManager.ResolveListener initializeDnsSDResolveListener(
            final DiscoveryCallback<DiscoveryEntry> callback
    ) {
        return new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed: " + errorCode);
                Result<DiscoveryEntry> errorResult = new Result.Error<>(errorCode);
                notifyResult(errorResult, callback);
            }
            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                InetAddress host = serviceInfo.getHost();
                try {
                    parseDescriptionXml(host.getHostAddress(), callback);
                } catch (IOException e) {
                    e.printStackTrace();
                    Result<DiscoveryEntry> errorResult = new Result.Error<>(e);
                    notifyResult(errorResult, callback);
                }
            }
        };
    }

    public void initializeSynchronousNUPNPDiscovery(
            final Context ctx,
            final DiscoveryCallback<DiscoveryEntry> callback){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Initialize NUPnP discovery...");
                String portal = "https://discovery.meethue.com";
                JsonCustomRequest jcr = getJsonNUPNP(portal, callback);
                RequestQueueSingleton.getInstance(ctx).addToRequestQueue(ctx, jcr);
            }
        });
    }

    private JsonCustomRequest getJsonNUPNP(final String portal,
                                           final DiscoveryCallback<DiscoveryEntry> callback){
        return new JsonCustomRequest(Request.Method.GET, portal, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "Request responds " + response.toString());
                        Iterator<String> responseKeys; // iterator for response JSONArray
                        for (int i = 0; i < response.length(); i++){
                            try {
                                JSONObject job = response.getJSONObject(i);
                                String ip = job.getString("internalipaddress");
                                parseDescriptionXml(ip, callback);
                                /*Result<DiscoveryEntry> result = new Result.Success<>(entry);
                                notifyResult(result, callback);*/
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                                Result<DiscoveryEntry> errorResult = new Result.Error<>(e);
                                notifyResult(errorResult, callback);
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.toString());
                    }
                }
        );
    }

    public interface DiscoveryCallback<T> {
        void onComplete(Result<T> result);
    }

    private void notifyResult(
            final Result<DiscoveryEntry> result,
            final DiscoveryCallback<DiscoveryEntry> callback
    ) {
        resultHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onComplete(result);
            }
        });
    }

    private void parseDescriptionXml(String address, final DiscoveryCallback<DiscoveryEntry> callback) throws IOException {
        final URL url = new URL("http://" + address + "/description.xml");
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try (InputStream in = url.openConnection().getInputStream()) {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                    parser.setInput(in, null);
                    parser.nextTag();
                    Result<DiscoveryEntry> result = new Result.Success<>(readXml(parser));
                    in.close();
                    notifyResult(result, callback);
                } catch (Exception e){
                    Result<DiscoveryEntry> errorResult = new Result.Error<>(e);
                    notifyResult(errorResult, callback);
                }
            }
        });
    }

    private DiscoveryEntry readXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "root");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("device")) {
                return readEntry(parser);
            } else {
                skip(parser);
            }
        }
        return null;
    }

    private DiscoveryEntry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "device");
        String friendlyName = null;
        String modelDescription = null;
        String serialNumber = null;
        String logoUrl = null; //TODO
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("friendlyName")) {
                friendlyName = readField(parser, "friendlyName");
            } else if (name.equals("modelDescription")) {
                modelDescription = readField(parser, "modelDescription");
            } else if (name.equals("serialNumber")) {
                serialNumber = readField(parser, "serialNumber");
            } else {
                skip(parser);
            }
        }
        if(modelDescription.contains("Philips hue"))
            return new DiscoveryEntry(friendlyName, modelDescription, serialNumber, logoUrl);
        else
            return null;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private String readField(XmlPullParser parser, String field) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, field);
        String summary = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, field);
        return summary;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }
}
