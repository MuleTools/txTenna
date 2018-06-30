package com.samourai.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.samourai.txtenna.payload.PayloadFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SMSReceiver extends BroadcastReceiver {

    /** TAG used for Debug-Logging */
    protected static final String LOG_TAG = "SMSReceiver";

    /** The Action fired by the Android-System when a SMS was received.
     * We are using the Default Package-Visibility */
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    private static HashMap<String, HashMap<String, HashMap<String, String>>> incoming = new HashMap<String, HashMap<String, HashMap<String, String>>>();

    private static List<String> seen = new ArrayList<String>();

    // @Override
    public void onReceive(final Context context, Intent intent) {

        if ((intent.getAction().equals(ACTION) || intent.getAction().contains("SMS_RECEIVED"))) {

            StringBuilder sb = new StringBuilder();
            Bundle bundle = intent.getExtras();

            Handler handler = new Handler();

            if (bundle != null) {

                Object[] pdusObj = (Object[]) bundle.get("pdus");
                SmsMessage[] messages = new SmsMessage[pdusObj.length];
                for(int i = 0; i < pdusObj.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[])pdusObj[i]);
                }

                String incomingTelNo = null;
                int id = -1;
                for(SmsMessage currentMessage : messages)	{
                    String msg = currentMessage.getDisplayMessageBody().trim();
                    incomingTelNo = currentMessage.getDisplayOriginatingAddress();

                    if(seen.contains(incomingTelNo + ":" + msg))    {
                        continue;
                    }
                    else    {
                        seen.add(incomingTelNo + ":" + msg);
                    }

                    Log.d("SMSReceiver", incomingTelNo + ":" + msg);

                    //
                    // test for segment count, if present assume Segment0
                    //
                    int i = -1;
                    int c = -1;
                    PayloadFactory.Seg0 seg0 = null;
                    PayloadFactory.SegN segn = null;
                    Gson gson = new Gson();
                    if(msg.contains("\"s\"="))    {
                        seg0 = gson.fromJson(msg, PayloadFactory.Seg0.class);
                        c = 0;
                        i = seg0.i;
                    }
                    else    {
                        segn = gson.fromJson(msg, PayloadFactory.SegN.class);
                        c = segn.c;
                        i = segn.i;
                    }

                    if(i != -1 && c != -1)    {
                        HashMap<String, HashMap<String,String>> ids = incoming.get(incomingTelNo);
                        HashMap<String, String> segments = null;
                        if(ids == null)    {
                            ids = new HashMap<String, HashMap<String,String>>();
                        }
                        else    {
                            segments = ids.get(Integer.toString(i));
                        }
                        id = i;
                        if(segments == null)    {
                            segments = new HashMap<String, String>();
                        }
                        segments.put(Integer.toString(c), msg);
                        if(ids == null)    {
                            ids = new HashMap<String, HashMap<String, String>>();
                        }
                        ids.put(Integer.toString(i), segments);
                        incoming.put(incomingTelNo, ids);
                    }

                }

                /*
                final String _incomingTelNo = incomingTelNo;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent("com.samourai.ponydirect.LOG");
                        intent.putExtra("msg", "incoming from:" + _incomingTelNo);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                });
                */

                if(incomingTelNo != null && id != -1)    {
                    verifyIncoming(context, incomingTelNo, id);
                }

            }
        }
    }

    private void verifyIncoming(final Context context, String incomingTelNo, int id)   {

        HashMap<String, HashMap<String,String>> ids = incoming.get(incomingTelNo);
        HashMap<String, String> segments = ids.get(Integer.toString(id));

        int segs = -1;
        String hash = null;
        String net = null;

        Gson gson = new Gson();
        PayloadFactory.Seg0 seg0 = null;
        PayloadFactory.SegN segn = null;

        for(String key : segments.keySet())   {

            String msg = segments.get(key);
            if(msg.contains("\"s\"="))    {
                seg0 = gson.fromJson(msg, PayloadFactory.Seg0.class);
                segs = seg0.s;
                hash = seg0.h;
                net = (seg0.n != null && seg0.n.length() > 0) ? seg0.n : "m";
            }
            else    {
                segn = gson.fromJson(msg, PayloadFactory.SegN.class);
            }

        }

        if(segs != -1 && segs == segments.size())    {

            String[] s = new String[segs];

            int c = -1;

            for(String key : segments.keySet())   {

                String msg = segments.get(key);

                if(msg.contains("\"s\"="))    {
                    seg0 = gson.fromJson(msg, PayloadFactory.Seg0.class);
                    c = 0;
                }
                else    {
                    segn = gson.fromJson(msg, PayloadFactory.SegN.class);
                    c = segn.c;
                }

                if(c != -1)    {
                    s[c] = msg;
                }

            }

            List<String> segmentList = Arrays.asList(s);

            /*
            final String _hash = hash;
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent("com.samourai.txtenna.LOG");
                    intent.putExtra("msg", "broadcasting:" + _hash);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
            });
            */

            PayloadFactory.getInstance(context).broadcastPayload(segmentList, (net != null && net.equals("t")) ? false : true);

        }
        else    {
            Log.d("SMSReceiver", "verifyIncoming(): segment size not recognized");
        }

    }

}
