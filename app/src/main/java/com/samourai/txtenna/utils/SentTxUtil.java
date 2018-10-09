package com.samourai.txtenna.utils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SentTxUtil {

    private static SentTxUtil instance = null;

    private static List<String> sentTxs = null;

    private SentTxUtil() { ; }

    public static SentTxUtil getInstance() {

        if(instance == null) {
            sentTxs = new ArrayList<String>();
            instance = new SentTxUtil();
        }

        return instance;
    }

    public void reset() {
        sentTxs.clear();
    }

    public void add(String id) {
        if(!sentTxs.contains(id))    {
            sentTxs.add(id);
        }
    }

    public boolean contains(String id) {
        return sentTxs.contains(id) ? true : false;
    }

    public void add(String hash, int idx) {
        if(!sentTxs.contains(hash + "-" + idx))    {
            sentTxs.add(hash + "-" + idx);
        }
    }

    public boolean contains(String hash, int idx) {
        return sentTxs.contains(hash + "-" + idx) ? true : false;
    }

}
