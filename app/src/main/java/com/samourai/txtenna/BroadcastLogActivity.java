package com.samourai.txtenna;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.samourai.txtenna.adapters.BroadcastLogsAdapter;
import com.samourai.txtenna.utils.BroadcastLogUtil;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class BroadcastLogActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BroadcastLogsAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_log);
        recyclerView = findViewById(R.id.RVBroadCastLog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BroadcastLogsAdapter(this);
        recyclerView.setAdapter(adapter);

        refreshData();
    }

    private void refreshData()  {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                for(int i = 0; i < BroadcastLogUtil.getInstance().getBroadcastLog().size(); i++)    {

                    if(BroadcastLogUtil.getInstance().getBroadcastLog().get(i).confirmed == false || BroadcastLogUtil.getInstance().getBroadcastLog().get(i).ts < 0L)    {

                        try {
                            String URL = null;
                            if(BroadcastLogUtil.getInstance().getBroadcastLog().get(i).net.equalsIgnoreCase("t"))    {
                                URL = "https://api.samourai.io/test/v2/tx/" + BroadcastLogUtil.getInstance().getBroadcastLog().get(i).hash;
                            }
                            else    {
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

                                if (connection.getResponseCode() == 200)    {
                                    result = IOUtils.toString(connection.getInputStream(), "UTF-8");
                                    JSONObject obj = new JSONObject(result);
                                    if(obj != null && obj.has("block"))    {

                                        JSONObject bObj = obj.getJSONObject("block");
                                        if(bObj.has("height") && bObj.has("time"))  {
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

                            }
                            finally {
                                connection.disconnect();
                            }

                        }
                        catch (Exception e) {
                            ;
                        }

                    }

                }

                Looper.loop();

            }
        }).start();

    }

}
