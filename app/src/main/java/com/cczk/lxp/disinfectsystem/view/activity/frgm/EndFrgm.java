package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.animation.ObjectAnimator;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.PlanItem;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.FileUtils;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.base.SocketUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pc on 2018/2/7.
 */
public class EndFrgm extends Fragment implements IFrgmCallBack {
    private static final String TAG = "EndFrgm";
    private MainActivity activity;

    private int flag=0;
    public final static int flag_sub_idle =  800;           // 消毒等待界面
    public final static int flag_sub_end =  801;            // 消毒结束界面

    //View
    FrameLayout layout_idle;
    FrameLayout layout_end;
    TextView tv_idle;
    TextView tv_end;

    public static int time_idle;
    public static int time_end;

    private boolean IsWebControlEnd=false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_end, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        //获取控件
        layout_idle=v.findViewById(R.id.end_layout_idle);
        layout_end=v.findViewById(R.id.end_layout_end);
        tv_idle =v.findViewById(R.id.end_tv_idle_time);
        tv_end =v.findViewById(R.id.end_tv_end_time);

        //回退按钮
        ImageView img=v.findViewById(R.id.end_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //服务器发送终端停止任务
                DisinfectData.planItem.NowRunMode= PlanItem.RunMode_End;
                SocketUtils.getInstance().SendStopDisinfect();

                activity.AllBtnFrgmHome();
            }
        });

        img=v.findViewById(R.id.end_img_finish);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.AllBtnFrgmHome();
            }
        });

        img=v.findViewById(R.id.end_img_ok);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtnOk();
            }
        });
    }

    public void BtnOk()
    {
        //回退到登录页面
        activity.ComeGoFrame(MainActivity.flag_login);
    }

    @Override
    public void FrgmMessage(Message msg) {
        switch (msg.what){
            case MainActivity.MsgID_UI_Time:
                switch (flag){
                    case flag_sub_idle:
                        //撤离时间
                        if(time_idle>0){
                            //时间减少
                            time_idle--;
                        }else{
                            //时间归零
                            ChangeSubFlag(flag_sub_end);
                        }
                        tv_idle.setText(AndroidUtils.UIGetTimeStr(time_idle));
                        break;
                    case flag_sub_end:
                        //结束时间
                        if(time_end>0){
                            //时间减少
                            time_end--;
                        }else{
                            //时间归零
                            onTouchNext();
                        }
                        tv_end.setText(AndroidUtils.UIGetTimeStr(time_end));
                        break;
                }
                break;
        }
    }

    @Override
    public void InitData() {
        time_idle=ParmFrgm.time_idle;
        Log.d(TAG, "EndTime: "+time_idle);
        tv_idle.setText(AndroidUtils.UIGetTimeStr(time_idle));

        if(!IsWebControlEnd) {
            ChangeSubFlag(flag_sub_idle);
        }else{
            IsWebControlEnd=false;
            ChangeSubFlag(flag_sub_end);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(activity.customDialog!=null)
        {
            activity.customDialog.dismiss();
        }
        if(!hidden){
            InitData();
        }
    }

    public void ChangeIdleMode(){
        time_idle=ParmFrgm.time_idle;
        ChangeSubFlag(flag_sub_idle);
    }

    public void ChangeEndMode(){
        ChangeSubFlag(flag_sub_end);
    }

    public void WebControlEnd(){
        IsWebControlEnd=true;
    }

    private void ChangeSubFlag(int flag){
        this.flag=flag;

        ObjectAnimator animator;
        switch (flag){
            case flag_sub_idle:
                animator = ObjectAnimator.ofFloat(layout_idle, "translationX", 2000, 0);
                animator.setDuration(10);
                animator.start();
                layout_idle.setVisibility(View.VISIBLE);
                layout_end.setVisibility(View.GONE);
                break;
            case flag_sub_end:
                SocketUtils.getInstance().IsAddSensort=false;

                if(activity==null)
                {
                    activity=MainActivity.Instance;
                }
                activity.SetFrgmChange(flag_sub_end);

                time_end=60;
                //UI
                tv_end.setText(AndroidUtils.UIGetTimeStr(time_end));

                //UIGO
                animator = ObjectAnimator.ofFloat(layout_idle, "translationX", 0, -2000);
                animator.setDuration(200);
                animator.start();
                animator = ObjectAnimator.ofFloat(layout_end, "translationX", 2000, 0);
                animator.setDuration(200);
                animator.start();
                layout_end.setVisibility(View.VISIBLE);
                //layout_idle.setVisibility(View.GONE);

                //绿色工作灯闪烁
                ControlUtils.getInstance().OpenRGBGreedBlink();

                //发送消毒结束
                SocketUtils.getInstance().SendPlanOver();

                if(!HttpUtils.IsOnLine)
                {
                    //单机数据
                    SetLocalData();
                }
                break;
        }
    }

    void SetLocalData()
    {
        try {
            SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            DisinfectData.localParmData.put("finishTime",sdf.format(System.currentTimeMillis()));
            //0-消毒完成，1-消毒中，2-消毒终止，3-异常",
            DisinfectData.localParmData.put("status",0);
            DisinfectData.localParmData.put("data",DisinfectData.localSensort);

            String SaveData=FileUtils.SplitKey;
            //SaveData+=DisinfectData.localParmData.toString();
            SaveData = DisinfectData.localParmData.toString();

            ArrayList<String> list = FileUtils.getTxtFile();
            FileUtils.wirteTxt(SaveData,"LoacalData"+list.size()+".json",true,true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTouchNext() {
        if(flag==flag_sub_end) {
            activity.ClearFrgmList();
            activity.ComeGoFrame(MainActivity.flag_login);
        }
    }

    @Override
    public void onTouchBack(int flag) {
//         if(this.flag==flag_sub_end) {
//            activity.ComeGoFrame(MainActivity.flag_login);
//            activity.ClearFrgmList();
//        }
    }

    public void ThreadOneSecFun() {
        //更新UI时间
        Message msg = activity.FrgmHandler.obtainMessage();
        msg.what = activity.MsgID_UI_Time;
        msg.obj = AndroidUtils.GetSystemTime();
        activity.FrgmHandler.sendMessage(msg);
    }
}

