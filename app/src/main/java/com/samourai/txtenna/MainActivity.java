package com.samourai.txtenna;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.constraint.Group;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.gotenna.sdk.GoTenna;
import com.gotenna.sdk.bluetooth.GTConnectionManager;
import com.gotenna.sdk.commands.GTCommand;
import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.commands.Place;
import com.gotenna.sdk.interfaces.GTErrorListener;
import com.gotenna.sdk.responses.GTResponse;
import com.gotenna.sdk.types.GTDataTypes;
import com.samourai.sms.SMSReceiver;
import com.samourai.txtenna.adapters.BroadcastLogsAdapter;
import com.samourai.txtenna.payload.PayloadFactory;
import com.samourai.txtenna.prefs.PrefsUtil;
import com.samourai.txtenna.utils.BroadcastLogUtil;
import com.samourai.txtenna.utils.IncomingMessagesManager;
import com.samourai.txtenna.utils.goTennaUtil;
import com.yanzhenjie.zbar.Symbol;

import org.apache.commons.io.IOUtils;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    private final static int SCAN_HEX_TX = 2011;

    private static final int SMS_PERMISSION_CODE = 0;
    private static final int CAMERA_PERMISSION_CODE = 1;
    private Group emptyView;
    private LinearLayout BottomSheetMenu;
    private LinearLayout txTennaSelection;
    private LinearLayout smsSelection;
    private BottomSheetBehavior bottomSheetBehavior;
    private FloatingActionButton fab;
    private RecyclerView recyclerView;
    private BroadcastLogsAdapter adapter = null;

    private IntentFilter isFilter = null;
    private BroadcastReceiver isReceiver = null;

    private Boolean relayViaGoTenna = null;

    public static final String ACTION_INTENT = "com.samourai.txtenna.LOG";
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {

            /*
            if(ACTION_INTENT.equals(intent.getAction()) && tvLog != null) {

                String strText = intent.getStringExtra("msg");
                String strLog = tvLog.getText().toString();

                if(strLog.length() > 0) {
                    strLog += "\n";
                }

                strLog += strText;

                tvLog.setText(strLog);

            }
            */

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        emptyView = findViewById(R.id.emptyView);
        fab = findViewById(R.id.fab);
        BottomSheetMenu = findViewById(R.id.fab_bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(BottomSheetMenu);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setBottomSheetCallback(bottomSheetCallback);

        recyclerView = findViewById(R.id.RVBroadCastLog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BroadcastLogsAdapter(this);
        recyclerView.setAdapter(adapter);

        refreshData();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
        });

        txTennaSelection = BottomSheetMenu.findViewById(R.id.txTenna);
        txTennaSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                relayViaGoTenna = true;
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                doGetHex();
            }
        });

        smsSelection = BottomSheetMenu.findViewById(R.id.sms);
        smsSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                relayViaGoTenna = false;
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                doGetHex();
            }
        });


        if (!hasCameraPermission()) {
            showRequestCameraPermissionInfoAlertDialog();
        }

        if (!hasCameraPermission()) {
            showRequestCameraPermissionInfoAlertDialog();
        }

        if (!hasReadSmsPermission() || !hasSendSmsPermission()) {
            showRequestSMSPermissionInfoAlertDialog();
        }

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(receiver, filter);

        try {
            PayloadFactory.getInstance(MainActivity.this).readBroadcastLog();
        }
        catch(JSONException | IOException e) {
            e.printStackTrace();
        }

        goTennaUtil.getInstance(MainActivity.this).init();
        Log.d("MainActivity", "checking connected address:" + goTennaUtil.getInstance(MainActivity.this).getGtConnectionManager().getConnectedGotennaAddress());

        if(GoTenna.tokenIsVerified() && goTennaUtil.getInstance(MainActivity.this).isPaired())    {

            GTCommandCenter.getInstance().sendSetGeoRegion(Place.EUROPE, new GTCommand.GTCommandResponseListener()
            {
                @Override
                public void onResponse(GTResponse response)
                {
                    if (response.getResponseCode() == GTDataTypes.GTCommandResponseCode.POSITIVE)
                    {
                        Log.d("MainActivity", "GID set OK");
                    }
                    else
                    {
                        Log.d("MainActivity", "GID set OK:" + response.toString());
                    }
                }
            }, new GTErrorListener()
            {
                @Override
                public void onError(GTError error)
                {
                    Log.d("MainActivity", error.toString() + "," + error.getCode());
                }
            });

            GTCommandCenter.getInstance().setGoTennaGID(new SecureRandom().nextLong(), UUID.randomUUID().toString(), new GTErrorListener()
            {
                @Override
                public void onError(GTError error)
                {
                    Log.d("MainActivity", error.toString() + "," + error.getCode());
                }
            });
            goTennaUtil.getInstance(MainActivity.this).getGtConnectionManager().scanAndConnect(GTConnectionManager.GTDeviceType.MESH);

            IncomingMessagesManager.getInstance(MainActivity.this.getApplicationContext()).startListening();

        }

        String action = getIntent().getAction();
        String scheme = getIntent().getScheme();
        if(action != null && action.equals("com.samourai.txtenna.HEX")) {
            String hex = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            Log.d("MainActivity", "hex:" + hex);
            if(hex != null && hex.length() > 0 && hex.matches("^[0-9A-Fa-f]+$"))    {
                relayViaGoTenna = null;
                doSendHex(hex);
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(isReceiver == null)    {
            isFilter = new IntentFilter();
            isFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
            isFilter.setPriority(2147483647);
            isReceiver = new SMSReceiver();
            MainActivity.this.registerReceiver(isReceiver, isFilter);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            if(isReceiver != null)    {
                MainActivity.this.unregisterReceiver(isReceiver);
            }
        }
        catch(IllegalArgumentException iae) {
            ;
        }

    }

    @Override
    protected void onDestroy() {

        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(receiver);

        try {
            if(isReceiver != null)    {
                MainActivity.this.unregisterReceiver(isReceiver);
            }
        }
        catch(IllegalArgumentException iae) {
            ;
        }

        try {
            PayloadFactory.getInstance(MainActivity.this).writeBroadcastLog();
        }
        catch(JSONException | IOException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    /**
     * Listener fpr bottomsheet events
     * this will set fab icon based on the bottomsheet's state
     */
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                fab.setImageResource(R.drawable.ic_keyboard_arrow_down);
            }
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                fab.setImageResource(R.drawable.ic_txtenna_fab_new);
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.broadcastlog) {
            startActivity(new Intent(this, BroadcastLogActivity.class));
            return true;
        }
        if (id == R.id.networkmenu) {
            startActivity(new Intent(this, NetworkingActivity.class));
            return true;
        }
        if (id == R.id.qr_scan) {
            relayViaGoTenna = null;
            doScanHexTx();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshData() {

        if (BroadcastLogUtil.getInstance().getBroadcastLog().size() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                for (int i = 0; i < BroadcastLogUtil.getInstance().getBroadcastLog().size(); i++) {

                    if (!BroadcastLogUtil.getInstance().getBroadcastLog().get(i).confirmed || BroadcastLogUtil.getInstance().getBroadcastLog().get(i).ts < 0L) {

                        try {
                            String URL = null;
                            if (BroadcastLogUtil.getInstance().getBroadcastLog().get(i).net.equalsIgnoreCase("t")) {
                                URL = "https://api.samourai.io/test/v2/tx/" + BroadcastLogUtil.getInstance().getBroadcastLog().get(i).hash;
                            } else {
                                URL = "https://api.samourai.io/v2/tx/" + BroadcastLogUtil.getInstance().getBroadcastLog().get(i).hash;
                            }
                            URL url = new URL(URL);

                            String result = null;
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                            try {
                                connection.setRequestMethod("GET");
                                connection.setRequestProperty("charset", "utf-8");
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

                                connection.setConnectTimeout(60000);
                                connection.setReadTimeout(60000);

                                connection.setInstanceFollowRedirects(false);

                                connection.connect();

                                if (connection.getResponseCode() == 200) {
                                    result = IOUtils.toString(connection.getInputStream(), "UTF-8");
                                    JSONObject obj = new JSONObject(result);
                                    if (obj != null && obj.has("block")) {

                                        JSONObject bObj = obj.getJSONObject("block");
                                        if (bObj.has("height") && bObj.has("time")) {
                                            BroadcastLogUtil.getInstance().getBroadcastLog().get(i).confirmed = true;
                                            BroadcastLogUtil.getInstance().getBroadcastLog().get(i).ts = bObj.getLong("time");
                                        }

                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                adapter.notifyDataSetChanged();
                                            }
                                        });

                                    }

                                }

                                Thread.sleep(250);

                            } finally {
                                connection.disconnect();
                            }

                        } catch (Exception e) {
                            ;
                        }

                    }

                }

                Looper.loop();

            }
        }).start();

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_HEX_TX) {

            if (data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null) {

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT).trim();

                doSendHex(strResult);

            }
        } else {
            ;
        }

    }

    private void doGetHex() {

        final EditText edHexTx = new EditText(MainActivity.this);
        edHexTx.setSingleLine(false);
        edHexTx.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        edHexTx.setLines(10);
        edHexTx.setHint(R.string.tx_hex);
        edHexTx.setGravity(Gravity.START);
        TextWatcher textWatcher = new TextWatcher() {

            public void afterTextChanged(Editable s) {
                edHexTx.setSelection(0);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ;
            }
        };
        edHexTx.addTextChangedListener(textWatcher);

        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.app_name)
                .setView(edHexTx)
                .setMessage(R.string.enter_tx_hex)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();
                        relayViaGoTenna = null;

                        final String strHexTx = edHexTx.getText().toString().trim();

                        Log.d("MainActivity", "hex:" + strHexTx);

                        Transaction tx = new Transaction(PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.USE_MAINNET, true) ? MainNetParams.get() : TestNet3Params.get(), Hex.decode(strHexTx));
                        Log.d("MainActivity", "hash:" + tx.getHashAsString());
                        try {
                            tx.verify();
                            doSendHex(strHexTx);
                        }
                        catch(VerificationException ve) {
                            Toast.makeText(MainActivity.this, R.string.invalid_tx, Toast.LENGTH_SHORT).show();
                        }

                    }
                }).setNegativeButton(R.string.scan, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();
                        relayViaGoTenna = null;

                        doScanHexTx();
                    }
                });

        dlg.show();

    }

    private void doScanHexTx()   {
        Intent intent = new Intent(MainActivity.this, ZBarScannerActivity.class);
        intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        startActivityForResult(intent, SCAN_HEX_TX);
    }

    private void showRequestCameraPermissionInfoAlertDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_camera_alert_dialog_title);
        builder.setMessage(R.string.permission_camera_dialog_message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                requestCameraPermission();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();

    }

    private void showRequestSMSPermissionInfoAlertDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_sms_alert_dialog_title);
        builder.setMessage(R.string.permission_sms_dialog_message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                requestSmsPermission();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();

    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasSendSmsPermission() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.SEND_SMS) &&
                ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_SMS)) {
            Log.d("MainActivity", "shouldShowRequestPermissionRationale(), no permission requested");
            return;
        }

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);

    }

    private void requestCameraPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)
                ) {
            Log.d("MainActivity", "shouldShowRequestPermissionRationale(), no permission requested");
            return;
        }

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);

    }

    private void doSendHex(final String hexTx)    {

        if(!hexTx.matches("^[A-Fa-f0-9]+$")) {
            return;
        }

        final Transaction tx = new Transaction(PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.USE_MAINNET, true) == true ? MainNetParams.get() : TestNet3Params.get(), Hex.decode(hexTx));
        final String msg = MainActivity.this.getString(R.string.broadcast) + ":" + tx.getHashAsString() + " " + getText(R.string.to) + " " + PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.SMS_RELAY, MainActivity.this.getText(R.string.default_relay).toString()) + " ?";

        final TextView tvHexTx = new TextView(MainActivity.this);
        tvHexTx.setSingleLine(false);
        tvHexTx.setLines(10);
        tvHexTx.setGravity(Gravity.START);
        tvHexTx.setText(hexTx);

        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(msg)
                .setCancelable(true);

        if(relayViaGoTenna != null)    {
            dlg.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    dialog.dismiss();

                    List<String> payload  = PayloadFactory.getInstance(MainActivity.this).toJSON(hexTx, relayViaGoTenna);
                    PayloadFactory.getInstance(MainActivity.this).relayPayload(payload, relayViaGoTenna);
                    relayViaGoTenna = null;

                }

            });
            dlg.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    dialog.dismiss();
                    relayViaGoTenna = null;

                }
            });
        }
        else    {
            dlg.setPositiveButton(R.string.gotenna_mesh, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    dialog.dismiss();

                    List<String> payload  = PayloadFactory.getInstance(MainActivity.this).toJSON(hexTx, true);
                    PayloadFactory.getInstance(MainActivity.this).relayPayload(payload, true);
                    relayViaGoTenna = null;

                }

            });
            dlg.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    dialog.dismiss();
                    relayViaGoTenna = null;

                }

            });
            dlg.setNegativeButton(R.string.sms_broadcast, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    dialog.dismiss();

                    List<String> payload  = PayloadFactory.getInstance(MainActivity.this).toJSON(hexTx, false);
                    PayloadFactory.getInstance(MainActivity.this).relayPayload(payload, false);
                    relayViaGoTenna = null;

                }
            });
        }

        dlg.show();

    }

}
