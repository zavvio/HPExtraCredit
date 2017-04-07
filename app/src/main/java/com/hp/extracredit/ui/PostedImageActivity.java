package com.hp.extracredit.ui;

/**
 * Created by JohnYang on 4/6/17.
 */

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;

import com.hp.extracredit.R;

public class PostedImageActivity extends AbstractAppCompatActivity {

    GridView gridView;

    static final String[] MOBILE_OS = new String[] {
            "Android", "iOS","Windows", "Blackberry" };


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_gridview);

        gridView = (GridView) findViewById(R.id.gridView1);

        gridView.setAdapter(new PostedImageAdapter(this, MOBILE_OS));

        gridView.setNumColumns(3);

        gridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Toast.makeText(
                        getApplicationContext(),
                        ((TextView) v.findViewById(R.id.grid_item_label))
                                .getText(), Toast.LENGTH_SHORT).show();

            }
        });

    }

}

