package com.samourai.txtenna.payload;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.samourai.txtenna.utils.BroadcastLogUtil;
import com.samourai.txtenna.utils.Z85;
import com.samourai.txtenna.prefs.PrefsUtil;

import org.apache.commons.io.IOUtils;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import org.bouncycastle.util.encoders.Hex;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PayloadFactory {

    public class Seg0   {
        public int s = -1;
        public int c = 0;
        public int i = -1;
        public String n = "m";
        public String h = null;
        public String t = null;
    };

    public class SegN   {
        public int c = -1;
        public int i = -1;
        public String t = null;
    };

    private static final int smsSegment0Len = 40;
    private static final int smsSegment1Len = 120;

    private static final int goTennaSegment0Len = 115;
    private static final int goTennaSegment1Len = 190;

    private static int messageIdx = 0;

    private static PayloadFactory instance = null;

    private static Context context = null;

    private PayloadFactory() { ; }

    public static PayloadFactory getInstance(Context ctx)   {

        context = ctx;

        if(instance == null)    {
            instance = new PayloadFactory();
            messageIdx = PrefsUtil.getInstance(context).getValue(PrefsUtil.MESSAGE_IDX, 0);
        }

        return instance;
    }

    public List<String> toJSON(String strHexTx, boolean isGoTenna) {

        final int segment0Len = isGoTenna ? goTennaSegment0Len : smsSegment0Len;
        final int segment1Len = isGoTenna ? goTennaSegment1Len : smsSegment1Len;

        List<String> ret = new ArrayList<String>();

        Log.d("PayloadFactory", "hex tx:" + strHexTx);

        final Transaction tx = new Transaction(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_MAINNET, true) == true ? MainNetParams.get() : TestNet3Params.get(), Hex.decode(strHexTx));
        String strRaw = null;
        if(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_Z85, false) == true)    {
            strRaw = Z85.getInstance().encode(Hex.decode(strHexTx));
            Log.d("PayloadFactory", "hex tx Z85:" + strRaw);
        }
        else    {
            strRaw = strHexTx;
        }

        int count = 0;
        if(strRaw.length() <= segment0Len)    {
            count = 1;
        }
        else    {
            int len = strRaw.length();
            len -= segment0Len;
            count = 1;
            count += (len / segment1Len);
            if(len % segment1Len > 0)    {
                count++;
            }
        }

        int id = messageIdx;

        messageIdx++;
        if(messageIdx > 9999)    {
            messageIdx = 0;
        }
        PrefsUtil.getInstance(context).setValue(PrefsUtil.MESSAGE_IDX, messageIdx);

        for(int i = 0; i < count; i++)   {

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();

            if(i == 0)    {
                Seg0 seg0 = new Seg0();
                seg0.s = count;
//                seg0.c = i;
                seg0.i = id;
                if(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_MAINNET, true) == false)    {
                    seg0.n = "t";
                }
                seg0.h = tx.getHashAsString();
                seg0.t = strRaw.substring(0, strRaw.length() > segment0Len ? segment0Len : strRaw.length());
                if(strRaw.length() > segment0Len)    {
                    strRaw = strRaw.substring(segment0Len);
                }

                ret.add(gson.toJson(seg0));
            }
            else    {
                SegN segn = new SegN();
                segn.c = i;
                segn.i = id;
                segn.t = strRaw.substring(0,  strRaw.length() > segment1Len ? segment1Len : strRaw.length());
                if(strRaw.length() > segment1Len)    {
                    strRaw = strRaw.substring(segment1Len);
                }

                ret.add(gson.toJson(segn));
            }

        }

/*
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent("com.samourai.ponydirect.LOG");
                intent.putExtra("msg", context.getText(R.string.sending) + ":" + tx.getHashAsString() + " " + context.getText(R.string.to) + ":" + PrefsUtil.getInstance(context).getValue(PrefsUtil.SMS_RELAY, context.getText(R.string.default_relay).toString()));
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        });

        sendPayload(ret);
*/
        return ret;

    }

    public String fromJSON(List<String> payload)   {

        Gson gson = new Gson();
        String txHex = "";
        String net = "m";
        String hash = null;

        for(int i = 0; i < payload.size(); i++)   {

            int count = -1;
            int id = -1;
            int idx = -1;
            String _txHex = null;

            String s = payload.get(i);

            Log.d("PayloadFactory", "incoming:" + s);

            if(i == 0)    {
                Seg0 seg0 = gson.fromJson(s, Seg0.class);

                count = seg0.s;
//                idx = seg0.c;
                idx = 0;
                id = seg0.i;
                hash = seg0.h;
                txHex = seg0.t;
                net = seg0.n;

                Log.d("PayloadFactory", "incoming:" + seg0.s);
                Log.d("PayloadFactory", "incoming:" + 0);
                Log.d("PayloadFactory", "incoming:" + seg0.i);
                Log.d("PayloadFactory", "incoming:" + seg0.h);
                Log.d("PayloadFactory", "incoming:" + seg0.t);
                Log.d("PayloadFactory", "incoming:" + seg0.n);

                if(count == -1 || idx == -1 || id == -1 || hash == null || txHex == null)    {
                    return null;
                }

                if(idx != i)    {
                    return null;
                }

                if(count != payload.size())    {
                    return null;
                }

            }
            else    {
                SegN segn = gson.fromJson(s, SegN.class);

                idx = segn.c;
                id = segn.i;
                _txHex = segn.t;

                if(idx == -1 || id == -1 || _txHex == null)    {
                    return null;
                }

                if(idx != i)    {
                    return null;
                }

                txHex += _txHex;

            }

        }

        Log.d("PayloadFactory", "payload:" + txHex);

        if(Z85.getInstance().isZ85(txHex))    {
            Log.d("PayloadFactory", "payload encoded:" + txHex);
            String _txHex = Hex.toHexString(Z85.getInstance().decode(txHex));
            Log.d("PayloadFactory", "payload decoded:" + _txHex);
            txHex = _txHex;
        }

        Transaction tx = new Transaction((net != null && net.equals("t")) ? TestNet3Params.get() : MainNetParams.get(), Hex.decode(txHex));
        Log.d("PayloadFactory", "payload:" + tx.getHashAsString());
        if(!tx.getHashAsString().equalsIgnoreCase(hash))    {
            return null;
        }
        else    {
            return txHex;
        }

    }

    public void broadcastPayload(final List<String> payload, final boolean useMainNet)   {

        final String txHex = fromJSON(payload);
        Log.d("PayloadFactory", "payload retrieved:" + txHex);

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                String response = null;
                String api = useMainNet ? "v2/pushtx/" : "test/v2/pushtx/";

                try {
                    response = postURL(null, "https://api.samouraiwallet.com/" + api, "tx=" + txHex);
                }
                catch(Exception e) {
                    Log.d("PayloadFactory", e.getMessage());
                    e.printStackTrace();
                    response = e.getMessage();
                }

                Log.d("PayloadFactory", response);

                /*
                final String _response = response;

                Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent("com.samourai.ponydirect.LOG");
                        intent.putExtra("msg", context.getText(R.string.broadcasted) + ":" + _response);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                });
                */

                BroadcastLogUtil.getInstance().add(payload.get(0));

                Looper.loop();

            }
        }).start();

    }

    public String postURL(String contentType, String request, String urlParameters) throws Exception {

        String error = null;

        for (int ii = 0; ii < 3; ++ii) {
            URL url = new URL(request);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            try {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", contentType == null ? "application/x-www-form-urlencoded" : contentType);
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

                connection.setUseCaches (false);

                connection.setConnectTimeout(60000);
                connection.setReadTimeout(60000);

                connection.connect();

                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                connection.setInstanceFollowRedirects(false);

                if (connection.getResponseCode() == 200) {
//					System.out.println("postURL:return code 200");
                    return IOUtils.toString(connection.getInputStream(), "UTF-8");
                }
                else {
                    error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
//                    System.out.println("postURL:return code " + error);
                }

                Thread.sleep(5000);
            } finally {
                connection.disconnect();
            }
        }

        return error;
    }

}
