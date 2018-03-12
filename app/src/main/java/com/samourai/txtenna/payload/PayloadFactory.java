package com.samourai.txtenna.payload;

import com.google.gson.Gson;

import org.bitcoinj.core.Transaction;

public class PayloadFactory {

    private static final int segment0Len = 110;
    private static final int segment1Len = 200;

    private static PayloadFactory instance = null;

    private PayloadFactory() { ; }

    public static PayloadFactory getInstance()   {

        if(instance == null)    {
            instance = new PayloadFactory();
        }

        return instance;
    }

    //
    // xlat tx to txTenna JSON
    //
    public Gson toJSON(String strTxHex) {

        return null;
    }

}
