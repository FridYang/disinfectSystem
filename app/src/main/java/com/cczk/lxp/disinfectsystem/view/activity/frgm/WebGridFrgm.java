package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.PlanItem;
import com.cczk.lxp.disinfectsystem.bean.ScensItem;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;
import com.cczk.lxp.disinfectsystem.view.utils.GridItemView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by pc on 2018/2/7.
 */
public class WebGridFrgm extends Fragment implements IFrgmCallBack {
    private static final String TAG = "WebGridFrgm";
    private MainActivity activity;
    private static final int MaxRowColumn=4;

    Spinner sp_no;
    Spinner sp_floor;
    GridLayout layout;
    EditText edit_size;

    String[] List_No;
    String[] List_Floor;

    String tempNo="";
    String tempFloor="";

    int selectRoom=-1;
    GridItemView[] roomItems;

    //获取到的房间信息
    public ScensItem[] roomSceneItems;
    public PlanItem[] roomPlanItems;

    private ImageView img_reagent;
    private boolean showReagent1=true;

    //从编辑页面返回
    public static boolean needInitData = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_webgrid, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        //获取控件
        sp_no=v.findViewById(R.id.grid_spin_no);
        sp_floor=v.findViewById(R.id.grid_spin_floor);
        layout=v.findViewById(R.id.grid_layout_context);
        edit_size =v.findViewById(R.id.grid_edit_size);

        img_reagent =v.findViewById(R.id.grid_img_reagent);
        img_reagent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.ComeGoFrame(MainActivity.flag_reagent);
            }
        });

        //回退按钮
        ImageView img=v.findViewById(R.id.grid_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.AllBtnFrgmHome();
            }
        });

        sp_no.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                tv.setTextColor(activity.color_tv);
                tv.setTextSize(27);

                if (List_No != null && List_No.length > position) {
                    tempNo = List_No[position].trim();
                    NetWorkGetFloors(AndroidUtils.GetMacAddress(), tempNo);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        sp_floor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView)view;
                tv.setTextColor(activity.color_tv);
                tv.setTextSize(27);

                if(List_Floor!=null && List_Floor.length>position)
                {
                    tempFloor = List_Floor[position].trim();;
                    GetRoomData(tempNo, tempFloor);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        edit_size=v.findViewById(R.id.grid_edit_size);
        edit_size.addTextChangedListener(new MyTextWatcher());
    }

    private class MyTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            //GetRoomData(tempNo, tempFloor);
        }
    }

    public void GetRoomData( String no,  String floor){
        int num = 999;

        /*
        String str = edit_size.getText().toString();
        if (!str.isEmpty() && str != "") {
            try {
                num = Integer.parseInt(str);
            } catch (Exception e) {}
        }*/
        NetWorkGetData(AndroidUtils.GetMacAddress(), num, no, floor);
    }

    @Override
    public void FrgmMessage(Message msg) {
        if(msg.what==MainActivity.MsgID_UI_Time)
        {
            UpDateReagent();
        }else {
            try {
                Object[] data = (Object[]) msg.obj;
                switch (msg.what) {
                    case MainActivity.MsgID_Web_GridGetNo:
                        if ((boolean) data[0]) {
                            JSONArray list = (JSONArray) data[1];
                            List_No = new String[list.length()];
                            for (int i = 0; i < list.length(); i++) {
                                List_No[i] = "  "+list.get(i).toString();
                            }
                            //初始化选框
                            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                                    android.R.layout.simple_spinner_item, List_No);
                            dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                            sp_no.setAdapter(dataAdapter);
                            sp_no.setSelection(0);
                        } else {
                            activity.MyToast(data[1].toString());
                        }
                        break;
                    case MainActivity.MsgID_Web_GridGetFloor:
                        if ((boolean) data[0]) {
                            JSONArray list = (JSONArray) data[1];
                            List_Floor = new String[list.length()];
                            for (int i = 0; i < list.length(); i++) {
                                List_Floor[i] = "  "+list.get(i).toString();
                            }
                            //初始化选框
                            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(activity,
                                    android.R.layout.simple_spinner_item, List_Floor);
                            dataAdapter.setDropDownViewResource(R.layout.str_dropdown2);
                            sp_floor.setAdapter(dataAdapter);
                            sp_floor.setSelection(0);
                        } else {
                            activity.MyToast(data[1].toString());
                        }
                        break;
                    case MainActivity.MsgID_Web_GridGetData:
                        if ((boolean) data[0]) {
                            JSONArray list = (JSONArray) data[1];
                            int size = list.length();
                        /*
                        try{
                            size=Integer.parseInt(edit_size.getText().toString());
                        }catch (Exception e){}*/
                            edit_size.setText(size + "");

                            roomSceneItems = new ScensItem[size];
                            roomPlanItems = new PlanItem[size];
                            for (int i = 0; i < size; i++) {
                                JSONObject jo = list.getJSONObject(i);
                                //JSONObject jo=list.getJSONObject(0);
                                Log.d(TAG, "FrgmMessage: " + jo);

                                //{"roomId":74,"building":"1","floor":"1","roomName":"房间名称","macAdd":"7e300299","hosName":"广州安然医疗科技发展有限公司","userId":81,"roomType":7,"roomTypeName":"Panda","length":1,"width":1,"height":1,"densityId":2,"densityName":"3","disinId":4,"disinName":"H₂O₂","sprayTime":2,"strengTime":63168,"disinfectTime":63168,"leaveTime":63168,"reserveTime":null,"createTime":"2022-05-27 17:33:10","updateTime":"2022-05-27 17:33:10","rate":80,"plValue":null,"phValue":null}

                                //获取场景数据
                                roomSceneItems[i] = new ScensItem();
                                try {
                                    roomSceneItems[i].length = AndroidUtils.GetJsonInt(jo, "length");
                                    roomSceneItems[i].width = AndroidUtils.GetJsonInt(jo, "width");
                                    roomSceneItems[i].height = AndroidUtils.GetJsonInt(jo, "height");
                                    roomSceneItems[i].scenesName = jo.getString("roomName");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                //获取任务数据
                                roomPlanItems[i] = new PlanItem();
                                try {
                                    roomPlanItems[i].sprayTime = AndroidUtils.GetJsonInt(jo, "sprayTime");
                                    roomPlanItems[i].strengTime = AndroidUtils.GetJsonInt(jo, "strengTime");
                                    roomPlanItems[i].idleTime = AndroidUtils.GetJsonInt(jo, "disinfectTime");
                                    roomPlanItems[i].leaveTime = AndroidUtils.GetJsonInt(jo, "leaveTime");
                                    roomPlanItems[i].roomId = AndroidUtils.GetJsonInt(jo, "roomId");

                                    roomPlanItems[i].typeId = AndroidUtils.GetJsonInt(jo, "disinId");
                                    roomPlanItems[i].planTime = AndroidUtils.GetJsonInt(jo, "reserveTime");

                                    if (jo.has("densityName") &&
                                            jo.isNull("densityName") == false) {
                                        roomPlanItems[i].ratioValue = Integer.parseInt(jo.getString("densityName"));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                            Log.d(TAG, "InitData: " + roomPlanItems[0].ratioValue + " - " + roomPlanItems[0].leaveTime);
                            InitGridData(size);
                        } else {
                            activity.MyToast(data[1].toString());
                        }
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "FrgmMessage: " + msg.obj);
            }
        }
    }

    @Override
    public void InitData()
    {
        if(needInitData)
        {
            needInitData =false;
            //初始化数据
            InitGridData(0);
            NetWorkGetNos(AndroidUtils.GetMacAddress());
        }else{
            //去掉选择
            if(roomItems!=null && roomItems.length>0)
            {
                for (int i = 0; i < roomItems.length; i++)
                {
                    roomItems[i].isOn=false;
                    roomItems[i].SelectClick(false);
                }
            }

            //判断跳转记录 删除
            int size=activity.FrgmChangeList.size();
            if(size>=2)
            {
                if(activity.FrgmChangeList.get(size - 2)==MainActivity.flag_parm &&
                        activity.FrgmChangeList.get(size - 1)==MainActivity.flag_webgrid)
                {
                    activity.FrgmChangeList.remove(size-1);
                    activity.FrgmChangeList.remove(size-2);
                }
            }
        }
        selectRoom = -1;

        img_reagent.setImageBitmap(activity.bm_reagent1);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            InitData();
        }
    }

    public void InitGridData(int no)
    {
        selectRoom=-1;

        int width = 915;
        int itemHeight =335;
        layout.removeAllViews();

        if(no>0) {
            int column = 1;
            if (no < MaxRowColumn) {
                column = no;
                if (no == 1) {
                    width *= 0.5f;
                } else if (no == 2) {
                    width *= 0.7f;
                }
            } else {
                if (no > MaxRowColumn) {
                    itemHeight /= 2;
                }
                column = MaxRowColumn;
            }
            if (no == 5 || no == 6) {
                column = 3;
            } else if (no == 7 || no == 8) {
                column = 4;
            }

            layout.setColumnCount(column);
            int itemWidth = (int) ((width * 1f) / column * 1f);

            roomItems = new GridItemView[no];
            for (int i = 0; i < no; i++) {
                roomItems[i] = new GridItemView(activity);
                int fontsize = 50;
                if (no > MaxRowColumn-1) {
                    fontsize = 30;
                }
                roomItems[i].Init(i,roomSceneItems[i].scenesName, fontsize);
                final int roomNo = i;
                roomItems[i].img_select.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SelectClick(roomNo);
                    }
                });
                roomItems[i].img_edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditClick(roomNo);
                    }
                });

                LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(itemWidth, itemHeight);
                parms.weight = itemWidth;
                parms.height = itemHeight;

                switch (no) {
                    case 1:
                        parms.weight = 800;
                        parms.height = 300;
                        parms.setMargins(260, 10, 0, 10);
                        break;
                    case 2:
                        parms.weight = 800;
                        parms.height = 300;
                        if (i == 0) {
                            parms.setMargins(130, 10, 20, 10);
                        } else {
                            parms.setMargins(30, 10, 0, 10);
                        }
                        break;
                }

                layout.addView(roomItems[i], parms);
            }
        }
    }

    public void SelectClick(int no)
    {
        //当前取反
        boolean clickOn=!roomItems[no].isOn;
        for (int i = 0; i < roomItems.length; i++) {
            roomItems[i].SelectClick(false);
        }
        roomItems[no].SelectClick(clickOn);

        if(clickOn)
        {
            selectRoom=no;
        }else{
            selectRoom=-1;
        }
    }

    public void EditClick(int no)
    {
        selectRoom=no;
        MainActivity.Instance.parmFrgm.isGridMode = true;
        MainActivity.Instance.ComeGoFrame(MainActivity.flag_parm);
    }

    // ------------------------- Web -------------------------

    //获取房间楼号
    public void NetWorkGetNos(final String Mac){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    //组合数据
                    String url= HttpUtils.HostURL+"/room/mac/buildings";
                    String parm="?";
                    parm+="macAdd="+Mac;
                    String result= HttpUtils.GetData(url+parm);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GridGetNo;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String msgStr= jsonObject.getString("msg");
                    Log.d(TAG, "NetWorkGetNos: "+msgStr);
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
                    activity.MyToastThread("获取房间楼号异常");
                }
            }
        });
    }

    //获取房间楼层
    public void NetWorkGetFloors(final String Mac,final String no){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    //组合数据
                    String url= HttpUtils.HostURL+"/room/mac/floors";
                    String parm="?";
                    parm+="macAdd="+Mac+"&";
                    parm+="building="+no;
                    String result= HttpUtils.GetData(url+parm);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GridGetFloor;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String msgStr= jsonObject.getString("msg");
                    Log.d(TAG, "NetWorkGetFloors: "+msgStr);
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
                    activity.MyToastThread("获取房间楼层异常");
                }
            }
        });
    }

    //获取房间列表
    public void NetWorkGetData(final String Mac,final int limit, final String no, final String floor)
    {
        Log.d(TAG, "NetWorkGetData: ");
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/room/list";
                    String parm="?";
                    parm+="page=1&";
                    parm+="macAdd="+Mac+"&";
                    parm+="limit="+limit+"&";
                    parm+="building="+no+"&";
                    parm+="floor="+floor+"";
                    String result= HttpUtils.GetData(url+parm);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what=MainActivity.MsgID_Web_GridGetData;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String msgStr= jsonObject.getString("msg");
                    Log.d(TAG, "NetWorkGetData: "+msgStr);
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
                    activity.MyToastThread("获取房间列表异常");
                }
            }
        });

    }

    @Override
    public void onTouchNext()
    {
        if(selectRoom!=-1)
        {
            //足够溶液
            if(DisinfectData.GetReagentSuffice())
            {
            if (selectRoom < roomSceneItems.length &&
                    selectRoom < roomPlanItems.length) {
                int typeId = roomPlanItems[selectRoom].typeId;
                int ratioValue = roomPlanItems[selectRoom].ratioValue;
                int sprayT = roomPlanItems[selectRoom].sprayTime;
                int strengT = roomPlanItems[selectRoom].strengTime;
                int idleT = roomPlanItems[selectRoom].idleTime;
                int leaveT = roomPlanItems[selectRoom].leaveTime;
                int planT = roomPlanItems[selectRoom].planTime;
                int roomId = roomPlanItems[selectRoom].roomId;

                Log.d(TAG, "UseData: " + ratioValue + " - " + leaveT);
                activity.parmFrgm.WebGridRunPlan(typeId, ratioValue,
                        sprayT, strengT, idleT, leaveT,
                        planT, roomId);
            } else {
                activity.MyToast("数据异常,请重新选择");
            }
            }else
             {
                    //跳转消毒溶液
                    activity.ComeGoFrame(MainActivity.flag_reagent);
            }
        }else
        {
            activity.MyToast("请选择房间");
        }
    }

    @Override
    public void onTouchBack(int flag) {

    }

    public void ThreadOneSecFun()
    {
        if(activity !=null && activity.FrgmHandler!=null) {
            //更新UI时间
            Message msg = activity.FrgmHandler.obtainMessage();
            msg.what = activity.MsgID_UI_Time;
            msg.obj = AndroidUtils.GetSystemTime();
            activity.FrgmHandler.sendMessage(msg);
        }
    }

    //更新溶液显示
    public void UpDateReagent()
    {
        if(selectRoom==-1)
        {
            //消毒液 默认足够
            DisinfectData.SetReagentSuffice(true,0);
        }else
        {
            float time=roomPlanItems[selectRoom].sprayTime;
            //喷洒速率 每分钟喷xml
            float rate= ControlUtils.getInstance().GetHareWareRate();

            //共消耗 毫升
            float need=(float) Math.ceil(time*rate);

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

