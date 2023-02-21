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

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.ParmItem;
import com.cczk.lxp.disinfectsystem.bean.PlanItem;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.base.SocketUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.CarPlanUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
/**
 * Created by pc on 2018/2/7.
 */
public class CarChoiceFrgm extends Fragment implements IFrgmCallBack  {
    private static final String TAG = "CarChoiceFrgm";
    private MainActivity activity;

    //View
    Spinner sp_map;
    Spinner sp_reagent;
    Spinner sp_ratio;
    Spinner sp_type;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_car, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        //Init
        sp_map =v.findViewById(R.id.car_sp_map);
        sp_reagent =v.findViewById(R.id.car_sp_reagent);
        sp_ratio =v.findViewById(R.id.car_sp_ratio);
        sp_type =v.findViewById(R.id.car_sp_type);

        String[] List_Type = new String[]{"定点模式","移动模式","混合模式"};
        ArrayAdapter typeAdapter = new ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, List_Type);
        typeAdapter.setDropDownViewResource(R.layout.str_dropdown2);
        sp_type.setAdapter(typeAdapter);

        //按钮点击
        ImageView img=v.findViewById(R.id.car_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.FrgmGoBack();
            }
        });

        sp_map.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                if(tv!=null) {
                    tv.setTextColor(activity.color_tv);
                    tv.setTextSize(27);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        sp_reagent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                if(tv!=null) {
                    tv.setTextColor(activity.color_tv);
                    tv.setTextSize(27);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        sp_ratio.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                if(tv!=null) {
                    tv.setTextColor(activity.color_tv);
                    tv.setTextSize(27);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        sp_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                if(tv!=null) {
                    tv.setTextColor(activity.color_tv);
                    tv.setTextSize(27);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void FrgmMessage(Message msg) {
        Object[] data=(Object[]) msg.obj;

        String[] List_Item;
        ArrayAdapter<String> dataAdapter;

        switch (msg.what){
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

                        //初始化 浓度 选框
                        Log.d(TAG, "InitData: "+DisinfectData.ratioItems);
                        if(DisinfectData.ratioItems!=null) {
                            List_Item = new String[DisinfectData.ratioItems.length];
                            for (int i = 0; i < List_Item.length; i++) {
                                List_Item[i] = DisinfectData.ratioItems[i].name;
                            }
                            dataAdapter = new ArrayAdapter<String>(activity,
                                    android.R.layout.simple_spinner_item, List_Item);
                            dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                            sp_ratio.setAdapter(dataAdapter);
                        }
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

                        Log.d(TAG, "InitData: "+DisinfectData.typeItems);
                        if(DisinfectData.typeItems!=null) {
                            //初始化 消毒剂 选框
                            List_Item = new String[DisinfectData.typeItems.length];
                            for (int i = 0; i < List_Item.length; i++) {
                                List_Item[i] = DisinfectData.typeItems[i].name;
                            }
                            dataAdapter = new ArrayAdapter<String>(activity,
                                    android.R.layout.simple_spinner_item, List_Item);
                            dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                            sp_reagent.setAdapter(dataAdapter);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case MainActivity.MsgID_Car_GetMap:
                Log.d(TAG, "InitData: "+CarPlanUtils.getInstance().mapItems.size());
                if(CarPlanUtils.getInstance().mapItems!=null) {
                    //初始化 地图 选框
                    List_Item = new String[CarPlanUtils.getInstance().mapItems.size()];
                    for (int i = 0; i < List_Item.length; i++) {
                        List_Item[i] = CarPlanUtils.getInstance().mapItems.get(i).name;
                    }
                    dataAdapter = new ArrayAdapter<String>(activity,
                            android.R.layout.simple_spinner_item, List_Item);
                    dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                    sp_map.setAdapter(dataAdapter);
                }
                break;
            //获取用户标识
            case MainActivity.MsgID_Web_GetUserType:
                try {
                    data= (Object[]) msg.obj;
                    if((boolean)data[0]){
                        JSONObject joData=(JSONObject) data[1];
                        int id=joData.getInt("userId");
                        //初始化任务参数
                        DisinfectData.planItem=new PlanItem();
                        DisinfectData.planItem.InitUser(id);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void InitData() {
        NetWorkParmData();
        NetWorkGetMapIdList();
        NetWorkGetUserRole();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            InitData();
        }
    }

    private void ConfirmOnClick() {
            List<ParmItem> mapItems = CarPlanUtils.getInstance().mapItems;
            int mapIndex = sp_map.getSelectedItemPosition();
            if (mapIndex > -1 && mapIndex < mapItems.size()) {
                int mapId = mapItems.get(mapIndex).id;
                CarPlanUtils.getInstance().CarChangeMap(mapId);

                //更改消毒模式
                DisinfectData.CarWorkMode=0;
                DisinfectData.IsCarPointMode=true;
                switch (sp_type.getSelectedItemPosition())
                {
                    case 0:
                        DisinfectData.CarWorkMode=0;
                        break;
                    case 1:
                        DisinfectData.CarWorkMode=1;
                        DisinfectData.IsCarPointMode=false;
                        break;
                    case 2:
                        DisinfectData.CarWorkMode=2;
                        break;
                }
                Log.d(TAG, "消毒模式"+DisinfectData.IsCarPointMode);
                CarPlanUtils.getInstance().AddInfoList("本机启动："+DisinfectData.CarWorkMode);

                int reagentId = DisinfectData.typeItems[sp_reagent.getSelectedItemPosition()].id;
                int ratiodId = DisinfectData.ratioItems[sp_ratio.getSelectedItemPosition()].value;
                int userId = DisinfectData.planItem.userId;
                SocketUtils.getInstance().SendLocalCarControl(reagentId, ratiodId, userId, mapId);
            } else {
                activity.MyToast("地图切换失败");
            }
            activity.MyToast("地图加载中");
    }

    @Override
    public void onTouchNext() {
        ConfirmOnClick();
    }

    @Override
    public void onTouchBack(int flag) {

    }


    //获取消毒机信息
    public void NetWorkParmData() {
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

    //获取地图信息
    public void NetWorkGetMapIdList(){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/robot/map/list";
                    String result= HttpUtils.GetData(url);
                    Log.v(TAG,"GetMapIdList:"+result);

                    //{"code":0,"msg":"success","data":[{"mapId":9,"userId":1,"hosName":"1","mapName":"map-11223344-20201007-180152","mapContent":null,"createTime":"2020-10-13 20:24:58","updateTime":"2020-10-13 20:24:58"}],"count":1}

                    JSONObject json=new JSONObject(result);
                    JSONArray dataList=json.getJSONArray("data");
                    CarPlanUtils.getInstance().mapItems.clear();
                    for (int i = 0; i <dataList.length(); i++) {
                        JSONObject jo= dataList.getJSONObject(i);
                        int mapId=jo.getInt("mapId");
                        String mapName=jo.getString("mapName");
                        int userId=jo.getInt("userId");

                        ParmItem item=new ParmItem(mapName,mapId,userId,false);
                        CarPlanUtils.getInstance().mapItems.add(item);
                    }

                    Message msg = activity.FrgmHandler.obtainMessage();
                    msg.what = MainActivity.MsgID_Car_GetMap;
                    activity.FrgmHandler.sendMessage(msg);
                }catch (Exception e){
                    e.printStackTrace();
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
}

