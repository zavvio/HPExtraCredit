package com.hp.extracredit.ui;

import android.app.ListActivity;
import android.os.Bundle;
import android.support.v4.BuildConfig;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Created by JohnYang on 4/6/17.
 */

public class ScannedItemsActivity extends ListActivity{
    private final static String TAG = ScannedItemsActivity.class.getName();
    private boolean mIsDebuggable = BuildConfig.DEBUG;

    static final String[] MOBILE_OS =
            new String[] { "Android", "iOS", "WindowsMobile", "Blackberry"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setListAdapter(new ScannedItemsAdapter(this, MOBILE_OS));

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        //get selected items
        String selectedValue = (String) getListAdapter().getItem(position);
        Toast.makeText(this, selectedValue, Toast.LENGTH_SHORT).show();

    }


}
