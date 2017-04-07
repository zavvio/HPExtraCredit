package com.hp.extracredit.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.hp.extracredit.R;

/**
 * Created on 2/19/15.
 */


public class CircularImageView extends ImageView {

    private Context context = null;

    public CircularImageView(Context context) {
        super(context);
        this.context = context;
    }

    public CircularImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public CircularImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (context == null || context.getResources() == null)
            return;

        Paint paint = new Paint();

        paint.setAntiAlias(true);
        paint.setColor(context.getResources().getColor(R.color.hp_light_blue));
        paint.setStyle(Paint.Style.FILL);

        float radius = ((this.getWidth() > this.getHeight()) ? this.getHeight() : this.getWidth()) * 4 / 9;
        canvas.drawCircle(this.getWidth() / 2, this.getHeight() / 2, radius, paint);
    }

}