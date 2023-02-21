package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.os.Message;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pc on 2018/2/7.
 */
public class ChangeOffPwFrgm extends Fragment implements IFrgmCallBack {
    private static final String TAG = "ChangeOffPwFrgm";
    private MainActivity activity;
    EditText edit_pw;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_changeoffpw, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        //获取控件
        edit_pw=v.findViewById(R.id.changeoffpw_tv_pw);

        //回退按钮
        ImageView img=v.findViewById(R.id.changeoffpw_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.AllBtnFrgmHome();
            }
        });
    }

    @Override
    public void FrgmMessage(Message msg) {
        try{
            Object[] data;
            switch (msg.what){
                case MainActivity.MsgID_UI_Time:
                    break;
            }
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void InitData()
    {
        edit_pw.setText("");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden)
        {
            InitData();
        }else
        {
            activity.NFCClose();
        }
    }

    @Override
    public void onTouchNext() {
        if(edit_pw.getText().toString().length()==6)
        {
            //保存新密码

            activity.AllBtnFrgmHome();
        }else{
            //弹窗提示
            //AndroidUtils.GetStr(R.string.offpwinfo);
        }
    }

    @Override
    public void onTouchBack(int flag) {}
}

