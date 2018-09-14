package com.samourai.txtenna.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.samourai.txtenna.payload.PayloadFactory;

import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BroadcastLogUtil {

    public class BroadcastLogEntry  {
        public long ts = -1L;
        public String hash = null;
        public String net = null; // "m" == mainnet, "t" == testnet, "g" == goTenna meshnet
        public boolean confirmed = false;
        public boolean relayed = false; // true == relayed via sms or goTenna, false == uploaded to network
        public boolean goTenna = false; // true == relayed via goTenna, false == relayed via sms
    }

    private static List<BroadcastLogEntry> broadcastLog = null;

    private static BroadcastLogUtil instance = null;

    private BroadcastLogUtil()  { ; }

    public static BroadcastLogUtil getInstance() {

        if(instance == null)    {
            instance = new BroadcastLogUtil();
            broadcastLog = new ArrayList<BroadcastLogEntry>();
        }

        return instance;
    }

    public void add(BroadcastLogEntry entry)    {

        if(Z85.getInstance().isZ85(entry.hash))    {
            byte[] h = Z85.getInstance().decode(entry.hash);
            entry.hash = Hex.toHexString(h);
        }

        broadcastLog.add(0, entry);

        if(broadcastLog.size() > 50)    {
            broadcastLog = broadcastLog.subList(0, 50);
        }
    }

    public void add(String s, boolean relayed, boolean goTenna) {

        BroadcastLogEntry entry = new BroadcastLogEntry();
        Gson gson = new Gson();

        PayloadFactory.Seg0 seg0 = gson.fromJson(s, PayloadFactory.Seg0.class);

        if(seg0.h == null)    {
            return;
        }

        entry.ts = System.currentTimeMillis() / 1000L;
        if(Z85.getInstance().isZ85(seg0.h))    {
            byte[] h = Z85.getInstance().decode(seg0.h);
            entry.hash = Hex.toHexString(h);
        }
        else    {
            entry.hash = seg0.h;
        }
        entry.net = (seg0.n != null || seg0.n.length() > 0) ? seg0.n : "m";
        entry.relayed = relayed;
        entry.goTenna = goTenna;

        add(entry);
    }

    public List<BroadcastLogEntry> getBroadcastLog()    {
        return broadcastLog;
    }

    public void setBroadcastLog(List<BroadcastLogEntry> log)    {
        broadcastLog = log;
    }

    public JSONArray toJSON() {

        JSONArray entries = new JSONArray();
        try {
            for(BroadcastLogEntry entry : broadcastLog) {
                JSONObject obj = new JSONObject();
                obj.put("hash", entry.hash);
                obj.put("ts", entry.ts);
                obj.put("net", entry.net);
                obj.put("confirmed", entry.confirmed);
                obj.put("relayed", entry.relayed);
                obj.put("goTenna", entry.goTenna);
                Log.d("BroadcastLogUtil", "toJSON:" + obj.toString());
                entries.put(obj);
            }
        }
        catch(JSONException je) {
            ;
        }

        return entries;
    }

    public void fromJSON(JSONArray entries) {

        broadcastLog.clear();

        try {
            for(int i = 0; i < entries.length(); i++) {
                JSONObject obj = entries.getJSONObject(i);
                Log.d("BroadcastLogUtil", "fromJSON:" + obj.toString());
                BroadcastLogEntry entry = new BroadcastLogEntry();
                entry.hash = obj.getString("hash");
                entry.ts = obj.getLong("ts");
                entry.net = obj.getString("net");
                entry.confirmed = obj.getBoolean("confirmed");
                entry.relayed = obj.getBoolean("relayed");
                entry.goTenna = obj.getBoolean("goTenna");
                add(entry);
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
