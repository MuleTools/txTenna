package com.samourai.txtenna.utils;

import android.content.Context;

public class goTennaUtil {

    private static final String GOTENNA_APP_TOKEN = ""; // TODO: Insert your token

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

}
