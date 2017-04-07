package com.hp.extracredit.ui;

import android.app.ListActivity;
import android.os.Bundle;
import android.support.v4.BuildConfig;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hp.extracredit.R;

/**
 * Created by JohnYang on 4/6/17.
 */

public class ScannedItemsActivity extends AppCompatActivity{
    private final static String TAG = ScannedItemsActivity.class.getName();
    private boolean mIsDebuggable = BuildConfig.DEBUG;

    static final String[] MOBILE_OS =
            new String[] { "Android", "iOS", "WindowsMobile", "Blackberry"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        setListAdapter(new ScannedItemsAdapter(this, MOBILE_OS));
        setContentView(R.layout.my_listview);

        ListView listView = (ListView) findViewById(R.id.list_view1);

        listView.setAdapter(new ScannedItemsAdapter(this, MOBILE_OS));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                /*Toast.makeText(
                        getApplicationContext(),
                        ((TextView) v.findViewById(R.id.grid_item_label))
                                .getText(), Toast.LENGTH_SHORT).show();*/

            }
        });

    }

    /*@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        //get selected items
        String selectedValue = (String) getListAdapter().getItem(position);
        Toast.makeText(this, selectedValue, Toast.LENGTH_SHORT).show();

    }*/


}
