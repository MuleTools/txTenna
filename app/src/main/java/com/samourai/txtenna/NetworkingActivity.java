package com.samourai.txtenna;

import android.media.Image;
import android.support.constraint.Group;
import android.support.transition.ChangeBounds;
import android.support.transition.ChangeTransform;
import android.support.transition.Slide;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.samourai.txtenna.prefs.PrefsUtil;

public class NetworkingActivity extends AppCompatActivity {

    Group ConstraintGroup;
    CardView mesh_card;
    ImageView status_img_mesh;
    TextView mesh_card_detail_title;
    TextView tvSMSRelay = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_networking);
        ConstraintGroup = findViewById(R.id.mesh_card_group);
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
    }
    private void changeState(){
        TransitionManager.beginDelayedTransition((ViewGroup) ConstraintGroup.getParent(), new ChangeBounds());

        int visibility = ConstraintGroup.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        ConstraintGroup.setVisibility(visibility);

        if (visibility == View.GONE) {
            status_img_mesh.setImageDrawable(getResources().getDrawable(R.drawable.circle_green));
            mesh_card_detail_title.setText("Connected to TDevD's GoTenna Mesh");
        }else{
            status_img_mesh.setImageDrawable(getResources().getDrawable(R.drawable.circle_red));
            mesh_card_detail_title.setText("No mesh device detected");
        }
    }
}
