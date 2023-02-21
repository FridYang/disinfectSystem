package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.ParmItem;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.FileUtils;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.base.SocketUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.UpDateAppUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;
import com.cczk.lxp.disinfectsystem.view.utils.CustomDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by pc on 2018/2/7.
 */
public class LoginFrgm extends Fragment implements IFrgmCallBack  {
    private static final String TAG = "LoginFrgm";
    private MainActivity activity;
    private static int NowLanguageType=0;
    private boolean isChangeLanguage=true;

    //View
    TextView tv_local;
    TextView tv_name;
    Spinner sp_language;

    Spinner sp_hospital;
    Spinner sp_department;
    Spinner sp_name;
    EditText edit_name;
    EditText edit_password;

    TextView tv_register;

    String[] List_Hos;
    String[] List_Dep;
    ArrayList List_Name=new ArrayList<String>();

    public String cacheHos="";
    public String cacheDep="";
    public String cacheName="";
    public int cacheIndex=0;
    int HideKeyBoardCnt=0;

    //更新
    FrameLayout layout_update;
    TextView tv_update;

    public static final String ACTION_REQUEST_SHUTDOWN = "android.intent.action.ACTION_REQUEST_SHUTDOWN";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_login, container, false);
        activity = (MainActivity) this.getActivity();

        cacheHos=AndroidUtils.QuickDataGet("cacheHos");
        cacheDep=AndroidUtils.QuickDataGet("cacheDep");
        cacheName=AndroidUtils.QuickDataGet("cacheName");
        cacheIndex=AndroidUtils.QuickDataGetInt("cacheIndex");

        //Log.d(TAG, "ThreadOneSecFun: " +(activity==null)+(activity.FrgmHandler==null)+(activity.FrgmHandler.obtainMessage()==null));

        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        //初始化
        sp_language=v.findViewById(R.id.login_sp_language);

        sp_hospital =v.findViewById(R.id.login_sp_info_hospital);
        sp_department =v.findViewById(R.id.login_sp_info_department);
        sp_name =v.findViewById(R.id.login_sp_info_name);
        edit_name =v.findViewById(R.id.login_edit_info_name);
        edit_password =v.findViewById(R.id.login_edit_info_password);
        tv_register=v.findViewById(R.id.login_tv_register);

        //按钮点击
        TextView tv =  v.findViewById(R.id.login_tv_confirm);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConfirmOnClick();
            }
        });
        tv =  v.findViewById(R.id.login_tv_register);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RegisterOnClick();
            }
        });
        //单机模式
        tv_local=v.findViewById(R.id.login_tv_local);
        tv_local.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                //播放NFC音效
                //AndroidUtils.PlayNFCSound();

                LocalLogin();
                //showDialog("测试");
            }
        });
        tv_name=v.findViewById(R.id.login_tv_name);
        tv_name.setText("");
        ImageView img=v.findViewById(R.id.login_img_shutdown);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.ComeGoFrame(MainActivity.flag_toSettings);
            }
        });
        img=v.findViewById(R.id.login_img_restart);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    @Override
                    public void run() {
                        try{
                            Thread.sleep(1000);
                            System.exit(0);
                        }catch (Exception e){}
                    }
                }.start();
            }
        });
        img.setVisibility(View.GONE);

        //文字变色
        sp_language.setOnItemSelectedListener(new SpLanguageSelected());
        sp_hospital.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                if(tv != null)
                {
                    tv.setTextColor(activity.color_tv);
                    tv.setTextSize(27);
                    try {
                        NetWorkGetDep(List_Hos[position]);
                    } catch (Exception e) {
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        sp_department.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                if(tv!=null) {
                    tv.setTextColor(activity.color_tv);
                    tv.setTextSize(27);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        sp_name.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                if(tv!=null) {
                    tv.setTextColor(activity.color_tvB);
                    tv.setTextSize(27);
                }
                try{
                    edit_name.setText(List_Name.get(position).toString());
                    Log.d(TAG, "onItemSelected: "+edit_name.getText().toString());
                }catch (Exception e){e.printStackTrace();}
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //APK更新框
        layout_update = v.findViewById(R.id.login_layout_update);
        tv_update = v.findViewById(R.id.login_tv_update);
        layout_update.setVisibility(View.GONE);

        //机器人跟喷淋不能单机
        if(DisinfectData.HwDevCate!=DisinfectData.Dev_Walker &&
                DisinfectData.HwDevCate!=DisinfectData.Dev_Guard)
        {
            tv_local.setVisibility(View.VISIBLE);
        }else
        {
            tv_local.setVisibility(View.GONE);
        }
    }

    //关机操作
    public void AppClose()
    {
        new Thread(){
            @Override
            public void run() {
                try{
                    Thread.sleep(500);

                    System.exit(0);
                }catch (Exception e){}
            }
        }.start();

        /*
        Log.v(TAG, "broadcast->shutdown");
        Intent intent = new Intent(ACTION_REQUEST_SHUTDOWN);
        intent.putExtra("EXTRA_KEY_CONFIRM", true);
        //当中false换成true,会弹出是否关机的确认窗体
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        */
    }

    //单机登录
    void LocalLogin()
    {
        String nowPw = edit_password.getText().toString();
        String needPw = AndroidUtils.QuickDataGet("settingspw","1234");
        if(nowPw.equals(needPw))
        {
            HttpUtils.IsOnLine=false;
            activity.OtherFrgmComeMain();
            activity.mainFrgm.isUpDateAllScenes =false;

            //单机消毒数据
            ParmItem[] parm=new ParmItem[2];
            parm[0]=new ParmItem("3 ml/m³",0,3,true);
            parm[1]=new ParmItem("6 ml/m³",1,6,false);
            DisinfectData.ratioItems=parm;
        }else
        {
            ShowErrDialog(AndroidUtils.GetStr(R.string.offloginerr));
        }
    }

    //语言选择后
    class SpLanguageSelected implements AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//            TextView tv = (TextView)view;
//            if(tv != null)
//            {
//                tv.setTextColor(ContextCompat.getColor(activity, R.color.uitvcolor));
//            }

            if(!isChangeLanguage)
            {
                isChangeLanguage=true;
            }else {
                //if(NowLanguageType != position)
                {
                    //保存缓存数据
                    Map<String, String> params = new HashMap<>();
                    params.put("NowLanguageType", String.valueOf(position));
                    AndroidUtils.QuickDataSet(params);

                    switch (position) {
                        case 0:
                            //设置中文
                            activity.ChangeLanguage(Locale.SIMPLIFIED_CHINESE);
                            break;
                        case 1:
                            //设置英文
                            activity.ChangeLanguage(Locale.ENGLISH);
                            break;
                    }
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    @Override
    public void InitData() {
        DisinfectData.IsWebUser=false;
        //确定上个用户退出登录
        activity.WebUserLogout();
        SocketUtils.getInstance().IsAddSensort=false;

        //显示下拉框
        sp_hospital.setVisibility(View.VISIBLE);
        sp_department.setVisibility(View.VISIBLE);
        sp_name.setVisibility(View.VISIBLE);

        //默认有网络
        HttpUtils.IsOnLine=true;
        //DisinfectData.IsCarControl=false;

        //NFC
        activity.NFCOpen();

        //获取数据
        //edit_name.setText(AndroidUtils.QuickDataGet("cacheName"));
        //edit_password.setText("");

        //初始化语言相关
        InitLanguage();

        //初始化登录用户标识
        DisinfectData.UserIsAdmin=false;

        NetWorkGetHos();

        //隐藏输入框
        HideKeyBoardCnt=3;
        edit_name.setEnabled(false);
        edit_password.setEnabled(false);
        activity.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        //绿色工作灯
        ControlUtils.getInstance().OpenRGBGreen();

        //初始化姓名
        List_Name.clear();
        for (int i = 0; i < 3; i++)
        {
            String temp = AndroidUtils.QuickDataGet("cacheName"+(i+1));
            //Log.d(TAG, "Get 缓存姓名:"+temp+" -"+"cacheName"+(i+1));
            if(temp.length()>0)
            {
                List_Name.add(temp);
            }
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, List_Name);
        dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
        //sp_name.setAdapter(dataAdapter);
        //sp_name.setSelection(cacheIndex);
    }

    public void InitLanguage(){
        //语言切换
        tv_register.setText(AndroidUtils.GetStr(R.string.register));
        //初始化语言选框
        //声明一个简单simpleAdapter
        SimpleAdapter simpleAdapter =new SimpleAdapter(activity, getListData(), R.layout.item_sppic_,
                new String[]{"npic","namepic"}, new int[]{R.id.imageview,R.id.textview});

        isChangeLanguage=false;
        //sp_language.setAdapter(dataAdapter);
        sp_language.setAdapter(simpleAdapter);

        //获取记录的语言序号
        NowLanguageType=0;
        try{
            NowLanguageType=Integer.parseInt( AndroidUtils.QuickDataGet("NowLanguageType"));
        }catch (Exception e){}

        if(sp_language.getSelectedItemPosition()!=NowLanguageType){
            isChangeLanguage=true;
            sp_language.setSelection(NowLanguageType);
        }
    }

    public List<Map<String, Object>> getListData() {

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        //每个Map结构为一条数据，key与Adapter中定义的String数组中定义的一一对应。

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("npic", R.drawable.flag_zh);
        map.put("namepic", "国旗1");
        list.add(map);

        map= new HashMap<String, Object>();
        map.put("npic", R.drawable.flag_en);
        map.put("namepic", "图片2");
        list.add(map);

        return list;
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

    @Override
    public void FrgmMessage(Message msg) {
        Object[] data;
        int index=0;
        try{
            switch (msg.what){
                //刷卡NFC
                case MainActivity.MsgID_HW_NFCID:
                    Log.v(TAG,"NFCID: "+msg.obj);
                    String Temp_nfc=msg.obj.toString();
                    Temp_nfc=Temp_nfc.replace(" ","");
                    NetWorkLogin(Temp_nfc);
                    break;
                //登录
                case MainActivity.MsgID_Web_Login:
                    Log.d(TAG, "Main:  2");

                    data= (Object[]) msg.obj;
                    Log.v(TAG,"LOGIN: "+data[1]+" "+data[0]);
                    activity.MyToast(data[1].toString());

                    if((boolean)data[0])
                    {
                        if(sp_hospital.getSelectedItemPosition()>=0){
                            cacheHos=sp_hospital.getSelectedItem().toString();
                        }
                        if(sp_department.getSelectedItemPosition()>=0){
                            cacheDep=sp_department.getSelectedItem().toString();
                        }
                        cacheName=edit_name.getText().toString();
                        cacheIndex=sp_name.getSelectedItemPosition();

                        //保存缓存数据
                        Map<String, String> params=new HashMap<>();
                        params.put("cacheHos",cacheHos);
                        params.put("cacheDep",cacheDep);
                        params.put("cacheName",cacheName);
                        params.put("cacheIndex",String.valueOf(cacheIndex));

                        //保存缓存姓名
                        params=NewTempName(params,cacheName);
                        AndroidUtils.QuickDataSet(params);

                        //跳入主界面
//                    activity.OtherFrgmComeMain();
//                    activity.mainFrgm.isUpDateAllScenes =true;

                        Log.d(TAG, "FrgmMessage: 检查是否已绑定好设备");
                        //检查是否已绑定好设备
                        NetWorkBindDev(AndroidUtils.GetMacAddress());

                    }
                    break;
                //检查绑定设备
                case MainActivity.MsgID_Web_CheckBindDev:
                    data= (Object[]) msg.obj;
                    String info=(String)data[1];
                    Integer type=(Integer)data[2];

                    Log.d(TAG, "MsgID_Web_CheckBindDev "+type);
                    if((boolean)data[0]) {
                        //获取数据
                        if(type==0){
                            //未绑定
                            activity.ComeGoFrame(MainActivity.flag_binddev);
                        }else{
                            //已绑定
                            //跳入主界面
                            activity.OtherFrgmComeMain();
                            activity.mainFrgm.isUpDateAllScenes =true;
                        }
                    }else{
                        //异常
                        activity.MyToast(info);
                    }
                    break;
                case MainActivity.MsgID_UI_Time:
                    //初始化数据
                    if(sp_hospital.getSelectedItemPosition()==-1)
                    {
                        //InitData();
                        NetWorkGetHos();
                    }

                    if(HideKeyBoardCnt>0)
                    {
                        HideKeyBoardCnt--;

                        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        //imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS,0);
                        imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                        if(HideKeyBoardCnt<=0)
                        {
                            edit_name.setEnabled(true);
                            edit_password.setEnabled(true);
                        }
                    }
                    break;

                //获取医院
                case MainActivity.MsgID_Web_GetHos:
                    data= (Object[]) msg.obj;
                    MainActivity.mainTest="GetHos:"+data.length+"\r\n";
                    cacheHos=AndroidUtils.QuickDataGet("cacheHos");

                    if((boolean)data[0]){
                        JSONArray list= (JSONArray) data[1];
                        List_Hos =new String[list.length()];
                        for (int i = 0; i < list.length(); i++) {
                            try {
                                String temp=list.get(i).toString();
                                List_Hos[i]=temp;

                                if(temp.equals(cacheHos)){
                                    index=i;
                                }
                            } catch (JSONException e) {}
                        }
                        //初始化选框
                        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                                android.R.layout.simple_spinner_item, List_Hos);
                        dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                        sp_hospital.setAdapter(dataAdapter);

                        Log.d(TAG, "FrgmMessage:hos "+cacheHos+"-"+index);
                        sp_hospital.setSelection(index);
                    }else{
                        activity.MyToast(data[1].toString());
                    }

                    //机器名称
                    String name=data[2].toString();
                    int len=name.length();
                    if(len>5)
                    {
                        len=5;
                    }
                    tv_name.setText(name.substring(0,len));

                    //刷新获取设备类型
                    activity.NetWorkGetDevCate(AndroidUtils.GetMacAddress());
                    break;
                //获取科室
                case MainActivity.MsgID_Web_GetDep:
                    data= (Object[]) msg.obj;
                    cacheDep=AndroidUtils.QuickDataGet("cacheDep");

                    if((boolean)data[0]){
                        JSONArray list= (JSONArray) data[1];
                        List_Dep =new String[list.length()];
                        for (int i = 0; i < list.length(); i++) {
                            try {
                                String temp=list.get(i).toString();
                                List_Dep[i]=temp;

                                if(temp.equals(cacheDep)){
                                    index=i;
                                }
                            } catch (JSONException e) {}
                        }
                        //初始化选框
                        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                                android.R.layout.simple_spinner_item, List_Dep);
                        dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                        sp_department.setAdapter(dataAdapter);

                        Log.d(TAG, "FrgmMessage:dep "+cacheDep+"-"+index);
                        sp_department.setSelection(index);
                    }else{
                        activity.MyToast(data[1].toString());
                    }
                    break;
            }
        }catch (Exception e){e.printStackTrace();}
    }

    private void ScanOnClick(){

    }

    private void RegisterOnClick() {
        activity.ComeGoFrame(MainActivity.flag_register);
        //activity.WebTest();
    }

    public void StartApkUpDate(String url)
    {
        layout_update.setVisibility(View.VISIBLE);

        tv_update.setText("更新Apk 0%");
        //url="http://192.168.0.152:885/update1_V1.apk";
        UpDateAppUtils.getInstance().downloadAPK(url, tv_update);

    }

    private void ConfirmOnClick() {
        String name=edit_name.getText().toString();
        String password=edit_password.getText().toString();
        int hosIndex=sp_hospital.getSelectedItemPosition();
        int depIndex=sp_department.getSelectedItemPosition();
        if(!name.isEmpty() && !password.isEmpty() &&
                hosIndex!= -1 && depIndex!=-1 ) {
            NetWorkLogin(List_Hos[hosIndex],List_Dep[depIndex], name,password);
            //activity.MyToast("登录中...");
        }else{
            ShowErrDialog(AndroidUtils.GetStr(R.string.loginerr));
        }
    }

    // ====================================== 手势 ======================================

    @Override
    public void onTouchNext() {
        Log.d(TAG, "Main:  onTouchNext");
        ConfirmOnClick();
    }

    @Override
    public void onTouchBack(int listFlag) {
//        if(listFlag<200) {
//            activity.FrgmGoBack();
//        }

    }

    public void NetWorkLogin(String host,String dep,String name,String password)
    {
        NetWorkLogin(host,dep,name,password,"");

    }

    public void NetWorkLogin(String nfc){
        if(!nfc.isEmpty()){
            NetWorkLogin("","","","",nfc);
        }else{
            activity.MyToast(AndroidUtils.GetStr(R.string.scenerr));
        }
    }

    public void NetWorkLogin(final String hosName,final String departName,final String name, final String password,final String nfc){
        //Log.d(TAG, "Main:  1");

        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/login";
                    Map<String, String> params=new HashMap<>();
                    params.put("hosName",hosName);
                    params.put("departName",departName);
                    params.put("userName",name);
                    params.put("password",password);
                    //0917 测试更新 改成2
                    params.put("sign","2");//登录标识  DEVICE = "0";  //机台
                    params.put("macAdd", AndroidUtils.GetMacAddress());
                    params.put("nfc",nfc);

                    String result= HttpUtils.PostData(url,params);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_Login;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==0){
                        //{"msg":"SUCCESS，LOGIN","code":0,"token":"a79bc65a-3b3f-42ca-8fbb-764a184505b5"}

                        String token= jsonObject.getString("token");
                        HttpUtils.NetWork_ToKen=token;
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

                    activity.MyToastThread("网络连接异常");
                }
            }
        });
    }

    public void NetWorkGetHos(){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/user/hos?isLogin=true&macAdd="+AndroidUtils.GetMacAddress();
                    String result= HttpUtils.GetData(url);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GetHos;
                    Object[] msgData=new Object[3];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    String name= jsonObject.getString("name");
                    if(code==0){
                        //{"code":0,"msg":"success","data":["1","东莞人民医院","中山人民医院","医院1","广州人民医院","惠州人民医院","深圳人民医院"]}
                        JSONArray list=jsonObject.getJSONArray("data");
                        //成功
                        msgData[0]=true;
                        msgData[1]=list;
                        msgData[2]=name;
                        Log.v(TAG,data);
                    }else{
                        //失败
                        msgData[0]=false;
                        msgData[1]=data;
                        msgData[2]=name;
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

    public void NetWorkGetDep(final String hos){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/user/depart?hosName="+hos;
                    String result= HttpUtils.GetData(url);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GetDep;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==0){
                        //{"code":0,"msg":"success","data":["1","东莞人民医院","中山人民医院","医院1","广州人民医院","惠州人民医院","深圳人民医院"]}
                        JSONArray list=jsonObject.getJSONArray("data");
                        //成功
                        msgData[0]=true;
                        msgData[1]=list;
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

    //初始化绑定设备
    public void NetWorkBindDev(final String mac){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/device/check?macAdd="+mac;
                    String result= HttpUtils.GetData(url);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_CheckBindDev;
                    Object[] msgData=new Object[3];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String msgStr= jsonObject.getString("msg");
                    Integer data= jsonObject.getInt("data");
                    if(code==0){
                        //成功
                        msgData[0]=true;
                        msgData[1]=msgStr;
                        msgData[2]=data;
                    }else{
                        //失败
                        msgData[0]=false;
                        msgData[1]=msgStr;
                        msgData[2]=data;
                    }
                    Log.v(TAG,"绑定设备 "+msgStr+" "+data);

                    msg.obj=msgData;
                    activity.FrgmHandler.sendMessage(msg);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    public Map<String, String> NewTempName(Map<String, String> params,String temp)
    {
        //保存缓存姓名
        if(!List_Name.contains(temp))
        {
            List_Name.add(temp);
        }
        if(List_Name.size()>3){
            for (int i = 0; i < List_Name.size()-3; i++)
            {
                List_Name.remove(0);
            }
        }
        for (int i = 0; i < List_Name.size(); i++)
        {
            params.put("cacheName"+(i+1),List_Name.get(i).toString());
            Log.d(TAG, "Set 缓存姓名:"+"temp"+(i+1)+":"+List_Name.get(i).toString());
        }
        /*
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, List_Name);
        dataAdapter.setDropDownViewResource(R.layout.str_dropdown);
        sp_name.setAdapter(dataAdapter);
        */

        return params;
    }

    public void ShowErrDialog(String title)
    {
        CustomDialog.Builder builder = new CustomDialog.Builder(activity);
        builder.setTitle(title);
        builder.hideButtonCancel();
        builder.setButtonConfirm( "确认", null);

        CustomDialog customDialog = builder.create();
        customDialog.show();
    }

//    private void showDialog(String title) {
//        View.OnClickListener onCancelClickListener = new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                MainActivity.Instance.MyToastThread("取消");
//            }
//        };
//
//        View.OnClickListener onConfimClickListener = new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                MainActivity.Instance.MyToastThread("确认");
//            }
//        };
//
//        CustomDialog.Builder builder = new CustomDialog.Builder(MainActivity.Instance);
//        builder.setTitle(title);
//        builder.setButtonCancel( "取消", onCancelClickListener);
//        builder.setButtonConfirm( "确认", onConfimClickListener);
//
//        CustomDialog customDialog = builder.create();
//        customDialog.show();
//    }

    public void ThreadOneSecFun() {

        //更新UI时间
        Message msg = activity.FrgmHandler.obtainMessage();
        msg.what = activity.MsgID_UI_Time;
        msg.obj = AndroidUtils.GetSystemTime();
        activity.FrgmHandler.sendMessage(msg);
    }

}

