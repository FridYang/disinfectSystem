package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
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
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.base.SocketUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.CarPlanUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.SensortUtils1;
import com.cczk.lxp.disinfectsystem.utils.hw.heat.HeatUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by pc on 2018/2/7.
 */
public class RunFrgm extends Fragment implements IFrgmCallBack {
    private static final String TAG = "RunFrgm";
    private MainActivity activity;
    //true表示正在工作
    public static boolean timeCanRun=true;
    private boolean IsWebControlType =false;
    //等会显示的状态
    private boolean LaterControlStart =true;

    //热成像有人
    public boolean IsHeatCheck=false;
    //上一个热成像状态
    private boolean LastHeatCheck=false;

    //需要立刻打开硬件
    public boolean NeedStartHW=false;

    private boolean isCheckRemote=true;
    private final int MsgID_HW_Remote=7001;

    TextView tv_time;
    ImageView img_state;
    ImageView img_now;
    ImageView img_new;
    ImageView img_bg;

    public int timeCnt;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_run, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        //获取控件
        tv_time =v.findViewById(R.id.run_tv_time);
        img_state =v.findViewById(R.id.run_img_state);

        //点击停止按钮
        img_state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RunStateOnClick(true,false);
            }
        });
        //回退按钮
        ImageView img=v.findViewById(R.id.run_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(timeCanRun) {
//                    RunStateOnClick(true);
//                }

                activity.DisinfectBtnFrgmHome();
                //暂停位置
                if(DisinfectData.IsDevCateCar) {
                    CarPlanUtils.getInstance().CarStop();
                }

            }
        });

        img_now =v.findViewById(R.id.run_img_testNow);
        img_now.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CarPlanUtils.getInstance().CarMoveNow();
                //ControlUtils.getInstance().FunStartDisinfect();

                //IsHeatCheck=!IsHeatCheck;

                //activity.WebApkUpDate(1);
            }
        });

        img_new =v.findViewById(R.id.run_img_testNew);
        img_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CarPlanUtils.getInstance().CarMove();
            }
        });
        img_now.setVisibility(View.GONE);
        img_new.setVisibility(View.GONE);

        img_bg =v.findViewById(R.id.run_img_bg);
        img_bg.setImageBitmap(activity.bm_pandaBg);
        if(DisinfectData.HwDevCate==DisinfectData.Dev_Pretty)
        {
            img_bg.setImageBitmap(activity.bm_prettyBg);
        }
        if(DisinfectData.HwDevCate==DisinfectData.Dev_Little)
        {
            img_bg.setImageBitmap(activity.bm_littleBg);
        }
    }

    @Override
    public void FrgmMessage(Message msg) {
        switch (msg.what){
            case MainActivity.MsgID_UI_Time:
                //普通类型
                if(!DisinfectData.IsCarControl)
                {
                    //撤离时间
                    if(timeCnt >0){
                        if(timeCanRun) {
                            //时间减少
                            timeCnt--;
                        }
                    }else{
                        //时间归零
                        //ChangeSubFlag(flag_sub_run);

                        //停止硬件
                        ControlUtils.getInstance().FunStopDisinfect();

                            //服务器发送终端停止任务
                            DisinfectData.planItem.NowRunMode = PlanItem.RunMode_Idle;
                            SocketUtils.getInstance().SendStopDisinfect();

                            activity.ComeGoFrame(MainActivity.flag_end);
                    }
                    tv_time.setText(AndroidUtils.UIGetTimeStr(timeCnt));
                }else
                {
                    //底盘模式
                    tv_time.setText(AndroidUtils.GetStr(R.string.atwork));

                    //定点模式
                    if(DisinfectData.IsCarPointMode &&
                       CarPlanUtils.getInstance().Mode == CarPlanUtils.Mode_Wait)
                    {
                        //如果在消毒中
                        if (timeCanRun) {
                            if (timeCnt > 0) {
                                //时间减少
                                timeCnt--;
                                tv_time.setText(AndroidUtils.UIGetTimeStr(timeCnt));
                            }
                        }
                    }

                    //移动模式
                    if(!DisinfectData.IsCarPointMode &&
                        timeCanRun)
                    {
                        tv_time.setText(AndroidUtils.GetStr(R.string.movework));
                    }
                }

                //如果为热成像类型
                if(DisinfectData.IsDevHeatCheck)
                {
                    if(timeCanRun)
                    {

                        if(HeatUtils.getInstance().GetNowClearScene())
                        {
                            //无人
                            IsHeatCheck=false;
                        }else{
                            //CarPlanUtils.getInstance().AddInfoList("变有人 "+HeatUtils.getInstance().ClearSceneCnt);
                            //有人
                            IsHeatCheck=true;
                        }

                        //如果是定点模式
                        if(DisinfectData.IsCarPointMode)
                        {
                            //从无人变为有人
                            if (!LastHeatCheck) {
                                if (IsHeatCheck) {
                                    CarPlanUtils.getInstance().AddInfoList("无人变有人 暂停工作");
                                    LastHeatCheck = IsHeatCheck;
                                    //有人 暂停工作与移动
                                    ControlUtils.getInstance().FunStopDisinfect();
                                    CarPlanUtils.getInstance().CarStop();
                                }
                            } else
                            //从有人变为无人
                            {
                                if (!IsHeatCheck) {
                                    LastHeatCheck = IsHeatCheck;
                                    //无人 开启工作或移动
                                    if (timeCnt > 0) {
                                        //刚才在消毒
                                        //开启硬件
                                        ControlUtils.getInstance().FunStartDisinfect();

                                        //重新赋值时间
                                        int time = CarPlanUtils.getInstance().WaitTimeCnt;
                                        if (time <= 0) {
                                            //刚才在移动
                                            CarPlanUtils.getInstance().CarMove();
                                        }
                                        timeCnt = time;

                                        CarPlanUtils.getInstance().Mode = CarPlanUtils.getInstance().Mode_Wait;

                                        CarPlanUtils.getInstance().AddInfoList("有人变无人 开始消毒");
                                    } else {
                                        //刚才在移动
                                        new Thread() {
                                            @Override
                                            public void run() {
                                                //延时片刻
                                                try {
                                                    Thread.sleep(200);
                                                    CarPlanUtils.getInstance().CarMove();
                                                } catch (Exception e) {
                                                }
                                            }
                                        }.start();

                                        CarPlanUtils.getInstance().AddInfoList("有人变无人 开始移动");
                                    }
                                }
                            }
                        }else{
                            //持续工作模式

                            if (!LastHeatCheck)
                            {
                                //从无人变为有人
                                if (IsHeatCheck)
                                {
                                    LastHeatCheck = IsHeatCheck;

                                    CarPlanUtils.getInstance().AddInfoList("无人变有人 暂停工作");
                                    LastHeatCheck = IsHeatCheck;
                                    //有人 暂停工作与移动
                                    ControlUtils.getInstance().FunStopDisinfect();
                                    CarPlanUtils.getInstance().CarStop();
                                }
                            }else
                            {
                                //从有人变为无人
                                if (!IsHeatCheck)
                                {
                                    CarPlanUtils.getInstance().AddInfoList("有人变无人 开启标识:"+CarPlanUtils.getInstance().WaitTimeCnt);

                                    LastHeatCheck = IsHeatCheck;
                                    CarPlanUtils.getInstance().CarMove();
                                    //大于0说明要启动
                                    if(CarPlanUtils.getInstance().WaitTimeCnt>0)
                                    {
                                        CarPlanUtils.getInstance().AddInfoList("开启硬件");
                                        //开启硬件
                                        ControlUtils.getInstance().FunStartDisinfect();
                                    }else{
                                        CarPlanUtils.getInstance().AddInfoList("暂停硬件");
                                        //暂停硬件
                                        ControlUtils.getInstance().FunStopDisinfect();
                                    }
                                }
                            }

                        }
                    }
                }
                break;
            case MsgID_HW_Remote:
                //获得遥控器信号
                Log.d(TAG, "MsgID_HW_Remote: Check "+isCheckRemote+"  Data:"+ControlUtils.getInstance().GetRetomeData());
                if(isCheckRemote)
                {
                    if(ControlUtils.getInstance().GetRetomeData())
                    {
                        isCheckRemote=false;
                        RunStateOnClick(true,true);
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
        //机器人不发送传感器数据
        if(!DisinfectData.IsCarControl) {
            SocketUtils.getInstance().IsAddSensort = true;
        }

        //不能是 底盘类型 与 热成像类型
        if(!DisinfectData.IsDevCateCar &&
           !DisinfectData.IsDevHeatCheck)
        {
            if (NeedStartHW) {
                //开启硬件
                ControlUtils.getInstance().FunStartDisinfect();
            }
        }

        img_state.setImageBitmap(activity.bm_BtnStop);

        //运行时间
        //喷洒时间 + 增强时间
        timeCnt =ParmFrgm.time_spray + ParmFrgm.time_streng;
        tv_time.setText(AndroidUtils.GetStr(R.string.atwork));
        timeCanRun=true;

        if(DisinfectData.IsDevCateCar)
        {
            if(timeCnt==0)
            {
                tv_time.setText(AndroidUtils.GetStr(R.string.atwork));
            }
        }

        //初始化浓度时间累计
        SocketUtils.getInstance().SensortDataInit();

        if(IsWebControlType)
        {
            timeCanRun=LaterControlStart;
            RunStateOnClick(false,false);
            IsWebControlType = false;
        }

        //黄色工作灯
        ControlUtils.getInstance().OpenRGBYellow();
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

    public void RunStateOnClick(boolean isLocal,boolean isRemote){
        if(!DisinfectData.IsCarControl)
        {
            if(timeCanRun){
                timeCanRun=false;
                img_state.setImageBitmap(activity.bm_BtnPlay);

                if(!isRemote) {
                    //停止硬件
                    ControlUtils.getInstance().FunStopDisinfect();
                }else{
                    //遥控器停止硬件
                    ControlUtils.getInstance().FunRemoteStopDisinfect();
                }

                if(isLocal) {
                    //向服务器发送 线下停止
                    DisinfectData.planItem.NowRunMode = PlanItem.RunMode_Spray;
                    SocketUtils.getInstance().SendStopDisinfect();
                }
            }else{
                timeCanRun=true;
                img_state.setImageBitmap(activity.bm_BtnStop);

                //开启硬件
                ControlUtils.getInstance().FunStartDisinfect();

                if(isLocal) {
                    //向服务器发送 线下开启
                    DisinfectData.planItem.sprayTime = timeCnt;
                    DisinfectData.planItem.NowRunMode = PlanItem.RunMode_Spray;
                    SocketUtils.getInstance().SendStartDisinfect();
                }
            }
        }else
        {
            //机器人模式

            //切换状态
            if(timeCanRun){
                timeCanRun=false;
                img_state.setImageBitmap(activity.bm_BtnPlay);

                if(!isRemote) {
                    //停止硬件
                    ControlUtils.getInstance().FunStopDisinfect();
                }else{
                    //遥控器停止硬件
                    ControlUtils.getInstance().FunRemoteStopDisinfect();
                }

                if(isLocal) {
                    //终端暂停机器人任务
                    //向服务器发送 线下停止
                    DisinfectData.planItem.NowRunMode = PlanItem.RunMode_CarPlanStop;
                    SocketUtils.getInstance().SendStopDisinfect();
                }

                CarPlanUtils.getInstance().CarStop();
            }else{
                timeCanRun=true;
                img_state.setImageBitmap(activity.bm_BtnStop);

                if(isLocal) {
                    //终端启动机器人任务
                    int reagentId = DisinfectData.planItem.typeId;
                    int ratiodId = DisinfectData.planItem.ratioValue;
                    int userId = DisinfectData.planItem.userId;
                    int mapId = CarPlanUtils.getInstance().NowMapId;
                    SocketUtils.getInstance().SendLocalCarControl(reagentId, ratiodId, userId, mapId);
                }

                if(!DisinfectData.IsDevHeatCheck)
                {
                    //检查运行模式
                    //混合模式变状态
                    if(DisinfectData.CarWorkMode==2)
                    {
                        DisinfectData.IsCarPointMode = CarPlanUtils.tempPointMode;
                    }

                    //如果不附带热成像
                    String show="点击运行 ";
                    if (timeCnt > 0)
                    {
                        //刚才在消毒
                        //开启硬件
                        ControlUtils.getInstance().FunStartDisinfect();

                        //重新赋值时间
                        int time = CarPlanUtils.getInstance().WaitTimeCnt;
                        timeCnt = time;
                        if(DisinfectData.IsCarPointMode)
                        {
                            if (time <= 0) {
                                //刚才在移动
                                CarPlanUtils.getInstance().CarMove();
                            }
                        }else{
                            //移动模式一定要移动
                            CarPlanUtils.getInstance().CarMove();
                        }

                        //向服务器发送 线下开启
                        DisinfectData.planItem.sprayTime = timeCnt;
                        DisinfectData.planItem.NowRunMode = PlanItem.RunMode_Spray;
                        SocketUtils.getInstance().SendStartDisinfect();

                        CarPlanUtils.getInstance().Mode = CarPlanUtils.getInstance().Mode_Wait;
                    } else {
                        //刚才在移动
                        show+="移动";
                        CarPlanUtils.getInstance().CarMove();
                    }
                    CarPlanUtils.getInstance().AddInfoList(show);
                }else{
                    //附带热成像
                   //等时间判断是否需要打开
                    if(DisinfectData.IsCarPointMode)
                    {
                        //定点模式
                        //默认有人 暂停工作
                        LastHeatCheck=true;
                        ControlUtils.getInstance().FunStopDisinfect();
                        CarPlanUtils.getInstance().CarStop();
                    }else{
                        //持续模式
                        // 默认有人 暂停工作
                        LastHeatCheck=true;
                        ControlUtils.getInstance().FunStopDisinfect();
                        CarPlanUtils.getInstance().CarStop();
                    }
                }


            }
        }
    }

    public void WebControlStart(){
        IsWebControlType=true;
        //等会界面显示暂停
        LaterControlStart =false;

        if(DisinfectData.IsDevCateCar){
            ParmFrgm.time_spray = CarPlanUtils.getInstance().WaitTimeCnt;
            ParmFrgm.time_streng=0;
        }
    }

    public void WebControlStop(){
        IsWebControlType=true;
        //等会界面显示启动
        LaterControlStart =true;

        if(DisinfectData.IsDevCateCar){
            ParmFrgm.time_spray = CarPlanUtils.getInstance().WaitTimeCnt;
            ParmFrgm.time_streng=0;
        }
    }

    public void CarStartDisinfect(int time){
        timeCnt=time;
        tv_time.setText(AndroidUtils.GetStr(R.string.atwork));
        ControlUtils.getInstance().FunStartDisinfect();

        //向服务器发送 线下开启
        DisinfectData.planItem.sprayTime = timeCnt;
        DisinfectData.planItem.NowRunMode = PlanItem.RunMode_Spray;
        SocketUtils.getInstance().SendStartDisinfect();
    }

    public void CarStopDisinfect(){
        ControlUtils.getInstance().FunStopDisinfect();

        //向服务器发送 线下停止
        DisinfectData.planItem.NowRunMode = PlanItem.RunMode_Spray;
        SocketUtils.getInstance().SendStopDisinfect();
    }

    @Override
    public void onTouchNext() {

    }

    @Override
    public void onTouchBack(int flag) {

    }

    int TimeAdd=0;
    void SendLocalSensort()
    {
        TimeAdd+=5;
        try {
            JSONObject item=new JSONObject();
            item.put("time",TimeAdd);
            item.put("value",SensortUtils1.Data_Value);
            DisinfectData.localSensort.put(item);
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
        //更新UI时间
        Message msg = activity.FrgmHandler.obtainMessage();
        msg.what = activity.MsgID_UI_Time;
        msg.obj = AndroidUtils.GetSystemTime();
        activity.FrgmHandler.sendMessage(msg);

        if(DisinfectData.IsDevHeatCheck) {
            HeatUtils.getInstance().UIThreadOneSecFun();
        }
    }

    //每5秒运行
    public void ThreadFiveSecFun()
    {
            SendLocalSensort();
    }
}

