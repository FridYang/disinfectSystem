package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Debug;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.ParmItem;
import com.cczk.lxp.disinfectsystem.bean.PlanItem;
import com.cczk.lxp.disinfectsystem.bean.ScensItem;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pc on 2018/2/7.
 */
public class ParmFrgm extends Fragment implements IFrgmCallBack {
    private static final String TAG = "ParmFrgm";
    private MainActivity activity;

    //View UI
    FrameLayout layout_parent;
    //长宽高体积
    EditText edit_length;
    EditText edit_width;
    EditText edit_height;
    EditText edit_size;

    //浓度
    EditText edit_ratio;
    Button btn_ratio1;
    Button btn_ratio2;
    Button btn_ratio3;
    FrameLayout layout_ratio3;

    //消毒剂种类
    Button btn_type1;
    Button btn_type2;

    //喷洒时间
    EditText edit_sprayT_hour;
    EditText edit_sprayT_minute;
    EditText edit_sprayT_second;

    //增强时间
    EditText edit_strengT_hour;
    EditText edit_strengT_minute;
    EditText edit_strengT_second;

    //消毒时间
    EditText edit_idleT_hour;
    EditText edit_idleT_minute;
    EditText edit_idleT_second;

    //撤离时间
    EditText edit_leaveT_hour;
    EditText edit_leaveT_minute;
    EditText edit_leaveT_second;

    //预约时间
    public EditText edit_planT_hour;
    public EditText edit_planT_minute;
    EditText edit_planT_second;

    private ImageView img_reagent;
    private boolean showReagent1=true;

    //消毒任务所需要的溶剂量
    public int PlanDose=0;
    public static int time_leave;
    public static int time_spray;
    public static int time_streng;
    public static int time_idle;
    public int time_plan;

    //场景ID
    public  int sceneId;
    public  int roomId;
    private ScensItem scensItem;
    private  int selectNo_ratio;
    private  int selectNo_type;

    int color_btnSelect;
    int color_btnNotSelect;

    //自定义模式
    public boolean isCustomMode = false;
    ImageView iv_CustomReset;
    //网格修改模式
    public boolean isGridMode = false;

    private LinearLayout layoutTopTitle;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_parm, container, false);
        activity = (MainActivity) this.getActivity();

        InitView(v);
        InitData();
        layout_parent.clearFocus();
        layout_parent.findFocus();
        return v;
    }

    @Override
    public void FrgmMessage(Message msg) {
        int tempA,tempB,tempC,tempD;
        switch (msg.what){
            case MainActivity.MsgID_UI_Time:
                //计算体积
                tempA= GetNumFloat(edit_length.getText().toString(),1,99);
                tempB= GetNumFloat(edit_width.getText().toString(),1,99);
                tempC= GetNumFloat(edit_height.getText().toString(),1,99);
                tempD=tempA*tempB*tempC;
                edit_size.setText(MyFloatToStr(tempD));

                if(!isCustomMode)
                {
                    //计算喷洒时间
                    int[] time = CalcSprayTime();
                    edit_sprayT_hour.setText(String.format("%02d", time[0]));
                    edit_sprayT_minute.setText(String.format("%02d", time[1]));
                    edit_sprayT_second.setText(String.format("%02d", time[2]));
                }

                CheckReagent();
                UpDateReagent();
                break;
        }
    }

    private void InitView(View v) {
        layout_parent=v.findViewById(R.id.parm_layout_parent);
        layoutTopTitle=v.findViewById(R.id.parm_layout_topTitle);

        //长宽高体积
        edit_length= InitMyNumEdit(v,R.id.parm_edit_length);
        edit_width= InitMyNumEdit(v,R.id.parm_edit_width);
        edit_height= InitMyNumEdit(v,R.id.parm_edit_height);
        edit_size =v.findViewById(R.id.parm_edit_size);

        //浓度
        btn_ratio1=v.findViewById(R.id.parm_btn_ratio1);
        btn_ratio2=v.findViewById(R.id.parm_btn_ratio2);
        btn_ratio3=v.findViewById(R.id.parm_btn_ratio3);
        layout_ratio3=v.findViewById(R.id.parm_layout_ratio3);
        edit_ratio=v.findViewById(R.id.parm_edit_ratio);

        //消毒剂种类
        btn_type1=v.findViewById(R.id.parm_btn_type1);
        btn_type2=v.findViewById(R.id.parm_btn_type2);

        //喷洒时间
        edit_sprayT_hour= InitMyTimeEdit(v,R.id.parm_edit_sprayT_hour);
        edit_sprayT_minute= InitMyTimeEdit(v,R.id.parm_edit_sprayT_minute);
        edit_sprayT_second= InitMyTimeEdit(v,R.id.parm_edit_sprayT_second);

        //增强时间
        edit_strengT_hour= InitMyTimeEdit(v,R.id.parm_edit_strengT_hour);
        edit_strengT_minute= InitMyTimeEdit(v,R.id.parm_edit_strengT_minute);
        edit_strengT_second= InitMyTimeEdit(v,R.id.parm_edit_strengT_second);

        //消毒时间
        edit_idleT_hour= InitMyTimeEdit(v,R.id.parm_edit_idleT_hour);
        edit_idleT_minute= InitMyTimeEdit(v,R.id.parm_edit_idleT_minute);
        edit_idleT_second= InitMyTimeEdit(v,R.id.parm_edit_idleT_second);

        //撤离时间
        edit_leaveT_hour= InitMyTimeEdit(v,R.id.parm_edit_leaveT_hour);
        edit_leaveT_minute= InitMyTimeEdit(v,R.id.parm_edit_leaveT_minute);
        edit_leaveT_second= InitMyTimeEdit(v,R.id.parm_edit_leaveT_second);

        //预约时间
        edit_planT_hour= InitMyTimeEdit(v,R.id.parm_edit_planT_hour);
        edit_planT_minute= InitMyTimeEdit(v,R.id.parm_edit_planT_minute);
        edit_planT_second= InitMyTimeEdit(v,R.id.parm_edit_planT_second);

        //空间
        edit_length.setOnFocusChangeListener(tvFocusLsn);
        edit_width.setOnFocusChangeListener(tvFocusLsn);
        edit_height.setOnFocusChangeListener(tvFocusLsn);

        //增强时间
        edit_strengT_hour.setOnFocusChangeListener(tvFocusLsn);
        edit_strengT_minute.setOnFocusChangeListener(tvFocusLsn);
        edit_strengT_second.setOnFocusChangeListener(tvFocusLsn);

        //消毒时间
        edit_idleT_hour.setOnFocusChangeListener(tvFocusLsn);
        edit_idleT_minute.setOnFocusChangeListener(tvFocusLsn);
        edit_idleT_second.setOnFocusChangeListener(tvFocusLsn);

        //撤离时间
        edit_leaveT_hour.setOnFocusChangeListener(tvFocusLsn);
        edit_leaveT_minute.setOnFocusChangeListener(tvFocusLsn);
        edit_leaveT_second.setOnFocusChangeListener(tvFocusLsn);

        //预约时间
        edit_planT_hour.setOnFocusChangeListener(tvFocusLsn);
        edit_planT_minute.setOnFocusChangeListener(tvFocusLsn);
        edit_planT_second.setOnFocusChangeListener(tvFocusLsn);


        //类型选择
        color_btnSelect=Color.WHITE;
        //如果版本小于 sdk2.3
        Log.d(TAG, "InitView: "+Build.VERSION.SDK_INT+" "+Build.VERSION_CODES.LOLLIPOP_MR1);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            color_btnNotSelect=activity.getResources().getColor(R.color.background);
        }else{
            color_btnNotSelect=activity.getColor(R.color.background);
        }

        btn_ratio1.setOnClickListener(new  View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SelectRatio(0);
            }
        });
        btn_ratio2.setOnClickListener(new  View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SelectRatio(1);
            }
        });
        btn_ratio3.setOnClickListener(new  View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SelectRatio(2);
                edit_ratio.requestFocus();
                InputMethodManager im = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                im.showSoftInput(edit_ratio, 0);
            }
        });
        layout_ratio3.setOnClickListener(new  View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ratio3");
                SelectRatio(2);
                edit_ratio.requestFocus();
                InputMethodManager im = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                im.showSoftInput(edit_ratio, 0);
            }
        });

        btn_type1.setOnClickListener(new  View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SelectType(0);
            }
        });
        btn_type2.setOnClickListener(new  View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SelectType(1);
            }
        });

        //按钮事件
        ImageView img=v.findViewById(R.id.parm_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.AllBtnFrgmHome();
            }
        });
        iv_CustomReset=v.findViewById(R.id.parm_img_reset);
        iv_CustomReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //计算喷洒时间
                Log.d("LXPTest", "iv_CustomReset");
                int[] time = CalcSprayTime();
                edit_sprayT_hour.setText(String.format("%02d", time[0]));
                edit_sprayT_minute.setText(String.format("%02d", time[1]));
                edit_sprayT_second.setText(String.format("%02d", time[2]));
            }
        });

        edit_ratio.setOnTouchListener(new MyRatioTextTouch());

        //取消焦点
        layout_parent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    layout_parent.clearFocus();
                    layout_parent.findFocus();
                }
                return false;
            }
        });

        img_reagent =  v.findViewById(R.id.parm_img_reagent);
        img_reagent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.ComeGoFrame(MainActivity.flag_reagent);
            }
        });
        if(DisinfectData.HwDevCate!=DisinfectData.Dev_Little)
        {
            img_reagent.setVisibility(View.VISIBLE);
        }else
        {
            img_reagent.setVisibility(View.GONE);
        }
    }

    android.view.View.OnFocusChangeListener tvFocusLsn = new android.view.View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus)
        {
            EditText mEdit = (EditText) v;
            if (hasFocus) {
                // 此处为得到焦点时的处理内容
            } else {
                // 此处为失去焦点时的处理内容
                boolean isTime = true;
                int max = 59;
                if (mEdit == edit_length || mEdit == edit_width ||
                        mEdit == edit_height || mEdit == edit_size) {
                    isTime = false;
                    max = 99;
                }
                if (mEdit == edit_ratio) {
                    max = 99;
                }

                String target = mEdit.getText().toString();
                if (mEdit != null) {
                    if (target.isEmpty() || target.equals("") || target == null) {
                        if (isTime) {
                            target = "00";
                        } else {
                            target = "0";
                        }
                    } else {
                        try {

                            if (isTime) {
                                int num = Integer.parseInt(target);
                                target = String.valueOf(num);
                                if (num < 10) {
                                    target = "0" + num;
                                }
                                //大于最大值
                                if (num > max) {
                                    target = String.valueOf(max);
                                }
                            }
                                /*else
                                {
                                    float num=Float.parseFloat(target);
                                    if(num>max){
                                        num=max;
                                    }
                                    num*=10f;
                                    num=((int)num)/10f;
                                    target=String.valueOf(num);
                                }*/
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "TextOver 2: " + target);
                            Log.d(TAG, "TextOver 2: " + e.toString());
                        }
                    }
                }

                if (!target.equals(mEdit.getText().toString())) {
                    mEdit.setText(target);
                }
            }
        }
    };

    @Override
    public void InitData() {
        if(scensItem==null){
            Log.e(TAG, "InitData: "+scensItem );
            scensItem=ScensItem.Empty;
        }

        SelectRatio(0);
        SelectType(0);

        layout_ratio3.callOnClick();
        if(!isCustomMode)
        {
            edit_sprayT_hour.setEnabled(false);
            edit_sprayT_minute.setEnabled(false);
            edit_sprayT_second.setEnabled(false);
            iv_CustomReset.setVisibility(View.GONE);

            //喷洒时间 0106添加
            edit_sprayT_hour.setOnFocusChangeListener(null);
            edit_sprayT_minute.setOnFocusChangeListener(null);
            edit_sprayT_second.setOnFocusChangeListener(null);
        }else
        {
            edit_sprayT_hour.setEnabled(true);
            edit_sprayT_minute.setEnabled(true);
            edit_sprayT_second.setEnabled(true);
//            if (DisinfectData.HwDevCate == DisinfectData.Dev_Little)
//            {
//                iv_CustomReset.setVisibility(View.GONE);
//            }else
//                {
                iv_CustomReset.setVisibility(View.VISIBLE);
//            }

            //喷洒时间 0106添加
            edit_sprayT_hour.setOnFocusChangeListener(tvFocusLsn);
            edit_sprayT_minute.setOnFocusChangeListener(tvFocusLsn);
            edit_sprayT_second.setOnFocusChangeListener(tvFocusLsn);
        }

        //设置房间信息
        edit_length.setText(String.valueOf(scensItem.length));
        edit_width.setText(String.valueOf(scensItem.width));
        edit_height.setText(String.valueOf(scensItem.height));

        edit_sprayT_hour.setText("00");
        edit_sprayT_minute.setText("00");
        edit_sprayT_second.setText("00");

        edit_strengT_hour.setText("00");
        edit_strengT_minute.setText("00");
        edit_strengT_second.setText("00");

        edit_idleT_hour.setText("00");
        edit_idleT_minute.setText("00");
        edit_idleT_second.setText("00");

        edit_leaveT_hour.setText("00");
        edit_leaveT_minute.setText("00");
        edit_leaveT_second.setText("00");

        //设置时间
        String[] list=AndroidUtils.QuickDataGet("cacheSprayTime").split(":");
        if(list.length==3){
            edit_sprayT_hour.setText(list[0]);
            edit_sprayT_minute.setText(list[1]);
            edit_sprayT_second.setText(list[2]);
        }
        list=AndroidUtils.QuickDataGet("cacheStrengTime").split(":");
        if(list.length==3){
            edit_strengT_hour.setText(list[0]);
            edit_strengT_minute.setText(list[1]);
            edit_strengT_second.setText(list[2]);
        }
        list=AndroidUtils.QuickDataGet("cacheIdleTime").split(":");
        if(list.length==3){
            edit_idleT_hour.setText(list[0]);
            edit_idleT_minute.setText(list[1]);
            edit_idleT_second.setText(list[2]);
        }
        list=AndroidUtils.QuickDataGet("cacheLeaveTime").split(":");
        if(list.length==3)
        {
            edit_leaveT_hour.setText(list[0]);
            edit_leaveT_minute.setText(list[1]);
            edit_leaveT_second.setText(list[2]);
        }

        if(isCustomMode)
        {
            //自定义场景
            edit_length.setEnabled(true);
            edit_width.setEnabled(true);
            edit_height.setEnabled(true);

            edit_length.setText(AndroidUtils.QuickDataGet("cacheLength"));
            edit_width.setText(AndroidUtils.QuickDataGet("cacheWidth"));
            edit_height.setText(AndroidUtils.QuickDataGet("cacheHeight"));

            try{
                SelectRatio(AndroidUtils.QuickDataTryGetInt("cacheRatioNo"));
                SelectType(AndroidUtils.QuickDataTryGetInt("cacheTypeNo"));
            }catch (Exception e){e.printStackTrace();}

            if(selectNo_ratio==2)
            {
                SetRatioEdit(AndroidUtils.QuickDataTryGetInt("cacheRatio") + "");
            }
        }else if(isGridMode)
        {
            //网络化场景
            edit_length.setEnabled(true);
            edit_width.setEnabled(true);
            edit_height.setEnabled(true);

            int no=activity.webGridFrgm.selectRoom;
            ScensItem scens=activity.webGridFrgm.roomSceneItems[no];
            PlanItem plan=activity.webGridFrgm.roomPlanItems[no];

            edit_length.setText(String.valueOf(scens.length));
            edit_width.setText(String.valueOf(scens.width));
            edit_height.setText(String.valueOf(scens.height));

            int[] time= GetTimeList(plan.sprayTime);
            edit_sprayT_hour.setText(String.format("%02d",time[0]));
            edit_sprayT_minute.setText(String.format("%02d",time[1]));
            edit_sprayT_second.setText(String.format("%02d",time[2]));

            time= GetTimeList(plan.strengTime);
            edit_strengT_hour.setText(String.format("%02d",time[0]));
            edit_strengT_minute.setText(String.format("%02d",time[1]));
            edit_strengT_second.setText(String.format("%02d",time[2]));

            time= GetTimeList(plan.idleTime);
            edit_idleT_hour.setText(String.format("%02d",time[0]));
            edit_idleT_minute.setText(String.format("%02d",time[1]));
            edit_idleT_second.setText(String.format("%02d",time[2]));

            time= GetTimeList(plan.leaveTime);
            edit_leaveT_hour.setText(String.format("%02d",time[0]));
            edit_leaveT_minute.setText(String.format("%02d",time[1]));
            edit_leaveT_second.setText(String.format("%02d",time[2]));

            time= GetTimeList(plan.planTime);
            edit_planT_hour.setText(String.format("%02d",time[0]));
            edit_planT_minute.setText(String.format("%02d",time[1]));
            edit_planT_second.setText(String.format("%02d",time[2]));

            for (int i = 0; i < 2; i++) {
                if(plan.typeId==DisinfectData.typeItems[i].id)
                {
                    SelectType(i);
                }
            }

            if(plan.ratioValue!=3 && plan.ratioValue!=6)
            {
                if (plan.ratioValue != 0) {
                    SetRatioEdit(String.valueOf(plan.ratioValue));
                }
                SelectRatio(2);
            }

            for (int i = 0; i < 2; i++) {
                if(plan.ratioValue==DisinfectData.ratioItems[i].value)
                {
                    SelectRatio(i);
                }
            }
        }else
        {
            //有房间信息
            //不能修改房间大小
            edit_length.setEnabled(false);
            edit_width.setEnabled(false);
            edit_height.setEnabled(false);

            //设置浓度
            ParmItem[] parm= DisinfectData.ratioItems;
            layout_ratio3.callOnClick();
            if(parm!=null && parm.length==2){
                btn_ratio1.setText(parm[0].name);
                btn_ratio2.setText(parm[1].name);
                if(parm[0].isDefault){
                    btn_ratio1.callOnClick();
                }
                if(parm[1].isDefault){
                    btn_ratio2.callOnClick();
                }
            }

            //设置药剂
            parm= DisinfectData.typeItems;
            if(parm!=null && parm.length==2){
                btn_type1.setText(parm[0].name);
                btn_type2.setText(parm[1].name);
                if(parm[0].isDefault){
                    btn_type1.callOnClick();
                }
                if(parm[1].isDefault){
                    btn_type2.callOnClick();
                }
            }
        }

        //语言选择
        Log.d(TAG, "InitData: "+AndroidUtils.langue);
        /*
        if(AndroidUtils.langue== Locale.ENGLISH)
        {
            layoutTopTitle.setPadding(40,0,0,0);
        }else
        {
            layoutTopTitle.setPadding(200,0,0,0);
        }*/

        layout_parent.requestFocus();
        layout_parent.clearFocus();

        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);

        img_reagent.setImageBitmap(activity.bm_reagent1);
    }

    void SetRatioEdit(String data)
    {
        Log.d(TAG, "SetRatioEdit: "+edit_ratio.getText().toString().trim()+" : "+data.trim());
        if(!edit_ratio.getText().toString().trim().equals(data.trim()))
        {
            edit_ratio.setText(data.trim());

            layout_parent.requestFocus();
            layout_parent.clearFocus();
        }
    }

    public void SetSceneData(int sceneId,ScensItem item){
        this.sceneId=sceneId;
        scensItem=item;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            InitData();
        }
    }


    private void RunOnClick() {
        try
        {
            //检查编辑数据
            CheckAllEdit();
            //获取编辑数据
            GetEditData();

            //保存缓存数据
            SetTempData();
            if(HttpUtils.IsOnLine)
            {
                //保存任务数据
                SetPlanData();

                //上传自定义场景信息
                if(isCustomMode)
                {
                    int tempA, tempB, tempC;
                    tempA = CheckNumFloat(edit_length, 1, 99);
                    tempB = CheckNumFloat(edit_width, 1, 99);
                    tempC = CheckNumFloat(edit_height, 1, 99);
                    NetWorkSendCustomData(tempA, tempB, tempC);
                }
            }

            //单机数据
            SetLocalData();

            //检查消毒剂容量
            CheckReagent();

            //足够溶液
            if(DisinfectData.GetReagentSuffice())
            {
                //跳转开启机器
                activity.ComeGoFrame(MainActivity.flag_switch);
            }else{
                //跳转消毒溶液
                activity.ComeGoFrame(MainActivity.flag_reagent);
            }

        }catch (Exception e){
            e.printStackTrace();
            activity.MyToast("数据异常");
        }
    }

    private  void BackGridFrgm()
    {
        Log.d("WebGridFrgm", "BackGridFrgm");
        try
        {
            //检查编辑数据
            CheckAllEdit();
            //获取编辑数据
            GetEditData();
            //检查消毒剂容量
            CheckReagent();

            //保存网络化数据
            SetEditGridData();
            //跳转返回网络化房间
            activity.ComeGoFrame(MainActivity.flag_webgrid);

        }catch (Exception e){
            e.printStackTrace();
            activity.MyToast("数据异常");
        }
    }

    //网络化数据
    public void WebGridRunPlan(int typeId,int ratioValue,
                               int sprayT,int strengT,int idleT,int leaveT,int planT,
                               int roomId){

        time_spray=sprayT;
        time_streng=strengT;
        time_idle=idleT;
        time_leave=leaveT;
        time_plan=planT;

        this.sceneId=0;
        this.roomId=roomId;

        DisinfectData.planItem.InitParm(
                typeId,ratioValue,
                time_spray,time_streng,time_idle,time_leave,time_plan,
                sceneId,roomId, PlanItem.RunMode_Init);

        activity.ComeGoFrame(MainActivity.flag_switch);
    }

    public void CheckAllEdit(){
        int tempA,tempB,tempC,tempD;
        tempA= CheckNumFloat(edit_length,1,99);
        tempB= CheckNumFloat(edit_width,1,99);
        tempC= CheckNumFloat(edit_height,1,99);
        tempD=tempA*tempB*tempC;
        edit_size.setText(MyFloatToStr(tempD));

        CheckNum(edit_ratio,1,99);

        //喷洒时间
        CheckNumTime(edit_sprayT_hour);
        CheckNumTime(edit_sprayT_minute);
        CheckNumTime(edit_sprayT_second);

        //增强时间
        CheckNumTime(edit_strengT_hour);
        CheckNumTime(edit_strengT_minute);
        CheckNumTime(edit_strengT_second);

        //消毒时间
        CheckNumTime(edit_idleT_hour);
        CheckNumTime(edit_idleT_minute);
        CheckNumTime(edit_idleT_second);

        //撤离时间
        CheckNumTime(edit_leaveT_hour);
        CheckNumTime(edit_leaveT_minute);
        CheckNumTime(edit_leaveT_second);

        //预约时间
        CheckNumTime(edit_planT_hour);
        CheckNumTime(edit_planT_minute);
        CheckNumTime(edit_planT_second);

    }

    public int MyStrToInt(String data){
        int temp=0;
        try{
            if(!data.isEmpty() && data!="" && data!=null)
            {
                temp=Integer.parseInt(data);
            }
        }catch (Exception e){}
        return temp;
    }

    public int MyStrToFloat(String data){
        int temp=0;
        try{
            if(!data.isEmpty() && data!="" && data!=null)
            {
                temp=Integer.parseInt(data);
            }
        }catch (Exception e){}

        return temp;
    }

    public String MyFloatToStr(int data){
        return String.valueOf(data);
    }

    public int GetNumFloat(String data, int min, int max){
        int temp=MyStrToFloat(data);

        if(temp<min){
            temp=min;
        }
        if(temp>max){
            temp=max;
        }
        return temp;
    }

    public int CheckNumFloat(EditText tv, int min, int max){
        int temp=GetNumFloat(tv.getText().toString(),min,max);
        tv.setText(String.valueOf(temp));
        return temp;
    }

    public int CheckNum(EditText tv, int min, int max){
        int temp=0;
        temp= MyStrToInt(tv.getText().toString());
        if(temp<min){
            temp=min;
        }
        if(temp>max){
            temp=max;
        }
        tv.setText(String.valueOf(temp));
        return temp;
    }

    public int CheckNumTime(EditText tv){
        int temp=0;
        temp= MyStrToInt(tv.getText().toString());
        int min=0,max=59;
        if(temp<min){
            temp=min;
        }
        if(temp>max){
            temp=max;
        }
        tv.setText(String.format("%02d",temp));
        return temp;
    }

//    View.OnFocusChangeListene`r EditChangedListener = new View.OnFocusChangeListener() {
//        @Override
//        public void onFocusChange(View v, boolean hasFocus) {
//            if(hasFocus){
//
//            }else{
//
//            }
//        }
//    };

    public void GetEditData(){
        int hour=0,minute=0,second=0;
        //喷洒时间
        try{
            hour=Integer.parseInt(edit_sprayT_hour.getText().toString().trim());
            minute=Integer.parseInt(edit_sprayT_minute.getText().toString().trim());
            second=Integer.parseInt(edit_sprayT_second.getText().toString().trim());
        }catch (Exception e){
            hour=0;minute=0;second=0;
        }
        time_spray=(hour*60*60)+(minute*60)+(second);

        //增强时间
        hour=0;minute=0;second=0;
        try{
            hour=Integer.parseInt(edit_strengT_hour.getText().toString().trim());
            minute=Integer.parseInt(edit_strengT_minute.getText().toString().trim());
            second=Integer.parseInt(edit_strengT_second.getText().toString().trim());
        }catch (Exception e){
            hour=0;minute=0;second=0;
        }
        time_streng=(hour*60*60)+(minute*60)+(second);

        //消毒时间
        hour=0;minute=0;second=0;
        try{
            hour=Integer.parseInt(edit_idleT_hour.getText().toString().trim());
            minute=Integer.parseInt(edit_idleT_minute.getText().toString().trim());
            second=Integer.parseInt(edit_idleT_second.getText().toString().trim());
        }catch (Exception e){
            hour=0;minute=0;second=0;
        }
        time_idle=(hour*60*60)+(minute*60)+(second);

        //撤离时间
        hour=0;minute=0;second=0;
        try{
            hour=Integer.parseInt(edit_leaveT_hour.getText().toString().trim());
            minute=Integer.parseInt(edit_leaveT_minute.getText().toString().trim());
            second=Integer.parseInt(edit_leaveT_second.getText().toString().trim());
        }catch (Exception e){
            hour=0;minute=0;second=0;
        }
        time_leave=(hour*60*60)+(minute*60)+(second);

        //预约时间
        hour=0;minute=0;second=0;
        try{
            hour=Integer.parseInt(edit_planT_hour.getText().toString().trim());
            minute=Integer.parseInt(edit_planT_minute.getText().toString().trim());
            second=Integer.parseInt(edit_planT_second.getText().toString().trim());
        }catch (Exception e){
            hour=0;minute=0;second=0;
        }
        time_plan=(hour*60*60)+(minute*60)+(second);

    }

    public String GetAllTimeStr(EditText tvH,EditText tvM,EditText tvS){
        String result="";
        int temp= CheckNumTime(tvH);
        result+=String.format("%02d",temp)+":";
        temp= CheckNumTime(tvM);
        result+=String.format("%02d",temp)+":";
        temp= CheckNumTime(tvS);
        result+=String.format("%02d",temp);
        return result;
    }

    //保存缓存数据
    public void SetTempData(){
        //保存缓存数据
        Map<String, String> params=new HashMap<>();
        params.put("cacheLength",edit_length.getText().toString());
        params.put("cacheWidth",edit_width.getText().toString());
        params.put("cacheHeight",edit_height.getText().toString());

        params.put("cacheRatioNo",selectNo_ratio+"");
        params.put("cacheRatio",edit_ratio.getText().toString());
        params.put("cacheTypeNo",selectNo_type+"");

        params.put("cacheSprayTime",GetAllTimeStr(edit_sprayT_hour,edit_sprayT_minute,edit_sprayT_second));
        params.put("cacheStrengTime",GetAllTimeStr(edit_strengT_hour,edit_strengT_minute,edit_strengT_second));
        params.put("cacheIdleTime",GetAllTimeStr(edit_idleT_hour,edit_idleT_minute,edit_idleT_second));
        params.put("cacheLeaveTime",GetAllTimeStr(edit_leaveT_hour,edit_leaveT_minute,edit_leaveT_second));
        AndroidUtils.QuickDataSet(params);
    }

    //保存单机数据
    public void SetLocalData()
    {
        //初始化单机数据
        try {
            JSONObject json=new JSONObject();
            json.put("macAdd", AndroidUtils.GetMacAddress());

            int tempA,tempB,tempC,tempD;
            tempA= CheckNumFloat(edit_length,1,99);
            tempB= CheckNumFloat(edit_width,1,99);
            tempC= CheckNumFloat(edit_height,1,99);
            tempD=tempA*tempB*tempC;
            json.put("length",tempA);
            json.put("width",tempB);
            json.put("height",tempC);
            json.put("volume",tempD);

            if(selectNo_type==0)
            {
                json.put("disinName",btn_type1.getText().toString());
            }else
            {
                json.put("disinName",btn_type2.getText().toString());
            }

            int ratioValue = 0;
            switch (selectNo_ratio){
                case 0:
                    ratioValue=DisinfectData.ratioItems[selectNo_ratio].value;
                    break;
                case 1:
                    ratioValue=DisinfectData.ratioItems[selectNo_ratio].value;
                    break;
                case 2:
                    try{
                        ratioValue=Integer.parseInt(edit_ratio.getText().toString());
                    }catch (Exception e){}
                    break;
            }
            json.put("density",ratioValue);

            json.put("sprayTime",time_spray);
            json.put("strengTime",time_streng);
            json.put("disinfectTime",time_idle);
            json.put("leaveTime",time_leave);

            SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            json.put("beginTime",sdf.format(System.currentTimeMillis()));
            json.put("finishTime","");
            //0-消毒完成，1-消毒中，2-消毒终止，3-异常",
            json.put("status",1);

            //清空浓度
            DisinfectData.localSensort=new JSONArray();

            DisinfectData.localParmData=json;
        } catch (Exception e) {e.printStackTrace();}
    }

    //保存任务数据提交到服务器
    public void SetPlanData(){
        // 消毒剂id
        int typeId=DisinfectData.typeItems[selectNo_type].id;
        //目标浓度 数据
        int ratioValue = 0;
        switch (selectNo_ratio){
            case 0:
                ratioValue=DisinfectData.ratioItems[selectNo_ratio].value;
                break;
            case 1:
                ratioValue=DisinfectData.ratioItems[selectNo_ratio].value;
                break;
            case 2:
                try{
                    ratioValue=Integer.parseInt(edit_ratio.getText().toString());
                }catch (Exception e){}
                break;
        }
        // 房间id   网络化才使用，正常用场景ID即可
        int roomId=0;

        Log.d("LXPTIME", "Parm:"+time_plan);
        DisinfectData.planItem.InitParm(
                typeId,ratioValue,
                time_spray,time_streng,time_idle,time_leave,time_plan,
                sceneId,roomId, PlanItem.RunMode_Init);
    }

    //保存修改后的网络化数据
    public void SetEditGridData()
    {
        int no=activity.webGridFrgm.selectRoom;
        if(no>=0 &&
                no<activity.webGridFrgm.roomSceneItems.length)
        {
            //目标浓度 数据
            int ratioValue = 0;
            switch (selectNo_ratio){
                case 0:
                    ratioValue=DisinfectData.ratioItems[selectNo_ratio].value;
                    break;
                case 1:
                    ratioValue=DisinfectData.ratioItems[selectNo_ratio].value;
                    break;
                case 2:
                    try{
                        ratioValue=Integer.parseInt(edit_ratio.getText().toString());
                    }catch (Exception e){}
                    break;
            }

            //修改场景数据
            try {
                ScensItem scens=activity.webGridFrgm.roomSceneItems[no];
                scens.length = CheckNumFloat(edit_length,1,99);
                scens.width = CheckNumFloat(edit_width,1,99);
                scens.height =CheckNumFloat(edit_height,1,99);
                activity.webGridFrgm.roomSceneItems[no]=scens;
            } catch (Exception e) {
                e.printStackTrace();
            }

            //修改任务数据
            try {
                PlanItem plan=activity.webGridFrgm.roomPlanItems[no];
                plan.sprayTime = time_spray;
                plan.strengTime = time_streng;
                plan.idleTime = time_idle;
                plan.leaveTime =time_leave;

                plan.typeId = DisinfectData.typeItems[selectNo_type].id;
                plan.planTime = time_plan;
                plan.ratioValue = ratioValue;
                activity.webGridFrgm.roomPlanItems[no]=plan;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void SelectRatio(int no){
        selectNo_ratio=no;
        btn_ratio1.setBackgroundColor(color_btnNotSelect);
        btn_ratio2.setBackgroundColor(color_btnNotSelect);
        layout_ratio3.setBackgroundColor(color_btnNotSelect);
        //edit_ratio.setBackground(ui_NotSelect);

        btn_ratio1.setTextColor(activity.color_tv);
        btn_ratio2.setTextColor(activity.color_tv);
        btn_ratio3.setTextColor(activity.color_tv);
        edit_ratio.setTextColor(activity.color_tv);
        switch (no){
            case 0:
                btn_ratio1.setBackgroundColor(color_btnSelect);
                btn_ratio1.setTextColor(activity.color_tvB);
                break;
            case 1:
                btn_ratio2.setBackgroundColor(color_btnSelect);
                btn_ratio2.setTextColor(activity.color_tvB);
                break;
            case 2:
                layout_ratio3.setBackgroundColor(color_btnSelect);
                btn_ratio3.setTextColor(activity.color_tvB);
                //edit_ratio.setBackground(ui_Select);
                edit_ratio.setTextColor(activity.color_tvB);

                //清空设置
                SetRatioEdit("");
                break;
        }
    }

    public void SelectType(int no)
    {
        selectNo_type=no;
        btn_type1.setBackgroundColor(color_btnNotSelect);
        btn_type2.setBackgroundColor(color_btnNotSelect);

        btn_type1.setTextColor(activity.color_tv);
        btn_type2.setTextColor(activity.color_tv);
        switch (no){
            case 0:
                btn_type1.setBackgroundColor(color_btnSelect);
                btn_type1.setTextColor(activity.color_tvB);
                break;
            case 1:
                btn_type2.setBackgroundColor(color_btnSelect);
                btn_type2.setTextColor(activity.color_tvB);
                break;
        }
    }

    //计算喷洒剂量
    private int CalcSprayDose(){
        //计算体积
        int tempA,tempB,tempC,size;
        tempA= GetNumFloat(edit_length.getText().toString(),1,99);
        tempB= GetNumFloat(edit_width.getText().toString(),1,99);
        tempC= GetNumFloat(edit_height.getText().toString(),1,99);
        size=tempA*tempB*tempC;

        //计算浓度
        int ratio=1;
        try{
            switch (selectNo_ratio){
                case 0:
                case 1:
                    ratio=DisinfectData.ratioItems[selectNo_ratio].value;
                    break;
                case 2:
                    ratio= MyStrToInt(edit_ratio.getText().toString());
                    if(ratio<1){
                        ratio=1;
                    }
                    if(ratio>99){
                        ratio=99;
                    }
                    break;
            }
        }catch (Exception e){e.printStackTrace();}

        int result=0;
        result= Math.round(size* ratio) ;
        //Log.d(TAG, "CalcSprayDose: "+size+" * "+ratio+" = "+result);
        return result;
    }

    //计算喷洒时间
    private int[] CalcSprayTime(){
        //获取喷洒剂量
        PlanDose=CalcSprayDose();

        //获取喷洒速率
        float rate=ControlUtils.getInstance().GetHareWareRate();
        int time=0;
        int sec=0; //秒数
        int[] result=new int[]{0,0,0};
        //计算总时间
        //   总量    / 速率
        //  PlanDose / rate
        if(PlanDose!=0 && rate!=0){
            time=(int) (PlanDose / rate);
            sec=(int)((PlanDose%rate)/rate*60f);

            Log.d(TAG, PlanDose+"|"+rate+" "+time+"|"+sec);
        }


        //分开时分秒
        if(time>60)
        {
            int hour=(time/60);
            int min=(time%60);
            result[0]=hour;
            result[1]=min;
            result[2]=sec;
        }else
        {
            //如果不是自定义模式
            //最低一分钟
            if (time <= 0) {
                time = 1;
                sec = 0;
            }

            result[1]=time;
            result[2]=sec;
        }
        return result;
    }

    int[] GetTimeList(int time)
    {
        int[] result = new int[3];
        float all = time;
        result[0] = (int)(all / (60f * 60f));
        all -= (int)(result[0] * (60f*60f));
        result[1] = (int)(all / 60f);
        all -= (int)(result[1] * 60f);
        result[2] = (int)all;
        return result;
    }

    private EditText InitMyTimeEdit(View v, int id){
        EditText edit=v.findViewById(id);
        edit.setOnTouchListener(new MyTextTouch());
        edit.addTextChangedListener(new MyTextWatcher(edit,59));
        //edit.setOnClickListener(new MyTextClick());
        return edit;
    }

    private EditText InitMyNumEdit(View v, int id){
        EditText edit=v.findViewById(id);
        edit.setOnTouchListener(new MyTextTouch());
        edit.addTextChangedListener(new MyTextWatcher(edit,99));
        return edit;
    }


//    private class MyTextClick implements View.OnClickListener{
//        @Override
//        public void onClick(View v) {
//            EditText edit=(EditText)v;
//            activity.MyToast(edit.getText().toString());
//            if(edit.getText().toString().equals("00")){
//                edit.setText("");
//            }
//        }
//    }

    private class MyTextTouch implements View.OnTouchListener{
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction()==MotionEvent.ACTION_DOWN){
                EditText edit=(EditText)v;
                //if(edit.getText().toString().equals("00"))
                {
                    edit.setText("");
                }
            }
            return false;
        }
    }

    //选择浓度点击
    private class MyRatioTextTouch implements View.OnTouchListener{
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            //选框
            if(selectNo_ratio!=2){
                SelectRatio(2);
            }
            //全选内容
            if(event.getAction()==MotionEvent.ACTION_UP){
                SetRatioEdit("");
            }
            return false;
        }
    }


    private class MyTextWatcher implements TextWatcher {
        private EditText mEdit;
        private int max;
        public MyTextWatcher(EditText e,int max) {
            mEdit = e;
            this.max=max;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //Log.d(TAG, "TextChange: "+s.toString()+" | "+mEdit.getText().toString());
        }

        @Override
        public void afterTextChanged(Editable s)
        {
            /*
            String target= s.toString();
            if(mEdit!=null) {
                if (target.isEmpty() || target.equals("") || target == null)
                {
                    if (mEdit != edit_length && mEdit != edit_width &&
                            mEdit != edit_height && mEdit != edit_size) {
                        target="00";
                    } else {
                        target="0.0";
                    }
                } else {
                    try {

                        if (mEdit != edit_length && mEdit != edit_width &&
                                mEdit != edit_height && mEdit != edit_size)
                        {
                            int num = Integer.parseInt(target);
                            target=String.valueOf(num);
                            if (num < 10) {
                                target="0" + num;
                            }
                            //大于最大值
                            if (num > max) {
                                target=String.valueOf(max);
                            }
                        } else {
                            //Log.d(TAG, "TextOver Float:" + str);

                            float num=Float.parseFloat(target);
                            num*=10f;
                            num=(((int)num)/10f);
                            target=String.valueOf(num);

                            //大于最大值
                            if(num>max){
                                num=max*10f;
                                num=((int)num)/10f;
                                target=String.valueOf(num);
                            }

                        }
                        //Log.d(TAG, "TextOver 1: "+s.toString()+" | "+mEdit.getText().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, "TextOver 2: " + s.toString() + " | " + mEdit.getText().toString());
                        Log.d(TAG, "TextOver 2: " + e.toString());
                    }
                }
            }

            if(!target.equals(mEdit.getText().toString())){
                mEdit.setText(target);
            }
            */
        }
    }

    //上传自定义场景信息
    private void NetWorkSendCustomData(final int length,final int width,final int height){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/user/scenes/custom";
                    Map<String, String> params=new HashMap<>();
                    params.put("length",String.valueOf( length));
                    params.put("width",String.valueOf( width));
                    params.put("height",String.valueOf( height));
                    String result= HttpUtils.PostData(url,params);

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==0){
                        //成功
                        activity.MyToastThread(data);

                        //保存新的自定义数据
                        //DisinfectData.scensCustom=new ScensItem();
                        //只更新大小
                        DisinfectData.scensCustom.length=length;
                        DisinfectData.scensCustom.width=width;
                        DisinfectData.scensCustom.height=height;

                        SetSceneData(0, DisinfectData.scensCustom);
                    }else{
                        //失败
                        activity.MyToastThread(data);
                    }
                    Log.v(TAG,data);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onTouchNext()
    {
        if(!isGridMode)
        {
            RunOnClick();
        }else
        {
            BackGridFrgm();
        }
    }

    @Override
    public void onTouchBack(int flag) {

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

    //计算消耗溶液
    public void CheckReagent()
    {
        //喷洒时间
        int[] list = CalcSprayTime();
        float time=list[0] * 60f; //时
        time+=list[1];                   //分
        time+=list[2]/60f;        //秒

        //喷洒速率 每分钟喷xml
        float rate= ControlUtils.getInstance().GetHareWareRate();

        //共消耗 毫升
        float need=(float) Math.ceil(time*rate);
        if(need<PlanDose){
            need=PlanDose;
        }

        Log.d("CaleReagent", "CaleReagent: 消耗："+need+" 现有："+ControlUtils.getInstance().GetNowReagent());
        if(need<=ControlUtils.getInstance().GetNowReagent())
        {
            //需要的小于现在的
            DisinfectData.SetReagentSuffice(true,need);
        }else{
            //需要的大于现在的
            DisinfectData.SetReagentSuffice(false,need);
        }
    }

    //更新溶液显示
    public void UpDateReagent()
    {
        //显示溶液
        if(DisinfectData.GetReagentSuffice())
        {
            img_reagent.setImageBitmap(activity.bm_reagent1);
        }else
        {
            //溶液不足
            if(showReagent1)
            {
                img_reagent.setImageBitmap(activity.bm_reagent1);
            }else{
                img_reagent.setImageBitmap(activity.bm_reagent2);
            }
            showReagent1=!showReagent1;
        }
    }
}

