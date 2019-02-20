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
import com.gotenna.sdk.user.User;
import com.gotenna.sdk.user.UserDataStore;
import com.samourai.txtenna.NetworkingActivity;
import com.gotenna.sdk.bluetooth.GTConnectionManager.GTDeviceType;
import com.gotenna.sdk.bluetooth.GTConnectionManager.GTConnectionListener;

import java.security.SecureRandom;
import java.util.UUID;

import static java.lang.StrictMath.abs;

public class goTennaUtil implements GTConnectionListener {

    private static BluetoothAdapterManager bluetoothAdapterManager = null;
    private GTConnectionManager gtConnectionManager = null;
    private static final int SCAN_TIMEOUT = 25000; // 25 seconds

    private static final String GOTENNA_APP_TOKEN = "[ --- REDACTED ---]";

    private static goTennaUtil instance = null;

    private static Context context = null;
    private Handler handler;
    private NetworkingActivity callbackActivity;
    private int regionIndex = 1;

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
            return true;
        }
        return false;
    }

    public String GetHardwareAddress() {
        return getGtConnectionManager().getConnectedGotennaAddress();
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
                    callbackActivity.setStatusText(false, false);
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
            case 2:
                place = Place.EUROPE;
                break;
            case 3:
                place = Place.SOUTH_AFRICA;
                break;
            case 4:
                place = Place.AUSTRALIA;
                break;
            case 5:
                place = Place.NEW_ZEALAND;
                break;
            case 6:
                place = Place.SINGAPORE;
                break;
            case 7:
                place = Place.TAIWAN;
                break;
            case 8:
                place = Place.JAPAN;
                break;
            case 9:
                place = Place.SOUTH_KOREA;
                break;
            case 10:
                place = Place.HONG_KONG;
                break;
            default:
                place = Place.NORTH_AMERICA;
                break;
        }

        if (isPaired()) {
            final Place currentPlace = place;
            GTCommandCenter.getInstance().sendSetGeoRegion(place, new GTCommand.GTCommandResponseListener() {
                @Override
                public void onResponse(GTResponse response) {
                    if (response.getResponseCode() == GTDataTypes.GTCommandResponseCode.POSITIVE) {
                        Log.d("goTennaUtil", "SetGeoRegion to " + currentPlace + " Success!");
                    } else {
                        Log.d("goTennaUtil", "SetGeoRegion to " + currentPlace + " Failed with response: " + response.toString());
                    }
                }
            }, new GTErrorListener() {
                @Override
                public void onError(GTError error) {
                    Log.d("goTennaUtil", "SetGeoRegion to " + currentPlace + " Failed with error code: " + error.getCode());
                }
            });
        }
    }

    public static void setGID(long gid) {
        GTCommandCenter.getInstance().setGoTennaGID(gid, UUID.randomUUID().toString(), new GTErrorListener() {
            @Override
            public void onError(GTError error) {
                User gtUser = UserDataStore.getInstance().getCurrentUser();
                Log.d("goTennaUtil", error.toString() + "," + error.getCode() + " gid: " + gtUser.getGID());
            }
        });

        User gtUser = UserDataStore.getInstance().getCurrentUser();
        Log.d("goTennaUtil", "gtUser.getGID: " + gtUser.getGID());
    }

    public static long getGID() {
        return UserDataStore.getInstance().getCurrentUser().getGID();
    }

    public void disconnect(NetworkingActivity activity) {
        callbackActivity = activity;
        gtConnectionManager.addGtConnectionListener(this);
        gtConnectionManager.disconnect();
    }

    public void connect(NetworkingActivity activity, int region) {
        // set new random GID every time we connect to a goTenna device
        long gid = abs(new SecureRandom().nextLong()) % 9999999999L;
        setGID(gid);

        callbackActivity = activity;
        regionIndex = region;
        gtConnectionManager.addGtConnectionListener(this);
        gtConnectionManager.scanAndConnect(GTDeviceType.MESH);
        handler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT);
    }

    public void sendEchoCommand() {
        if(GTConnectionManager.getInstance().isConnected())
        {
            GTCommandCenter.getInstance().sendEchoCommand(null, null);
        }
    }

    @Override
    public void onConnectionStateUpdated(GTConnectionState gtConnectionState) {
        if (callbackActivity != null) {
            switch (gtConnectionState) {
                case CONNECTED: {
                    Log.d("NetworkingActivity", "existing connected address:" + getGtConnectionManager().getConnectedGotennaAddress());
                    callbackActivity.setStatusText(true, false);
                }
                break;
                case DISCONNECTED: {
                    Log.d("NetworkingActivity", "no connection");
                    callbackActivity.setStatusText(false, false);
                }
                break;
                case SCANNING: {
                    Log.d("NetworkingActivity", "scanning for connection");
                    callbackActivity.setStatusText(false, true);
                }
                break;
            }
        }
        if (gtConnectionState != GTConnectionState.SCANNING) {
            getGtConnectionManager().removeGtConnectionListener(this);
            handler.removeCallbacks(scanTimeoutRunnable);
            callbackActivity = null;
        }

        if (gtConnectionState == GTConnectionState.CONNECTED) {
           setGeoloc(regionIndex);
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
