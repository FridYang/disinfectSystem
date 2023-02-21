package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.SocketUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;
import com.cczk.lxp.disinfectsystem.view.utils.CustomDialog;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pc on 2018/2/7.
 */
public class SwitchFrgm extends Fragment implements IFrgmCallBack {
    private static final String TAG = "SwitchFrgm";
    private MainActivity activity;

    private ImageView img_nowstart;
    private ImageView img_phonestart;

    private Bitmap bm_nowstart;
    private Bitmap bm_phonestart;

    CustomDialog customDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_switch, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {

        img_nowstart=v.findViewById(R.id.switch_img_nowstart);
        img_phonestart=v.findViewById(R.id.switch_img_phonestart);

        img_nowstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtnNowStartFun();
            }
        });
        img_phonestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtnPhoneStartFun();
            }
        });

        //离开页面
        ImageView img=v.findViewById(R.id.switch_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.AllBtnFrgmHome();
            }
        });
    }

    @Override
    public void InitData() {
    }

    public void BtnNowStartFun()
    {
        //判断预约时间
        if(DisinfectData.planItem.planTime<=0)
        {
            //向服务器发送开启任务数据
            SocketUtils.getInstance().SendStartDisinfect();
            activity.ComeGoFrame(MainActivity.flag_leave);
        }else{
            int plan=DisinfectData.planItem.planTime;
            int hour =(int)(plan / 3600f);
            plan -= hour * 3600;
            int min = (int)(plan / 60f);
            plan -= min * 60;
            int sec = plan;
            String str="任务将在 ";
            str+=String.format("%02d",hour)+"点"+String.format("%02d",min);
            str+="分 开始";
            ShowPlanDialog(str);
            activity.AddPlanTimeMission(DisinfectData.planItem.planTime);
        }
    }

    public void BtnPhoneStartFun(){
        //ShowSuccessDialog("房间大小：300m³ , 使用消毒剂：消毒剂1");

        //向服务器发送开启任务数据
        SocketUtils.getInstance().SendStartDisinfect();

        activity.leaveFrgm.RemoteStart();
        activity.ComeGoFrame(MainActivity.flag_leave);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            InitData();
        }

        if(customDialog!=null)
        {
            customDialog.dismiss();
        }
    }

    @Override
    public void onTouchNext() {
        BtnNowStartFun();
    }

    @Override
    public void onTouchBack(int flag) {

    }

    @Override
    public void FrgmMessage(Message msg) {

    }

//    public void ShowSuccessDialog(String data){
//        CustomDialog.Builder builder = new CustomDialog.Builder(activity);
//        builder.setTitle("等待遥控开启消毒任务\r\n"+data);
//        builder.hideButtonCancel();
//        builder.setButtonConfirm("取消任务", new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                activity.AllBtnFrgmHome();
//            }
//        });
//
//        CustomDialog customDialog = builder.create();
//        customDialog.show();
//    }

    public void ShowPlanDialog(String data){
        CustomDialog.Builder builder = new CustomDialog.Builder(activity);
        builder.setTitle("等待预约任务\r\n"+data);
        builder.hideButtonCancel();
        builder.setButtonConfirm("取消任务", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DisinfectData.planTimeStr="";
                //保存缓存数据
                Map<String, String> params=new HashMap<>();
                params.put("planTimeStr",DisinfectData.planTimeStr);
                AndroidUtils.QuickDataSet(params);

                activity.AllBtnFrgmHome();
            }
        });

        customDialog = builder.create();
        customDialog.show();
    }
}

