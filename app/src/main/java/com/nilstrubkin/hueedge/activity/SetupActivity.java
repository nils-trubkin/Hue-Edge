package com.nilstrubkin.hueedge.activity;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.util.Xml;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.nilstrubkin.hueedge.DiscoveryEngine;
import com.nilstrubkin.hueedge.DiscoveryEntry;
import com.nilstrubkin.hueedge.HueEdgeProvider;
import com.nilstrubkin.hueedge.HueBridge;
import com.nilstrubkin.hueedge.R;
import com.nilstrubkin.hueedge.Result;
import com.nilstrubkin.hueedge.adapter.BridgeDiscoveryResultAdapter;
import com.nilstrubkin.hueedge.api.JsonCustomRequest;
import com.nilstrubkin.hueedge.api.RequestQueueSingleton;
import com.philips.lighting.hue.sdk.wrapper.HueLog;
import com.philips.lighting.hue.sdk.wrapper.Persistence;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryImpl;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;
import com.philips.lighting.hue.sdk.wrapper.utilities.InitSdk;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.look.Slook;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SetupActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, Serializable {

    private transient static final int REQUEST_AMOUNT = 60;

    /*

    Log.d(TAG, "Instantiating HueBridge singleton");
                HueBridge.getInstance(
                        "192.168.69.166",
                        "aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD")
                        .requestHueState(getActivity());
                Log.d(TAG, "getInstance() returns (!= null):" + HueBridge.getInstance());

     */

    static {
        System.loadLibrary("huesdk");
    }

    private transient static final String TAG = SetupActivity.class.getSimpleName();
    private transient final Context ctx = this;

    private transient BridgeDiscovery bridgeDiscovery;

    private transient List<BridgeDiscoveryResult> bridgeDiscoveryResults;

    private transient int requestAmount;
    private transient Timer timer;
    private transient sendAuthRequestTask<Object> backgroundAuthRequestTask;

    // UI elements
    private transient TextView statusTextView;
    private transient ListView bridgeDiscoveryListView;
    private transient View pushlinkImage;
    private transient ProgressBar progressBar;
    private transient Button bridgeDiscoveryButton;
    private transient Button cheatButton;
    private transient Button manualIp;
    private transient Button manualIpConfirm;
    private transient Button manualIpHelp;
    private transient Button manualIpBack;
    private transient LinearLayout helpLayout;
    private transient Button helpCloseButton;
    private transient EditText ipField;
    private transient Button bridgeDiscoveryCancelButton;
    private transient Button quickButton;
    private transient Button customButton;
    private transient Button finishButton;
    private transient Button removeButton;
    private transient Button yesButton;
    private transient Button noButton;
    private transient LinearLayout aboutLayout;
    private transient Button aboutButton;
    private transient Button aboutCloseButton;
    private transient Button contactMe;
    private transient Button support;

    enum UIState {
        Welcome,
        ManualSetup,
        Search,
        Results,
        Connecting,
        Error,
        Auth,
        Settings,
        Final,
        Confirmation,
        Not_supported
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        setContentView(R.layout.setup_activity);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(getResources().getColor(R.color.navigation_bar_color_setup, getTheme()));

        // Setup the UI
        statusTextView = findViewById(R.id.status_text);
        bridgeDiscoveryListView = findViewById(R.id.bridge_discovery_result_list);
        bridgeDiscoveryListView.setOnItemClickListener(this);
        pushlinkImage = findViewById(R.id.pushlink_image);
        progressBar = findViewById(R.id.progress_bar);
        bridgeDiscoveryButton = findViewById(R.id.bridge_discovery_button);
        bridgeDiscoveryButton.setOnClickListener(this);
        cheatButton = findViewById(R.id.cheat_button);
        cheatButton.setOnClickListener(this);
        manualIp = findViewById(R.id.manual_ip);
        manualIp.setOnClickListener(this);
        manualIpConfirm = findViewById(R.id.manual_ip_confirm);
        manualIpConfirm.setOnClickListener(this);
        manualIpHelp = findViewById(R.id.manual_ip_help);
        manualIpHelp.setOnClickListener(this);
        manualIpBack = findViewById(R.id.manual_ip_back);
        manualIpBack.setOnClickListener(this);
        helpLayout = findViewById(R.id.help_layout);
        helpCloseButton = findViewById(R.id.close_help);
        helpCloseButton.setOnClickListener(this);
        ipField = findViewById(R.id.ip_field);
        bridgeDiscoveryCancelButton = findViewById(R.id.bridge_discovery_cancel_button);
        bridgeDiscoveryCancelButton.setOnClickListener(this);
        quickButton = findViewById(R.id.quick_setup_button);
        quickButton.setOnClickListener(this);
        customButton = findViewById(R.id.custom_setup_button);
        customButton.setOnClickListener(this);
        finishButton = findViewById(R.id.finish_button);
        finishButton.setOnClickListener(this);
        removeButton = findViewById(R.id.remove_button);
        removeButton.setOnClickListener(this);
        yesButton = findViewById(R.id.yes_button);
        yesButton.setOnClickListener(this);
        noButton = findViewById(R.id.no_button);
        noButton.setOnClickListener(this);
        aboutLayout = findViewById(R.id.about_layout);
        aboutButton = findViewById(R.id.about_button);
        aboutButton.setOnClickListener(this);
        aboutCloseButton = findViewById(R.id.about_close_button);
        aboutCloseButton.setOnClickListener(this);
        contactMe = findViewById(R.id.contact_me);
        contactMe.setOnClickListener(this);
        support = findViewById(R.id.support);
        support.setOnClickListener(this);
        InitSdk.setApplicationContext(getApplicationContext());
        Persistence.setStorageLocation(getFilesDir().getAbsolutePath(), Build.ID);
        HueLog.setConsoleLogLevel(HueLog.LogLevel.DEBUG); //TODO remove debug

        Slook slook = new Slook();

        try {
            slook.initialize(this);
        } catch (SsdkUnsupportedException e){
            updateUI(UIState.Not_supported);
            return;
        }

        // The device doesn't support Edge Single Mode, Edge Single Plus Mode, and Edge Feeds Mode.
        if (!slook.isFeatureEnabled(Slook.COCKTAIL_PANEL)) {
            updateUI(UIState.Not_supported);
            return;
        }

        if (HueBridge.getInstance(ctx) == null){
            updateUI(UIState.Welcome);
        }
        else{
            updateUI(UIState.Final);
        }
    }

    /**
     * Start the bridge discovery search
     * Read the documentation on meethue for an explanation of the bridge discovery options
     */
    public void startBridgeDiscovery() {
        Log.i(TAG, "startBridgeDiscovery()");
        stopBridgeDiscovery();
        bridgeDiscovery = new BridgeDiscoveryImpl();
        // ALL Include [UPNP, IPSCAN, NUPNP, MDNS] but in some nets UPNP, NUPNP and MDNS is not working properly
        bridgeDiscovery.search(bridgeDiscoveryCallback);
        updateUI(UIState.Search);
    }

    public void initializeDnsSDDiscovery() {
        final String SERVICE_TYPE = "_hue._tcp.";
        final NsdManager nsdManager = (NsdManager) ctx.getSystemService(NSD_SERVICE);
        NsdManager.ResolveListener resolveListener =
                initializeDnsSDResolveListener();
        NsdManager.DiscoveryListener discoveryListener =
                initializeDnsSDDiscoveryListener(nsdManager, resolveListener, SERVICE_TYPE);
        nsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public NsdManager.DiscoveryListener initializeDnsSDDiscoveryListener(
            final NsdManager nsdManager,
            NsdManager.ResolveListener resolveListener,
            final String SERVICE_TYPE) {
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
                            initializeDnsSDResolveListener();
                    nsdManager.resolveService(service, resolveListener);
                }
            }
            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost: " + service);
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

    private NsdManager.ResolveListener initializeDnsSDResolveListener() {
        return new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed: " + errorCode);
            }
            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                int port = serviceInfo.getPort();
                InetAddress host = serviceInfo.getHost();
            }
        };
    }

    private void initializeNUPNPDiscovery(){
        String portal = "https://discovery.meethue.com";
        JsonCustomRequest jcr = getJsonNUPNP(ctx, portal);
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(this).addToRequestQueue(this, jcr);
        Log.d(TAG, "initializeNUPnP");
    }

    private void initializeUPNPDiscovery(){
        Thread t = new Thread(){
            @Override
            public void run() {
                try {
                    byte[] sendData = new byte[1024];
                    final byte[] receiveData = new byte[1024];
                    final int timeout = 5000; // 5 seconds according to Hue Bridge best practice

                    /* our M-SEARCH data as a byte array */
                    String MSEARCH = "M-SEARCH * HTTP/1.1\r\n" +
                            "HOST: 239.255.255.250:1900\r\n"+
                            "MAN: \"ssdp:discover\"\r\n"+
                            "MX: 10\r\n"+
                            "ST: ssdp:all\r\n"+  // Use this for all UPnP Devices
                            "\r\n";
                    sendData = MSEARCH.getBytes();

                    /* create a packet from our data destined for 239.255.255.250:1900 */
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("239.255.255.250"), 1900);

                    /* send packet to the socket we're creating */
                    final DatagramSocket clientSocket = new DatagramSocket();
                    clientSocket.setSoTimeout(timeout);
                    clientSocket.send(sendPacket);

                    /* recieve response and store in our receivePacket */
                    final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                    Thread t2 = new Thread(){
                        ArrayList<InetAddress> ipList = new ArrayList<>();
                        @Override
                        public void run() {
                            long timeStart = System.currentTimeMillis();
                            while(System.currentTimeMillis() - timeStart < timeout) {
                                try {
                                    clientSocket.receive(receivePacket);
                                    InetAddress ip = receivePacket.getAddress();
                                    if(!ipList.contains(ip)){
                                        /* get the response as a string */
                                        String response = new String(receivePacket.getData());
                                        if (response.contains("IpBridge")) {
                                            ipList.add(receivePacket.getAddress());
                                            /* print the response */
                                            System.out.println(receivePacket.getAddress());
                                        }
                                    }
                                } catch (SocketTimeoutException e){
                                    Log.d(TAG, "SocketTimeoutException, closing socket.");
                                    /* close the socket */
                                    clientSocket.close();
                                    return;
                                }  catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            Log.d(TAG, "5 seconds passed, closing socket.");
                            clientSocket.close();
                        }
                    };
                    t2.start();

                }
                catch (Exception e){
                    e.printStackTrace();
                }
                Log.d(TAG, "Done");
            }
        };
        t.start();
    }

    private void initializeIpScan(){
        WifiManager wifiManager = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifiInfo = wifiManager.getConnectionInfo();
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        int gateway = wifiManager.getDhcpInfo().gateway;
        try {
            InetAddress inetAddress = InetAddress.getByAddress(extractBytes(dhcpInfo.ipAddress));
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                /* not an IPv6 */
                if (!address.toString().contains(":")) {
                    short netPrefix = address.getNetworkPrefixLength();
                    Log.d(TAG, address.toString());
                    checkHosts(gateway, netPrefix);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void checkHosts(int gateway, int prefix) {
        final int clients = (int) (Math.pow(2, 32 - prefix) - 2);
        Log.d(TAG, "Clients size: " + clients);
        int netmask = generateMaskFromPrefix(prefix);
        final int subnetAddress = getSubnetAddress(gateway, netmask);
        Log.d(TAG, "Netmask is: " + getStringIpAddress(netmask));
        Log.d(TAG, "Gateway is: " + getStringIpAddress(gateway));
        Log.d(TAG, "subnetAddress is: " + getStringIpAddress(subnetAddress));
        Thread t = new Thread(){
            @Override
            public void run() {
                try {
                    int timeout = 5;
                    for (int i = 1; i <= clients; i++) {
                        String host = getStringIpAddress(subnetAddress + swap(i));
                        if (InetAddress.getByName(host).isReachable(timeout))
                        {
                            Log.d(TAG, "checkHosts() :: "+host + " is reachable");
                            try {
                                DiscoveryEntry e = parseDescriptionXml(host);
                                Log.d(TAG, "Confirmed bridge: " + e.friendlyName);
                            } catch (IOException e) {
                                Log.e(TAG,"Could not get description.xml from: " + host);
                            }
                        }
                    }
                } catch (UnknownHostException e) {
                    Log.d(TAG, "checkHosts() :: UnknownHostException e : "+e);
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    Log.d(TAG, "checkHosts() :: IOException e : "+e);
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    public DiscoveryEntry parseDescriptionXml(String address) throws XmlPullParserException, IOException {
        URL url = new URL("http://" + address + "/description.xml");
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readXml(parser);
        } finally {
            in.close();
        }
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

    public int generateMaskFromPrefix(int prefix) {
        int netmask = 0;
        for (int i = 0; i < 32; i++){
            netmask <<= 1;
            if (i < prefix)
                netmask++;
        }
        return swap(netmask);
    }

    public int swap (int value) {
        int b1 = value & 0xff;
        int b2 = (value >>  8) & 0xff;
        int b3 = (value >> 16) & 0xff;
        int b4 = (value >> 24) & 0xff;

        return b1 << 24 | b2 << 16 | b3 << 8 | b4;
    }

    private byte[] extractBytes(int address){
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++){
            result[i] = (byte) (address >> (i * 8) & 0xff);
        }
        return result;
    }

    private int getSubnetAddress(int gateway, int netmask) {
        return gateway & netmask;
    }

    private String getStringIpAddress(int address){
        return String.format(
                Locale.ENGLISH,
                "%d.%d.%d.%d",
                (address & 0xff),
                (address >> 8 & 0xff),
                (address >> 16 & 0xff),
                (address >> 24 & 0xff));
    }

    /**
     * Stops the bridge discovery if it is still running
     */
    public void stopBridgeDiscovery() {
        Log.i(TAG, "stopBridgeDiscovery()");
        if (bridgeDiscovery != null) {
            bridgeDiscovery.stop();
            bridgeDiscovery = null;
        }
    }

    private final BridgeDiscovery.Callback bridgeDiscoveryCallback = new BridgeDiscovery.Callback() {
        @Override
        public void onFinished(@NonNull final List<BridgeDiscoveryResult> results, @NonNull final BridgeDiscovery.ReturnCode returnCode) {
            // Set to null to prevent stopBridgeDiscovery from stopping it
            bridgeDiscovery = null;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (returnCode == BridgeDiscovery.ReturnCode.SUCCESS) {
                        bridgeDiscoveryListView.setAdapter(new BridgeDiscoveryResultAdapter(getApplicationContext(), results));
                        bridgeDiscoveryResults = results;
                        Log.i(TAG, "Bridge discovery found " + results.size() + " bridge(s) in the network");
                        if(results.size() == 0)
                            updateUI(UIState.Error);
                        else
                            updateUI(UIState.Results);
                    } else if (returnCode == BridgeDiscovery.ReturnCode.STOPPED) {
                        Log.i(TAG, "Bridge discovery stopped");
                        updateUI(UIState.Welcome);
                    } else {
                        Log.e(TAG, "Bridge discovery error");
                        updateUI(UIState.Error);
                    }
                }
            });
        }
    };

    static private void sendAuthRequest(SetupActivity ins, JSONObject job, String bridgeIp){
        if(ins.requestAmount == 0){
            ins.timer.cancel();
            ins.progressBar.clearAnimation();
            ins.updateUI(UIState.Error);
            return;
        }
        else if(ins.requestAmount == -1){
            ins.timer.cancel();
            ins.progressBar.clearAnimation();
            ins.updateUI(UIState.Results);
            return;
        }
        ins.statusTextView.setText(ins.getResources().getString(R.string.fragment_auth_label, ins.requestAmount));
        //ins.progressBar.incrementProgressBy(ins.progressBar.getMax() / REQUEST_AMOUNT);
        Log.d(TAG, "requestAmount = " + ins.requestAmount);
        ins.requestAmount--;
        JsonCustomRequest jcr = getJsonCustomRequest(ins, job, bridgeIp);
        Log.d(TAG, "changeHueState postRequest created for this ip " + bridgeIp);
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(ins).addToRequestQueue(ins, jcr);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        final String bridgeIp = bridgeDiscoveryResults.get(i).getIp();
        Log.i(TAG, "Selected Bridge " + bridgeIp);
        connectToBridge(bridgeIp);
    }

    private void connectToBridge(final String bridgeIp){
        Log.d(TAG, "Sending request for this devicetype: " + "HueEdge#" + android.os.Build.MODEL);
        final JSONObject job = HueBridge.createJsonOnObject("devicetype", "HueEdge#" + android.os.Build.MODEL);
        assert job != null;
        updateUI(UIState.Auth);
        requestAmount = REQUEST_AMOUNT; //Requests to send
        final Handler handler = new Handler();
        timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            backgroundAuthRequestTask = new sendAuthRequestTask<>((SetupActivity) ctx);
                            // PerformBackgroundTask this class is the class that extends AsyncTask
                            backgroundAuthRequestTask.execute(job, bridgeIp);
                        } catch (Exception ex) {
                            Log.e(TAG, "Could not start background auth request async task");
                            ex.printStackTrace();
                        }
                    }
                });
            }
        };
        progressBar.setMax(10000);
        ProgressBarAnimation anim = new ProgressBarAnimation(progressBar, 0, progressBar.getMax());
        anim.setDuration(1000 * REQUEST_AMOUNT);
        progressBar.startAnimation(anim);
        timer.schedule(doAsynchronousTask, 0, 1000); //execute every second
    }

    private static class sendAuthRequestTask<T> extends AsyncTask<T, T, T> {
        private transient final WeakReference<SetupActivity> activityReference;
        sendAuthRequestTask(SetupActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected T doInBackground(T[] objects) {
            sendAuthRequest(activityReference.get(), (JSONObject) objects[0], (String) objects[1]);
            return null;
        }

        public void success() {
            activityReference.get().timer.cancel();
            activityReference.get().progressBar.clearAnimation();
        }
    }

    private static class ProgressBarAnimation extends Animation {
        private final ProgressBar progressBar;
        private final float from;
        private final float to;

        public ProgressBarAnimation(ProgressBar progressBar, float from, float to) {
            super();
            this.progressBar = progressBar;
            this.from = from;
            this.to = to;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            float value = from + (to - from) * interpolatedTime;
            progressBar.setProgress((int) value);
        }

    }

    @Override
    public void onClick(View view) {
        if (view == bridgeDiscoveryButton) {
            //startBridgeDiscovery();
            //initializeDnsSDDiscovery();
            //initializeNUPNPDiscovery();
            //initializeUPNPDiscovery();
            //initializeIpScan();
            Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
            Executor executor = new Executor() {
                @Override
                public void execute(Runnable r) {
                    new Thread(r).start();
                }
            };
            DiscoveryEngine de = new DiscoveryEngine(executor, mainThreadHandler);
            de.initializeSynchronousNUPNPDiscovery(ctx, new DiscoveryEngine.DiscoveryCallback<DiscoveryEntry>() {
                @Override
                public void onComplete(Result<DiscoveryEntry> result) {
                    if (result instanceof Result.Success) {
                        // Happy path
                        DiscoveryEntry de = ((Result.Success<DiscoveryEntry>) result).data;
                        Log.d(TAG, "!!!Result: " + de.friendlyName);
                    } else {
                        // Show error in UI
                        Exception e = ((Result.Error<DiscoveryEntry>) result).exception;
                        int eCode = ((Result.Error<DiscoveryEntry>) result).errorCode;
                        Log.d(TAG, "!!!Error: " + eCode + " " + e.toString());
                    }
                }
            });
        }
        /*else if (view == cheatButton) {
            Log.d(TAG, "Instantiating HueBridge singleton");
            HueBridge.getInstance(
                    ctx,
                    "192.168.69.166",
                    "aR8A1sBC-crUyPeCjtXJKKm0EEcxr6nXurdOq4gD")
                    .requestHueState(this);
            Log.d(TAG, "getInstance() returns cheat bridge:" + HueBridge.getInstance(ctx));
            updateUI(UIState.Settings);
        }*/
        else if (view == manualIp) {
            updateUI(UIState.ManualSetup);
        }
        else if (view == manualIpConfirm) {
            String ip = ipField.getText().toString();
            if (Patterns.IP_ADDRESS.matcher(ip).matches())
                connectToBridge(ip);
            else {
                String toastString = ctx.getString(R.string.toast_ip_mistake);
                Toast.makeText(ctx, toastString, Toast.LENGTH_LONG).show();
            }
        }
        else if (view == manualIpHelp) {
            helpLayout.setVisibility(View.VISIBLE);
            aboutLayout.setVisibility(View.GONE);
        }
        else if (view == manualIpBack) {
            updateUI(UIState.Welcome);
        }
        else if (view == helpCloseButton) {
            helpLayout.setVisibility(View.GONE);
        }
        else if (view == bridgeDiscoveryCancelButton) {
            stopBridgeDiscovery();
            requestAmount = -1;
        }
        else if (view == quickButton) {
            HueEdgeProvider.quickSetup(this);
            updateUI(UIState.Final);
        }
        else if (view == customButton) {
            HueEdgeProvider.saveAllConfiguration(ctx);
            updateUI(UIState.Final);
        }
        else if (view == finishButton) {
            this.finish();
        }
        else if (view == removeButton) {
            updateUI(UIState.Confirmation);
        }
        else if (view == yesButton) {
            HueBridge.deleteInstance(this);
            updateUI(UIState.Welcome);
        }
        else if (view == noButton) {
            updateUI(UIState.Final);
        }
        else if (view == aboutButton){
            aboutLayout.setVisibility(View.VISIBLE);
            helpLayout.setVisibility(View.GONE);
        }
        else if (view == aboutCloseButton){
            aboutLayout.setVisibility(View.GONE);
        }
        else if (view == contactMe){
            Intent email = new Intent(Intent.ACTION_SEND);
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{ctx.getString(R.string.email)});
            //email.putExtra(Intent.EXTRA_SUBJECT, "");
            //email.putExtra(Intent.EXTRA_TEXT, "");

            //need this to prompts email client only
            email.setType("message/rfc822");
            startActivity(Intent.createChooser(email, "Choose an Email client :"));
        }
        else if (view == support){
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(ctx.getString(R.string.paypal_url)));
            startActivity(i);
        }
    }

    private void updateUI(final UIState state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Status: " + state.toString());

                bridgeDiscoveryListView.setVisibility(View.GONE);
                statusTextView.setVisibility(View.VISIBLE);
                pushlinkImage.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                bridgeDiscoveryButton.setVisibility(View.GONE);
                cheatButton.setVisibility(View.GONE);
                manualIp.setVisibility(View.GONE);
                manualIpConfirm.setVisibility(View.GONE);
                manualIpHelp.setVisibility(View.GONE);
                manualIpBack.setVisibility(View.GONE);
                ipField.setVisibility(View.GONE);
                bridgeDiscoveryCancelButton.setVisibility(View.GONE);
                quickButton.setVisibility(View.GONE);
                customButton.setVisibility(View.GONE);
                finishButton.setVisibility(View.GONE);
                removeButton.setVisibility(View.GONE);
                yesButton.setVisibility(View.GONE);
                noButton.setVisibility(View.GONE);
                aboutLayout.setVisibility(View.GONE);
                aboutButton.setText(ctx.getString(R.string.about_button_text));
                helpLayout.setVisibility(View.GONE);

                switch (state) {
                    case Welcome:
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setText(getResources().getText(R.string.fragment_welcome_button));
                        statusTextView.setText(getResources().getString(R.string.fragment_welcome_label));
                        //cheatButton.setVisibility(View.VISIBLE);
                        manualIp.setVisibility(View.VISIBLE);
                        break;
                    case ManualSetup:
                        statusTextView.setText(getResources().getString(R.string.fragment_manual_label));
                        manualIpConfirm.setVisibility(View.VISIBLE);
                        manualIpHelp.setVisibility(View.VISIBLE);
                        manualIpBack.setVisibility(View.VISIBLE);
                        ipField.setVisibility(View.VISIBLE);
                        break;
                    case Search:
                        bridgeDiscoveryCancelButton.setVisibility(View.VISIBLE);
                        statusTextView.setText(getResources().getString(R.string.fragment_search_label));
                        break;
                    case Results:
                        statusTextView.setVisibility(View.GONE);
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setText(getResources().getText(R.string.fragment_result_button));
                        bridgeDiscoveryListView.setVisibility(View.VISIBLE);
                        break;
                    case Connecting:
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        statusTextView.setText(getResources().getString(R.string.fragment_connect_label));
                        bridgeDiscoveryButton.setText(getResources().getText(R.string.fragment_result_button));
                        break;
                    case Error:
                        bridgeDiscoveryButton.setVisibility(View.VISIBLE);
                        bridgeDiscoveryButton.setText(getResources().getText(R.string.fragment_error_button));
                        statusTextView.setText(getResources().getString(R.string.fragment_error_label));
                        //cheatButton.setVisibility(View.VISIBLE);
                        break;
                    case Auth:
                        statusTextView.setText(getResources().getString(R.string.fragment_auth_label));
                        pushlinkImage.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(0);
                        bridgeDiscoveryCancelButton.setVisibility(View.VISIBLE);
                        break;
                    case Settings:
                        statusTextView.setText(getResources().getString(R.string.fragment_settings_label));
                        quickButton.setVisibility(View.VISIBLE);
                        customButton.setVisibility(View.VISIBLE);
                        break;
                    case Final:
                        statusTextView.setText(getResources().getString(R.string.fragment_final_label));
                        finishButton.setVisibility(View.VISIBLE);
                        removeButton.setVisibility(View.VISIBLE);
                        break;
                    case Confirmation:
                        statusTextView.setText(getResources().getString(R.string.fragment_confirmation_label));
                        yesButton.setVisibility(View.VISIBLE);
                        noButton.setVisibility(View.VISIBLE);
                        break;
                    case Not_supported:
                        statusTextView.setText(getResources().getString(R.string.fragment_not_supported_label));
                        break;
                    default:
                        Log.e(TAG, "Unknown UI fragment");
                }
            }
        });
    }

    static private JsonCustomRequest getJsonCustomRequest(Context ctx, final JSONObject jsonObject, final String ip){
        final SetupActivity ins = (SetupActivity) ctx;
        if(!jsonObject.keys().hasNext()){ // make sure we get an object that is not empty
            Log.wtf(TAG, "!jsonObject.keys().hasNext() Is this an empty request?");
            return null;
        }
        Log.d(TAG, "setHueState url " + ip); // this is the actual resource path
        return new JsonCustomRequest(Request.Method.POST, "http://" + ip + "/api", jsonObject,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "Auth request responds " + response.toString());
                        Iterator<String> responseKeys; // iterator for response JSONObject
                        String responseKey; // index for response JSONObject
                        try {
                            JSONObject jsonResponse = response.getJSONObject(0);
                            responseKeys = jsonResponse.keys();
                            if(responseKeys.hasNext()) {
                                responseKey = responseKeys.next();
                                if(!responseKey.equals("success")){  //response key should be success
                                    Log.i(TAG, "Unsuccessful! Check reply");
                                    return;
                                }
                                JSONObject usernameContainer = (JSONObject) jsonResponse.get("success");    // this should be JSONObject with username field
                                if(!usernameContainer.keys().next().equals("username")) {
                                    Log.e(TAG, "Unsuccessful! Check reply");            //  really weird if it fails here. API for HUE might have been changed recently
                                    return;
                                }
                                ins.backgroundAuthRequestTask.success();
                                String username = usernameContainer.getString("username");
                                Log.d(TAG, "Auth successful: " + username.substring(0,5) + "*****");
                                HueBridge.getInstance(ins, ip, username)
                                        .requestHueState(ins);

                                ins.updateUI(UIState.Settings);
                            }
                        } catch (JSONException ex) {
                            ex.printStackTrace();
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

    static private JsonCustomRequest getJsonNUPNP(Context ctx, final String portal){
        final SetupActivity ins = (SetupActivity) ctx;
        return new JsonCustomRequest(Request.Method.GET, portal, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "Request responds " + response.toString());
                        Iterator<String> responseKeys; // iterator for response JSONObject
                        String responseKey; // index for response JSONObject
                        try {
                            JSONObject jsonResponse = response.getJSONObject(0);
                            responseKeys = jsonResponse.keys();
                        } catch (JSONException ex) {
                            ex.printStackTrace();
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

}
