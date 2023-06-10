package com.frost.magiccamera.widgets;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.frost.magiccamera.R;

/**
 * Created by anton on 11/12/15.
 */

public class MyHeaderView extends LinearLayout {

    TextView tbTitle;

//    @Bind(R.id.last_seen)
//    TextView lastSeen;

    public MyHeaderView(Context context) {
        super(context);
    }

    public MyHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MyHeaderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.tbTitle = findViewById(R.id.toolbar_title);
    }

    public void bindTo(String tbTitle) {
       this.tbTitle.setText(tbTitle);
    }

//    public void bindTo(String tbTitle, String lastSeen) {
//        this.tbTitle.setText(tbTitle);
//        this.lastSeen.setText(lastSeen);
//    }

    public void setTextSize(float size) {
        tbTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }
}