package com.hp.extracredit.ui;

/**
 * Created by JohnYang on 4/6/17.
 */


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.hp.extracredit.R;
import com.hp.extracredit.Utils.CircleImageView;

public class AbstractListViewAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final int[] images;

    public AbstractListViewAdapter(Context context, String[] values, int[] images) {
        super(context, R.layout.my_scan_list_item, values);
        this.context = context;
        this.values = values;
        this.images = images;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.my_scan_list_item, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.label);
        CircleImageView imageView = (CircleImageView) rowView.findViewById(R.id.logo);
        textView.setText(values[position]);

        // Change icon based on name
        String s = values[position];

        System.out.println(s);

        imageView.setImageResource(images[position]);
       /* if (s.equals("WindowsMobile")) {
            imageView.setImageResource(R.drawable.ic_launcher);
        } else if (s.equals("iOS")) {
            imageView.setImageResource(R.drawable.ic_launcher);
        } else if (s.equals("Blackberry")) {
            imageView.setImageResource(R.drawable.ic_launcher);
        } else {
            imageView.setImageResource(R.drawable.ic_launcher);
        }*/

        return rowView;
    }
}