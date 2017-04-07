package com.hp.extracredit.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.BuildConfig;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.hp.extracredit.R;
import com.hp.extracredit.Utils.StoreInfo;

/**
 * Created by JohnYang on 4/6/17.
 */

public class AvailableStoreActivity extends AbstractAppCompatActivity{
    private final static String TAG = AvailableStoreActivity.class.getName();
    private boolean mIsDebuggable = BuildConfig.DEBUG;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        setListAdapter(new ScannedItemsAdapter(this, MOBILE_OS));
        setContentView(R.layout.my_listview);

        ListView listView = (ListView) findViewById(R.id.list_view1);

        listView.setAdapter(new AbstractListViewAdapter(this, StoreInfo.REWARDS, StoreInfo.STORE_images));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                /*Toast.makeText(
                        getApplicationContext(),
                        ((TextView) v.findViewById(R.id.grid_item_label))
                                .getText(), Toast.LENGTH_SHORT).show();*/
                Intent intent = new Intent(AvailableStoreActivity.this, StoreProductActivity.class);
                intent.putExtra(StoreInfo.STORE_SELECTION, position);
                startActivity(intent);

            }
        });

        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(AvailableStoreActivity.this, StoreProductActivity.class);
                intent.putExtra(StoreInfo.STORE_SELECTION, i);
                startActivity(intent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

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
