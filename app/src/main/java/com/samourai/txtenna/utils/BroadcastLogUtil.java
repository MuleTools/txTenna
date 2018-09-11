package com.samourai.txtenna.utils;

import com.google.gson.Gson;
import com.samourai.txtenna.payload.PayloadFactory;

import java.util.ArrayList;
import java.util.List;

public class BroadcastLogUtil {

    public class BroadcastLogEntry  {
        public long ts = -1L;
        public String hash = null;
        public String net = null; // "m" == mainnet, "t" == testnet, "g" == goTenna meshnet
        public boolean completed = false;
        public boolean relayed = false; // true == relayed via sms or goTenna, false == uploaded to network
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

    public void add(String s, boolean relayed) {

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

        add(entry);
    }

    public List<BroadcastLogEntry> getBroadcastLog()    {
        return broadcastLog;
    }

    public void setBroadcastLog(List<BroadcastLogEntry> log)    {
        broadcastLog = log;
    }

}
