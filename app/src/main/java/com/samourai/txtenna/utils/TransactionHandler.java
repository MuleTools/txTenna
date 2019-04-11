package com.samourai.txtenna.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.samourai.txtenna.adapters.BroadcastLogsAdapter;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.UnknownHostException;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.config.RequestConfig;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;

public class TransactionHandler extends HandlerThread {

    private Handler handler = null;
    private BroadcastLogsAdapter adapter = null;

    private static int CHECK_INTERVAL = 30000;

    public TransactionHandler(String name, BroadcastLogsAdapter adapter) {
        super(name);
        this.adapter = adapter;
        Log.d("TransactionHandler", "create object: " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
    }

    public synchronized void refresh() {
        try {
            android.os.Message message = new android.os.Message();
            message.arg1 = 0;
            this.handler.sendMessage(message);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void confirmFromGateway(String segment) {
        try {
            android.os.Message message = new android.os.Message();
            message.arg1 = 1;
            message.obj = segment;
            this.handler.sendMessage(message);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected synchronized void onLooperPrepared() {

        this.handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(android.os.Message msg) {
                // process incoming messages here
                switch (msg.arg1) {
                    case 0:
                        // refresh broadcast log view
                        adapter.notifyDataSetChanged();
                        break;
                    case 1:
                        // update broadcast log view for incoming return receipt
                        try {
                            JSONObject obj = new JSONObject((String) msg.obj);
                            if(obj.has("h")) {
                                String hash = obj.getString("h");
                                int blockHeight = 0;
                                if (obj.has("b")) {
                                    blockHeight = obj.getInt("b");
                                }

                                int pos = BroadcastLogUtil.getInstance().findTransaction(hash);
                                if (pos > -1) {
                                    BroadcastLogUtil.BroadcastLogEntry entry = BroadcastLogUtil.getInstance().getBroadcastLog().get(pos);
                                    entry.broadcast = true;
                                    if (blockHeight >= 1) {
                                        entry.confirmed = true;
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                                Log.d("TransactionHandler", "Return receipt received for hash: " + hash + " block height: " + blockHeight);
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        };

        notify();
    }

    @Override
    protected void finalize () throws Throwable {
        super.finalize();
        Log.d("TransactionHandler", "finalize object: @" + Integer.toHexString(this.hashCode()) + " [lifecycle]");
    }

    Runnable transactionChecker = new Runnable() {
        @Override
        public void run() {
            try {
                confirmFromServer();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                handler.postDelayed(transactionChecker, CHECK_INTERVAL);
            }
        }
    };

    public void startTransactionChecker() {
        try {
            transactionChecker.run();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopTransactionChecker() {
        try {
            this.handler.removeCallbacks(transactionChecker);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void confirmFromServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                boolean isChanged = false;
                HttpResponse response = null;

                try {
                    RequestConfig.Builder requestBuilder = RequestConfig.custom();
                    requestBuilder.setConnectTimeout(60000);
                    requestBuilder.setConnectionRequestTimeout(60000);

                    HttpClientBuilder builder = HttpClientBuilder.create();
                    builder.setDefaultRequestConfig(requestBuilder.build());
                    HttpClient httpClient = builder.build();

                    for (BroadcastLogUtil.BroadcastLogEntry entry : BroadcastLogUtil.getInstance().getBroadcastLog()) {
                        // query server with transaction hash if transaction is not marked as confirmed, or timestamp not set
                        if (!entry.confirmed || entry.ts < 0L) {
                            // check PonyDirect server to update confirmed transactions

                            // use this confirmed hash for testing: "e9a66845e05d5abc0ad04ec80f774a7e585c6e8db975962d069a522137b80c1d"
                            final String hash = entry.hash;
                            String getUrl = null;
                            if (entry.net.equalsIgnoreCase("t")) {
                                getUrl = "https://api.samourai.io/test/v2/tx/" + hash;
                            } else {
                                getUrl = "https://api.samourai.io/v2/tx/" + hash;
                            }

                            String result = null;

                            HttpGet get = new HttpGet(getUrl);
                            response = httpClient.execute(get);
                            Log.d("TransactionHandler", "HTTP GET return:" + response.getStatusLine().getStatusCode());
                            if (response.getStatusLine().getStatusCode() == 200) {
                                result = IOUtils.toString(response.getEntity().getContent(), "UTF-8");;
                                JSONObject obj = new JSONObject(result);
                                if (obj != null && obj.has("block")) {
                                    JSONObject bObj = obj.getJSONObject("block");
                                    if (bObj.has("height") && bObj.has("time")) {
                                        entry.confirmed = true;
                                        entry.ts = bObj.getLong("time");
                                        isChanged = true;

                                        // send return receipt
                                        final long blockHeight = bObj.getLong("height");
                                        JSONObject rObj = new JSONObject();
                                        rObj.put("b", (long) blockHeight);
                                        rObj.put("h", entry.hash);
                                        String segment = rObj.toString();

                                        SendMessageInteractor smi = new SendMessageInteractor();
                                        final long senderGID = goTennaUtil.getGID();
                                        final long receiverGID = entry.gid;
                                        final Message messageToSend = Message.createReadyToSendMessage(senderGID,
                                                receiverGID,
                                                segment);

                                        smi.sendMessage(messageToSend, true,
                                                new SendMessageInteractor.SendMessageListener() {
                                                    @Override
                                                    public void onMessageResponseReceived() {
                                                        if (messageToSend.getMessageStatus() == Message.MessageStatus.SENT_SUCCESSFULLY) {
                                                            Log.d("TransactionHandler", "Confirmation receipt succeeded! sent by: " + senderGID + " received by: " + receiverGID + " for hash: " + hash);
                                                        } else {
                                                            Log.d("TransactionHandler", "Confirmation receipt failed! sent by: " + senderGID + " received by: " + receiverGID + " for hash: " + hash);
                                                        }
                                                    }
                                                });

                                        Log.d("MainActivity", "Confirmation receipt sent by: " + senderGID + " to: " + receiverGID + " for tx id: " + hash + " height: " + blockHeight);
                                    }
                                }
                            }

                            // sleep before checking next transaction
                            Thread.sleep(250);
                        }
                    }
                    if (isChanged) {
                        refresh();
                    }
                }
                catch(UnknownHostException e) {
                    Log.d("TransactionHandler", "No Internet Connection. " + e);
                }
                catch(IOException e) {
                    Log.d("TransactionHandler", e.getMessage());
                    e.printStackTrace();
                }
                catch(Exception e) {
                    Log.d("TransactionHandler", e.getMessage());
                    e.printStackTrace();
                }

            } // run
        }).start();
    }
}