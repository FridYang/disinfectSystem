package com.cczk.lxp.disinfectsystem.utils.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;


import androidx.appcompat.widget.AppCompatEditText;

import com.cczk.lxp.disinfectsystem.R;

/**
 * Created by pc on 2020/5/20.
 */

public class ShowEditText extends AppCompatEditText {

    public ShowEditText(Context context) {
        super(context);
        Init();
    }

    public ShowEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        Init();
    }

    public ShowEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Init();
    }

    public void Init(){
        //setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        setBackgroundResource(R.drawable.stroke_edit);

        setShadowLayer(1,10,10,Color.argb(250,250,0,0));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}