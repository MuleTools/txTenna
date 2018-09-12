package com.samourai.txtenna.utils;

import com.google.gson.Gson;
import com.samourai.txtenna.payload.PayloadFactory;

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
        broadcastLog.add(entry);
    }

    public void add(String s, boolean relayed, boolean goTenna) {

        BroadcastLogEntry entry = new BroadcastLogEntry();
        Gson gson = new Gson();

        PayloadFactory.Seg0 seg0 = gson.fromJson(s, PayloadFactory.Seg0.class);

        if(seg0.h == null)    {
            return;
        }

        entry.ts = System.currentTimeMillis() / 1000L;
        entry.hash = seg0.h;
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
                entries.put(obj);
            }
        }
        catch(JSONException je) {
            ;
        }

        return entries;
    }

    public void fromJSON(JSONArray entries) {
        try {
            for(int i = 0; i < entries.length(); i++) {
                JSONObject obj = entries.getJSONObject(i);
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
