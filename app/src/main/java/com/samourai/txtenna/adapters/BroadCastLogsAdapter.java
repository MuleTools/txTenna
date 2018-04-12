package com.samourai.txtenna.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.constraint.ConstraintLayout;
import android.support.transition.ChangeBounds;
import android.support.transition.TransitionManager;
import android.support.transition.Visibility;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.samourai.txtenna.R;

public class BroadCastLogsAdapter extends RecyclerView.Adapter<BroadCastLogsAdapter.viewHolder> {

    private Context mContext;

    class viewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private LinearLayout cardBody;
        private Button share, explore;
        private TextView txId, cardTitle, timeStamp;
        private FrameLayout cardBodyContainer;
        private CardView card;
        private ImageView icon;
        private ConstraintLayout constraintLayout;
        private Boolean isExpandable = false;
        private View root;

        viewHolder(View itemView) {
            super(itemView);
            root = itemView;
            cardBody = itemView.findViewById(R.id.cardBody);
            explore = itemView.findViewById(R.id.btn_explore);
            cardTitle = itemView.findViewById(R.id.broadcast_cardTitle);
            timeStamp = itemView.findViewById(R.id.broadcast_card_timestamp);
            icon = itemView.findViewById(R.id.broadcast_card_icon);
            txId = itemView.findViewById(R.id.tx_broadcast_card_body);
            share = itemView.findViewById(R.id.btn_share);
            card = itemView.findViewById(R.id.card);
            constraintLayout = itemView.findViewById(R.id.constraintLayout);
            cardBody.setVisibility(View.GONE);
        }

        @Override
        public void onClick(View v) {
            TransitionManager.beginDelayedTransition((ViewGroup) constraintLayout.getParent(), new ChangeBounds());
            int visibility = cardBody.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            cardBody.setVisibility(visibility);
        }

        void setExpandable() {
            constraintLayout.setOnClickListener(this);
        }

        void setExapanded(@Visibility.Mode int mode) {
            cardBody.setVisibility(mode);
        }

    }

    public BroadCastLogsAdapter(Context context) {
        mContext = context;
    }

    @Override
    public viewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.collapsible_info_cardview, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(final viewHolder holder, int position) {
        if (position == 0) {
            holder.cardTitle.setText("Broadcasted to mesh network");
            holder.timeStamp.setText("Feb 12,2018\n10:50");
            Drawable mWrappedDrawable = DrawableCompat.wrap(mContext.getResources().getDrawable(R.drawable.ic_txtenna_done).mutate());
            DrawableCompat.setTint(mWrappedDrawable, Color.WHITE);
            DrawableCompat.setTintMode(mWrappedDrawable, PorterDuff.Mode.SRC_ATOP);
            holder.icon.setImageDrawable(mWrappedDrawable);
            holder.icon.setBackground(mContext.getResources().getDrawable(R.drawable.circle_green));
        }
        if (position == 1) {
            holder.setExpandable();
            holder.cardTitle.setText("Relaying to bitcoin network");
            holder.timeStamp.setText("Unknown");
            holder.txId.setText("ada14e06e6b33ddb811d17f55916dbd6dbe2e2f0e3b3d4dd88fc706eb90b9af5");
            holder.explore.setVisibility(View.GONE);
            holder.setExapanded(View.VISIBLE);
        }
        if (position == 2) {
            holder.cardTitle.setText("Broadcasted to mesh network");
            holder.timeStamp.setText("Feb 12,2018\n10:50");
            Drawable mWrappedDrawable = DrawableCompat.wrap(mContext.getResources().getDrawable(R.drawable.ic_txtenna_done).mutate());
            DrawableCompat.setTint(mWrappedDrawable, Color.WHITE);
            DrawableCompat.setTintMode(mWrappedDrawable, PorterDuff.Mode.SRC_ATOP);
            holder.icon.setImageDrawable(mWrappedDrawable);
            holder.icon.setBackground(mContext.getResources().getDrawable(R.drawable.circle_green));
            holder.setExpandable();
            holder.setExapanded(View.VISIBLE);

        }

    }

    @Override
    public int getItemCount() {
        return 3;
    }


}
