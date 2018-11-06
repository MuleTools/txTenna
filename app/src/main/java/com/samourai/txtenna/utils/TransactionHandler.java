package com.samourai.txtenna.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.samourai.txtenna.adapters.BroadcastLogsAdapter;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class TransactionHandler extends HandlerThread {

    private static Handler handler = null;
    private static BroadcastLogsAdapter adapter = null;
    private static TransactionHandler instance = null;
    private static int CHECK_INTERVAL = 30000;

    public static TransactionHandler getInstance(BroadcastLogsAdapter adapter)   {

        if(instance == null)    {
            instance = new TransactionHandler("TransactionHandler", adapter);
        }

        return instance;
    }

    private TransactionHandler(String name, BroadcastLogsAdapter adapter) {
        super(name);
        this.adapter = adapter;
    }

    public synchronized void refresh() {
        try {
            android.os.Message message = new android.os.Message();
            message.arg1 = 0;
            waitUntilReady();
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
            waitUntilReady();
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
                                    if (blockHeight > 1) {
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

    public synchronized void waitUntilReady() throws InterruptedException {
        while (this.handler == null) {
            wait();
        }
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
            waitUntilReady();
            transactionChecker.run();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopTransactionChecker() {
        try {
            waitUntilReady();
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
                try {
                    for (BroadcastLogUtil.BroadcastLogEntry entry : BroadcastLogUtil.getInstance().getBroadcastLog()) {
                        // query server with transaction hash if transaction is not marked as confirmed, or timestamp not set
                        if (!entry.confirmed || entry.ts < 0L ) {
                            // check PonyDirect server to update confirmed transactions

                            final String hash = entry.hash;
                            String URL = null;
                            if (entry.net.equalsIgnoreCase("t")) {
                                URL = "https://api.samourai.io/test/v2/tx/" + hash;
                            } else {
                                URL = "https://api.samourai.io/v2/tx/" + hash;
                            }

                            java.net.URL url = new URL(URL);

                            String result = null;
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                            try {
                                connection.setRequestMethod("GET");
                                connection.setRequestProperty("charset", "utf-8");
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

                                connection.setConnectTimeout(60000);
                                connection.setReadTimeout(60000);

                                connection.setInstanceFollowRedirects(false);

                                connection.connect();

                                if (connection.getResponseCode() == 200) {
                                    result = IOUtils.toString(connection.getInputStream(), "UTF-8");
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
                                                            }
                                                            else {
                                                                Log.d("TransactionHandler", "Confirmation receipt failed! sent by: " + senderGID + " received by: " + receiverGID + " for hash: " + hash);
                                                            }
                                                        }
                                                    });

                                            Log.d("MainActivity", "Confirmation receipt sent by: " + senderGID + " to: " + receiverGID + " for tx id: " + hash  + " height: " + blockHeight);
                                        }
                                    }
                                }

                                if (isChanged) {
                                    refresh();
                                }

                                Thread.sleep(250);

                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                connection.disconnect();
                            }
                        }
                    }
                } catch (Exception e) {
                    ;
                }
            }
        }).start();
    }
}