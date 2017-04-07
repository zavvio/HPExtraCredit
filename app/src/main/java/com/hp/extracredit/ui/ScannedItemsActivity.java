package com.hp.extracredit.ui;

import android.os.Bundle;
import android.support.v4.BuildConfig;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.hp.extracredit.R;

/**
 * Created by JohnYang on 4/6/17.
 */

public class ScannedItemsActivity extends AbstractAppCompatActivity{
    private final static String TAG = ScannedItemsActivity.class.getName();
    private boolean mIsDebuggable = BuildConfig.DEBUG;

    static final String[] REWARDS =
            new String[] { "Buy 1 get 1 free", "20% off single item", "10% off purchase", "Free item next visit."};

    static final int[] images =
            new int[] { R.drawable.store_nike, R.drawable.store_2, R.drawable.store_3, R.drawable.store_hp};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        setListAdapter(new ScannedItemsAdapter(this, MOBILE_OS));
        setContentView(R.layout.my_listview);

        ListView listView = (ListView) findViewById(R.id.list_view1);

        listView.setAdapter(new AbstractListViewAdapter(this, REWARDS, images));

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
