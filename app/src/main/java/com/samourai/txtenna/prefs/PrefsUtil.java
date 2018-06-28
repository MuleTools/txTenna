package com.samourai.txtenna.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefsUtil {

    public static final String SMS_RELAY = "smsRelay";
    public static final String PUSHTX = "pushTx";
    public static final String USE_MAINNET = "mainNet";
    public static final String USE_Z85 = "z85";
    public static final String MESSAGE_IDX = "msgIdx";

    private static Context context = null;
    private static PrefsUtil instance = null;

    private PrefsUtil() { ; }

    public static PrefsUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new PrefsUtil();
        }

        return instance;
    }

    public String getValue(String name, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(name, (value == null || value.length() < 1) ? "" : value);
    }

    public boolean setValue(String name, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(name, (value == null || value.length() < 1) ? "" : value);
        return editor.commit();
    }

    public int getValue(String name, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(name, 0);
    }

    public boolean setValue(String name, int value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(name, (value < 0) ? 0 : value);
        return editor.commit();
    }

    public long getValue(String name, long value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getLong(name, 0L);
    }

    public boolean setValue(String name, long value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(name, (value < 0L) ? 0L : value);
        return editor.commit();
    }

    public boolean getValue(String name, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(name, value);
    }

    public boolean setValue(String name, boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(name, value);
        return editor.commit();
    }

    public boolean removeValue(String name) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(name);
        return editor.commit();
    }

    public boolean has(String name) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.contains(name);
    }

    public boolean clear() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.clear();
        return editor.commit();
    }

}
