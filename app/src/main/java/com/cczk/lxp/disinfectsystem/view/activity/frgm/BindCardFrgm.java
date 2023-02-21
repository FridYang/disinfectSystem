package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.os.Message;
import android.os.Bundle;
import android.util.Log;
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
import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
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
public class BindCardFrgm extends Fragment implements IFrgmCallBack {
    private static final String TAG = "BindCardFrgm";
    private MainActivity activity;

    Spinner sp_name;
    TextView tv_card;

    int[] List_UserId;
    int ChoiceId =-1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_bindcard, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        //获取控件
        sp_name=v.findViewById(R.id.bindcard_spin_name);
        tv_card=v.findViewById(R.id.bindcard_tv_card);

        //回退按钮
        ImageView img=v.findViewById(R.id.bindcard_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.AllBtnFrgmHome();
            }
        });

        sp_name.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                tv.setTextColor(activity.color_tv);
                tv.setTextSize(27);

                if(position >=0 && position< List_UserId.length) {
                    ChoiceId = List_UserId[position];
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void FrgmMessage(Message msg) {
        try{
            Object[] data;
        switch (msg.what){
            case MainActivity.MsgID_Web_GetCardName:
                 data=(Object[]) msg.obj;
                if((boolean)data[0])
                {
                    JSONArray list= (JSONArray) data[1];
                    List_UserId =new int[list.length()];
                    String[] strList =new String[list.length()];

                    String str=data[1].toString();
                    Log.d(TAG, "GetCardName0: "+str);
                    Log.d(TAG, "GetCardName1: "+str.length());
                    /*
                    for (int k = 1; k < 200; k++)
                    {
                        if(str.length()>=k*300)
                        {
                            Log.d(TAG, "GetCardName: "+k+" "+str.substring((k-1)*300,k*300));
                        }
                    }*/
                    Log.d(TAG, "GetCardName2: "+list.length());
                    for (int i = 0; i < list.length(); i++)
                    {
                        try {
                            JSONObject jo=(JSONObject)list.get(i);
                            strList[i]=jo.getString("hosName")+":"+jo.getString("userName");
                            List_UserId[i]=jo.getInt("userId");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    //初始化选框
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                            android.R.layout.simple_spinner_item, strList);
                    dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                    sp_name.setAdapter(null);
                    Log.d(TAG, "End GetCardName: "+strList.length);
                    sp_name.setAdapter(dataAdapter);
                }else{
                    Log.d(TAG, "Err GetCardName: "+data[1].toString());
                    activity.MyToast(data[1].toString());
                }
                break;
            //刷卡NFC
            case MainActivity.MsgID_HW_NFCID:
                Log.v(TAG,"NFCID: "+msg.obj);
                String Temp_nfc=msg.obj.toString();
                Temp_nfc=Temp_nfc.replace(" ","");
                tv_card.setText(Temp_nfc);
                break;
            case MainActivity.MsgID_Web_UpdateNFC:
                data= (Object[]) msg.obj;
                Log.v(TAG,"NFC: "+data[1]);
                activity.MyToast(data[1].toString());
                if((boolean)data[0]){
                    activity.MyToast(AndroidUtils.GetStr(R.string.bindsuccee));

                    //清除本场景跳转
                    activity.ClearFrgmList();
                    activity.SetFrgmChange(MainActivity.flag_login);

                    //跳入主界面
                    activity.OtherFrgmComeMain();
                }
                break;
        }

        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public void InitData() {
        //初始化
        NetWorkGetName();

        ChoiceId =-1;
        tv_card.setText("");

        //NFC
        activity.NFCOpen();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden)
        {
            InitData();
        }else
        {
            activity.NFCClose();
        }
    }

    @Override
    public void onTouchNext() {
        String nfc=tv_card.getText().toString();
        if(!nfc.isEmpty() &&
            nfc!="" )
            //&& ChoiceId!=-1 )
        {
            //NetWorkBindCard(ChoiceId,nfc);
            NetWorkBindCard(DisinfectData.planItem.userId,nfc);
        }
    }

    @Override
    public void onTouchBack(int flag) {}

//    public void ThreadOneSecFun() {
//        //更新UI时间
//        Message msg = activity.FrgmHandler.obtainMessage();
//        msg.what = activity.MsgID_UI_Time;
//        msg.obj = AndroidUtils.GetSystemTime();
//        activity.FrgmHandler.sendMessage(msg);
//    }

    public void NetWorkGetName(){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    //组合数据
                    String url= HttpUtils.HostURL+"/user/list";
                    String parm="?";
                    parm+="page=1&";
                    parm+="limit=999";
                    String result= HttpUtils.GetData(url+parm);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GetCardName;
                    Object[] msgData=new Object[2];
                    Log.d(TAG, "GetCardName: "+result);

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
                    activity.MyToastThread("获取用户名异常");
                }
            }
        });
    }

    public void NetWorkBindCard(final int id,final String nfc){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/user/nfc/create";
                    Map<String, String> params=new HashMap<>();
                    params.put("nfc",nfc);
                    Map<String, Integer> paramsInt=new HashMap<>();
                    paramsInt.put("userId",id);
                    String result= HttpUtils.PostData(url,params,paramsInt);

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
}

