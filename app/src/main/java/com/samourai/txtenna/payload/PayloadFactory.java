package com.samourai.txtenna.payload;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.gotenna.sdk.gids.GIDManager;
import com.samourai.sms.SMSSender;
import com.samourai.txtenna.utils.Message;
import com.samourai.txtenna.R;
import com.samourai.txtenna.utils.SendMessageInteractor;
import com.samourai.txtenna.utils.BroadcastLogUtil;
import com.samourai.txtenna.utils.SentTxUtil;
import com.samourai.txtenna.utils.Z85;
import com.samourai.txtenna.prefs.PrefsUtil;

import org.apache.commons.io.IOUtils;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.entity.StringEntity;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;

public class PayloadFactory {

    private final static String dataDir = "wallet";
    private final static String strFilename = "txtenna.dat";

    public class Seg0   {
        public int s = -1;
        public String i = null;
        public String n = "m";
        public String h = null;
        public String t = null;
    };

    public class SegN   {
        public int c = -1;
        public String i = "";
        public String t = null;
    };

    private static final int smsSegment0Len = 40;
    private static final int smsSegment1Len = 120;

    private static final int goTennaSegment0Len = 100;  // 110?
    private static final int goTennaSegment1Len = 180;  // 190?

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

    public List<String> toJSON(String strHexTx, boolean isGoTenna, NetworkParameters params) {

        int segment0Len = isGoTenna ? goTennaSegment0Len : smsSegment0Len;
        int segment1Len = isGoTenna ? goTennaSegment1Len : smsSegment1Len;

        //
        // if Z85 encoding, use 24 extra characters for tx in segment0. Hash encoded on 40 characters instead of 64
        //
        if(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_Z85, false) == true)    {
            segment0Len += 24;
        }

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

        String id = null;
        if(isGoTenna)    {
            String _id = PrefsUtil.getInstance(context).getValue(PrefsUtil.GOTENNA_UID, "");
            _id = _id + "|" + messageIdx;

            try {
                byte[] buf = _id.getBytes("UTF-8");
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hash = md.digest(buf);
                byte[] idBytes = new byte[8];
                System.arraycopy(hash, 0, idBytes, 0, idBytes.length);
                if(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_Z85, false) == true)    {
                    id = Z85.getInstance().encode(idBytes);
                }
                else    {
                    id = Hex.toHexString(idBytes);
                }
            }
            catch(UnsupportedEncodingException | NoSuchAlgorithmException e) {
                ;
            }
        }
        else    {
            id = Integer.toString(messageIdx);
        }

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
                seg0.i = id;
                if((params != null && params instanceof TestNet3Params) || PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_MAINNET, true) == false)    {
                    seg0.n = "t";
                }
                if(PrefsUtil.getInstance(context).getValue(PrefsUtil.USE_Z85, false) == true)    {
                    seg0.h = Z85.getInstance().encode(Hex.decode(tx.getHashAsString()));
                }
                else    {
                    seg0.h = tx.getHashAsString();
                }
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

        return ret;

    }

    public String fromJSON(List<String> payload)   {

        Gson gson = new Gson();
        String txHex = "";
        String net = "m";
        String hash = null;

        for(int i = 0; i < payload.size(); i++)   {

            int count = -1;
            String id = null;
            int idx = -1;
            String _txHex = null;

            String s = payload.get(i);

            Log.d("PayloadFactory", "incoming:" + s);

            if(i == 0)    {
                Seg0 seg0 = gson.fromJson(s, Seg0.class);

                count = seg0.s;
                idx = 0;
                if(Z85.getInstance().isZ85(seg0.i))    {
                    id = Hex.toHexString(Z85.getInstance().decode(seg0.i));
                }
                else    {
                    id = seg0.i;
                }
                if(Z85.getInstance().isZ85(seg0.h))    {
                    hash = Hex.toHexString(Z85.getInstance().decode(seg0.h));
                }
                else    {
                    hash = seg0.h;
                }
                txHex = seg0.t;
                net = seg0.n;

                Log.d("PayloadFactory", "incoming:" + seg0.s);
                Log.d("PayloadFactory", "incoming:" + 0);
                Log.d("PayloadFactory", "incoming:" + seg0.i);
                Log.d("PayloadFactory", "incoming:" + seg0.h);
                Log.d("PayloadFactory", "incoming:" + seg0.t);
                Log.d("PayloadFactory", "incoming:" + seg0.n);

                if(count == -1 || idx == -1 || id == null || id.length() == 0 || hash == null || txHex == null)    {
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
                if(Z85.getInstance().isZ85(segn.i))    {
                    id = Hex.toHexString(Z85.getInstance().decode(segn.i));
                }
                else    {
                    id = segn.i;
                }
                _txHex = segn.t;

                if(idx == -1 || id == null || id.length() == 0 || _txHex == null)    {
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

    public void relayPayload(final List<String> payload, final boolean isGoTenna)   {

        final long smsDelay = 5000L;
        final long goTennaDelay = 15000L;

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                for(int i = 0; i < payload.size(); i++)   {

                    final String s = payload.get(i);

                    final int ii = i + 1;

                    if(isGoTenna)    {
                        SendMessageInteractor smi = new SendMessageInteractor();

                        Message messageToSend = Message.createReadyToSendMessage(new SecureRandom().nextLong(),
                                GIDManager.SHOUT_GID,
                                s);

                        smi.sendBroadcastMessage(messageToSend,
                                new SendMessageInteractor.SendMessageListener()
                                {
                                    @Override
                                    public void onMessageResponseReceived()
                                    {
                                        Log.d("PayloadFactory", "response received:" + ii);
                                        registerSent(s);

                                    }

                                });

                        Log.d("PayloadFactory", "goTenna relayed:" + s);
                    }
                    else    {
                        SMSSender.getInstance(context).send(s, PrefsUtil.getInstance(context).getValue(PrefsUtil.SMS_RELAY, context.getString(R.string.default_relay)));
                        Log.d("PayloadFactory", "sms relayed:" + s);
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "sent:" + s, Toast.LENGTH_SHORT).show();
                        }
                    });

                    try {
                        Thread.sleep(isGoTenna ? goTennaDelay : smsDelay);
                    }
                    catch(Exception e) {
                        ;
                    }

                }

                BroadcastLogUtil.getInstance().add(payload.get(0), true, isGoTenna);

                Looper.loop();

            }
        }).start();

    }

    public void broadcastPayload(final List<String> payload, final boolean useMainNet, final boolean goTenna)   {

        final String txHex = fromJSON(payload);
        Log.d("PayloadFactory", "payload retrieved:" + txHex);

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                String response = null;
                String url = (useMainNet) ? context.getText(R.string.default_pushtx_mainnet).toString() : context.getText(R.string.default_pushtx_testnet).toString();

                try {
                    response = postURL(null, url, "tx=" + txHex);
                }
                catch(Exception e) {
                    Log.d("PayloadFactory", e.getMessage());
                    e.printStackTrace();
                    response = e.getMessage();
                }

                final String _response = response;

                Log.d("PayloadFactory", _response);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, _response, Toast.LENGTH_SHORT).show();
                    }
                });

                BroadcastLogUtil.getInstance().add(payload.get(0), false, goTenna);

                Looper.loop();

            }
        }).start();

    }

    public void uploadSegment(final String segment)   {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                HttpResponse response = null;

                try {

                    String postUrl = context.getText(R.string.default_txtenna).toString();
                    HttpClient httpClient = HttpClientBuilder.create().build();
                    HttpPost post = new HttpPost(postUrl);
                    StringEntity postingString = new StringEntity(segment);
                    post.setEntity(postingString);
                    post.setHeader("Content-type", "application/json");
                    response = httpClient.execute(post);
                    Log.d("PayloadFactory", "HTTP POST return:" + response.getStatusLine().getStatusCode());
                    if(response.getStatusLine().getStatusCode() == 200)    {
                        registerSent(segment);
                    }

                }
                catch(Exception e) {
                    Log.d("PayloadFactory", e.getMessage());
                    e.printStackTrace();
//                    response = e.getMessage();
                }

                final String _response = Integer.toString(response.getStatusLine().getStatusCode());

                Log.d("PayloadFactory", _response);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, _response + ":" + segment, Toast.LENGTH_SHORT).show();
                    }
                });

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

    public void writeBroadcastLog() throws IOException, JSONException {
        JSONObject obj = new JSONObject();
        obj.put("logs", BroadcastLogUtil.getInstance().toJSON());
        Log.d("PayloadFactory", "writing:" + obj.toString());
        serialize(obj);
    }

    public void readBroadcastLog() throws IOException, JSONException {
        JSONObject obj = deserialize();
        Log.d("PayloadFactory", "reading:" + obj.toString());
        if(obj != null && obj.has("logs"))    {
            BroadcastLogUtil.getInstance().fromJSON(obj.getJSONArray("logs"));
        }
    }

    private synchronized void serialize(JSONObject jsonobj) throws IOException, JSONException   {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File newfile = new File(dir, strFilename);
        newfile.setWritable(true, true);

        if(newfile.exists()) {
            newfile.delete();
        }
        newfile.createNewFile();

        String data = jsonobj.toString(4);
        if(data != null)    {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newfile), "UTF-8"));
            try {
                out.write(data);
            } finally {
                out.close();
            }
            Log.d("PayloadFactory", "serializing:" + data);
        }

    }

    private synchronized JSONObject deserialize() throws IOException, JSONException {

        File dir = context.getDir(dataDir, Context.MODE_PRIVATE);
        File file = new File(dir, strFilename);
        StringBuilder sb = new StringBuilder();

        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
        String str = null;
        while((str = in.readLine()) != null) {
            sb.append(str);
        }
        in.close();

        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(sb.toString());
        }
        catch(JSONException je)   {
            ;
        }
        Log.d("PayloadFactory", "deserializing:" + sb.toString());

        return jsonObj;
    }

    private void registerSent(String s) {

        try {
            JSONObject obj = new JSONObject(s);
            if(obj.has("i"))    {
                String id = obj.getString("i");
                if(obj.has("c"))    {
                    SentTxUtil.getInstance().add(id, obj.getInt("c"));
                }
                else    {
                    SentTxUtil.getInstance().add(id, 0);
                }

            }
        }
        catch(JSONException je) {
            ;
        }

    }

}
