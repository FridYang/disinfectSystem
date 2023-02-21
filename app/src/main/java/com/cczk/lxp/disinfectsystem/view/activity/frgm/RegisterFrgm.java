package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.FileUtils;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;
import com.cczk.lxp.disinfectsystem.view.utils.CustomDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pc on 2018/2/7.
 */
public class RegisterFrgm extends Fragment implements IFrgmCallBack  {
    private static final String TAG = "RegisterFrgm";
    private MainActivity activity;

    //View
    Spinner sp_hospital;
    Spinner sp_department;
    Spinner sp_type;
    EditText edit_name;
    EditText edit_password;
    EditText edit_phone;

    String[] List_Hos;
    String[] List_Dep;

    private int VersionCnt=0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_register, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        //Init
        sp_hospital =v.findViewById(R.id.register_edit_info_hospital);
        sp_department =v.findViewById(R.id.register_edit_info_department);
        sp_type =v.findViewById(R.id.register_sp_info_type);
        edit_name =v.findViewById(R.id.register_edit_info_name);
        edit_password =v.findViewById(R.id.register_edit_info_password);
        edit_phone =v.findViewById(R.id.register_edit_info_phone);

        //按钮点击
        TextView tv =  v.findViewById(R.id.register_tv_confirm);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConfirmOnClick();
            }
        });
        ImageView img=v.findViewById(R.id.register_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.FrgmGoBack();
            }
        });

         img=v.findViewById(R.id.register_img_version);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowVersionClick();
            }
        });


        //初始化下拉选框
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, new String[]{"M 管理员","E 执行员"});
        dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
        sp_type.setAdapter(dataAdapter);
        //sp_Data.setSelection(Reader.CARD_WARM_RESET);

        sp_hospital.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                tv.setTextColor(activity.color_tv);
                tv.setTextSize(27);
                try{
                    NetWorkGetDep(List_Hos[position]);
                }catch (Exception e){}
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        sp_department.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                tv.setTextColor(activity.color_tv);
                tv.setTextSize(27);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public void FrgmMessage(Message msg) {
        Object[] data=(Object[]) msg.obj;
        switch (msg.what){
            case MainActivity.MsgID_Web_Register:
                String show=data[1].toString();
                if((boolean)data[0]){
                    //activity.ComeGoFrame(MainActivity.flag_login);
                    //activity.MyToast("注册成功");
                    ShowSuccessDialog("用户名为："+data[1].toString());
                }else{
                    activity.MyToast(show);
                }
                break;
            case MainActivity.MsgID_Web_GetHos:
                if((boolean)data[0]){
                    JSONArray list= (JSONArray) data[1];
                    List_Hos =new String[list.length()];
                    for (int i = 0; i < list.length(); i++) {
                        try {
                            List_Hos[i]=list.get(i).toString();
                        } catch (JSONException e) {}
                    }
                    //初始化选框
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                            android.R.layout.simple_spinner_item, List_Hos);
                    dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                    sp_hospital.setAdapter(dataAdapter);
                }else{
                    activity.MyToast(data[1].toString());
                }
                break;
            case MainActivity.MsgID_Web_GetDep:
                if((boolean)data[0]){
                    JSONArray list= (JSONArray) data[1];
                   List_Dep =new String[list.length()];
                    for (int i = 0; i < list.length(); i++) {
                        try {
                            List_Dep[i]=list.get(i).toString();
                        } catch (JSONException e) {}
                    }
                    //初始化选框
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                            android.R.layout.simple_spinner_item, List_Dep);
                    dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                    sp_department.setAdapter(dataAdapter);
                }else{
                    activity.MyToast(data[1].toString());
                }
                break;
        }
    }


    @Override
    public void InitData() {
        edit_name.setText("");
        edit_password.setText("");
        edit_phone.setText("");

        NetWorkGetHos();

        VersionCnt=0;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            InitData();
        }
    }

    private void ConfirmOnClick() {
        String name=edit_name.getText().toString().trim();
        String password=edit_password.getText().toString().trim();
        String phone=edit_phone.getText().toString().trim();

        if(!name.isEmpty() && !password.isEmpty() && !phone.isEmpty() &&
           sp_hospital.getSelectedItemPosition()!= -1 &&
           sp_department.getSelectedItemPosition()!=-1 )
        {
            Log.d(TAG, "ConfirmOnClick: "+name);

            String host = List_Hos[sp_hospital.getSelectedItemPosition()];
            String department = List_Dep[sp_department.getSelectedItemPosition()];

            Map<String, String> params = new HashMap<>();
            params.put("hosName", host);
            params.put("departName", department);
            params.put("userName", name);
            params.put("userTel", phone);
            params.put("password", password);
            NetWorkRegister(params);

            //getRequestData: {"userTel":"4321","departName":"部门1","password":"1234","hosName":"医院1"}
            //data:{"code":0,"msg":"success","data":{"userStatus":"正常","hosName":"医院1","userName":"E002","departName":"部门1"}}

        }else{
            ShowErrDialog("注册信息不完整");
        }

    }

    @Override
    public void onTouchNext() {
        ConfirmOnClick();
    }

    @Override
    public void onTouchBack(int flag) {

    }

    public void NetWorkRegister(final Map<String, String> params_str){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/user/reg";
                    String result= HttpUtils.PostData(url,params_str);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_Register;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==0){
                        //成功
                        msgData[0]=true;

                        JSONObject joData=jsonObject.getJSONObject("data");
                        msgData[1]=joData.getString("userName");
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
                    activity.MyToastThread("注册用户异常");
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
                    String url= HttpUtils.HostURL+"/user/hos?isLogin=true";
                    String result= HttpUtils.GetData(url);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GetHos;
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
                    activity.MyToastThread("获取医院异常");
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
                    activity.MyToastThread("获取设备异常");
                }
            }
        });
    }


    public void ShowSuccessDialog(String data)
    {
        View.OnClickListener onConfimClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.FrgmGoBack();
            }
        };
        CustomDialog.Builder builder = new CustomDialog.Builder(MainActivity.Instance);
        builder.setTitle(data);
        builder.setButtonConfirm( "确定", onConfimClickListener);
        builder.hideButtonCancel();

        CustomDialog customDialog = builder.create();
        customDialog.show();
    }

    public void ShowErrDialog(String data){
        CustomDialog.Builder builder = new CustomDialog.Builder(activity);
        builder.setTitle(data);
        builder.hideButtonCancel();
        builder.setButtonConfirm( "确定", null);

        CustomDialog customDialog = builder.create();
        customDialog.show();
    }

    public void ShowVersionClick(){
        VersionCnt++;
        if(VersionCnt>=6){
            VersionCnt=0;

            ArrayList<String> list = FileUtils.getTxtFile();
            for (String name:list) {
                FileUtils.delTxt(name);
            }
            activity.MyToast("Ver:"+AndroidUtils.Version+" Mac:"+AndroidUtils.GetMacAddress());
        }
    }
}

