package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

/**
 * Created by pc on 2018/2/7.
 */
public class DemoFrgm extends Fragment implements IFrgmCallBack {
    private MainActivity activity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_demo, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        return v;
    }

    private void InitView(View v) {
        ImageView img =  v.findViewById(R.id.demo_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.AllBtnFrgmHome();
            }
        });
    }

    @Override
    public void InitData() {

    }

    @Override
    public void onTouchNext() {

    }

    @Override
    public void onTouchBack(int flag) {

    }

    @Override
    public void FrgmMessage(Message msg) {

    }
}

