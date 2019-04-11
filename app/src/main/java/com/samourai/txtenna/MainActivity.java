package com.samourai.txtenna;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.gotenna.sdk.exceptions.GTInvalidAppTokenException;
import com.samourai.sms.SMSReceiver;
import com.samourai.txtenna.adapters.BroadcastLogsAdapter;
import com.samourai.txtenna.payload.PayloadFactory;
import com.samourai.txtenna.prefs.PrefsUtil;
import com.samourai.txtenna.utils.BroadcastLogUtil;
import com.samourai.txtenna.utils.IncomingMessagesManager;
import com.samourai.txtenna.utils.Message;
import com.samourai.txtenna.utils.SentTxUtil;
import com.samourai.txtenna.utils.TransactionHandler;
import com.samourai.txtenna.utils.goTennaUtil;
import com.yanzhenjie.zbar.Symbol;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import static java.lang.StrictMath.abs;


public class MainActivity extends AppCompatActivity implements IncomingMessagesManager.IncomingMessageListener {

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
    private TransactionHandler transactionHandler = null;

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

        transactionHandler = new TransactionHandler("TransactionHandler", adapter);

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

        transactionHandler = new TransactionHandler("TransactionHandler", adapter);
        transactionHandler.start();

        Log.d("MainActivity", "onCreate " + Integer.toHexString(this.hashCode()) + " transactionHandler:" + Integer.toHexString(transactionHandler.hashCode()) + " [lifecycle]");

        try {
            PayloadFactory.getInstance(MainActivity.this, transactionHandler).readBroadcastLog();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        try {
            goTennaUtil.getInstance(MainActivity.this).init();
        }
        catch (GTInvalidAppTokenException | java.lang.IllegalArgumentException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.invalid_token_title);
            builder.setMessage(R.string.invalid_token);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();

            // should set the value goTennaUtil::GOTENNA_APP_TOKEN
            e.printStackTrace();
        }

        if (goTennaUtil.tokenIsVerified()) {
            // if NOT already paired, and previous hardware address saved, try to connect to a goTenna
            Log.d("MainActivity", "checking connected address:" + goTennaUtil.getInstance(MainActivity.this).getGtConnectionManager().getConnectedGotennaAddress());

            if (!goTennaUtil.getInstance(MainActivity.this).isPaired() &&
                    goTennaUtil.getInstance(MainActivity.this).GetHardwareAddress() != null) {
                // set geolocation after we connect to a device
                int region = PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.REGION, 1);

                IncomingMessagesManager.getInstance(MainActivity.this.getApplicationContext()).addIncomingMessageListener(this);
                IncomingMessagesManager.getInstance(MainActivity.this.getApplicationContext()).startListening();

                // use GID from last *unconfirmed* broadcast transaction, otherwise a new random GID
                long gid = BroadcastLogUtil.getInstance().lastGID();
                goTennaUtil.getInstance(MainActivity.this).connect(null, region);
            }
        }

        String action = getIntent().getAction();
        String scheme = getIntent().getScheme();
        if (action != null && action.equals("com.samourai.txtenna.HEX")) {
            String hex = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            String[] s = hex.split("-");
            Log.d("MainActivity", "hex:" + hex);
            NetworkParameters params = null;
            if (s.length > 1) {
                params = (s[1].equalsIgnoreCase("t") ? TestNet3Params.get() : MainNetParams.get());
            }
            if (s[0] != null && s[0].length() > 0 && s[0].matches("^[0-9A-Fa-f]+$")) {
                relayViaGoTenna = null;
                doSendHex(s[0], params);
            }
        }
    }

    @Override
    protected void onRestart() {
        Log.d("MainActivity", "onRestart " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
        super.onRestart();
        if (transactionHandler == null) {
            Log.d("MainActivity", "onRestart, transactionHandler is null!! " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
        }
    }

    @Override
    protected void onStart() {
        Log.d("MainActivity", "onStart " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
        super.onStart();
        if (transactionHandler == null) {
            Log.d("MainActivity", "onStart, transactionHandler is null!! " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
        }
    }

    @Override
    protected void onResume() {
        Log.d("MainActivity", "onResume " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
        super.onResume();

        if (transactionHandler == null) {
            Log.d("MainActivity", "onResume, transactionHandler is null!! " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
        }

        if(isReceiver == null)    {
            // register as an SMS message receiver
            isFilter = new IntentFilter();
            isFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
            isFilter.setPriority(2147483647);
            isReceiver = new SMSReceiver(transactionHandler);
            MainActivity.this.registerReceiver(isReceiver, isFilter);
        }
        refreshData();
        transactionHandler.startTransactionChecker();
        IncomingMessagesManager.getInstance(MainActivity.this.getApplicationContext()).addIncomingMessageListener(this);
    }

    @Override
    protected void onPause() {
        Log.d("MainActivity", "onPause " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
        super.onPause();

        if (transactionHandler == null) {
            Log.d("MainActivity", "onPause, transactionHandler is null!! " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
        }

        try {
            PayloadFactory.getInstance(MainActivity.this, transactionHandler).writeBroadcastLog();
        }
        catch(JSONException | IOException e) {
            e.printStackTrace();
        }

        try {
            if(isReceiver != null) {
                // unregister as an SMS message receiver
                MainActivity.this.unregisterReceiver(isReceiver);
            }
        }
        catch(IllegalArgumentException iae) {
            ;
        }

        IncomingMessagesManager.getInstance(MainActivity.this.getApplicationContext()).removeIncomingMessageListener(this);
        transactionHandler.stopTransactionChecker();
    }

    @Override
    protected void onStop() {
        Log.d("MainActivity", "onStop " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
        super.onStop();

        if (transactionHandler == null) {
            Log.d("MainActivity", "onStop, transactionHandler is null!! " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("MainActivity", "onDestroy " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
        super.onDestroy();

        transactionHandler.quit();
        transactionHandler = null;
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
        Log.d("MainActivity", "onBackPressed " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
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
        transactionHandler.refresh();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK && requestCode == SCAN_HEX_TX) {

            if (data != null && data.getStringExtra(ZBarConstants.SCAN_RESULT) != null) {

                final String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT).trim();

                doSendHex(strResult, null);

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

                        doSendHex(strHexTx, null);

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

    private void doSendHex(final String hexTx, final NetworkParameters params)    {

        // show transaction log after sending a transaction
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        if(!hexTx.matches("^[A-Fa-f0-9]+$")) {
            return;
        }

        Transaction tx = null;
        String msg = null;
        try {
            tx = new Transaction(PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.USE_MAINNET, true) == true ? MainNetParams.get() : TestNet3Params.get(), Hex.decode(hexTx));
            msg = MainActivity.this.getString(R.string.broadcast) + ":" + tx.getHashAsString() + " " + getText(R.string.to) + " " + PrefsUtil.getInstance(MainActivity.this).getValue(PrefsUtil.SMS_RELAY, MainActivity.this.getText(R.string.default_relay).toString()) + " ?";
            Log.d("MainActivity", "hash:" + tx.getHashAsString());
            tx.verify();
        }
        catch(VerificationException ve) {
            Log.d("MainActivity", "Invalid transaction, hash:" + tx.getHashAsString());
            Toast.makeText(MainActivity.this, R.string.invalid_tx, Toast.LENGTH_SHORT).show();
            return;
        }

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

                    List<String> payload  = PayloadFactory.toJSON(hexTx, relayViaGoTenna, params);
                    PayloadFactory.getInstance(MainActivity.this, transactionHandler).relayPayload(payload, relayViaGoTenna);
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

                    List<String> payload  = PayloadFactory.toJSON(hexTx, true, params);
                    PayloadFactory.getInstance(MainActivity.this, transactionHandler).relayPayload(payload, true);
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

                    List<String> payload  = PayloadFactory.toJSON(hexTx, false, params);
                    PayloadFactory.getInstance(MainActivity.this, transactionHandler).relayPayload(payload, false);
                    relayViaGoTenna = null;
                }
            });
        }

        dlg.show();
    }

    public void onIncomingMessage(Message incomingMessage) {

        // show transaction log after receiving an incoming message
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        try {
            JSONObject obj = new JSONObject(incomingMessage.getText());
            if(obj.has("i")) {
                String id = obj.getString("i");
                int idx = 0;
                if (obj.has("c")) {
                    idx = obj.getInt("c");
                }

                if (!SentTxUtil.getInstance().contains(id, idx)) {
                    // handle upload of segment to server
                    // if(ConnectivityStatus.hasConnectivity(this))    {
                    PayloadFactory.getInstance(this, transactionHandler).broadcastPayload(obj.toString(), incomingMessage.getSenderGID());
                    // }
                    // else    {
                    // rebroadcast
                    // }
                }
            }
            else if (obj.has("b") && incomingMessage.getReceiverGID() == goTennaUtil.getGID()) {
                if (transactionHandler == null) {
                    Log.d("MainActivity", "onIncomingMessage, transactionHandler is null!! " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
                }
                // handle return receipt message
                transactionHandler.confirmFromGateway(incomingMessage.getText());
            }
        }
        catch(JSONException je) {
            Log.d("MainActivity", "onIncomingMessage, JSONException = " + je);
        }
    }
}
