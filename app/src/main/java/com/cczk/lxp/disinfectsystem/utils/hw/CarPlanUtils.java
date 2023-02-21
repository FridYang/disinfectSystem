package com.cczk.lxp.disinfectsystem.utils.hw;

import android.os.Message;
import android.util.Log;

import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.MapItem;
import com.cczk.lxp.disinfectsystem.bean.ParmItem;
import com.cczk.lxp.disinfectsystem.test.CarTestActivity;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.base.SocketUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.RunFrgm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pc on 2020/10/22.
 */

public class CarPlanUtils {
    private static final String TAG = "CarPlanUtils";

    //地图信息
    public List<ParmItem> mapItems =new ArrayList<>();
    //坐标点信息
    public List<MapItem> mapPosItems =new ArrayList<>();
    //初始点
    public MapItem InitPos=new MapItem();

    //当前地图ID
    public int NowMapId=-1;
    //上个地图ID
    public int LastMapId=-1;

    //检查是否到点
    private int CheckPosTimeCnt=0;
    //重发切换地图
    private int ChangeMapTimeCnt=0;
    private int ChangeMapStopCnt=0;
    //检查是否定点模式 重新启动赋值
    public static boolean tempPointMode=true;

    //检查是否到点
    private int RepeatTimeCnt=0;

    public List<String> list_Info=new ArrayList<>();

    //运行列表
    public static final byte Mode_Idle = 0;         // 默认
    public static final byte Mode_Move = 1;         // 移动
    public static final byte Mode_Wait = 2;         // 计时开始
    public final byte Mode_Over = 3;         // 计时结束
    public int Mode=Mode_Idle;

    //信息标识
    public static final int Info_MapErr = 0;         // 地图切换失败
    public static final int Info_MapSuc = 1;         // 地图切换成功
    public static final int Info_MoveErr = 2;        // 任务失败
    public static final int Info_MoveSuc = 3;        // 任务成功

    //是否执行任务中
    public boolean isExecute=false;
    public int ExecuteCnt=0;

    private boolean isStartPlan=false;
    //读取消毒时间 并用作缓存
    public int WaitTimeCnt=0;
    //是否等待切换地图
    boolean waitChangeMap=false;

    //获取单例
    private static CarPlanUtils instance=null;
    public static CarPlanUtils getInstance() {
        if (instance == null) {
            synchronized (CarPlanUtils.class) {
                if (instance == null) {
                    instance = new CarPlanUtils();
                }
            }
        }
        return instance;
    }

    //每一次移动调用
    public void CarMove()
    {
        CarPlanUtils.getInstance().AddInfoList("硬件开始移动");
        if(mapPosItems.size()>0)
        {
            MapItem item=mapPosItems.get(0);
             Log.d(TAG, "开始移动  点："+item.MarkName);
            isStartPlan=true;
            Log.d(TAG, "Mode_Move");

            CarUtils.getInstance().SendMovePos(item.x,item.y,item.z,item.w,
                    item.valueX,item.valueY,item.valueZ,item.valueW);

            CheckPosTimeCnt=120;
            RepeatTimeCnt=5;
            Mode=Mode_Move;

            //定点模式清空时间
            if(DisinfectData.IsCarPointMode)
            {
                WaitTimeCnt=0;

                Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                msg.what = MainActivity.MsgID_Car_StopDisinfect;
                MainActivity.Instance.MainHandler.sendMessage(msg);
            }else{
                //持续工作
                if(WaitTimeCnt<=0)
                {
                    Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                    msg.what = MainActivity.MsgID_Car_StopDisinfect;
                    MainActivity.Instance.MainHandler.sendMessage(msg);
                }
            }
        }else
        {
            Mode=Mode_Wait;
            CarTimeEnd();
        }
    }

    //到点暂停开始计时
    public void CarWait()
    {
        CarPlanUtils.getInstance().AddInfoList("到达目标点 Mode:"+Mode);
        //if(Mode==Mode_Move)
        {
            //CarUtils.getInstance().SendStop();

            Log.d(TAG, "CarWait");

            //计数
            CarPlanUtils.getInstance().AddInfoList(mapPosItems.get(0).MarkName+":"+
                    mapPosItems.get(0).MarkWait  +":"+
                    mapPosItems.get(0).MarkMode);
            WaitTimeCnt = mapPosItems.get(0).MarkWait;

            //混合模式变状态
            if(DisinfectData.CarWorkMode==2)
            {
                if (mapPosItems.get(0).MarkMode == 0)
                {
                    tempPointMode=true;
                } else {
                    tempPointMode=false;
                }
                DisinfectData.IsCarPointMode = tempPointMode;
            }

            Mode = Mode_Wait;

            if(WaitTimeCnt>0)
            {
                //如果计数不为0
                Log.d(TAG, "开始消毒 用时：" + WaitTimeCnt);
                //喷雾
                Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                msg.what = MainActivity.MsgID_Car_StartDisinfect;
                msg.obj = WaitTimeCnt;
                MainActivity.Instance.MainHandler.sendMessage(msg);
            }else{
                //计数为0立刻跳转
                CarTimeEnd();
            }

            //如果不是定点模式 立刻移动
            if(!DisinfectData.IsCarPointMode)
            {
                //计数为0立刻跳转
                CarTimeEnd();
            }
        }

        if(Mode==Mode_Over)
        {
            isStartPlan = false;
            Log.d(TAG, "移动到初始点成功");
        }
    }

    //计时结束
    public void CarTimeEnd()
    {
        if(Mode==Mode_Wait)
        {
            //CarUtils.getInstance().SendStop();

            Log.d(TAG, "Mode_Over");
             Log.d(TAG, "消毒完成");

            //0119
            isExecute=false;
            //定点模式清空时间
            if(DisinfectData.IsCarPointMode){
                WaitTimeCnt=0;
            }

            //删除当前位置点
            if (mapPosItems.size() > 0) {
                mapPosItems.remove(0);
                CarMove();
            } else {
                WaitTimeCnt=0;

                Log.d(TAG, "CarTimeEnd");
                 Log.d(TAG, "所有消毒任务已成功");
                SocketUtils.getInstance().SendCarControlInfo(Info_MoveSuc);

                Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                msg.what = MainActivity.MsgID_Car_OverPos;
                MainActivity.Instance.MainHandler.sendMessage(msg);
            }
        }
    }

    public void AddInfoList(String add){
        list_Info.add(add);
        if(list_Info.size()>10)
        {
            list_Info.remove(0);
        }
        String str="";
        for (int i = 0; i <list_Info.size(); i++)
        {
            str+=list_Info.get(i)+" \r\n";
        }

        Message msg = MainActivity.Instance.MainHandler.obtainMessage();
        msg.what = MainActivity.MsgID_UI_TestInfo;
        msg.obj=str;
        MainActivity.Instance.MainHandler.sendMessage(msg);
    }

    //车持续移动
    public void CarKeepMove()
    {
        //不允许移动
        if(!RunFrgm.timeCanRun)
        {
            CarUtils.getInstance().SendStop();
        }else{

            //热成像模式有人时
            if(DisinfectData.IsDevHeatCheck)
            {
                if (MainActivity.Instance.runFrgm.IsHeatCheck)
                {
                    CarUtils.getInstance().SendStop();
                }
            }

        }

    }

    //点击暂停后移动到初始点 只供给外部调用
    public void CarOver(){
        NowMapId=-1;
        isExecute=false;

        mapPosItems.clear();

        MapItem item=InitPos;
         Log.d(TAG, "移动到初始点  点："+item.MarkName);

        CarUtils.getInstance().SendMovePos(item.x,item.y,item.z,item.w,
                item.valueX,item.valueY,item.valueZ,item.valueW);

        Mode = Mode_Over;

        CheckPosTimeCnt=120;
        RepeatTimeCnt=5;
    }

    /*
    public void CarCheckPos(){
        if(isStartPlan) {
            if (Mode == Mode_Move) {
                //当位置非常靠近 确定成功
                if (CarUtils.getInstance().InPlace()) {
                     Log.d(TAG, "非常靠近 确定成功" + CarUtils.getInstance().InPlaceStr);
                    CarWait();
                } else {
                     Log.d(TAG, "离得远 重新发送" + CarUtils.getInstance().InPlaceStr);
                }
            }
        }
    }
    */

    public void ThreadOneSecFun(){
        //CarUtils.getInstance().SendGetCarPos();
        //CarUtils.getInstance().InPlace();

        //获取小车当前点
        if(CheckPosTimeCnt>0){
            CheckPosTimeCnt--;
        }else{
            CheckPosTimeCnt=10;
            CarUtils.getInstance().RunCheckPos();
            CarUtils.getInstance().SendGetPower();
        }

        if(waitChangeMap)
        {
            if(ChangeMapTimeCnt<30)
            {
                ChangeMapTimeCnt++;
            }else{
                ChangeMapTimeCnt=0;
                CarChangeMap(NowMapId);

                ChangeMapStopCnt++;
                if(ChangeMapStopCnt>=3)
                {
                    waitChangeMap = false;

                    Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                    msg.what = MainActivity.MsgID_Car_OverPos;
                    MainActivity.Instance.MainHandler.sendMessage(msg);
                }
            }
        }

        //模式运行
        if(isStartPlan){
            switch (Mode){
                case Mode_Move:
//                    //获取小车当前点
//                    if(CheckPosTimeCnt>0){
//                        CheckPosTimeCnt--;
//                    }else{
//                        CheckPosTimeCnt=120;
//
//                        CarUtils.getInstance().RunCheckPos();
//                    }

                    //重发
                    if(RepeatTimeCnt>0 && RunFrgm.timeCanRun){
                        RepeatTimeCnt--;
                    }else
                    {
                        if(CarUtils.getInstance().CarMoveCnt<=0)
                        {
                            //如果小车没移动
                            RepeatTimeCnt=5;
                        }else
                        {
                            //如果小车刚移动
                            RepeatTimeCnt=15;
                        }

                        //保留记录时间
                        int temp= CarPlanUtils.getInstance().ExecuteCnt;
                        MapItem item = mapPosItems.get(0);
                        CarUtils.getInstance().SendMovePos(item.x, item.y, item.z, item.w,
                                item.valueX, item.valueY, item.valueZ, item.valueW);
                        CarPlanUtils.getInstance().ExecuteCnt=temp;
                    }
                    break;
                case Mode_Wait:
                    //if(WaitTimeCnt>0){
                    //    WaitTimeCnt--;
                    if(RunFrgm.timeCanRun)
                    {
                        if (MainActivity.Instance.runFrgm.timeCnt > 0) {
                            //任务进行中
                            ExecuteCnt = 60;
                        } else {
                            //计时结束
                            CarTimeEnd();
                        }
                    }
                    break;
                case Mode_Over:
                    if(CheckPosTimeCnt>0){
                        CheckPosTimeCnt--;
                    }else{
                        CheckPosTimeCnt=120;

                        //默认结束
                        CarWait();
                    }

                    //重发
                    if(RepeatTimeCnt>0  && RunFrgm.timeCanRun){
                        RepeatTimeCnt--;
                    }else
                    {
                        if(CarUtils.getInstance().CarMoveCnt<=0)
                        {
                            //如果小车没移动
                            RepeatTimeCnt=30;//15
                        }else
                        {
                            //如果小车刚移动
                            RepeatTimeCnt=60;//20
                        }

                        //重发
                        //保留记录时间
                        int temp= CarPlanUtils.getInstance().ExecuteCnt;
                        MapItem item=InitPos;
                        CarUtils.getInstance().SendMovePos(item.x, item.y, item.z, item.w,
                                item.valueX, item.valueY, item.valueZ, item.valueW);
                        CarPlanUtils.getInstance().ExecuteCnt=temp;
                    }
                    break;
            }
        }

        if(CarUtils.getInstance().CarMoveCnt>0){
            CarUtils.getInstance().CarMoveCnt--;
        }

        //Log.d(TAG, "ThreadOneSecFun: "+isExecute + ExecuteCnt);
        //执行命令检查
        if(isExecute){
            if(ExecuteCnt>0){
                ExecuteCnt--;
            }else{
                // 小车还在移动
                if(CarUtils.getInstance().CarMoveCnt>0)
                {
                    ExecuteCnt=60;
                }
                // 消毒还没结束
                else if(MainActivity.Instance.runFrgm.timeCanRun)
                {
                    ExecuteCnt=60;
                }
                //已经跑完地图
                else if (mapPosItems.size() <= 0)
                {
                    ExecuteCnt=60;
                }
                else
                {
                    //倒计时结束 任务失败
                    if(CarTestActivity.instance!=null) {
                        Message msg = CarTestActivity.instance.handler.obtainMessage();
                        msg.what = CarTestActivity.flag_info;
                        msg.obj = "任务失败 请重新执行";
                        CarTestActivity.instance.handler.sendMessage(msg);
                    }

                    if(MainActivity.Instance!=null) {
                        Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                        msg.what = MainActivity.MsgID_Car_OverPos;
                        MainActivity.Instance.MainHandler.sendMessage(msg);
                    }

                    NowMapId=-1;
                    isExecute=false;
                    Mode=Mode_Over;
                    SocketUtils.getInstance().SendCarControlInfo(Info_MoveErr);
                }
            }
        }
    }

    //控制底盘暂停
    public void CarStop()
    {
        Mode=Mode_Idle;

        CarPlanUtils.getInstance().AddInfoList("硬件暂停移动");
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 3; i++)
                {
                    try {
                        CarUtils.getInstance().SendStop();

                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public  void CarChangeMap(int mapId){
        DisinfectData.IsCarControl=true;
        Log.d(TAG, "移动调用CarChangeMap: "+DisinfectData.IsCarControl);

        //更换地图
        waitChangeMap=true;
        ChangeMapTimeCnt=0;
        ChangeMapStopCnt=0;

        NowMapId=mapId;
        CarPlanUtils.getInstance().NetWorkGetMapInfoList(mapId);

        Message msg = MainActivity.Instance.MainHandler.obtainMessage();
        msg.what = MainActivity.MsgID_Car_StopDisinfect;
        MainActivity.Instance.MainHandler.sendMessage(msg);
    }

    //底盘切换好地图后 可以开始任务
    public void CarChangeMapOver()
    {
        waitChangeMap=false;

        if(NowMapId!=LastMapId)
        {
            //不是同一个地图
            LastMapId = NowMapId;
            //先设置当前点为初始点
            CarUtils.getInstance().SendSetInitPos();
            //CarUtils.getInstance().SetNowIsInitPos();
        }

        isExecute=true;
        CarMove();

        Message msg = MainActivity.Instance.MainHandler.obtainMessage();
        msg.what = MainActivity.MsgID_UI_TestInfo;
        msg.obj="";
        MainActivity.Instance.MainHandler.sendMessage(msg);

        if(DisinfectData.IsWebUser) {
            SocketUtils.getInstance().SendWebCarStartRep();
        }
    }

    // ---------------------------   NetWork    ---------------------------

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
                    mapItems.clear();
                    for (int i = 0; i <dataList.length(); i++) {
                        JSONObject jo= dataList.getJSONObject(i);
                        int mapId=jo.getInt("mapId");
                        String mapName=jo.getString("Name");
                        int userId=jo.getInt("userId");

                        ParmItem item=new ParmItem(mapName,mapId,userId,false);
                        mapItems.add(item);

                        //NetWorkGetMapInfoList(mapId);
                    }

                    if(CarTestActivity.instance!=null)
                    {
                        Message msg = CarTestActivity.instance.handler.obtainMessage();
                        msg.what = CarTestActivity.flag_map;
                        CarTestActivity.instance.handler.sendMessage(msg);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    public void NetWorkGetMapInfoList(final int mapId){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                boolean isErr=true;
                try{
                    String url= HttpUtils.HostURL+"/robot/map/"+mapId;
                    String result= HttpUtils.GetData(url);

                    //result="{\"code\":0,\"msg\":\"success\",\"data\":{\"data\":\"[{\\\"mAvoidPointID\\\":null,\\\"mID\\\":null,\\\"mLaneMarkDescript\\\":\\\"\\\",\\\"mLaneMarkID\\\":60000,\\\"mLaneMarkName\\\":\\\"点2\\\",\\\"mLaneMarkType\\\":2,\\\"mLaneMarkXYZW\\\":{\\\"w\\\":1,\\\"x\\\":-0.5734400000000001,\\\"y\\\":6.774784,\\\"z\\\":0},\\\"mMapName\\\":\\\"\\\",\\\"mPrepointID\\\":null,\\\"mTaskListName\\\":\\\"\\\"},{\\\"mAvoidPointID\\\":null,\\\"mID\\\":null,\\\"mLaneMarkDescript\\\":\\\"\\\",\\\"mLaneMarkID\\\":60001,\\\"mLaneMarkName\\\":\\\"点1\\\",\\\"mLaneMarkType\\\":2,\\\"mLaneMarkXYZW\\\":{\\\"w\\\":1,\\\"x\\\":-1.36,\\\"y\\\":6.784000000000001,\\\"z\\\":0},\\\"mMapName\\\":\\\"\\\",\\\"mPrepointID\\\":null,\\\"mTaskListName\\\":\\\"\\\"}]\",\"name\":\"map-201022-20201022-102936\"}}";

                    Log.v(TAG,"GetMapInfoList:"+result);

                    //{"code":0,"msg":"success","data":
                    //      {"mapId":9,"userId":1,"hosName":"1","mapName":"map-11223344-20201007-180152","
                    //                  mapContent":"[
                    // {\"mLaneMarkDescript\":\"\",\"mLaneMarkID\":60000,\"mLaneMarkName\":\"\",\"mLaneMarkType\":0,\"mLaneMarkXYZW\":{\"w\":1,\"x\":0,\"y\":0.0256,\"z\":0}},
                    // {\"mLaneMarkDescript\":\"\",\"mLaneMarkID\":60001,\"mLaneMarkName\":\"\",\"mLaneMarkType\":0,\"mLaneMarkXYZW\":{\"w\":1,\"x\":1.1136,\"y\":0.064,\"z\":0}},
                    // {\"mLaneMarkDescript\":\"\",\"mLaneMarkID\":60002,\"mLaneMarkName\":\"\",\"mLaneMarkType\":0,\"mLaneMarkXYZW\":{\"w\":1,\"x\":2.1248,\"y\":0.0576,\"z\":0}},
                    // {\"mLaneMarkDescript\":\"\",\"mLaneMarkID\":60003,\"mLaneMarkName\":\"\",\"mLaneMarkType\":0,\"mLaneMarkXYZW\":{\"w\":1,\"x\":2.8544,\"y\":0.0512,\"z\":0}},
                    // {\"mLaneMarkDescript\":\"\",\"mLaneMarkID\":60004,\"mLaneMarkName\":\"22\",\"mLaneMarkType\":2,\"mLaneMarkXYZW\":{\"w\":1,\"x\":1.0624,\"y\":0.08960000000000001,\"z\":0},

                    try
                    {
                        JSONObject json=new JSONObject(result);
                        JSONObject data1=json.getJSONObject("data");
                        String data2=data1.getString("data");
                        String MapName=data1.getString("fileName");
                        JSONArray list=new JSONArray(data2);

                        mapPosItems.clear();
                        for (int i = 0; i <list.length(); i++) {
                            JSONObject jo=list.getJSONObject(i);
                            Log.d(TAG, "run: "+jo.toString());
                            int MarkType=jo.getInt("mLaneMarkType");
                            if(MarkType==2) {
                                String MarkName = jo.getString("mLaneMarkName");
                                int MarkWait=0;
                                int MarkMode=0;
                                try{
                                    String str = jo.getString("mLaneMarkDescript");
                                    if (!str.equals("") && str != null)
                                    {
                                        String[] strList = jo.getString("mLaneMarkDescript").split("-");
                                        if(strList.length>=2)
                                        {
                                            MarkWait = Integer.parseInt(strList[0]);
                                            MarkMode = Integer.parseInt(strList[1]);
                                        }else if(strList.length==1)
                                        {
                                            MarkWait = Integer.parseInt(strList[0]);
                                        }
                                    }
                                }catch (Exception e){}

                                JSONObject MarkXYZW = jo.getJSONObject("mLaneMarkXYZW");
                                float x = (float) MarkXYZW.getDouble("x");
                                float y = (float) MarkXYZW.getDouble("y");
                                float z = (float) MarkXYZW.getDouble("z");
                                float w = (float) MarkXYZW.getDouble("w");

                                x = ((int) (x * 1000f)) / 1000f;
                                y = ((int) (y * 1000f)) / 1000f;
                                z = ((int) (z * 1000f)) / 1000f;
                                w = ((int) (w * 1000f)) / 1000f;


                                Log.d(TAG, String.format("type:%s name:%s wait:%s mode:%s",
                                        MarkType, MarkName, MarkWait,MarkMode));
                                Log.d(TAG, String.format("data1 x:%s y:%s z:%s w:%s",
                                        x, y, z, w));
//                                Log.d(TAG, String.format("data2 x:%s y:%s z:%s w:%s",
//                                        AndroidUtils.BytesToHexString(DataToBytes4(x)),
//                                        AndroidUtils.BytesToHexString(DataToBytes4(y)),
//                                        AndroidUtils.BytesToHexString(DataToBytes4(z)),
//                                        AndroidUtils.BytesToHexString(DataToBytes4(w))));

                                MapItem item = new MapItem(MarkType, MarkName, MarkWait,MarkMode,
                                        x,y,z,w,
                                        CarUtils.getInstance().DataToBytes4(x),
                                        CarUtils.getInstance().DataToBytes4(y),
                                        CarUtils.getInstance().DataToBytes4(z),
                                        CarUtils.getInstance().DataToBytes4(w));
                                mapPosItems.add(item);
                            }
                        }

                        if(CarTestActivity.instance!=null) {
                            Message msg = CarTestActivity.instance.handler.obtainMessage();
                            msg.what = CarTestActivity.flag_mapPos;
                            CarTestActivity.instance.handler.sendMessage(msg);
                        }

                        ////设置初始点为第一个地图点
                        //MapItem item=mapPosItems.get(0);
                        //CarUtils.getInstance().SendSetInitPos(item.valueX,item.valueY,item.valueZ,item.valueW);
                        InitPos=mapPosItems.get(0);

                        //有点信息
                        Log.d(TAG, "SendChangeMap: "+MapName);
                        if(mapPosItems.size()>0)
                        {
                            CarUtils.getInstance().SendChangeMap(MapName);

                            SocketUtils.getInstance().SendCarControlInfo(Info_MapSuc);

                            isErr=false;
                        }else
                        {
//                            Log.d(TAG, "Info_MapErr: "+MapName+"-"+ mapPosItems.size());
//                            SocketUtils.getInstance().SendCarControlInfo(Info_MapErr);
                        }

                    }catch (Exception e){
                        Log.e(TAG, "run: "+e.toString());
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

//                if(isErr){
//                    //切换地图异常
//                    Message msg = MainActivity.Instance.MainHandler.obtainMessage();
//                    msg.what = MainActivity.MsgID_Car_OverPos;
//                    MainActivity.Instance.MainHandler.sendMessage(msg);
//                }
            }
        });
    }

    //设置开启任务
}
