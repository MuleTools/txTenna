package com.samourai.txtenna;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.constraint.Group;
import android.support.transition.ChangeBounds;
import android.support.transition.TransitionManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gotenna.sdk.GoTenna;
import com.samourai.txtenna.prefs.PrefsUtil;

import com.samourai.txtenna.utils.goTennaUtil;

public class NetworkingActivity extends AppCompatActivity {

    private static final int BLUETOOTH_PERMISSION_CODE = 1;
    private static final int LOCATION_PERMISSION_CODE = 2;

    Group ConstraintGroup;
    CardView mesh_card;
    ImageView status_img_mesh;
    TextView mesh_card_detail_title;
    TextView mesh_card_detail;
    TextView tvSMSRelay = null;

    private Button btGetMesh = null;
    private Button btUnpair = null;
    private Button btPair = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_networking);
        ConstraintGroup = findViewById(R.id.mesh_card_group);
        mesh_card_detail = findViewById(R.id.mesh_card_detail);
        mesh_card_detail_title = findViewById(R.id.mesh_card_detail_title);
        status_img_mesh = findViewById(R.id.status_img_mesh);
        mesh_card = findViewById(R.id.mesh_card);
        mesh_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeState();
            }
        });
        tvSMSRelay = findViewById(R.id.smsRelay);
        tvSMSRelay.setText(PrefsUtil.getInstance(NetworkingActivity.this).getValue(PrefsUtil.SMS_RELAY, getString(R.string.default_relay)));

        btGetMesh = findViewById(R.id.btn_get_mesh_device);
        btGetMesh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gotenna.com/discount/SAMOURAI"));
                startActivity(browserIntent);
            }
        });

        btUnpair = findViewById(R.id.btn_unpair);
        btPair = findViewById(R.id.btn_pair);

        boolean paired = goTennaUtil.getInstance(NetworkingActivity.this).isPaired();
        setStatusText(paired, false);

        btPair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if(GoTenna.tokenIsVerified()) {
                if (!hasBluetoothPermisson()) {
                    requestBluetoothPermission();
                }
                if (!hasLocationpermission()) {
                    requestLocationPermission();
                }

                if (hasLocationpermission() && hasBluetoothPermisson()) {

                    // set geolocation after we connect to a device
                    int region = PrefsUtil.getInstance(NetworkingActivity.this).getValue(PrefsUtil.REGION, 1);

                    goTennaUtil.getInstance(NetworkingActivity.this).connect(NetworkingActivity.this, region);
                    btPair.setText(R.string.mesh_device_scanning);
                    btPair.setEnabled(false);
                }
            }
            setStatusText(false, true);
            }
        });

        btUnpair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(goTennaUtil.getInstance(NetworkingActivity.this).isPaired())    {
                    // disconnect, but remember current goTenna hardware address to connect to
                    goTennaUtil.getInstance(NetworkingActivity.this).disconnect(NetworkingActivity.this);
                    btPair.setText(R.string.mesh_device_disconnecting);
                    btPair.setEnabled(false);
                    btUnpair.setEnabled(false);
                }

                // clear the remembered goTenna hardware address, allowing you to connect to other goTenna devices.
                goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().clearConnectedGotennaAddress();
                setStatusText(false, false);
            }
        });

        status_img_mesh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // blink the connected goTenna device
                goTennaUtil.getInstance(NetworkingActivity.this).sendEchoCommand();
            }
        });

        Log.d("NetworkActivity", "gtConnectionState:" + goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().getGtConnectionState());
        Log.d("NetworkActivity", "connected address:" + goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().getConnectedGotennaAddress());
    }

    public void setStatusText(boolean isPaired, boolean isScanning) {
        String deviceName = goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().getConnectedGotennaAddress();
        int pairText = R.string.scan;
        if(isPaired && deviceName != null)    {
            mesh_card_detail.setText(getText(R.string.mesh_device_detected) + ": " + deviceName);
            mesh_card_detail_title.setText(R.string.mesh_device_detected_title);
            status_img_mesh.setImageDrawable(getResources().getDrawable(R.drawable.circle_green));
            btUnpair.setEnabled(true);
            btUnpair.setVisibility(View.VISIBLE);
        }
        else if (deviceName != null) {
            mesh_card_detail.setText(R.string.unpair_or_rescan);
            mesh_card_detail_title.setText(getText(R.string.unpair_or_rescan_title) + ": " + deviceName);
            status_img_mesh.setImageDrawable(getResources().getDrawable(R.drawable.circle_red));
            btUnpair.setEnabled(true);
            btUnpair.setVisibility(View.VISIBLE);
        }
        else    {
            mesh_card_detail.setText(R.string.rescan_or_buy);
            mesh_card_detail_title.setText(R.string.no_mesh_device_detected);
            status_img_mesh.setImageDrawable(getResources().getDrawable(R.drawable.circle_red));
            pairText = R.string.pair_device;
            btUnpair.setVisibility(View.GONE);
        }
        if (isScanning) {
            pairText = R.string.mesh_device_scanning;
        }

        btPair.setText(pairText);
        btPair.setEnabled(!isScanning);
        btPair.setVisibility(isPaired || deviceName != null ? View.GONE : View.VISIBLE);
    }

    private void changeState(){
        TransitionManager.beginDelayedTransition((ViewGroup) ConstraintGroup.getParent(), new ChangeBounds());

        int visibility = ConstraintGroup.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        ConstraintGroup.setVisibility(visibility);
    }

    private boolean hasBluetoothPermisson() {
        return ContextCompat.checkSelfPermission(NetworkingActivity.this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationpermission() {
        return ContextCompat.checkSelfPermission(NetworkingActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(NetworkingActivity.this, Manifest.permission.BLUETOOTH)) {
            Log.d("NetworkingActivity", "shouldShowRequestPermissionRationale(), no permission requested");
            return;
        }

        ActivityCompat.requestPermissions(NetworkingActivity.this, new String[]{Manifest.permission.BLUETOOTH}, BLUETOOTH_PERMISSION_CODE);

    }

    private void requestLocationPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(NetworkingActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.d("NetworkingActivity", "shouldShowRequestPermissionRationale(), no permission requested");
            return;
        }

        ActivityCompat.requestPermissions(NetworkingActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);

    }

}
