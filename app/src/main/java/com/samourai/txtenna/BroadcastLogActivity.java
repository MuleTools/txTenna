package com.samourai.txtenna;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.samourai.txtenna.adapters.BroadCastLogsAdapter;

public class BroadcastLogActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_log);
        recyclerView = findViewById(R.id.RVBroadCastLog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        BroadCastLogsAdapter adapter = new BroadCastLogsAdapter(this);
        recyclerView.setAdapter(adapter);
    }
}
