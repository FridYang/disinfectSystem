package com.cczk.lxp.disinfectsystem.view.activity.frgm;
import android.os.Message;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pc on 2018/2/7.
 */
public class ToSettingsFrgm extends Fragment implements IFrgmCallBack  {
    private static final String TAG = "ToSettingsFrgm";
    private MainActivity activity;

    //View
    LinearLayout layout_pw;
    LinearLayout layout_settings;

    EditText edit_pw;
    EditText edit_newpw;

    Button btn_newpw;
    Button btn_tosettins;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_tosettings, container, false);
        activity = (MainActivity) this.getActivity();

        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        //初始化
        layout_pw =v.findViewById(R.id.tosettings_layout_password);
        layout_settings =v.findViewById(R.id.tosettings_layout_settings);
        edit_pw =v.findViewById(R.id.tosettings_edit_password);
        edit_newpw =v.findViewById(R.id.tosettings_edit_newpassword);
        btn_newpw =v.findViewById(R.id.tosettings_btn_newpw);
        btn_tosettins =v.findViewById(R.id.tosettings_btn_toset);

        //按钮点击
        ImageView img=v.findViewById(R.id.tosettings_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.BtnFrgmLogin();
            }
        });
        btn_newpw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //修改密码
                String newPw=edit_newpw.getText().toString();
                if(newPw.length()>=4 && newPw.length()<= 10){
                    //保存缓存数据
                    Map<String, String> params=new HashMap<>();
                    params.put("settingspw",newPw);
                    AndroidUtils.QuickDataSet(params);
                    activity.MyToast("密码修改成功");
                }else{
                    activity.MyToast("密码长度必须为4~10位");
                }
            }
        });
        btn_tosettins.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               MainActivity.Instance.AppClose(500);
            }
        });
    }

    @Override
    public void InitData() {
        layout_pw.setVisibility(View.VISIBLE);
        layout_settings.setVisibility(View.GONE);
        edit_pw.setText("");
        edit_newpw.setText("");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            InitData();
        }
    }

    @Override
    public void FrgmMessage(Message msg) {
        Object[] data;
        int index=0;
        switch (msg.what){
            case MainActivity.MsgID_UI_Time:
                //初始化数据

                break;
        }
    }

    // ====================================== 手势 ======================================

    @Override
    public void onTouchNext() {
        if(layout_pw.getVisibility()==View.VISIBLE)
        {
            //判断密码
            String pw=AndroidUtils.QuickDataGet("settingspw","1234");
            String curPw=edit_pw.getText().toString();
            if(pw.equals(curPw))
            {
                layout_pw.setVisibility(View.GONE);
                layout_settings.setVisibility(View.VISIBLE);
            }else
                {
                activity.MyToast("密码错误");
            }
        }
    }

    @Override
    public void onTouchBack(int listFlag) {

    }

    /*
    public void ThreadOneSecFun()
    {
        //更新UI时间
        Message msg = activity.FrgmHandler.obtainMessage();
        msg.what = activity.MsgID_UI_Time;
        msg.obj = AndroidUtils.GetSystemTime();
        activity.FrgmHandler.sendMessage(msg);
    }
    */

}

