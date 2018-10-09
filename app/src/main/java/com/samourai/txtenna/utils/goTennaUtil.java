package com.samourai.txtenna.utils;

import android.content.Context;
import android.util.Log;

import com.gotenna.sdk.GoTenna;
import com.gotenna.sdk.bluetooth.BluetoothAdapterManager;
import com.gotenna.sdk.bluetooth.GTConnectionManager;
import com.gotenna.sdk.exceptions.GTInvalidAppTokenException;
import com.samourai.txtenna.prefs.PrefsUtil;

public class goTennaUtil {

    private static BluetoothAdapterManager bluetoothAdapterManager = null;
    private static GTConnectionManager gtConnectionManager = null;

    private static final String GOTENNA_APP_TOKEN = "";

    private static goTennaUtil instance = null;

    private static Context context = null;

    private goTennaUtil() { ; }

    public static goTennaUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new goTennaUtil();
        }

        return instance;
    }

    public String getAppToken() {
        return GOTENNA_APP_TOKEN;
    }

    public boolean isPaired()  {

        if(getGtConnectionManager().getConnectedGotennaAddress() != null)    {
            return true;
        }
        else    {
            return false;
        }

    }

    public void init() {
        try {
            GoTenna.setApplicationToken(context.getApplicationContext(), goTennaUtil.getInstance(context).getAppToken());
            if(GoTenna.tokenIsVerified())    {
                gtConnectionManager = GTConnectionManager.getInstance();
                Log.d("goTennaUtil", "goTenna token is verified:" + GoTenna.tokenIsVerified());
                Log.d("goTennaUtil", "connected address:" + gtConnectionManager.getConnectedGotennaAddress());
            }

        }
        catch(GTInvalidAppTokenException e) {
            e.printStackTrace();
        }

    }

    public GTConnectionManager getGtConnectionManager() {
        return gtConnectionManager;
    }

    public BluetoothAdapterManager getBluetoothAdapterManager() {
        return bluetoothAdapterManager;
    }

}
