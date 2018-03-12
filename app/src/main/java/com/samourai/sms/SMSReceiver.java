package com.samourai.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.ArrayList;
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
                    String msg = currentMessage.getDisplayMessageBody().trim().toLowerCase();
                    incomingTelNo = currentMessage.getDisplayOriginatingAddress();

                    if(seen.contains(incomingTelNo + ":" + msg))    {
                        continue;
                    }
                    else    {
                        seen.add(incomingTelNo + ":" + msg);
                    }

                    Log.d("SMSReceiver", incomingTelNo + ":" + msg);

                }

            }
        }
    }

}
