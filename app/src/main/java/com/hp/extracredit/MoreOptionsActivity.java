package com.hp.extracredit;

import android.content.Intent;
import android.os.Bundle;
import android.print.PrintJobInfo;
import android.printservice.PrintService;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

public class MoreOptionsActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MoreOptionsAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //noinspection ConstantConditions (actionBar will be present at create time)
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            // Display the fragment as the main content.
            getFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new MoreOptionsFragment())
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /** Called by fragments when the user changes job options */
    void onJobOptionsChanged(JobOptions options) {
        Log.d(LOG_TAG, "onJobOptionsChanged() " + options);
        PrintJobInfo jobInfo = options.record(
                this,
                (PrintJobInfo)getIntent().getParcelableExtra(PrintService.EXTRA_PRINT_JOB_INFO));
        Intent result = new Intent();
        result.putExtra(PrintService.EXTRA_PRINT_JOB_INFO, jobInfo);
        setResult(RESULT_OK, result);
    }
}
