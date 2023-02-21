package com.cczk.lxp.disinfectsystem.test;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.NFCUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lxp on 2020/4/25.
 */

public class NFCTestActivity extends Activity {
    public static NFCTestActivity activity;

    private TextView tv_Id;
    public TextView tv_Info;
    public TextView tv_Type;
    private Spinner sp_Data;
    private Button btn_Data;

    Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity=this;
        setContentView(R.layout.activity_testnfc);

        InitView();

        NFCUtils.getInstance().Init(this);
        //NFCUtils.getInstance().DeviceOpen(handler);
    }

    private void InitView(){
        tv_Id=findViewById(R.id.test_text_id);
        tv_Info=findViewById(R.id.test_text_info);
        tv_Type=findViewById(R.id.test_text_type);
        sp_Data=findViewById(R.id.test_spinner_data);
        btn_Data=findViewById(R.id.test_button_SetData);
//
//        //初始化选框
//        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
//                android.R.layout.simple_spinner_item, new String[]{"00","11","01","10"});
//        sp_Data = (Spinner) findViewById(R.id.test_spinner_data);
//        sp_Data.setAdapter(dataAdapter);
//        //sp_Data.setSelection(Reader.CARD_WARM_RESET);
//
//        //初始化按钮方法
//        btn_Data.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //载入密钥
//                NFCUtils.getInstance().SendMsg("FF 82 00 00 06 FF FF FF FF FF FF");
//                //认证密钥
//                NFCUtils.getInstance().SendMsg("FF 86 00 00 05 01 00 01 60 00");
//
//                //设置数据
//                String data="00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
//                switch (sp_Data.getSelectedItemPosition()){
//                    case 0:
//                        data="00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
//                        break;
//                    case 1:
//                        data="11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11";
//                        break;
//                    case 2:
//                        data="01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01";
//                        break;
//                    case 3:
//                        data="10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10";
//                        break;
//                }
//                NFCUtils.getInstance().SendMsg("FF D6 00 01 10 "+data);
//            }
//        });

        handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                String info=msg.obj.toString();
                ShowInfo(info);
                MainActivity.Instance.MyToast(info);
            }
        };
    }

    public void ShowId(String str){
        tv_Id.setText(str);
    }

    public void ShowInfo(String str){
        tv_Info.setText(str);
    }

    public void ShowType(String str){
        tv_Type.setText(str);
    }


    public void Btn1(View view) {
        //NFCUtils.getInstance().ResultList.clear();
        //tv_Info.setText("");
    }

    public void BtnCreate(View view) {
        new Thread(){
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/user/nfc/create";
                    Map<String, Integer> params_int=new HashMap<>();
                    params_int.put("userId",1);
                    Map<String, String> params_str=new HashMap<>();
                    params_str.put("nfc","12345");

                    String result= HttpUtils.PostData(url,params_str,params_int);

                    Message msg=handler.obtainMessage();

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==200){
                        //成功
                        msg.obj="成功 ";
                    }else{
                        //失败
                        msg.obj="失败 ";
                    }
                    msg.obj+=data;

                    handler.sendMessage(msg);

                    //String json="{\"id\":1,\"nfc\":\"12348\"}";
                    //HttpUtils.PostData(url,json);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void BtnUpdate(View view) {
        new Thread(){
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/user/nfc/update";
                    Map<String, Integer> params_int=new HashMap<>();
                    params_int.put("userId",1);
                    Map<String, String> params_str=new HashMap<>();
                    params_str.put("nfc","12345");

                    String result= HttpUtils.PostData(url,params_str,params_int);

                    Message msg=handler.obtainMessage();

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==200){
                        //成功
                        msg.obj="成功 ";
                    }else{
                        //失败
                        msg.obj="失败 ";
                    }
                    msg.obj+=data;

                    handler.sendMessage(msg);

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();

        /*
        {"code":400,"msg":"用户已录入NFC卡信息","data":null}
        {"code":200,"msg":"success","data":"修改成功"}
        {"code":401,"msg":"NFC卡未找到用户"}
         */
    }

    public void BtnLogin(View view) {
        new Thread(){
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/login";
                    Map<String, String> params=new HashMap<>();
                        params.put("userName","");
                        params.put("password","");
                        params.put("nfc","12345");


                    String result= HttpUtils.PostData(url,params);

                    Message msg=handler.obtainMessage();

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==200){
                        //成功
                        msg.obj="成功 ";
                    }else{
                        //失败
                        msg.obj="失败 ";
                    }
                    msg.obj+=data;

                    handler.sendMessage(msg);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

//    public void Btn2(View view) {
//        NFCUtils.getInstance().FunPower();
//    }
//    public void Btn3(View view) {
//        NFCUtils.getInstance().FunSetProtocol();
//    }
//    public void Btn4(View view) {
//        NFCUtils.getInstance().FunDataId();
//    }
//    public void Btn5(View view) {
//        NFCUtils.getInstance().FunOpen();
//    }
}
