package com.nilstrubkin.hueedge.discovery;

import android.content.Context;
import android.content.res.Resources;
import android.net.DhcpInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import android.util.Xml;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static android.content.Context.NSD_SERVICE;

public class DiscoveryEngine {

    private transient static final String TAG = DiscoveryEngine.class.getSimpleName();

    public transient static final int REQUEST_AMOUNT = 60;

    private transient final Executor executor;
    private transient final Handler resultHandler;

    int requestAmount; //Requests to send

    public DiscoveryEngine(Executor executor, Handler resultHandler){
        this.executor = executor;
        this.resultHandler = resultHandler;
    }

    public interface DiscoveryCallback<T> {
        void onComplete(Result<T> result);
    }

    private void notifyResult(
            final Result<DiscoveryEntry> result,
            final DiscoveryCallback<DiscoveryEntry> callback
    ) {
        resultHandler.post(() -> {
            if (Thread.currentThread().isInterrupted())
                return;
            callback.onComplete(result);
        });
    }

    private void notifyAuthResult(
            final Result<AuthEntry> result,
            final DiscoveryCallback<AuthEntry> callback
    ) {
        resultHandler.post(() -> {
            if (Thread.currentThread().isInterrupted())
                return;
            callback.onComplete(result);
        });
    }

    public void connectToBridge(
            final ExecutorService executorService,
            final DiscoveryCallback<AuthEntry> callback,
            final String bridgeIp){
        requestAmount = REQUEST_AMOUNT; //Requests to send
        final Timer timer = new Timer();
        TimerTask periodicalAuthTask = new TimerTask() {
            @Override
            public void run() {
                if (Thread.currentThread().isInterrupted() || executorService.isShutdown()) {
                    timer.cancel();
                }
                else
                    try {
                        executorService.submit(() -> {
                                    if (Thread.currentThread().isInterrupted() || executorService.isShutdown())
                                        return;
                                    if (requestAmount == 0) {
                                        Result<AuthEntry> errorResult = new Result.Error<>(new TimeoutException("requestAmount has reached zero"));
                                        notifyAuthResult(errorResult, callback);
                                        timer.cancel();
                                    } else
                                        sendAuthRequest(bridgeIp, callback);
                                }
                        );
                    } catch (Exception e){
                        e.printStackTrace();
                    }
            }
        };
        timer.schedule(periodicalAuthTask, 0, 1000); //execute every second
    }

    private void sendAuthRequest(
            final String ip,
            final DiscoveryCallback<AuthEntry> callback){
        final MediaType JSON = MediaType.get("application/json; charset=utf-8");
        final OkHttpClient client = new OkHttpClient.Builder().build();
        JSONObject json;
        try {
            json = new JSONObject().put("devicetype", "HueEdge#" + android.os.Build.MODEL);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        Log.i(TAG, "Sending request to ip: " + ip);
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url("http://" + ip + "/api")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute(); ResponseBody rb = Objects.requireNonNull(response.body())) {
            Type type = Types.newParameterizedType(List.class, AuthResponse.class);
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<List<AuthResponse>> adapter = moshi.adapter(type);
            String resp = rb.string();
            Log.d(TAG, "response: " + resp);
            List<AuthResponse> responses = adapter.fromJson(resp);
            if(!Objects.requireNonNull(responses).isEmpty())
                for (AuthResponse r : responses) {
                    if (r.success != null)
                        if (r.success.username != null) {
                            AuthEntry authEntry = new AuthEntry(ip, r.success.username);
                            Result<AuthEntry> result = new Result.Success<>(authEntry);
                            notifyAuthResult(result, callback);
                        }
                }
        } catch (IOException | NullPointerException e){
            e.printStackTrace();
        }
        requestAmount--;
    }

    public void initializeFullDiscovery(
            final Context ctx,
            final ExecutorService executorService,
            final DiscoveryCallback<DiscoveryEntry> callback){
        initializeSynchronousDnsSDDiscovery(ctx, executorService, callback);
        initializeSynchronousNUPNPDiscovery(executorService, callback);
        initializeSynchronousUPNPDiscovery(executorService, callback);
        initializeIpScan(ctx, executorService, callback);
        //initializeTest(executorService);
    }

    private void initializeSynchronousDnsSDDiscovery(
            final Context ctx,
            final ExecutorService executorService,
            final DiscoveryCallback<DiscoveryEntry> callback){
        executorService.submit(() -> {
            Log.d(TAG, "Initializing mDNS discovery...");
            final String SERVICE_TYPE = "_hue._tcp.";
            final NsdManager nsdManager = (NsdManager) ctx.getSystemService(NSD_SERVICE);
            NsdManager.DiscoveryListener discoveryListener =
                    initializeDnsSDDiscoveryListener(nsdManager, callback);
            nsdManager.discoverServices(
                    SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        });
    }

    /*private void initializeTest(final ExecutorService executorService){
        executorService.submit(() -> {
            while (true){
                try {
                    Thread.sleep(1000);
                    Log.d(TAG,"Executing");
                    if (Thread.currentThread().isInterrupted()){
                        Log.e(TAG,"Testing! Thread.currentThread().isInterrupted()");
                        break;
                    }
                    if (executorService.isShutdown()){
                        Log.e(TAG,"Testing! executorService.isShutdown()");
                        break;
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG,"Testing! InterruptedException");
                    return;
                }

            }
        });
    }*/

    private NsdManager.DiscoveryListener initializeDnsSDDiscoveryListener(
            final NsdManager nsdManager,
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
                if (Thread.currentThread().isInterrupted()) {
                    nsdManager.stopServiceDiscovery(this);
                    return;
                }
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals("_hue._tcp.")) {
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
                //nsdManager.stopServiceDiscovery(this);
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
                Resources.NotFoundException e = new Resources.NotFoundException("Error code: " + errorCode);
                Result<DiscoveryEntry> errorResult = new Result.Error<>(e);
                notifyResult(errorResult, callback);
            }
            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                if (Thread.currentThread().isInterrupted())
                    return;
                Log.d(TAG, "mDNS Resolve Succeeded. " + serviceInfo);
                InetAddress host = serviceInfo.getHost();
                try {
                    parseDescriptionXml(host, callback);
                } catch (IOException e) {
                    e.printStackTrace();
                    Result<DiscoveryEntry> errorResult = new Result.Error<>(e);
                    notifyResult(errorResult, callback);
                }
            }
        };
    }

    private void initializeSynchronousNUPNPDiscovery(
            final ExecutorService executorService,
            final DiscoveryCallback<DiscoveryEntry> callback){
        Log.d(TAG, "Initializing NUPnP discovery...");
        final String portal = "https://discovery.meethue.com";
        Callable<String> callable = () -> {
            Request request = new Request.Builder()
                    .url(portal)
                    .build();
            final OkHttpClient client = new OkHttpClient.Builder().build();
            try (Response response = client.newCall(request).execute(); ResponseBody rb = Objects.requireNonNull(response.body())) {
                String resp = rb.string();
                rb.close();
                return resp;
            } catch (UnknownHostException e) {
                Log.e(TAG, portal + " not reachable");
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
            return null;
        };
        Future<String> future = executorService.submit(callable);
        executorService.submit(() -> {
            try {
                String response = future.get();
                Type type = Types.newParameterizedType(List.class, NupnpResponse.class);
                Moshi moshi = new Moshi.Builder().build();
                JsonAdapter<List<NupnpResponse>> adapter = moshi.adapter(type);
                List<NupnpResponse> bridges = Objects.requireNonNull(adapter.fromJson(response));
                for(NupnpResponse b : bridges){
                    parseDescriptionXml(b.internalipaddress, callback);
                }
            } catch (InterruptedException ignored){
            } catch (ExecutionException | IOException | NullPointerException e) {
                e.printStackTrace();
                Result<DiscoveryEntry> errorResult = new Result.Error<>(e);
                notifyResult(errorResult, callback);
            }
        });
    }

    private void initializeSynchronousUPNPDiscovery(
            final ExecutorService executorService,
            final DiscoveryCallback<DiscoveryEntry> callback) {
        executorService.submit(() -> {
            Log.d(TAG, "Initializing UPnP discovery...");
            try {
                byte[] sendData;
                final byte[] receiveData = new byte[1024];
                final int timeout = 5000; // 5 seconds according to Hue Bridge best practice

                /* our M-SEARCH data as a byte array */
                String MSEARCH = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 10\r\n" +
                        "ST: ssdp:all\r\n" +  // Use this for all UPnP Devices
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

                ArrayList<InetAddress> ipList = new ArrayList<>();
                long timeStart = System.currentTimeMillis();
                while (System.currentTimeMillis() - timeStart < timeout) {
                    try {
                        if (Thread.currentThread().isInterrupted() || executorService.isShutdown()) {
                            clientSocket.close();
                            return;
                        }
                        clientSocket.receive(receivePacket);
                        InetAddress ip = receivePacket.getAddress();
                        if (!ipList.contains(ip)) {
                            /* get the response as a string */
                            String response = new String(receivePacket.getData());
                            if (response.contains("IpBridge")) {
                                ipList.add(receivePacket.getAddress());
                                Log.d(TAG, "UPnP discovered bridge: " + ip.getHostAddress());
                                parseDescriptionXml(ip, callback);
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        Log.d(TAG, "SocketTimeoutException, closing socket.");
                        /* close the socket */
                        clientSocket.close();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        Result<DiscoveryEntry> errorResult = new Result.Error<>(e);
                        notifyResult(errorResult, callback);
                    }
                }
                Log.d(TAG, "5 seconds passed, closing socket.");
                clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
                Result<DiscoveryEntry> errorResult = new Result.Error<>(e);
                notifyResult(errorResult, callback);
            }
            Log.d(TAG, "Done");
        });
    }

    private void initializeIpScan(
            final Context ctx,
            final ExecutorService executorService,
            final DiscoveryCallback<DiscoveryEntry> callback){
        executorService.submit(() -> {
            Log.d(TAG, "Initializing IP scan...");
            WifiManager wifiManager = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            int gateway = wifiManager.getDhcpInfo().gateway;
            try {
                InetAddress inetAddress = InetAddress.getByAddress(extractBytes(dhcpInfo.ipAddress));
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    /* not an IPv6 */
                    if (!address.toString().contains(":")) {
                        short netPrefix = address.getNetworkPrefixLength();
                        //Log.d(TAG, address.toString());
                        checkHosts(gateway, netPrefix, callback);
                    }
                }
            } catch (IOException e) {
                Result<DiscoveryEntry> errorResult = new Result.Error<>(e);
                notifyResult(errorResult, callback);
            }
        });
    }

    private void checkHosts(int gateway, int prefix, final DiscoveryCallback<DiscoveryEntry> callback) {
        final int clients = (int) (Math.pow(2, 32 - prefix) - 2);
        Log.d(TAG, "Clients size: " + clients);
        int netmask = generateMaskFromPrefix(prefix);
        final int subnetAddress = getSubnetAddress(gateway, netmask);
        Log.d(TAG, "Netmask is: " + getStringIpAddress(netmask));
        Log.d(TAG, "Gateway is: " + getStringIpAddress(gateway));
        Log.d(TAG, "subnetAddress is: " + getStringIpAddress(subnetAddress));
        try {
            int timeout = 5;
            for (int i = 1; i <= clients; i++) {
                if (Thread.currentThread().isInterrupted())
                    return;
                String host = getStringIpAddress(subnetAddress + swap(i));
                if (InetAddress.getByName(host).isReachable(timeout)) {
                    Log.d(TAG, "checkHosts() :: " + host + " is reachable");
                    try {
                        Log.d(TAG, "IP Scan discovered host: " + host);
                        parseDescriptionXml(host, callback);
                    } catch (IOException e) {
                        Log.d(TAG,"Could not get description.xml from: " + host);
                        Result<DiscoveryEntry> errorResult = new Result.Error<>(e);
                        notifyResult(errorResult, callback);
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
        }
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

    private void parseDescriptionXml(InetAddress address, final DiscoveryCallback<DiscoveryEntry> callback) throws IOException {
        parseDescriptionXml(address.getHostAddress(), callback);
    }

    private void parseDescriptionXml(final String address, final DiscoveryCallback<DiscoveryEntry> callback) throws IOException {
        final URL url = new URL("http://" + address + "/description.xml");
        executor.execute(() -> {
            try (InputStream in = url.openConnection().getInputStream()) {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(in, null);
                parser.nextTag();
                DiscoveryEntry de = readXml(parser);
                if (Thread.currentThread().isInterrupted()){
                    in.close();
                    return;
                }
                try {
                    Objects.requireNonNull(de).ip = address;
                }
                catch (NullPointerException e){
                    Log.e(TAG, "Failed to notify result. DiscoveryEntry is null.");
                    e.printStackTrace();
                }
                Result<DiscoveryEntry> result = new Result.Success<>(de);
                in.close();
                notifyResult(result, callback);
            } catch (Exception e){
                Result<DiscoveryEntry> errorResult = new Result.Error<>(e);
                notifyResult(errorResult, callback);
            }
        });
    }

    private DiscoveryEntry readXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "root");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (Thread.currentThread().isInterrupted())
                return null;
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
        String modelDescription = null;
        String friendlyName = null;
        String serialNumber = null;
        //String logoUrl = null; //TODO maybe add a logo
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "friendlyName":
                    friendlyName = readField(parser, "friendlyName");
                    break;
                case "modelDescription":
                    modelDescription = readField(parser, "modelDescription");
                    break;
                case "serialNumber":
                    serialNumber = readField(parser, "serialNumber");
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        boolean confirmedHue = false;
        try {
            confirmedHue = Objects.requireNonNull(modelDescription).contains("Philips hue");
        } catch (NullPointerException e){
            Log.e(TAG, "Not a Philips Hue description.xml");
            e.printStackTrace();
        }
        if(confirmedHue)
            return new DiscoveryEntry(friendlyName, serialNumber/*, logoUrl*/);
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
