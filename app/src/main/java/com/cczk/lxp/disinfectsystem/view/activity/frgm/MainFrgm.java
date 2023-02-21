package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.graphics.Bitmap;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.ParmItem;
import com.cczk.lxp.disinfectsystem.bean.PlanItem;
import com.cczk.lxp.disinfectsystem.bean.ScensItem;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.FileUtils;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.CarPlanUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;
import com.cczk.lxp.disinfectsystem.view.utils.ScenesAddView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pc on 2018/2/7.
 */
public class MainFrgm extends Fragment implements IFrgmCallBack {
    private final String TAG="MainFrgm";
    private MainActivity activity;

    private LinearLayout layout0;
    private LinearLayout layout1;
    private LinearLayout layout2;
    private LinearLayout layout3;

    private ImageView[] imgs_view1;
    private ImageView[] imgs_view2;
    private ImageView[] imgs_view3;

    private ScenesAddView scenesAddView;

    private int DelSelectIndex=-1;
    private ImageView img_viewDelete;
    private TextView tv_nfc;
    private TextView tv_bindcard;
    private TextView tv_uploadlocal;
    private TextView tv_changeOffPw;
    private String Temp_nfc;

    private ImageView img_reagent;
    private ImageView img_openReagent;
    private boolean showReagent1=true;

    //更换用户 获取新数据
    public boolean isUpDateAllScenes =true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_main, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    @Override
    public void FrgmMessage(Message msg) {
        Object[] data;
        switch (msg.what)
        {
            case MainActivity.MsgID_UI_Time:
                //if(FileUtils.existsTxt("LoacalData.json")){
                ArrayList<String> list = FileUtils.getTxtFile();
                if(list.size()>0){
                    tv_uploadlocal.setVisibility(View.VISIBLE);
                }else{
                    tv_uploadlocal.setVisibility(View.GONE);
                }

                UpDateReagent();
                break;
            //NFC相关
            case MainActivity.MsgID_HW_NFCID:
                Log.v(TAG,"NFCID: "+msg.obj);
                Temp_nfc=msg.obj.toString();
                Temp_nfc=Temp_nfc.replace(" ","");
                tv_nfc.setText(Temp_nfc);
                //tv_nfc.setVisibility(View.VISIBLE);
                tv_nfc.setVisibility(View.GONE);
                break;
            case MainActivity.MsgID_Web_UpdateNFC:
                data= (Object[]) msg.obj;
                Log.v(TAG,"NFC: "+data[1]);
                activity.MyToast(data[1].toString());
                if((boolean)data[0]){
                    activity.MyToast("修改成功");
                }
                break;

            //获取列表
            case MainActivity.MsgID_Web_GetNowScenList:
                try {
                    data= (Object[]) msg.obj;
                    if((boolean)data[0]){
                        JSONArray array= (JSONArray) data[2];
                        ScensItem[] scensItems=new ScensItem[array.length()];
                        int CustomIndex=-1;
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject jo=array.getJSONObject(i);
                            scensItems[i]=new ScensItem(jo);
                            if(scensItems[i].scenesName.equals(DisinfectData.CustomName)){
                                CustomIndex=i;
                            }

                        }

                        //如果是自定义 剔除保存到使用场景
                        if(CustomIndex!=-1){
                            DisinfectData.scensCustom=scensItems[CustomIndex];

                            ScensItem[] TempItems=new ScensItem[scensItems.length-1];
                            int count=0;
                            for (int i = 0; i < scensItems.length; i++) {
                                if(i!=CustomIndex){
                                    TempItems[count++]=scensItems[i];
                                }
                            }
                            scensItems=TempItems;
                        }

                        //赋值除图片外信息
                        DisinfectData.scensItems=scensItems;
                        UpDateSceneView();

                        if(isUpDateAllScenes){
                            //必须先获取当前使用再获取所有场景
                            NetWorkGetAllSceneList();
                        }else{
                            InitSceneImage();

                            //获取已下载图片
                            for (int i = 0; i < array.length(); i++) {
                                //找相同序号的已下载场景图片
                                for (ScensItem AllItem:
                                        DisinfectData.AllScensItems) {
                                    if(i<scensItems.length) {
                                        if (scensItems[i].scenesId == AllItem.scenesId) {
                                            if (AllItem.bm != null) {
                                                scensItems[i].bm = AllItem.bm;
                                                //获取后直接显示
                                                UpDateSceneImage(i);
                                            }
                                        }
                                    }
                                }
                            }

                            //赋值场景图片
                            DisinfectData.scensItems=scensItems;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            //获取所有场景列表
            case MainActivity.MsgID_Web_GetAllScenList:
                try {
                    data= (Object[]) msg.obj;
                    if((boolean)data[0]){
                        JSONArray array= (JSONArray) data[2];
                        ScensItem[] scensItems=new ScensItem[array.length()];
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject jo=array.getJSONObject(i);
                            scensItems[i]=new ScensItem(jo);
                        }
                        DisinfectData.AllScensItems=scensItems;

                        //初始化场景获取成功
                        isUpDateAllScenes =false;
                        scenesAddView.Init(DisinfectData.AllScensItems);
                        //获取场景信息后获取场景图片
                        NetWorkGetAllSceneImg(scensItems);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            //获取场景图片
            case MainActivity.MsgID_Web_GetAllScenImg:
                try {
                    data= (Object[]) msg.obj;
                    if((boolean)data[0]){
                        int scenesId=(int)data[1];
                        int Index=(int)data[2];
                        Bitmap bmp=(Bitmap)data[3];

                        if(bmp!=null &&  DisinfectData.scensItems!=null) {
                            //Log.d(TAG, "GetAllScenImg: "+scenesId);

                            //全部场景赋值
                            DisinfectData.AllScensItems[Index].bm=bmp;

                            //使用场景赋值
                            for (int i = 0; i < DisinfectData.scensItems.length; i++) {

                                //Log.d(TAG, "ScenImg: "+scenesId+"-"+DisinfectData.scensItems[i].scenesId+" "+(scenesId==DisinfectData.scensItems[i].scenesId));
                                if(scenesId==DisinfectData.scensItems[i].scenesId){
                                    DisinfectData.scensItems[i].bm=bmp;
                                    //Log.d(TAG, "OK: "+DisinfectData.scensItems[i].bm);
                                    UpDateSceneImage(i);
                                    break;
                                }
                            }

                            //添加场景赋值
                            scenesAddView.UpDateSceneImage(Index);

//                            for (int i = 0; i < DisinfectData.AllScensItems.length; i++) {
//                                if(scenesId==DisinfectData.AllScensItems[i].scenesId){
//                                    DisinfectData.AllScensItems[i].bm=bmp;
//                                    break;
//                                }
//                            }
//                            for (int i = 0; i < DisinfectData.scensItems.length; i++) {
//                                //Log.d(TAG, "ScenImg: "+scenesId+"-"+DisinfectData.scensItems[i].scenesId+" "+(scenesId==DisinfectData.scensItems[i].scenesId));
//                                if(scenesId==DisinfectData.scensItems[i].scenesId){
//                                    DisinfectData.scensItems[i].bm=bmp;
//                                    //Log.d(TAG, "OK: "+DisinfectData.scensItems[i].bm);
//                                    UpDateSceneImage(i);
//                                    break;
//                                }
//                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            //修改场景
            case MainActivity.MsgID_Web_ChangeScenList:
                data= (Object[]) msg.obj;
                activity.MyToast(data[1].toString());
                UIDontNeedHide();
                if((boolean)data[0]){
                    NetWorkGetNowSceneList(DisinfectData.planItem.userId,AndroidUtils.GetMacAddress());
                }
                break;

            //获取浓度信息
            case MainActivity.MsgID_Web_GetRatioList:
                try {
                    data= (Object[]) msg.obj;
                    if((boolean)data[0]){
                        JSONArray array= (JSONArray) data[2];
                        ParmItem[] parm=new ParmItem[array.length()];
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject jo=array.getJSONObject(i);
                            Log.d(TAG, "GetRatioList: "+jo.getInt("value"));
                            String name=jo.getInt("value")+" ml/m³";
                            int id=jo.getInt("densityId");
                            int value=jo.getInt("value");
                            boolean isDefault=jo.getInt("isDefault")==0?true:false;
                            parm[i]=new ParmItem(name,id,value,isDefault);
                        }
                        DisinfectData.ratioItems=parm;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            //获取消毒剂信息
            case MainActivity.MsgID_Web_GetTypeList:
                try {
                    data= (Object[]) msg.obj;
                    if((boolean)data[0]){
                        JSONArray array= (JSONArray) data[2];
                        ParmItem[] parm=new ParmItem[array.length()];
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject jo=array.getJSONObject(i);
                            Log.d(TAG, "GetRatioList: "+jo.getString("disinName"));
                            String name=jo.getString("disinName");
                            int id=jo.getInt("disinId");
                            int value=0;
                            boolean isDefault=jo.getInt("isDefault")==0?true:false;
                            parm[i]=new ParmItem(name,id,value,isDefault);
                        }
                        DisinfectData.typeItems=parm;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            //获取用户标识
            case MainActivity.MsgID_Web_GetUserType:
                try {
                    data= (Object[]) msg.obj;
                    if((boolean)data[0]){
                        JSONObject joData=(JSONObject) data[1];
                        JSONArray roles=joData.getJSONArray("roles");
                        int id=joData.getInt("userId");
                        //初始化任务参数
                        DisinfectData.planItem=new PlanItem();
                        DisinfectData.planItem.InitUser(id);

                        for (int i = 0; i < roles.length(); i++) {
                            String temp=roles.getString(i);
                            if(temp.equals(DisinfectData.UserRoleAdmin)){
                                DisinfectData.UserIsAdmin=true;
                                //Log.d(TAG, "NetWorkGetUserRole: "+temp);

//                                if(!DisinfectData.UserIsAdmin)
//                                {
//                                    tv_bindcard.setVisibility(View.GONE);
//                                }else
                                {
                                    tv_bindcard.setVisibility(View.VISIBLE);
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            //06-19 09:02:49.214 2954-2977/? V/HttpUtils: {"code":0,"msg":"success","data":[{"densityId":2,"value":30,"isDefault":0,"isDefaultMsg":"默认","createTime":"2020-05-23 05:18:13","updateTime":"2020-05-30 11:36:00"},{"densityId":7,"value":20,"isDefault":1,"isDefaultMsg":"非默认","createTime":"2020-05-27 09:09:02","updateTime":"2020-05-27 09:09:02"}],"count":2}
        }
    }

    @Override
    public void InitData()
    {
        activity.parmFrgm.isCustomMode = false;
        activity.parmFrgm.isGridMode = false;

        if(isUpDateAllScenes){
            //获取新用户标识
            NetWorkGetUserRole();

            //必须先获取当前使用再获取所有场景
            NetWorkGetNowSceneList(DisinfectData.planItem.userId,AndroidUtils.GetMacAddress());
            NetWorkParmData();

            InitSceneImage();

            //获取地图信息
            CarPlanUtils.getInstance().NetWorkGetMapIdList();
        }

        tv_nfc.setText("");
        tv_nfc.setVisibility(View.GONE);
        tv_uploadlocal.setVisibility(View.GONE);

        //if(DisinfectData.UserIsAdmin)
        if(!HttpUtils.IsOnLine)
        {
            UpDateSceneView();
            //tv_changeOffPw.setVisibility(View.VISIBLE);
        }

        if (DisinfectData.HwDevCate == DisinfectData.Dev_Little ||
            !HttpUtils.IsOnLine)
        {
            tv_bindcard.setVisibility(View.GONE);
        }else{
            tv_bindcard.setVisibility(View.VISIBLE);
        }

        UIDontNeedHide();
        //NFC
        activity.NFCOpen();

        ThreadOneSecFun();

        img_reagent.setImageBitmap(activity.bm_reagent1);

        //刷新场景数据
        NetWorkGetAllSceneList();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            InitData();
        }else{
            activity.NFCClose();
        }
    }

    private void InitView(View v) {
        //获取View
        tv_nfc=v.findViewById(R.id.main_tv_nfc);
        tv_bindcard=v.findViewById(R.id.main_tv_bindcard);
        tv_uploadlocal=v.findViewById(R.id.main_tv_uploadlocal);
        tv_changeOffPw=v.findViewById(R.id.main_tv_changeOffPw);
        img_viewDelete=v.findViewById(R.id.main_img_viewDelete);

        layout0=v.findViewById(R.id.main_layout0);
        layout1=v.findViewById(R.id.main_layout1);
        layout2=v.findViewById(R.id.main_layout2);
        layout3=v.findViewById(R.id.main_layout3);

        imgs_view1=new ImageView[4];
        imgs_view1[0]=v.findViewById(R.id.main_img1_1);
        imgs_view1[1]=v.findViewById(R.id.main_img1_2);
        imgs_view1[2]=v.findViewById(R.id.main_img1_3);
        imgs_view1[3]=v.findViewById(R.id.main_img1_4);

        imgs_view2=new ImageView[5];
        imgs_view2[0]=v.findViewById(R.id.main_img2_1);
        imgs_view2[1]=v.findViewById(R.id.main_img2_2);
        imgs_view2[2]=v.findViewById(R.id.main_img2_3);
        imgs_view2[3]=v.findViewById(R.id.main_img2_4);
        imgs_view2[4]=v.findViewById(R.id.main_img2_5);

        imgs_view3=new ImageView[6];
        imgs_view3[0]=v.findViewById(R.id.main_img3_1);
        imgs_view3[1]=v.findViewById(R.id.main_img3_2);
        imgs_view3[2]=v.findViewById(R.id.main_img3_3);
        imgs_view3[3]=v.findViewById(R.id.main_img3_4);
        imgs_view3[4]=v.findViewById(R.id.main_img3_5);
        imgs_view3[5]=v.findViewById(R.id.main_img3_6);


        //获取场景添加布局
        scenesAddView= new ScenesAddView(activity,v);

        //场景按钮事件
        ClickViewListener clickListener=new ClickViewListener();

        //单机模式
        ImageView img=v.findViewById(R.id.main_img0_1);
        img.setOnClickListener(clickListener);

        LongClickViewListener longClickListener=new LongClickViewListener();
        for (int i = 0; i < imgs_view1.length; i++) {
            imgs_view1[i].setOnClickListener(clickListener);
            imgs_view1[i].setOnLongClickListener(longClickListener);
        }
        for (int i = 0; i < imgs_view2.length; i++) {
            imgs_view2[i].setOnClickListener(clickListener);
            imgs_view2[i].setOnLongClickListener(longClickListener);
        }
        for (int i = 0; i < imgs_view3.length; i++) {
            imgs_view3[i].setOnClickListener(clickListener);
            imgs_view3[i].setOnLongClickListener(longClickListener);
        }

        TouchViewListener touchViewListener=new TouchViewListener();
        layout1.setOnTouchListener(touchViewListener);
        layout2.setOnTouchListener(touchViewListener);
        layout3.setOnTouchListener(touchViewListener);


        //增加按钮事件
        ImageView[] imgs_add=new ImageView[2];
        imgs_add[0]=v.findViewById(R.id.main_img1_add);
        imgs_add[1]=v.findViewById(R.id.main_img2_add);
        for (int i = 0; i < imgs_add.length; i++) {
            imgs_add[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UIBtnAddFun();
                }
            });
        }

        img_viewDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIBtnDeleteFun();
            }
        });

        img_reagent =  v.findViewById(R.id.main_img_reagent);
        img_reagent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReagentOnClick();
            }
        });
        img_openReagent =  v.findViewById(R.id.main_img_openreagent);
        img_openReagent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //开启门锁
                ControlUtils.getInstance().SetSwitchLock(true,true);
            }
        });
        if(DisinfectData.HwDevCate!=DisinfectData.Dev_Little)
        {
            img_reagent.setVisibility(View.VISIBLE);
            img_openReagent.setVisibility(View.GONE);
        }else
        {
            img_reagent.setVisibility(View.GONE);
            img_openReagent.setVisibility(View.VISIBLE);
        }

        //回退按钮
        img=v.findViewById(R.id.main_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.FrgmGoBack();
            }
        });

        tv_nfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtnNFCOnClick();
            }
        });

        tv_bindcard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.ComeGoFrame(MainActivity.flag_bindcard);
            }
        });
        tv_changeOffPw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.ComeGoFrame(MainActivity.flag_changeOffPw);
            }
        });
        tv_changeOffPw.setVisibility(View.GONE);

        tv_uploadlocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebPostFile();
            }
        });

        FrameLayout layout =  v.findViewById(R.id.main_AllLayout);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UIDontNeedHide();
            }
        });

/*
        Button btn=v.findViewById(R.id.main_btn1);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtnAddX();
            }
        });
        btn=v.findViewById(R.id.main_btn2);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtnDecX();
            }
        });
        btn=v.findViewById(R.id.main_btn3);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtnAddY();
            }
        });
        btn=v.findViewById(R.id.main_btn4);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtnDecY();
            }
        });*/
    }

    /*

    public void BtnAddX()
    {
        offsetX+=5;
        TestXY();
    }
    public void BtnDecX()
    {
        offsetX-=5;
        TestXY();
    }
    public void BtnAddY()
    {
        offsetY+=5;
        TestXY();
    }
    public void BtnDecY()
    {
        offsetY-=5;
        TestXY();
    }

    private void TestXY()
    {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) img_viewDelete.getLayoutParams();
        layoutParams.leftMargin = (int)CurX+offsetX;
        layoutParams.topMargin = (int)CurY+offsetY;
        img_viewDelete.setLayoutParams(layoutParams);
        Log.d("MainSize", "BtnAddX: "+CurX+" "+offsetX);
        Log.d("MainSize", "BtnAddY: "+CurY+" "+offsetY);
    }*/


    private void UIBtnAddFun(){
        UIDontNeedHide();
        scenesAddView.Show();
    }

    //点击场景按钮
    private class ClickViewListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if(lastLong){
                //长按后的松开触发 忽略
                lastLong=false;
            }else{
                Log.d(TAG, "onClick: ");
                UIDontNeedHide();
                GoViewFun(v);
            }
        }
    }

    private class TouchViewListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int fingerCount =event.getPointerCount();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if(lastLong){
                        //长按后的松开触发 忽略
                        lastLong=false;
                    }else{
                        Log.d(TAG, "onClick: ");
                        UIDontNeedHide();
                        GoViewFun(v);
                    }
                    break;
            }
            return false;
        }
    }

    private int CurX=0;
    private int CurY=0;
    private int offsetX=0;
    private int offsetY=0;
    boolean lastLong=false;
    //长按场景按钮
    private class LongClickViewListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            //获取点击序号
            int SceneIndex=GetSceneIndex(v);

            int MaxSceneCnt=DisinfectData.scensItems.length;
            if(SceneIndex>-1 && SceneIndex<MaxSceneCnt) {
                DelSelectIndex = SceneIndex;

                int[] point = new int[2];
                v.getLocationOnScreen(point);

                int sizeW = (int)(MainActivity.Instance.getResources().getDimension(R.dimen.mainScenWidth));
                int sizeH = (int)(MainActivity.Instance.getResources().getDimension(R.dimen.mainScenHeight));

                img_viewDelete.setVisibility(View.VISIBLE);
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) img_viewDelete.getLayoutParams();

                //大屏 -65
                offsetX=-200;
                offsetY=0;
                CurX=point[0]+sizeW;
                CurY=point[1];

                layoutParams.leftMargin = (int)CurX+offsetX;
                layoutParams.topMargin = (int)CurY+offsetY;
                img_viewDelete.setLayoutParams(layoutParams);

                lastLong = true;
            }
            return false;
        }
    }

    public int GetSceneIndex(View v){
        int result=-1;
        switch (v.getId()){
            //点击按钮1
            case R.id.main_img1_1:
            case R.id.main_img2_1:
            case R.id.main_img3_1:
                result=0;
                break;
            //点击按钮2
            case R.id.main_img1_2:
            case R.id.main_img2_2:
            case R.id.main_img3_2:
                result=1;
                break;
            //点击按钮3
            case R.id.main_img1_3:
            case R.id.main_img2_3:
            case R.id.main_img3_3:
                result=2;
                break;
            //点击按钮4
            case R.id.main_img1_4:
            case R.id.main_img2_4:
            case R.id.main_img3_4:
                result=3;
                break;
            //点击按钮5
            case R.id.main_img2_5:
            case R.id.main_img3_5:
                result=4;
                break;
            //点击按钮6
            case R.id.main_img3_6:
                result=5;
                break;
            //单机按钮
            case R.id.main_img0_1:
                result=6;
                break;
        }
        return result;
    }

    //点击按钮
    public void GoViewFun(View v){
        try{
            int SceneIndex=GetSceneIndex(v);
            if(SceneIndex!=-1){
                ClickSceneView(SceneIndex);
            }else {
                switch (v.getId()) {
                    //点击场景
                    case R.id.main_layout1:
                    case R.id.main_layout2:
                    case R.id.main_layout3:
                        UIDontNeedHide();
                        break;
                }
            }
        }catch (Exception e){e.printStackTrace();}
    }

    //隐藏不需要的UI 删除按钮与添加场景UI
    public void UIDontNeedHide(){
        lastLong=false;
        DelSelectIndex=-1;
        img_viewDelete.setVisibility(View.GONE);

        scenesAddView.Hide();
    }

    //删除场景按钮
    public void UIBtnDeleteFun(){
        if(DelSelectIndex>-1 && DelSelectIndex<DisinfectData.scensItems.length) {
            Log.d(TAG, "UIBtnDeleteFun: "+DelSelectIndex);
            int targetNo=DisinfectData.scensItems[DelSelectIndex].id;
            NetWorkGetDecList(String.valueOf(targetNo));
        }
        UIDontNeedHide();
    }

    //更新布局
    public void UpDateSceneView(){
        ScensItem[] scensItems=DisinfectData.scensItems;

        layout0.setVisibility(View.GONE);
        layout1.setVisibility(View.GONE);
        layout2.setVisibility(View.GONE);
        layout3.setVisibility(View.GONE);


        if(HttpUtils.IsOnLine)
        {
            //在线布局
            Log.d(TAG, "UpDateSceneView: " + scensItems.length);
            if (scensItems.length <= 2)
            {
                layout1.setVisibility(View.VISIBLE);
            } else if (scensItems.length <= 3)
            {
                layout2.setVisibility(View.VISIBLE);
            } else
            {
                layout3.setVisibility(View.VISIBLE);
            }
        }else
        {
            //离线布局
            layout0.setVisibility(View.VISIBLE);
        }
    }

    //初始化场景图片
    private void InitSceneImage(){
        Bitmap bm= activity.bm_SceneEmpty;
        //图片框赋值
        if(bm!=null) {
            for (int i = 0; i < imgs_view1.length-2; i++) {
                imgs_view1[i].setImageBitmap(bm);
            }
            for (int i = 0; i < imgs_view2.length-2; i++) {
                imgs_view2[i].setImageBitmap(bm);
            }
            for (int i = 0; i < imgs_view3.length-2; i++) {
                imgs_view3[i].setImageBitmap(bm);
            }
        }
    }

    //更新场景图片
    private void UpDateSceneImage(int id){
        //获取当前使用场景
        ScensItem[] scensItems=DisinfectData.scensItems;
        Bitmap bm=scensItems[id].bm;
        //图片框赋值
        if(bm!=null) {
            Log.d(TAG, "UpDateSceneImage: "+id+" "+scensItems.length);
            if (scensItems.length <= 2 && id<2) {
                imgs_view1[id].setImageBitmap(bm);
            } else if (scensItems.length <= 3 && id<3) {
                imgs_view2[id].setImageBitmap(bm);
            } else  if ( id<4) {
                imgs_view3[id].setImageBitmap(bm);
            }
        }else{
            Log.d(TAG, "UpDateSceneImage: NULL "+id);
        }
    }

    //点击消毒场景
    private void ClickSceneView(int no){
        Log.d(TAG, "ClickSceneView: "+no);
        ScensItem[] scensItems=DisinfectData.scensItems;

        if(HttpUtils.IsOnLine)
        {
            if(DisinfectData.scensItems==null ||
                    DisinfectData.AllScensItems==null ||
                    DisinfectData.ratioItems == null ||
                    DisinfectData.typeItems ==null)
            {
                if(DisinfectData.scensItems==null)
                {
                    activity.MyToast("未查询到房间信息");
                }
                if(DisinfectData.AllScensItems==null)
                {
                    activity.MyToast("未查询到全部房间信息");
                }
                if(DisinfectData.ratioItems==null)
                {
                    activity.MyToast("未查询到浓度信息");
                }
                if(DisinfectData.typeItems==null)
                {
                    activity.MyToast("未查询到消毒剂信息");
                }
            }
            else
            {
                if (scensItems.length == 0 && (no == 0 || no == 1)) {
                    // 无场景数据 点击12
                } else if (scensItems.length == 1 && no == 1) {
                    // 一数据场景 点击2
                } else
                    {
                    int ClickType=0; //0场景 1自定义 2网络化
                    if (scensItems.length <= 2) {
                        if (no == 2) {
                            ClickType=1;
                        }else if (no == 3) {
                            ClickType=2;
                        }
                    } else if (scensItems.length <= 3) {
                        if (no == 3) {
                            ClickType=1;
                        }else if (no == 4) {
                            ClickType=2;
                        }
                    } else{
                        if (no == 4) {
                            ClickType=1;
                        }else if (no == 5) {
                            ClickType=2;
                        }
                    }

                    switch (ClickType){
                        case 0:
                            //房间场景
                            if (no > -1 && no < scensItems.length) {
                                activity.parmFrgm.SetSceneData(scensItems[no].scenesId,scensItems[no]);
                            }
                            activity.ComeGoFrame(MainActivity.flag_parm);
                            break;
                        case 1:
                            //自定义
                            if (DisinfectData.scensCustom!=null) {
                                activity.parmFrgm.SetSceneData(DisinfectData.scensCustom.scenesId,DisinfectData.scensCustom);
                            }
                            activity.parmFrgm.isCustomMode = true;
                            activity.ComeGoFrame(MainActivity.flag_parm);
                            break;
                        case 2:
                            //网络化
                            WebGridFrgm.needInitData =true;
                            activity.ComeGoFrame(MainActivity.flag_webgrid);
                            break;
                    }
                }
            }
        }else
        {
            //离线模式
            //自定义
            if (DisinfectData.scensCustom!=null)
            {
                activity.parmFrgm.SetSceneData(DisinfectData.scensCustom.scenesId,DisinfectData.scensCustom);
            }
            activity.parmFrgm.isCustomMode = true;
            activity.ComeGoFrame(MainActivity.flag_parm);
        }
    }

    private void ReagentOnClick() {
        activity.ComeGoFrame(MainActivity.flag_reagent);
    }

    //网络修改NFC编号
    private void BtnNFCOnClick() {
/*
        data:{"code":10002,"msg":"JSON parse error: Cannot deserialize instance of `java.lang.String` out of START_OBJECT token; nested exception is com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot deserialize instance of `java.lang.String` out of START_OBJECT token\n at [Source: (PushbackInputStream); line: 1, column: 1]","data":null}
        06-09 03:12:30.454 12823-13596/? V/MainFrgm: JSON parse error: Cannot deserialize instance of `java.lang.String` out of START_OBJECT token; nested exception is com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot deserialize instance of `java.lang.String` out of START_OBJECT token
        at [Source: (PushbackInputStream); line: 1, column: 1]
        06-09 03:12:30.454 12823-12823/? V/MainFrgm: NFC: JSON parse error: Cannot deserialize instance of `java.lang.String` out of START_OBJECT token; nested exception is com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot deserialize instance of `java.lang.String` out of START_OBJECT token
        at [Source: (PushbackInputStream); line: 1, column: 1]
        */

        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/user/nfc/create";
                    Map<String, String> params=new HashMap<>();
                    params.put("nfc",Temp_nfc);
                    String result= HttpUtils.PostData(url,params);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_UpdateNFC;
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

    //网络获取场景列表
    public void NetWorkGetNowSceneList(final int userId,final String macAdd){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/user/scenes/list";
                    url+="?userId="+userId+"&macAdd="+macAdd;
                    String result= HttpUtils.GetData(url);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GetNowScenList;
                    Object[] msgData=new Object[3];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==0){
                        //id与数据库数据库有关 不需要管  userId为用户ID  scenesId为特点房间ID
                        //06-29 14:51:41.181 4133-5796/? V/HttpUtils: {"code":0,"msg":"success","data":[{"id":81,"userId":35,"scenesId":1,"icon":"http://192.168.0.152:81/1593239864804001手术室.png","length":1203.0,"width":120.0,"height":50.0,"scenesName":"[场景]01手术室","createTime":"2020-06-29 02:51:42","updateTime":"2020-06-29 02:51:41"}],"count":1}
                        JSONArray list=jsonObject.getJSONArray("data");
                        //成功
                        msgData[0]=true;
                        msgData[1]=jsonObject.getInt("count");
                        msgData[2]=list;
                        Log.v(TAG,data);

                        //String icon="http://192.168.0.152:81/1593239864804001手术室.png";

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

    //网络删除场景
    private void NetWorkGetDecList(final String scenId){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/user/scenes/del";
                    Map<String, String> params=new HashMap<>();
                    params.put("id",scenId);
                    String result= HttpUtils.PostData(url,params);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_ChangeScenList;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==0){
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

    //获取消毒机信息
    public void NetWorkParmData()
    {
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                //获取浓度
                try {
                    String url = HttpUtils.HostURL + "/density/list";
                    String result = HttpUtils.GetData(url);

                    Message msg = activity.FrgmHandler.obtainMessage();
                    msg.what = MainActivity.MsgID_Web_GetRatioList;
                    Object[] msgData = new Object[3];

                    JSONObject jsonObject = new JSONObject(result);
                    Integer code = jsonObject.getInt("code");
                    String data = jsonObject.getString("msg");
                    if (code == 0) {
                        //06-19 09:02:49.214 2954-2977/? V/HttpUtils: {"code":0,"msg":"success","data":[{"densityId":2,"value":30,"isDefault":0,"isDefaultMsg":"默认","createTime":"2020-05-23 05:18:13","updateTime":"2020-05-30 11:36:00"},{"densityId":7,"value":20,"isDefault":1,"isDefaultMsg":"非默认","createTime":"2020-05-27 09:09:02","updateTime":"2020-05-27 09:09:02"}],"count":2}
                        JSONArray list = jsonObject.getJSONArray("data");
                        //成功
                        msgData[0] = true;
                        msgData[1] = jsonObject.getInt("count");
                        msgData[2] = list;
                        Log.v(TAG, data);
                    } else {
                        //失败
                        msgData[0] = false;
                        msgData[1] = data;
                        Log.v(TAG, data);
                    }

                    msg.obj = msgData;
                    activity.FrgmHandler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //获取消毒剂
                try {
                    String url = HttpUtils.HostURL + "/disin/list";
                    String result = HttpUtils.GetData(url);

                    Message msg = activity.FrgmHandler.obtainMessage();
                    msg.what = MainActivity.MsgID_Web_GetTypeList;
                    Object[] msgData = new Object[3];

                    JSONObject jsonObject = new JSONObject(result);
                    Integer code = jsonObject.getInt("code");
                    String data = jsonObject.getString("msg");
                    if (code == 0) {
                        //06-19 09:02:49.214 2954-2977/? V/HttpUtils: {"code":0,"msg":"success","data":[{"densityId":2,"value":30,"isDefault":0,"isDefaultMsg":"默认","createTime":"2020-05-23 05:18:13","updateTime":"2020-05-30 11:36:00"},{"densityId":7,"value":20,"isDefault":1,"isDefaultMsg":"非默认","createTime":"2020-05-27 09:09:02","updateTime":"2020-05-27 09:09:02"}],"count":2}
                        JSONArray list = jsonObject.getJSONArray("data");
                        //成功
                        msgData[0] = true;
                        msgData[1] = jsonObject.getInt("count");
                        msgData[2] = list;
                        Log.v(TAG, data);
                    } else {
                        //失败
                        msgData[0] = false;
                        msgData[1] = data;
                        Log.v(TAG, data);
                    }

                    msg.obj = msgData;
                    activity.FrgmHandler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //！！必须先获取当前使用再获取所有场景！！
    //步骤 1-获取要使用的 2-获取所有场景 3-获取场景图片
    //网络获取场景列表
    public void NetWorkGetAllSceneList(){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/scenes/list";
                    String result= HttpUtils.GetData(url);

                    Message msg= activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GetAllScenList;
                    Object[] msgData=new Object[3];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==0){
                        //id与数据库数据库有关 不需要管  userId为用户ID  scenesId为特点房间ID
                        //06-29 14:51:41.181 4133-5796/? V/HttpUtils: {"code":0,"msg":"success","data":[{"id":81,"userId":35,"scenesId":1,"icon":"http://192.168.0.152:81/1593239864804001手术室.png","length":1203.0,"width":120.0,"height":50.0,"scenesName":"[场景]01手术室","createTime":"2020-06-29 02:51:42","updateTime":"2020-06-29 02:51:41"}],"count":1}
                        JSONArray list=jsonObject.getJSONArray("data");
                        //成功
                        msgData[0]=true;
                        msgData[1]=list.length();
                        msgData[2]=list;
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


    //网络获取场景图片
    private void NetWorkGetAllSceneImg(final ScensItem[] items){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {

                for (int i = 0; i < items.length; i++) {
                    try
                    {
                        if(items[i].icon!="" && !items[i].icon.isEmpty()) {
                            String url = items[i].icon;
                            Bitmap bmp= HttpUtils.GetBitmapData(url);

                            //使用MainHandler
                            Message msg=activity.MainHandler.obtainMessage();
                            msg.what=MainActivity.MsgID_Web_GetAllScenImg;
                            Object[] msgData=new Object[4];

                            if(bmp!=null){
                                //成功
                                msgData[0]=true;
                                msgData[1]=items[i].scenesId;
                                msgData[2]=i;
                                msgData[3]=bmp;
                                Log.v(TAG,items[i].scenesId+"-"+items[i].scenesName+" 下载成功");
                            }else{
                                //失败
                                msgData[0]=false;
                                msgData[1]=i;
                                Log.v(TAG,url+" 下载失败");
                            }

                            msg.obj=msgData;
                            activity.MainHandler.sendMessage(msg);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        //activity.MyToast(e.toString());
                        activity.MyToast("获取场景异常");
                    }
                }
            }
        });
    }

    //网络获取用户标识
    private void NetWorkGetUserRole() {
        //初始化登录用户标识
        DisinfectData.UserIsAdmin=false;

        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/role";
                    String result= HttpUtils.GetData(url);

                    Message msg= activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GetUserType;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==0){
                        JSONObject joData=jsonObject.getJSONObject("data");

                        //成功
                        msgData[0]=true;
                        msgData[1]=joData;
                        Log.v(TAG,data+" "+joData);

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


    @Override
    public void onTouchNext() {
        //ClickSceneView(0);
    }

    @Override
    public void onTouchBack(int flag) {

    }

    private void WebPostFile()
    {
        new Thread(){
            @Override
            public void run() {
                ArrayList<String> list = FileUtils.getTxtFile();
                for (String name:list)
                {
                    try{
                        //String url= HttpUtils.HostURL+"/offline/record/upload";
                        //String result= HttpUtils.PostFile(url,"LoacalData.json");

                        String url= HttpUtils.HostURL+"/offline/record/upload";
                        String result=HttpUtils.uploadLogFile(url,name);
                        Log.d("FileUtils", "WebPostFile: "+name);

                    /*
                    //上传字符串
                    String url=  HttpUtils.HostURL+"/offline/record/upload/str";
                    Map<String, String> params=new HashMap<>();
                    String jsonStr=itemData;
                    jsonStr=jsonStr.replaceAll("\"","\\\\\"");
                    params.put("jsonStr",jsonStr);
                    String result= HttpUtils.PostData(url,params);
                    Log.d(TAG, "WebPostFile: "+result);
                    */

                        Message msg=activity.MainHandler.obtainMessage();
                        msg.what=MainActivity.MsgID_UI_Toast;
                        String data ="";
                        try {
                            JSONObject jsonObject = new JSONObject(result);
                            Integer code = jsonObject.getInt("code");
                            data = jsonObject.getString("data");
                            if (code == 0) {
                                //成功
                                msg.obj = "成功 ";
                                FileUtils.delTxt(name);
                            } else {
                                //失败
                                msg.obj = "失败 ";
                            }
                            msg.obj+=data;
                        }catch (Exception e)
                        {
                            e.printStackTrace();
                            msg.obj ="上传失败 "+result;
                        }

                        activity.MainHandler.sendMessage(msg);

                        Log.d(TAG, "run: "+result);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                try{
                    Thread.sleep(1000);
                }catch (Exception e){}
                ThreadOneSecFun();
            }
        }.start();
    }

    public void ThreadOneSecFun()
    {
        if(HttpUtils.IsOnLine) {
            //更新UI时间
            Message msg = activity.FrgmHandler.obtainMessage();
            msg.what = activity.MsgID_UI_Time;
            msg.obj = AndroidUtils.GetSystemTime();
            activity.FrgmHandler.sendMessage(msg);
        }
    }

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

