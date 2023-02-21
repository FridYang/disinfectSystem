package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.graphics.Bitmap;
import android.graphics.Color;
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
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;
import com.cczk.lxp.disinfectsystem.view.utils.CustomDialog;
import com.cczk.lxp.disinfectsystem.view.utils.WaterView;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pc on 2018/2/7.
 */
public class ReagentFrgm extends Fragment implements  IFrgmCallBack {
    private static final String TAG = "Fragment";
    private MainActivity activity;

    private FrameLayout fram1;
    private FrameLayout fram2;
    private WaterView water_reagent1;
    private WaterView water_reagent2;

    private TextView tv_reagent1;
    private TextView tv_reagent2;
    private TextView tv_info;
    private ImageView img_info;
    private Bitmap bm_info;

    private ImageView img_scan1;
    private ImageView img_scan2;

    //注册NFC卡
    private TextView tv_PostNFC;  // 注册NFC 发送到服务器
    private TextView tv_calibL;   // 校准空桶L
    private TextView tv_calibR;   // 校准空桶R

    //补充溶液流程
    private final int Supply_Idle=0;        // 空闲
    private final int Supply_ScanA=1;      // 等待桶A刷卡
    private final int Supply_CheckA=2;      // 等待桶A检查NFC
    private final int Supply_LinkA=3;       // 等待桶A按确定
    private final int Supply_ScanB=4;      // 等待桶B刷卡
    private final int Supply_CheckB=5;      // 等待桶B检查NFC
    private final int Supply_LinkB=6;       // 等待桶B按确定
    private final int Supply_PostNFC=7;     // 注册NFC

    private int SupplyMode=Supply_Idle;
    public String MarkNFC;

    private int Color_Full= Color.rgb(95,105,160);
    private int Color_Null= Color.rgb(200,10,10);

    //空桶重量
    public static int[] PackWeights=new int[2];
    private boolean isChangeWeight=false;
    private boolean showReagent1=true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_reagent, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();

        //获取记录的语言序号
        try{
            PackWeights[0]=Integer.parseInt( AndroidUtils.QuickDataGet("PackWeightL"));
        }catch (Exception e){}
        try{
            PackWeights[1]=Integer.parseInt( AndroidUtils.QuickDataGet("PackWeightR"));
        }catch (Exception e){}

        return v;
    }

    private void InitView(View v) {
        //控件赋值
        fram1=v.findViewById(R.id.reagent_frame1);
        fram2=v.findViewById(R.id.reagent_frame2);
        water_reagent1=v.findViewById(R.id.reagent_water_reagent1);
        water_reagent2=v.findViewById(R.id.reagent_water_reagent2);

        tv_reagent1=v.findViewById(R.id.reagent_tv_reagent1);
        tv_reagent2=v.findViewById(R.id.reagent_tv_reagent2);
        tv_info=v.findViewById(R.id.reagent_tv_info);
        img_info=v.findViewById(R.id.reagent_img_info);

        //回退按钮
        ImageView img =  v.findViewById(R.id.reagent_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //activity.AllBtnFrgmHome();
                activity.FrgmGoBack();
            }
        });

        //开启门锁按钮
        img =  v.findViewById(R.id.reagent_img_open);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //开启门锁
                ControlUtils.getInstance().SetSwitchLock(true,true);
            }
        });


        //扫描按钮
        img_scan1 =  v.findViewById(R.id.reagent_img_scan1);
        img_scan1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BtnScan(0);
            }
        });
        img_scan2 =  v.findViewById(R.id.reagent_img_scan2);
        img_scan2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BtnScan(1);
            }
        });

        tv_calibL =  v.findViewById(R.id.reagent_tv_calib1);
        tv_calibL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BtnCailb(0);
            }
        });
        tv_calibR =  v.findViewById(R.id.reagent_tv_calib2);
        tv_calibR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BtnCailb(1);
            }
        });

        tv_PostNFC =  v.findViewById(R.id.reagent_tv_postnfc);
        tv_PostNFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BtnPostNFC();
            }
        });

        //便携式单桶
        if(DisinfectData.HwDevCate!=DisinfectData.Dev_Little)
        {
            fram2.setVisibility(View.VISIBLE);
        }else
        {
            fram2.setVisibility(View.GONE);
        }
    }

    @Override
    public void FrgmMessage(Message msg) {
        Object[] data;
        switch (msg.what) {
            //更新UI
            case MainActivity.MsgID_UI_Time:
                int value1=ControlUtils.getInstance().GetWeightRatio(0);
                int value2=ControlUtils.getInstance().GetWeightRatio(1);

                //更新溶液比例
                tv_reagent1.setText(value1+"%");
                tv_reagent2.setText(value2+"%");

                //tv_reagent1.setText(ControlUtils.getInstance().Data_Weights[0]+"");
                //tv_reagent2.setText(ControlUtils.getInstance().Data_Weights[1]+"");

                water_reagent1.ChangeRatio(value1);
                water_reagent2.ChangeRatio(value2);
                if(value1>=10){
                    water_reagent1.ChangeColor(Color_Full);
                }else{
                    water_reagent1.ChangeColor(Color_Null);
                }
                if(value2>=10){
                    water_reagent2.ChangeColor(Color_Full);
                }else{
                    water_reagent2.ChangeColor(Color_Null);
                }

                //更新溶液
                UpDateReagent();
                break;
            //刷卡NFC
            case MainActivity.MsgID_HW_NFCID:
                Log.v(TAG,"NFCID: "+msg.obj+" Mode:"+SupplyMode );
                String Temp_nfc=msg.obj.toString();
                Temp_nfc=Temp_nfc.replace(" ","");

                //刷卡获取NFC卡号
                switch (SupplyMode){
                    case Supply_ScanA:
                        SupplyMode=Supply_CheckA;

                        //发送到服务器检查
                        MarkNFC=Temp_nfc;
                        NetWorkCheckNFC(MarkNFC);
                        break;
                    case Supply_ScanB:
                        SupplyMode=Supply_CheckB;

                        //发送到服务器检查
                        MarkNFC=Temp_nfc;
                        NetWorkCheckNFC(MarkNFC);
                        break;
                    case Supply_PostNFC:
                        MarkNFC=Temp_nfc;
                        tv_PostNFC.setText(MarkNFC);
                        //扫码成功 立刻上传
                        NetWorkPostNFC(MarkNFC);
                        break;
                }
                break;
            //获取NFC是否合法
            case MainActivity.MsgID_Web_CheckNFC:
                try {
                    data= (Object[]) msg.obj;
                    //NFC合法
                    if((boolean)data[0])
                    {
                        /*
                        //消毒剂NFC验证
                        public static final int IS_USEFUL = 0; //可使用
                        public static final int IS_USED = 1; //已使用
                        public static final int UN_LEGAL = 2; //不合法
                        */

                        String msgStr=data[1].toString();
                        int status= (int) data[2];

                        if(status==0) {
                            //接下来等待换完消毒剂 按确定
                            switch (SupplyMode) {
                                case Supply_CheckA:
                                    SupplyMode = Supply_LinkA;

                                    //弹窗二次确认 与 教程动画
                                    ShowLinkOverDialog("请更换桶A消毒剂\r\n "+data[3]);
                                    break;
                                case Supply_CheckB:
                                    SupplyMode = Supply_LinkB;

                                    //弹窗二次确认 与 教程动画
                                    ShowLinkOverDialog("请更换桶B消毒剂\r\n"+data[3]);
                                    break;
                            }
                        }else{
                            switch (SupplyMode) {
                                case Supply_CheckA:
                                    //退回扫描环节
                                    SupplyMode = Supply_ScanA;
                                    break;
                                case Supply_CheckB:
                                    //退回扫描环节
                                    SupplyMode = Supply_ScanB;
                                    break;
                            }
                            activity.MyToast(msgStr);
                        }
                    }else{
                        ChangeModeIdle(AndroidUtils.GetStr(R.string.networkerr));
                        //activity.MyToast("消毒剂异常");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ChangeModeIdle(AndroidUtils.GetStr(R.string.networkerr));
                }
                break;
            //NFC记录成功
            case MainActivity.MsgID_Web_NFCUsed:
                try {
                    data= (Object[]) msg.obj;
                    //NFC合法
                    if((boolean)data[0])
                    {
                        //接下来等待换完消毒剂 按确定
                        //保存数据
                        Map<String, String> sets=new HashMap<>();
                        switch (SupplyMode) {
                            case Supply_LinkA:
                                //替换成功
                                //activity.MyToast(data[1].toString());
                                //ChangeModeIdle(AndroidUtils.GetStr(R.string.registerstr)+1);
                                ChangeModeIdle(activity.bm_txtOpen);

                                DisinfectData.ReagentWeights[0]=100;
                                sets.put("ReagentWeightA",String.valueOf(DisinfectData.ReagentWeights[0]));
                                DisinfectData.SetReagentSuffice(true,DisinfectData.ReagentSceneNeed);
                                break;
                            case Supply_LinkB:
                                //替换成功
                                //activity.MyToast(data[1].toString());
                                //ChangeModeIdle(AndroidUtils.GetStr(R.string.registerstr)+2);
                                ChangeModeIdle(activity.bm_txtOpen);

                                DisinfectData.ReagentWeights[1]=100;
                                sets.put("ReagentWeightB",String.valueOf(DisinfectData.ReagentWeights[1]));
                                DisinfectData.SetReagentSuffice(true,DisinfectData.ReagentSceneNeed);
                                break;
                        }
                        AndroidUtils.QuickDataSet(sets);
                    }else{
                        //activity.MyToast(data[1].toString());
                        ChangeModeIdle(data[1].toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ChangeModeIdle(AndroidUtils.GetStr(R.string.networkerr));
                }
                if(SupplyMode!=Supply_Idle){
                    ChangeModeIdle(AndroidUtils.GetStr(R.string.networkerr));
                }
                break;
            //NFC录入成功
            case MainActivity.MsgID_Web_PostNFC:
                try {
                    data= (Object[]) msg.obj;

                    activity.MyToast(data[1].toString());

                    MarkNFC="";
                    tv_PostNFC.setText(AndroidUtils.GetStr(R.string.setnewcard));
                } catch (Exception e) {
                    e.printStackTrace();
                    ChangeModeIdle(AndroidUtils.GetStr(R.string.networkerr));
                }
                break;
        }
    }

    @Override
    public void InitData() {
        //NFC
        activity.NFCOpen();

        if(DisinfectData.GetReagentSuffice())
        {
            //溶液正常
            //ChangeModeIdle(AndroidUtils.GetStr(R.string.reagentShow));
        }else
        {
            int value1=ControlUtils.getInstance().GetWeightRatio(0);
            int value2=ControlUtils.getInstance().GetWeightRatio(1);
            boolean isLess=false;
            if(DisinfectData.HwDevCate==DisinfectData.Dev_Little)
            {
                //单桶
                isLess = value1<=3;
            }else {
                //双桶
                isLess = (value1<=3 || value2 <=3);
            }

            if(isLess)
            {
                //溶液太少
                if(DisinfectData.HwDevCate==DisinfectData.Dev_Little)
                {
                    //ChangeModeIdle("消毒液不足，请点击按键进行更换");
                    ChangeModeIdle(activity.bm_txtLessScen);
                }else
                    {
                        if(value1<=3 && value2 <=3)
                        {
                            ChangeModeIdle(activity.bm_txtLessAB);
                        }else if(value1<=3)
                        {
                            ChangeModeIdle(activity.bm_txtLessA);
                        }else if(value2<=3)
                        {
                            ChangeModeIdle(activity.bm_txtLessB);
                        }
                }
            }else
                {
                //场景太大
                //ChangeModeIdle("剩余消毒液不能完成本次消毒\r\n请更换场景或更换消毒液");
                    ChangeModeIdle(activity.bm_txtLessScen);
            }
        }

        //如果用户是管理员 开启录入
        if(DisinfectData.UserIsAdmin){
            tv_PostNFC.setVisibility(View.VISIBLE);
            tv_calibL.setVisibility(View.VISIBLE);
            tv_calibR.setVisibility(View.VISIBLE);
        }else{
            tv_PostNFC.setVisibility(View.GONE);
            tv_calibL.setVisibility(View.GONE);
            tv_calibR.setVisibility(View.GONE);
        }

        Log.d(TAG, "InitView: "+HttpUtils.IsOnLine);
        if(HttpUtils.IsOnLine)
        {
            Log.d(TAG, "InitView: Show");
            img_scan1.setVisibility(View.VISIBLE);
            img_scan2.setVisibility(View.VISIBLE);
        }else
        {
            Log.d(TAG, "InitView: Hide");
            img_scan1.setVisibility(View.GONE);
            img_scan2.setVisibility(View.GONE);
        }

        //刷新UI控件
        ThreadOneSecFun();

        isChangeWeight=false;
    }

    public void ChangeModeIdle(String str){
        Log.d(TAG, "ChangeModeIdle: "+str);
        SupplyMode=Supply_Idle;
        tv_info.setText(str);
        //img_info.setImageBitmap(MainActivity.Instance.bm_txtLessA);

        tv_info.setVisibility(View.VISIBLE);
        img_info.setVisibility(View.GONE);
    }

    public void ChangeModeIdle(Bitmap bmp){
        Log.d(TAG, "ChangeModeIdle: "+bmp.toString());
        SupplyMode=Supply_Idle;
        //tv_info.setText(str);
        bm_info=bmp;
        img_info.setImageBitmap(bm_info);

        tv_info.setVisibility(View.GONE);
        img_info.setVisibility(View.VISIBLE);
    }

    //录入新卡
    public void BtnPostNFC(){
        SupplyMode = Supply_PostNFC;
        MarkNFC="";

        tv_PostNFC.setText(AndroidUtils.GetStr(R.string.setnewcard));
    }
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            InitData();
        }else{
            activity.NFCClose();

            if(isChangeWeight)
            {
                //保存缓存数据
                Map<String, String> params = new HashMap<>();
                params.put("PackWeightL", String.valueOf(PackWeights[0]));
                params.put("PackWeightR", String.valueOf(PackWeights[1]));
                AndroidUtils.QuickDataSet(params);
            }
        }
    }

    public void BtnScan(int no){
        MarkNFC="";
        switch (no){
            case 0:
                SupplyMode=Supply_ScanA;
                break;
            case 1:
                SupplyMode=Supply_ScanB;
                break;
        }
        tv_info.setText(AndroidUtils.GetStr(R.string.scenlabel));
        bm_info=activity.bm_txtScanning;
        img_info.setImageBitmap(bm_info);
    }

    public void BtnCailb(int no){
        isChangeWeight=true;
        PackWeights[no]=ControlUtils.getInstance().Data_Weights[no];
        activity.MyToast("UpDate  "+ControlUtils.getInstance().Data_Weights[no]);
    }

    @Override
    public void onTouchNext() {

    }

    @Override
    public void onTouchBack(int flag) {
        activity.OtherFrgmComeMain();
    }

    public void ThreadOneSecFun() {
        if(activity !=null && activity.FrgmHandler!=null) {
            //更新UI时间
            Message msg = activity.FrgmHandler.obtainMessage();
            msg.what = activity.MsgID_UI_Time;
            activity.FrgmHandler.sendMessage(msg);
        }
    }

    public void NetWorkCheckNFC(final String nfc){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/disin/nfc/active";
                    Map<String, String> params=new HashMap<>();
                    params.put("nfc",nfc);
                    String result= HttpUtils.PostData(url,params);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_CheckNFC;
                    Object[] msgData=new Object[4];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data="";
                    if(code==0){
                        //{"msg":"SUCCESS，LOGIN","code":0,"token":"a79bc65a-3b3f-42ca-8fbb-764a184505b5"}

                        //成功
                        msgData[0]=true;

                        JSONObject joData=jsonObject.getJSONObject("data");
                        msgData[1]=joData.getString("msg");
                        msgData[2]=joData.getInt("status");
                        msgData[3]=nfc;
                        Log.v(TAG,data);
                    }else{
                        //失败
                        msgData[0]=false;
                        data= jsonObject.getString("msg");
                        msgData[1]=data;
                        Log.v(TAG,data);
                    }

                    msg.obj=msgData;
                    activity.FrgmHandler.sendMessage(msg);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    //确认消毒剂已被使用
    public void NetWorkNFCUsed(final String mac,final String nfc){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/disin/nfc/used";
                    Map<String, String> params=new HashMap<>();
                    params.put("macAdd",mac);
                    params.put("nfc",nfc);
                    String result= HttpUtils.PostData(url,params);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_NFCUsed;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==0){
                        //{"msg":"SUCCESS，LOGIN","code":0,"token":"a79bc65a-3b3f-42ca-8fbb-764a184505b5"}

                        //成功
                        msgData[0]=true;
                        data= jsonObject.getString("data");
                        msgData[1]=data;
                        Log.v(TAG,data);
                    }else{
                        //失败
                        msgData[0]=false;
                        msgData[1]=data;
                        Log.v(TAG,data);
                    }

                    msg.obj=msgData;
                    activity.FrgmHandler.sendMessage(msg);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    //网络注册NFC编号
    private void NetWorkPostNFC(final String nfc) {
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/disin/nfc/create";
                    Map<String, String> params=new HashMap<>();
                    params.put("nfc",nfc);
                    String result= HttpUtils.PostData(url,params);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_PostNFC;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==0){
                        data= jsonObject.getString("data");
                        //成功
                        msgData[0]=true;
                        msgData[1]=data;
                        Log.v(TAG,data);
                    }else{
                        //失败
                        msgData[0]=false;
                        msgData[1]=data;
                        Log.v(TAG,data);
                    }

                    msg.obj=msgData;
                    activity.FrgmHandler.sendMessage(msg);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    //AlertDialog dialog_LinkOver;
    //等待换消毒剂窗口
    public void ShowLinkOverDialog(String data){
        /*
        dialog_LinkOver = new AlertDialog.Builder(activity)
                .setTitle("消毒剂补充")
                .setMessage(data)
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("补充完成", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MainActivity.Instance.MyToast("补充完成");
                        //服务器发送添加完毕
                        NetWorkNFCUsed(AndroidUtils.GetMacAddress(), MarkNFC);
                        dialog_LinkOver.hide();
                    }
                })
                .setNegativeButton("取消补充", new DialogInterface.OnClickListener() {//添加取消
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ChangeModeIdle("消毒液可正常使用");
                        dialog_LinkOver.hide();
                    }
                })
                .create();
        dialog_LinkOver.setCancelable(false);
        dialog_LinkOver.show();
*/

        View.OnClickListener onConfimClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.Instance.MyToast("补充完成");
                //服务器发送添加完毕
                NetWorkNFCUsed(AndroidUtils.GetMacAddress(), MarkNFC);
            }
        };

        CustomDialog.Builder builder = new CustomDialog.Builder(MainActivity.Instance);
        builder.setTitle(data);
        builder.setButtonConfirm( "补充完成", onConfimClickListener);
        builder.setButtonCancel( "取消补充", null);

        CustomDialog customDialog = builder.create();
        customDialog.show();
    }

    //更新溶液显示
    public void UpDateReagent()
    {
        //显示溶液
        if(DisinfectData.GetReagentSuffice())
        {
            img_scan1.setImageBitmap(activity.bm_refresh1);
            img_scan2.setImageBitmap(activity.bm_refresh1);
            tv_info.setTextColor(activity.color_tv);
            img_info.setVisibility(View.INVISIBLE);
        }else
        {
            DisinfectData.SetReagentSuffice(true,DisinfectData.ReagentSceneNeed);

            img_scan1.setImageBitmap(activity.bm_refresh2);
            img_scan2.setImageBitmap(activity.bm_refresh2);
            //溶液不足
            Bitmap bm_temp=bm_info;
            if(showReagent1)
            {
                tv_info.setTextColor(activity.color_tv);
                if(bm_info==activity.bm_txtLessA)
                {
                    bm_info=activity.bm_txtLessAr;
                }else if(bm_info==activity.bm_txtLessB)
                {
                    bm_info=activity.bm_txtLessBr;
                }else if(bm_info==activity.bm_txtLessAB)
                {
                    bm_info=activity.bm_txtLessABr;
                }else if(bm_info==activity.bm_txtLessScen)
                {
                    bm_info=activity.bm_txtLessScenr;
                }
            }else{
                tv_info.setTextColor(activity.color_tvLess);
                if(bm_info==activity.bm_txtLessAr)
                {
                    bm_info=activity.bm_txtLessA;
                }else if(bm_info==activity.bm_txtLessBr)
                {
                    bm_info=activity.bm_txtLessB;
                }else if(bm_info==activity.bm_txtLessABr)
                {
                    bm_info=activity.bm_txtLessAB;
                }else if(bm_info==activity.bm_txtLessScenr)
                {
                    bm_info=activity.bm_txtLessScen;
                }
            }

            if(bm_temp!=bm_info)
            {
                if(img_info.getVisibility()!=View.VISIBLE)
                {
                    img_info.setVisibility(View.VISIBLE);
                }
                img_info.setImageBitmap(bm_info);
            }
            showReagent1=!showReagent1;
        }
    }
}

