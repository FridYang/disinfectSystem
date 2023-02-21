package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.os.Message;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.PlanItem;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.SocketUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

/**
 * Created by pc on 2018/2/7.
 */
public class LeaveFrgm extends Fragment implements IFrgmCallBack {
    private static final String TAG = "LeaveFrgm";
    private MainActivity activity;
    public boolean timeCanRun=true;
    private boolean IsWebControl =false;

    private int NeedControlType = 0; // 0不改变 1暂停 2启动

    private boolean isCheckRemote=true;
    private final int MsgID_HW_Remote=6001;

    TextView tv_time;
    ImageView img_state;
    ImageView img_bg;

    public int timeCnt;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_leave, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        //获取控件
        tv_time = v.findViewById(R.id.leave_tv_time);
        img_state =v.findViewById(R.id.leave_img_state);
        //img_bg =v.findViewById(R.id.leave_img_bg);

        //点击暂停
        img_state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LeaveStateOnClick(true);
            }
        });
        //回退按钮
        ImageView img=v.findViewById(R.id.leave_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(timeCanRun) {
//                    LeaveStateOnClick(true);
//                }
                activity.DisinfectBtnFrgmHome();
            }
        });
    }

    @Override
    public void FrgmMessage(Message msg) {
        switch (msg.what){
            case MainActivity.MsgID_UI_Time:
                //撤离时间
                if(timeCnt >0){
                    if(timeCanRun) {
                        //时间减少
                        timeCnt--;
                    }
                }else{
                    //时间归零
                    activity.GoRunFrgm(true);
                }
                tv_time.setText(AndroidUtils.UIGetTimeStr(timeCnt));

                break;
            case MsgID_HW_Remote:
                //获得遥控器信号
                if(isCheckRemote)
                {
                    if(ControlUtils.getInstance().GetRetomeData())
                    {
                        isCheckRemote=false;
                        LeaveStateOnClick(true);
                    }
                }else{
                    if(!ControlUtils.getInstance().GetRetomeData())
                    {
                        isCheckRemote=true;
                    }
                }
                break;
        }
    }

    @Override
    public void InitData() {
        Log.d(TAG, "InitData: "+ParmFrgm.time_leave);
        timeCnt = ParmFrgm.time_leave;
        tv_time.setText(AndroidUtils.UIGetTimeStr(timeCnt));

        img_state.setImageBitmap(activity.bm_BtnStop);

        //隐藏上一个界面
        //选择的弹框

        // 0不改变 1暂停 2启动
        switch (NeedControlType){
            case 0:
                timeCanRun=true;
                break;
            case 1:
                //需要暂停 设置当前在运行再点击
                timeCanRun=true;
                break;
            case 2:
                //需要运行 设置当前在停止再点击
                timeCanRun=false;
                break;
        }

        if(IsWebControl)
        {
            IsWebControl =false;
            LeaveStateOnClick(false);
        }else{
            if(NeedControlType!=0){
                LeaveStateOnClick(true);
            }
        }
        NeedControlType = 0;

        //黄色工作灯闪烁
        ControlUtils.getInstance().OpenRGBYellowBlink();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            InitData();
        }
    }

    private void LeaveStateOnClick(boolean isLocal){
        if(timeCanRun){
            //暂停
            timeCanRun=false;
            img_state.setImageBitmap(activity.bm_BtnPlay);

            if(isLocal)
            {
                //向服务器发送 线下停止
                DisinfectData.planItem.NowRunMode = PlanItem.RunMode_Leave;
                SocketUtils.getInstance().SendStopDisinfect();
            }
        }else{
            //运行
            timeCanRun=true;
            img_state.setImageBitmap(activity.bm_BtnStop);

            if(isLocal)
            {
                //向服务器发送 线下开启
                DisinfectData.planItem.leaveTime = timeCnt;
                DisinfectData.planItem.NowRunMode = PlanItem.RunMode_Leave;
                SocketUtils.getInstance().SendStartDisinfect();
            }
        }
    }

    //遥控器跳转
    public void RemoteStart(){
        NeedControlType =1;
    }

    public void WebControlStart(){
        IsWebControl=true;
        NeedControlType =2;

        timeCnt = ParmFrgm.time_leave;
    }

    //预约时间模式
    public void WebControlPlan(){
        IsWebControl=true;
        NeedControlType =1;

        timeCnt = ParmFrgm.time_leave;
    }

    public void WebControlStop(){
        IsWebControl=true;
        NeedControlType =1;

        timeCnt = ParmFrgm.time_leave;
    }

    @Override
    public void onTouchNext() {

    }

    @Override
    public void onTouchBack(int flag) {

    }

    public void ThreadHalfSecFun() {
        if(activity !=null && activity.FrgmHandler!=null) {
            //更新UI时间
            Message msg = activity.FrgmHandler.obtainMessage();
            msg.what =MsgID_HW_Remote;
            activity.FrgmHandler.sendMessage(msg);
        }
    }

    public void ThreadOneSecFun() {
        if(activity !=null && activity.FrgmHandler!=null) {
            //更新UI时间
            Message msg = activity.FrgmHandler.obtainMessage();
            msg.what = activity.MsgID_UI_Time;
            msg.obj = AndroidUtils.GetSystemTime();
            activity.FrgmHandler.sendMessage(msg);
        }
    }
}

