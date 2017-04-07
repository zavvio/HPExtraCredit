package com.hp.extracredit.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.BuildConfig;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.hp.extracredit.CameraActivity;
import com.hp.extracredit.PrintActivity;
import com.hp.extracredit.R;
import com.hp.extracredit.Utils.StoreInfo;

/**
 * Created by JohnYang on 4/6/17.
 */

public class StoreProductActivity extends AbstractAppCompatActivity{
    private final static String TAG = StoreProductActivity.class.getName();
    private boolean mIsDebuggable = BuildConfig.DEBUG;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        setListAdapter(new ScannedItemsAdapter(this, MOBILE_OS));
        setContentView(R.layout.my_listview);

        ListView listView = (ListView) findViewById(R.id.list_view1);

        int storeSel = getIntent().getIntExtra(StoreInfo.STORE_SELECTION, 2); // 2 is nestle

        listView.setAdapter(new AbstractListViewAdapter(this, StoreInfo.getProductDes(storeSel), StoreInfo.getProduct(storeSel)));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                /*Toast.makeText(
                        getApplicationContext(),
                        ((TextView) v.findViewById(R.id.grid_item_label))
                                .getText(), Toast.LENGTH_SHORT).show();*/

                Intent intent = new Intent(StoreProductActivity.this, PrintActivity.class);
                intent.putExtra(CameraActivity.Flag, true);
                intent.putExtra("filename", PreferenceManager.getDefaultSharedPreferences(StoreProductActivity.this).getString("ssss", ""));
                startActivity(intent);


            }
        });

        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(StoreProductActivity.this, PrintActivity.class);
                intent.putExtra(CameraActivity.Flag, true);
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
