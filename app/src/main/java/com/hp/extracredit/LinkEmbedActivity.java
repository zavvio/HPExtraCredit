package com.hp.extracredit;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import android.widget.Toast;

public class LinkEmbedActivity extends AppCompatActivity {
    TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_embed);
        output = (TextView) findViewById(R.id.textView_response);
        output.setMovementMethod(new ScrollingMovementMethod());

        if (isOnline()) {
            requestData();
        } else {
            Toast.makeText(this, "Network isn't available", Toast.LENGTH_LONG).show();
        }
    }

    private void requestData() {
        MyTask task = new MyTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    private class MyTask extends AsyncTask<String> {
        @Override
        protected Object doInBackground(Object[] params) {
            return null;
        }

        @Override

    }
}
