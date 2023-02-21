package com.cczk.lxp.disinfectsystem.view.utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

/**
 * Created by pc on 2020/8/25.
 */

public class GridItemView  extends GridLayout {
    private int no=-1;
    private TextView tv;
    public ImageView img_select;
    public ImageView img_edit;
    public boolean isOn=false;

    public GridItemView(Context context) {
        super(context);

        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.item_grid, this);

        // 获取控件
        tv =  findViewById(R.id.grid_item_no);
        img_select =  findViewById(R.id.grid_item_select);
        img_edit =  findViewById(R.id.grid_item_edit);
    }

    public void Init(int no,String name,int size){
        this.no=no;
        //tv.setText(String.valueOf(no+1));
        tv.setText(name);
        tv.setTextSize(size);
        SelectClick(false);
    }

    public void SelectClick(boolean isOn){
        this.isOn=isOn;
        if(isOn){
            img_select.setImageBitmap(MainActivity.Instance.bm_RoomSelect);
        }else{
            img_select.setImageBitmap(MainActivity.Instance.bm_RoomNoSelect);
        }
    }
}
