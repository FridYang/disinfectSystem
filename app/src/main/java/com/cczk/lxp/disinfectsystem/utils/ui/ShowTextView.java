package com.cczk.lxp.disinfectsystem.utils.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * Created by pc on 2020/5/20.
 */

public class ShowTextView extends AppCompatTextView {

    public ShowTextView(Context context) {
        super(context);
        Init();
    }

    public ShowTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Init();
    }

    public ShowTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Init();
    }

    public void Init(){
        //setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        setShadowLayer(10,10,10,Color.argb(160,0,0,0));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}