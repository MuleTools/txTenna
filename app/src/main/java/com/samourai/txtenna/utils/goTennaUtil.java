package com.samourai.txtenna.utils;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;

import com.gotenna.sdk.GoTenna;
import com.gotenna.sdk.bluetooth.BluetoothAdapterManager;
import com.gotenna.sdk.bluetooth.GTConnectionManager;
import com.gotenna.sdk.bluetooth.GTConnectionManager.GTConnectionState;
import com.gotenna.sdk.commands.GTCommand;
import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.commands.Place;
import com.gotenna.sdk.exceptions.GTInvalidAppTokenException;
import com.gotenna.sdk.interfaces.GTErrorListener;
import com.gotenna.sdk.responses.GTResponse;
import com.gotenna.sdk.types.GTDataTypes;
import com.samourai.txtenna.NetworkingActivity;
import com.gotenna.sdk.bluetooth.GTConnectionManager.GTDeviceType;
import com.gotenna.sdk.bluetooth.GTConnectionManager.GTConnectionListener;

public class goTennaUtil implements GTConnectionListener {

    private static BluetoothAdapterManager bluetoothAdapterManager = null;
    private GTConnectionManager gtConnectionManager = null;
    private static final int SCAN_TIMEOUT = 25000; // 25 seconds

    private static final String GOTENNA_APP_TOKEN = "[ --- REDACTED ---]";

    private static goTennaUtil instance = null;

    private static Context context = null;
    private Handler handler;
    private NetworkingActivity callbackActivity;

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

        if(getGtConnectionManager().getGtConnectionState() == GTConnectionState.CONNECTED) {
            return (getGtConnectionManager().getConnectedGotennaAddress() != null);
        }
        return false;
    }

    public void init() throws StringIndexOutOfBoundsException, GTInvalidAppTokenException {

        GoTenna.setApplicationToken(context.getApplicationContext(), goTennaUtil.getInstance(context).getAppToken());
        if(GoTenna.tokenIsVerified())    {
            gtConnectionManager = GTConnectionManager.getInstance();
            Log.d("goTennaUtil", "goTenna token is verified:" + GoTenna.tokenIsVerified());
            Log.d("goTennaUtil", "connected address:" + gtConnectionManager.getConnectedGotennaAddress());
        }

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                if(msg.what == 0 && callbackActivity != null) {
                    // no connection, scanning timed out
                    callbackActivity.setStatusText("");
                }
            }
        };

    }

    public GTConnectionManager getGtConnectionManager() {
        return gtConnectionManager;
    }

    public BluetoothAdapterManager getBluetoothAdapterManager() {
        return bluetoothAdapterManager;
    }

    public void setGeoloc(int region){
        Place place = null;
        switch(region)    {
            case 1:
                place = Place.EUROPE;
                break;
            case 2:
                place = Place.AUSTRALIA;
                break;
            case 3:
                place = Place.NEW_ZEALAND;
                break;
            case 4:
                place = Place.SINGAPORE;
                break;
            default:
                place = Place.NORTH_AMERICA;
                break;
        }

        if (isPaired()) {
            GTCommandCenter.getInstance().sendSetGeoRegion(place, new GTCommand.GTCommandResponseListener() {
                @Override
                public void onResponse(GTResponse response) {
                    if (response.getResponseCode() == GTDataTypes.GTCommandResponseCode.POSITIVE) {
                        Log.d("goTennaUtil", "Region set OK");
                    } else {
                        Log.d("goTennaUtil", "Region set:" + response.toString());
                    }
                }
            }, new GTErrorListener() {
                @Override
                public void onError(GTError error) {
                    Log.d("MainActivity", error.toString() + "," + error.getCode());
                }
            });
        }
    }

    public void disconnect(NetworkingActivity activity) {
        callbackActivity = activity;
        gtConnectionManager.addGtConnectionListener(this);
        gtConnectionManager.disconnect();
    }

    public void connect(NetworkingActivity activity) {
        callbackActivity = activity;
        gtConnectionManager.addGtConnectionListener(this);
        gtConnectionManager.clearConnectedGotennaAddress();
        gtConnectionManager.scanAndConnect(GTDeviceType.MESH);
        handler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT);
    }

    @Override
    public void onConnectionStateUpdated(GTConnectionState gtConnectionState) {
        if (callbackActivity != null) {
            switch (gtConnectionState) {
                case CONNECTED: {
                    Log.d("NetworkingActivity", "existing connected address:" + getGtConnectionManager().getConnectedGotennaAddress());
                    callbackActivity.setStatusText(getGtConnectionManager().getConnectedGotennaAddress());
                }
                break;
                case DISCONNECTED: {
                    Log.d("NetworkingActivity", "no connection");
                    callbackActivity.setStatusText("");
                }
                break;
                case SCANNING: {
                    Log.d("NetworkingActivity", "scanning for connection");
                }
                break;
            }
        }
        if (gtConnectionState != GTConnectionState.SCANNING) {
            getGtConnectionManager().removeGtConnectionListener(this);
            callbackActivity = null;
        }
    }

    private final Runnable scanTimeoutRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            handler.removeCallbacks(scanTimeoutRunnable);
            handler.sendEmptyMessage(0);
        }
    };
}
