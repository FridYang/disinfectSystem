package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.os.Message;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pc on 2018/2/7.
 */
public class BindDevFrgm extends Fragment implements IFrgmCallBack {
    private static final String TAG = "BindDevFrgm";
    private MainActivity activity;

    Spinner sp_devcate;
    Spinner sp_devname;
    TextView tv_mac;

    String[] List_Cate;
    Integer[] List_CateId;
    String[] List_Name;

    String ChoiceDevName="";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_binddev, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        //获取控件
        sp_devcate=v.findViewById(R.id.binddev_spin_devcate);
        sp_devname=v.findViewById(R.id.binddev_spin_devname);
        tv_mac=v.findViewById(R.id.binddev_tv_mac);

        //回退按钮
        ImageView img=v.findViewById(R.id.binddev_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.BtnFrgmLogin();
            }
        });

        sp_devcate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                tv.setTextColor(activity.color_tv);
                tv.setTextSize(27);
                try{
                    NetWorkGetDevName(List_CateId[position]);
                }catch (Exception e){}
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        sp_devname.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                tv.setTextColor(activity.color_tv);
                tv.setTextSize(27);

                if(position >=0 && position< List_Name.length) {
                    ChoiceDevName=List_Name[position];
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public void FrgmMessage(Message msg) {
        try{

        Object[] data=(Object[]) msg.obj;
        switch (msg.what){
            case MainActivity.MsgID_Web_GetDevCate:
                if((boolean)data[0]){
                    JSONArray list= (JSONArray) data[1];
                    List_Cate =new String[list.length()];
                    List_CateId =new Integer[list.length()];
                    for (int i = 0; i < list.length(); i++) {
                        try {
                            JSONObject jo=(JSONObject)list.get(i);
                            List_Cate[i]=jo.getString("devCateName");
                            List_CateId[i]=jo.getInt("devCateId");
                        } catch (JSONException e) {}
                    }
                    //初始化选框
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                            android.R.layout.simple_spinner_item, List_Cate);
                    dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                    sp_devcate.setAdapter(dataAdapter);

                    sp_devname.setAdapter(null);
                }else{
                    activity.MyToast(data[1].toString());
                }
                break;
            case MainActivity.MsgID_Web_GetDevName:
                if((boolean)data[0]){
                    JSONArray list= (JSONArray) data[1];
                    List_Name =new String[list.length()];
                    for (int i = 0; i < list.length(); i++) {
                        try {
                            List_Name[i]=((JSONObject)list.get(i)).getString("devName");
                        } catch (JSONException e) {}
                    }
                    //初始化选框
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                            android.R.layout.simple_spinner_item, List_Name);
                    dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                    sp_devname.setAdapter(dataAdapter);
                }else{
                    activity.MyToast(data[1].toString());
                }
                break;
            case MainActivity.MsgID_Web_BindDev:
                activity.MyToast(data[1].toString());
                if((boolean)data[0]){

                    //清除本场景跳转
                    activity.ClearFrgmList();
                    activity.SetFrgmChange(MainActivity.flag_login);

                    //跳入主界面
                    activity.OtherFrgmComeMain();
                    activity.mainFrgm.isUpDateAllScenes =true;
                }
                break;
        }

        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void InitData() {
        //初始化
        NetWorkGetDevCate();

        ChoiceDevName="";
        tv_mac.setText("MAC:"+AndroidUtils.GetMacAddress());
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            InitData();
        }
    }

    @Override
    public void onTouchNext() {
        if(ChoiceDevName!="")
        {
            NetWorkBindDev(ChoiceDevName,AndroidUtils.GetMacAddress());
        }
    }

    @Override
    public void onTouchBack(int flag) {

    }

//    public void ThreadOneSecFun() {
//        //更新UI时间
//        Message msg = activity.FrgmHandler.obtainMessage();
//        msg.what = activity.MsgID_UI_Time;
//        msg.obj = AndroidUtils.GetSystemTime();
//        activity.FrgmHandler.sendMessage(msg);
//    }

    public void NetWorkGetDevCate(){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    ChoiceDevName="";

                    String url= HttpUtils.HostURL+"/device/user/dev/cate";
                    String result= HttpUtils.GetData(url);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GetDevCate;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String msgStr= jsonObject.getString("msg");
                    if(code==0){
                        JSONArray list=jsonObject.getJSONArray("data");
                        //成功
                        msgData[0]=true;
                        msgData[1]=list;
                    }else{
                        //失败
                        msgData[0]=false;
                        msgData[1]=msgStr;
                    }

                    msg.obj=msgData;
                    activity.FrgmHandler.sendMessage(msg);
                }catch (Exception e){
                    e.printStackTrace();
                    activity.MyToastThread("获取设备类型异常");
                }
            }
        });
    }

    public void NetWorkGetDevName(final int CateId){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    ChoiceDevName="";

                    //组合数据
                    String url= HttpUtils.HostURL+"/device/list";
                    String parm="?";
                    parm+="page=1&";
                    parm+="limit=10000&";
                    parm+="macStatus=0&";
                    parm+="devCateId="+CateId;
                    String result= HttpUtils.GetData(url+parm);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GetDevName;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String msgStr= jsonObject.getString("msg");
                    if(code==0){
                        JSONArray list=jsonObject.getJSONArray("data");
                        //成功
                        msgData[0]=true;
                        msgData[1]=list;
                    }else{
                        //失败
                        msgData[0]=false;
                        msgData[1]=msgStr;
                    }

                    msg.obj=msgData;
                    activity.FrgmHandler.sendMessage(msg);
                }catch (Exception e){
                    e.printStackTrace();
                    activity.MyToastThread("获取设备名异常");
                }
            }
        });
    }

    public void NetWorkBindDev(final String DevName,final String Mac){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    //组合数据
                    String url= HttpUtils.HostURL+"/device/mac/bind";
                    Map<String, String> params=new HashMap<>();
                    params.put("devName",DevName);
                    params.put("macAdd",Mac);
                    String result= HttpUtils.PostData(url,params);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_BindDev;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String msgStr= jsonObject.getString("data");
                    if(code==0){
                        //成功
                        msgData[0]=true;
                        msgData[1]=msgStr;
                    }else{
                        //失败
                        msgData[0]=false;
                        msgData[1]=msgStr;
                    }

                    msg.obj=msgData;
                    activity.FrgmHandler.sendMessage(msg);
                }catch (Exception e){
                    e.printStackTrace();
                    activity.MyToastThread("绑定设备异常");
                }
            }
        });
    }
}

