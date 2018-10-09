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

import com.samourai.txtenna.prefs.PrefsUtil;

import com.gotenna.sdk.bluetooth.GTConnectionManager.GTDeviceType;
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
    private Button btRescan = null;

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

        if(goTennaUtil.getInstance(NetworkingActivity.this).isPaired())    {
            String device = getText(R.string.mesh_device_detected2) + ": " + goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().getConnectedGotennaAddress();
            mesh_card_detail.setText(device);
            mesh_card_detail_title.setText(R.string.mesh_device_detected);
        }
        else    {
            mesh_card_detail.setText(R.string.rescan_or_buy);
            mesh_card_detail_title.setText(R.string.mesh_device_detected);
        }

        btRescan = findViewById(R.id.btn_rescan);
        if(goTennaUtil.getInstance(NetworkingActivity.this).isPaired())    {
            btRescan.setText(R.string.unpair_device);
        }
        else    {
            btRescan.setText(R.string.pair_device);
        }
        btRescan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(goTennaUtil.getInstance(NetworkingActivity.this).isPaired())    {
                    goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().disconnect();
                    goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().clearConnectedGotennaAddress();
                    goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().scanAndConnect(GTDeviceType.MESH);
                    btRescan.setText(R.string.pair_device);
                    mesh_card_detail.setText(R.string.rescan_or_buy);
                    mesh_card_detail_title.setText(R.string.mesh_device_detected);
                }
                else    {

                    if(!hasBluetoothPermisson())    {
                        requestBluetoothPermission();
                    }
                    if(!hasLocationpermission())    {
                        requestLocationPermission();
                    }

                    if(hasLocationpermission() && hasBluetoothPermisson())    {
//                        GTCommandCenter.getInstance().setGoTennaGID(1111111111L, "txTenna", null);
                        if(goTennaUtil.getInstance(NetworkingActivity.this).isPaired())    {
                            Log.d("NetworkingActivity", "existing connected address:" + goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().getConnectedGotennaAddress());
                            String device = getText(R.string.mesh_device_detected2) + ": " + goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().getConnectedGotennaAddress();
                            btRescan.setText(R.string.pair_device);
                            mesh_card_detail.setText(device);
                            mesh_card_detail_title.setText(R.string.mesh_device_detected);
                        }
                        else    {
                            goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().scanAndConnect(GTDeviceType.MESH);
                            Log.d("NetworkingActivity", "connected address:" + goTennaUtil.getInstance(NetworkingActivity.this).getGtConnectionManager().getConnectedGotennaAddress());
                            btRescan.setText(R.string.unpair_device);
                            mesh_card_detail.setText(R.string.rescan_or_buy);
                            mesh_card_detail_title.setText(R.string.mesh_device_detected);
                        }
                    }

                }

            }
        });

    }

    private void changeState(){
        TransitionManager.beginDelayedTransition((ViewGroup) ConstraintGroup.getParent(), new ChangeBounds());

        int visibility = ConstraintGroup.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        ConstraintGroup.setVisibility(visibility);

        if (visibility == View.GONE) {
            status_img_mesh.setImageDrawable(getResources().getDrawable(R.drawable.circle_green));
            mesh_card_detail_title.setText(R.string.mesh_device_detected2);
        }else{
            status_img_mesh.setImageDrawable(getResources().getDrawable(R.drawable.circle_red));
            mesh_card_detail_title.setText(R.string.no_mesh_device_detected);
        }
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
