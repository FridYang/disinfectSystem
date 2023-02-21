package com.cczk.lxp.disinfectsystem.utils.base;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.PlanItem;
import com.cczk.lxp.disinfectsystem.utils.hw.CarPlanUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.SensortUtils1;
import com.cczk.lxp.disinfectsystem.utils.hw.SensortUtils2;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.ParmFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.ReagentFrgm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by pc on 2020/5/25.
 */

public class SocketUtils {
    public static String TAG="SocketUtils";

    // Socket变量
    private Socket socket=null;
    //是否在线 登录标记在线 心跳标记离线
    public boolean isOnLine =false;
    //离线计数
    private int OffLineCnt =0;
    //需要累计数
    public boolean IsAddSensort=false;

    private final byte CMD_Login = (byte)0x00;            // 登录
    private final byte CMD_Heartbeat = (byte)0x01;       // 心跳
    private final byte CMD_Weight = (byte)0x02;           // 获取重量
    private final byte CMD_LocalStart = (byte)0x03;      // 本机启动机器
    private final byte CMD_WebStart = (byte)0x04;        // 服务器启动机器
    private final byte CMD_LocalStop = (byte)0x05;       // 本机暂停机器
    private final byte CMD_WebStop = (byte)0x06;         // 服务器暂停机器
    private final byte CMD_Sensort = (byte)0x07;         // 消毒剂浓度
    private final byte CMD_GetHWState = (byte)0x08;      // 查询设备工作状态
    private final byte CMD_PlanOver = (byte)0x09;        //   消毒结束

    private final byte CMD_WebCarControl = (byte)0x0A;     //   启动底盘移动
    private final byte CMD_CarInfo = (byte)0x0B;            //   机器底盘信息
    private final byte CMD_LocalCarControl = (byte)0x0C;   //   本机启动底盘
    private final byte CMD_UpDateApk = (byte)0x0D;          //   在线更新APK
    private final byte CMD_OpenLock = (byte)0x0E;           //   溶液开锁

    //离线保存数据
    private List<Integer> OffLineSensorts =new ArrayList<>();
    private List<Integer> OffLineTimeCnts =new ArrayList<>();

    //网络底盘回应
    private byte[] WebCarRepData;

    /**
     * 接收服务器消息 变量
     */
    // 输入流对象
    InputStream inStream;
    /**
     * 发送消息到服务器 变量
     */
    // 输出流对象
    OutputStream outStream;

    // 输入流读取器对象
    InputStreamReader isr ;
    BufferedReader br ;

    // 接收服务器发送过来的消息
    String response;
    Handler handler;
    int MsgId_Receive;

    //发送浓度数据时间累计
    public  int SendSensortCnt=0;
    //当前用户执行时间累计
    //public  int NowUserId=0;
    //public  int NowUserRunCnt=0;
    //等待监测重复数据时间累计
    private int LastDataCnt =0;
    //记录上次记录
    private byte[] LastData =new byte[2];


    public  final int INTMAX=500000;

    //获取单例
    private static SocketUtils instance=null;
    public static SocketUtils getInstance() {
        if (instance == null) {
            synchronized (SocketUtils.class) {
                if (instance == null) {
                    instance = new SocketUtils();
                }
            }
        }
        return instance;
    }

    public void Open(final String host, final int port, final Handler han, final int id){
        new Thread(){
            @Override
            public void run() {
                try {
                    Log.v(TAG,"InitUtils 0");
                    // 创建Socket对象 & 指定服务端的IP 及 端口号
                    socket = new Socket(host, port);
                    Log.v(TAG,"InitUtils 1");
                    Log.d(TAG, "Connect:"+SocketUtils.getInstance().isConnected());
                } catch (Exception e) {
                    Log.v(TAG,"InitUtils ERR"+e);
                    e.printStackTrace();
                }

                if(isConnected()){
                    ReceiveFun();
                }
                handler=han;
                MsgId_Receive=id;
            }
        }.start();

        OffLineCnt=0;

//        // 利用线程池直接开启一个线程 & 执行该线程
//        ThreadLoopUtils.getInstance().
//                mThreadPool.execute(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Log.v(TAG,"InitUtils 0");
//                    // 创建Socket对象 & 指定服务端的IP 及 端口号
//                    socket = new Socket(host, port);
//                    Log.v(TAG,"InitUtils 1");
//                    Log.d(TAG, "Connect:"+SocketUtils.getInstance().isConnected());
//                } catch (Exception e) {
//                    Log.v(TAG,"InitUtils ERR"+e);
//                    e.printStackTrace();
//                }
//
//                if(isConnected()){
//                    ReceiveFun();
//                }
//                handler=han;
//                MsgId_Receive=id;
//            }
//        });
    }

    public  void Close(){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 断开 客户端发送到服务器 的连接，即关闭输出流对象OutputStream
                    outStream.close();

                    // 断开 服务器发送到客户端 的连接，即关闭输入流读取器对象BufferedReader
                    br.close();

                    // 最终关闭整个Socket连接
                    socket.close();

                    // 判断客户端和服务器是否已经断开连接
                    Log.d(TAG, "Disconnect:"+socket.isConnected());

                    socket=null;
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public boolean isConnected(){
        if(socket==null){
            return  false;
        }
        return socket.isConnected();
    }

    /**
     * 发送消息 给 服务器
     */
    //注意不能在UI线程
    public void Send(final byte[] data){
        if(socket==null){
            Log.e(TAG, "socket is NULL");
        }else {

            ThreadLoopUtils.getInstance().
                    mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 步骤1：从Socket 获得输出流对象OutputStream
                        // 该对象作用：发送数据
                        outStream = socket.getOutputStream();

                        if (outStream != null) {
                            // 步骤2：需要发送的数据到输出流对象中
                            outStream.write(data);
                            Log.d(TAG, "Send: " + AndroidUtils.BytesToString(data));

                            // 步骤3：发送数据到服务端
                            outStream.flush();
                        } else {
                            Log.e(TAG, "outStream is NULL");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();

                        ExceptionRun();
                    }
                }
            });
        }
    }

    //监听服务器返回数据
    //会一直阻塞 必须放到新线程循环
    public void ReceiveFun(){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                while (MainActivity.Instance.IsRun) {

                    if(isConnected())
                    {
                        try {
                            // 步骤1：创建输入流对象InputStream
                            inStream = socket.getInputStream();

                            // 步骤2: 创建字节数组输出流 ，用来输出读取到的内容
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            //创建读取缓存,大小为1024
                            //缓存改大 1024   2021/03/23
                            byte[] buffer = new byte[10240];
                            //每次读取长度
                            int len = 0;
                            //开始读取输入流中的文件
                            try{
                                while ( isConnected() &&
                                        ((len = inStream.read(buffer)) != -1)) { //当等于-1说明没有数据可以读取了
                                    byteArrayOutputStream.write(buffer, 0, len); // 把读取的内容写入到输出流中
                                    if (inStream.available() <= 0) {//跳出阻塞
                                        break;
                                    }
                                }
                            }catch (Exception e){}

                            // 步骤3: 把读取到的字节数组转换为字符串
                            buffer = byteArrayOutputStream.toByteArray();
                            response = AndroidUtils.BytesToString(buffer);
                            //Log.d(TAG, "Receive:" + response);
                            //Receive:AA 0D 01 02 66 34 63 33 38 61 31 61 00 00 00 00 00 00 00 00 05 DD  len:22 22
                            //Log.d(TAG, "ReceiveAll:" + AndroidUtils.BytesToString(buffer));

                            for (int k = 0; k < 5; k++) {
                                int endIndex = -1;
                                for (int i = 0; i < buffer.length; i++) {
                                    if (buffer[i] == (byte) 0xDD) {
                                        endIndex = i;
                                        break;
                                    }
                                }

                                if (endIndex > -1) {
                                    //解析数据
                                    byte[] temp = AndroidUtils.SubBytes(buffer, 0, endIndex + 1);
                                    Log.d(TAG, "Receive:" + AndroidUtils.BytesToString(temp));
                                    GetCmd(temp);
                                }
                                if (endIndex >= buffer.length - 1) {
                                    break;
                                } else {
                                    //还有剩余数据
                                    buffer = AndroidUtils.SubBytes(buffer, endIndex + 1, buffer.length - endIndex - 1);
                                }
                            }

                            // 步骤4:通知主线程,将接收的消息显示到界面
                            Message msg = Message.obtain();
                            msg.what = MsgId_Receive;
                            msg.obj = buffer;
                            handler.sendMessage(msg);

                            Thread.sleep(500);
                        } catch (Exception e) {
                            e.printStackTrace();

                            ExceptionRun();
                        }
                    }
                }
            }
        });
    }

    public void ExceptionRun(){
        OffLineCnt++;
        if(OffLineCnt>=10){
            if(MainActivity.Instance!=null){

                MainActivity.Instance.SocketInit();
            }
        }
    }

    //-------------------------------------- 处理命令
    private void GetCmd(byte[] data) {
        int cmd = data[3];

        //需要执行数据
        boolean NeedUsedData=true;

        //获取重量不过滤
        if(cmd != CMD_Weight) {
            //判断数据是否跟上一次相同
            //Log.d(TAG, "NeedUsedData: " + Arrays.equals(data, LastData) + " Cnt:" + LastDataCnt);
            if (Arrays.equals(data, LastData)) {
                //并且过于频繁
                if (LastDataCnt > 0) {
                    NeedUsedData = false;
                }
            }
        }

        if(NeedUsedData) {
            //保存
            LastDataCnt = 5;
            LastData = data;

            try {
                int cnt = 0;
                switch (cmd) {
                    case CMD_Login:
                        Log.d(TAG, "接收登录 OffLineCnt"+OffLineCnt );


                        if (OffLineCnt >= 2) {
                            Log.d(TAG, "OffLineCnt"+OffLineCnt+
                                    " Instance"+(MainActivity.Instance == null)+
                                    " Handler"+(MainActivity.Instance.MainHandler == null) );
                            //重连成功
                            if (MainActivity.Instance != null &&
                                    MainActivity.Instance.MainHandler != null) {
                                Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                                msg.what = MainActivity.MsgID_Web_ReConnect;
                                MainActivity.Instance.MainHandler.sendMessage(msg);
                                Log.d(TAG, "MsgID_Web_ReConnect" );
                            }
                        }

                        //登录
                        OffLineCnt = 0;
                        isOnLine = true;

//                        if(MainActivity.Instance!=null) {
//                            MainActivity.Instance.WebSetNetWork(true);
//                        }

                        //是否需要发送离线数据
                        if(OffLineSensorts.size()>0){
                            SendOffLineData();
                        }
                        break;

                    case CMD_Heartbeat:
                        //Log.d(TAG, "接收心跳");
                        //心跳
                        OffLineCnt = 0;
                        break;

                    case CMD_Weight:
                        Log.d(TAG, "GetCmd: 获取重量");
                        //称重量
                        int tempA = ControlUtils.getInstance().Data_Weights[0];
                        int tempB = ControlUtils.getInstance().Data_Weights[1];
                        SocketUtils.getInstance().SendWeight(tempA, tempB);
                        break;
                    case CMD_LocalStart:
                        Log.d(TAG, "服务器已收到 开启");

                        break;
                    case CMD_WebStart:
                        Log.d(TAG, "服务器发送任务");

                        //执行服务器任务
                        GetCmdWebStart(data);

                        //应答服务器
                        ReplyWebStart();
                        break;
                    case CMD_LocalStop:
                        Log.d(TAG, "服务器已收到 停止");

                        break;
                    case CMD_WebStop:
                        Log.d(TAG, "服务器发送暂停" + data[11]);

                        //执行服务器任务
                        GetCmdWebStop(data);

                        //应答服务器
                        ReplyWebStop();
                        break;
                    case CMD_GetHWState:
                        Log.d(TAG, "服务器查询工作状态");

                        //应答服务器
                        ReplyWebGetHWState();
                        break;

                    case CMD_WebCarControl:
                        //只有底盘类型
                        if(DisinfectData.IsDevCateCar) {
                            DisinfectData.IsWebUser=true;

                            try {
                                byte[] temp = new byte[data.length - 6];
                                for (int i = 0; i < temp.length; i++) {
                                    temp[i] = data[i + 4];
                                }
                                byte[] send = AndroidUtils.tobeByte(CMD_WebCarControl, temp, (byte) 0x1B);
                                WebCarRepData=send;
                                Send(WebCarRepData);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            //更改消毒模式
                            DisinfectData.CarWorkMode=0;
                            switch (data[31])
                            {
                                case 1:
                                    DisinfectData.CarWorkMode=0;
                                    DisinfectData.IsCarPointMode=true;
                                    break;
                                case 2:
                                    DisinfectData.CarWorkMode=1;
                                    DisinfectData.IsCarPointMode=false;
                                    break;
                                case 3:
                                    DisinfectData.CarWorkMode=2;
                                    break;
                            }
                            CarPlanUtils.getInstance().AddInfoList("网络启动："+DisinfectData.CarWorkMode);

                            //如果当前地图为空
                            if(CarPlanUtils.getInstance().NowMapId==-1)
                            {
                                //接收到启动机器底盘信息
                                GetCmdCarControl(data);
                            }else
                            {
                                //发送事件
                                if (MainActivity.Instance != null &&
                                    MainActivity.Instance.MainHandler != null)
                                {
                                    Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                                    msg.what=MainActivity.MsgID_Web_StartDisinfect;
                                    MainActivity.Instance.MainHandler.sendMessage(msg);
                                }
                            }
                        }else{
                            Log.d(TAG, "CMD_WebCarControl: 不是机器底盘");
                        }
                        break;
                    case CMD_UpDateApk:
                        Log.d(TAG, "CMD_UpDateApk");

                        int index=4;
                        //设备Mac地址
                        String mac=new String(subBytes(data,index,8));
                        index+=8;
                        //apkid
                        int apkid=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
                        Log.d(TAG, "CMD_UpDateApk: "+apkid);
                        MainActivity.Instance.WebApkUpDate(apkid);

                        ReplyUpDateApk(apkid);
                        break;
                    case CMD_OpenLock:
                        Log.d("LXPLock", "服务器开锁");
                        //硬件开锁
                        ControlUtils.getInstance().SetSwitchLock(true,true);

                        //应答服务器
                        ReplyOpenLock();
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //每1秒运行
    public void ThreadOneSecFun()
    {
        if(!isOnLine)
        {
            //离线发送登录
            SendLogin();
        }

        //累减
        if(LastDataCnt>0){
            LastDataCnt--;
        }
    }

    //每5秒运行
    public void ThreadFiveSecFun(){
        if(isOnLine)
        {
            //Log.d(TAG, "发送心跳");
            //在线发送心跳
            SendHeartbeat();
        }

        //超过计数为离线
        OffLineCnt++;
        if(OffLineCnt>=10){//2
            Log.d(TAG, "OffLine "+OffLineCnt);
//            if(isOnLine &&
//               MainActivity.Instance!=null) {
//                MainActivity.Instance.WebSetNetWork(false);
//            }
            isOnLine=false;
        }

        String test="Sensort1:"+SensortUtils1.CheckType+" "+SensortUtils1.Data_Value+" ";
        test+="Sensort2:"+SensortUtils2.CheckType+" "+SensortUtils2.Data_Value+" ";


        //浓度值
        int Sensort = 0;
        int typeId=DisinfectData.planItem.typeId;
        //4是过氧化氢，5是过氧化氯
        if(typeId==4)
        {
            if(SensortUtils1.CheckType.equals("4832")){
                Sensort= SensortUtils1.Data_Value;
            }else if(SensortUtils2.CheckType.equals("4832")){
                Sensort= SensortUtils2.Data_Value;
            }
            test+=" 过氧化氢4832:"+Sensort;
            //Log.d(TAG, "typeId:"+typeId+" 过氧化氢 SensortUtils1 "+Sensort);
        }else
        {
            if(SensortUtils1.CheckType.equals("436C")){
                Sensort= SensortUtils1.Data_Value;
            }else if(SensortUtils2.CheckType.equals("436C")){
                Sensort= SensortUtils2.Data_Value;
            }
            test+=" 过氧化氯436C:"+Sensort;
        }

        //刷新数据
        SensortUtils1.CheckType="";
        SensortUtils2.CheckType="";

        //离线保存数据
        if(!isOnLine){
            OffLineSensorts.add(Sensort);
            OffLineTimeCnts.add(SendSensortCnt);
            Log.d(TAG, "离线保存数据"+OffLineSensorts.size());
        }
        //发送浓度数据 5秒
        if(IsAddSensort) {
            Log.d("SensortUtils",test );
            SendSensort(Sensort,SendSensortCnt);
            if (SendSensortCnt < INTMAX) {
                SendSensortCnt += 5;
            } else {
                SendSensortCnt = 0;
            }
        }

//        //网络用户运行累计
//        if(DisinfectData.IsWebUser){
//            NowUserRunCnt+=5;
//            if(NowUserRunCnt>INTMAX){
//                NowUserRunCnt=INTMAX;
//            }
//        }
    }

    public void SensortDataInit(){
        SendSensortCnt=0;
        OffLineSensorts.clear();
        OffLineTimeCnts.clear();
    }

    //发送离线数据
    public void SendOffLineData(){
        Log.d(TAG, "SendOffLineData: "+OffLineSensorts.size());
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                List<Integer> tempSensorts=new ArrayList<>();
                tempSensorts.addAll(OffLineSensorts);
                OffLineSensorts.clear();
                List<Integer> tempTimeCnts=new ArrayList<>();
                tempTimeCnts.addAll(OffLineTimeCnts);
                OffLineTimeCnts.clear();

                for (int i = 0; i < tempSensorts.size(); i++) {
                    int sensort=tempSensorts.get(i);
                    int timecnt=tempTimeCnts.get(i);
                    SendSensort(sensort,timecnt);

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {}
                }
            }
        });
    }

    public void SendLogin()
    {
        try {
            byte[] data=new byte[12];
            int cnt=0;
            //Data
            //mac
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //当前版本
            int ver=AndroidUtils.getVersionCode(MainActivity.Instance.getApplicationContext());
            temp=AndroidUtils.IntToBytes4(ver);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            byte[] send= AndroidUtils.tobeByte(CMD_Login,data,(byte)(data.length+3));
            String str="";
            for (int i = 0; i < send.length; i++) {
                str+=send[i]+" ";
            }
            Log.d(TAG, "SendLogin: "+str);
            Send(send);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void SendHeartbeat()
    {
        try {
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();

            byte[] data= AndroidUtils.tobeByte(CMD_Heartbeat,temp,(byte) 0x0B);
            Send(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //发送当前重量与空桶重量
    private void SendWeight(int leftValue,int rightValue)
    {
        Log.d(TAG, "SendWeight:"+leftValue+" "+rightValue);
        try {
            byte[] data=new byte[24];
            int cnt=0;
            //Data
            //mac
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //当前重量
            temp=AndroidUtils.IntToBytes4(leftValue);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            temp=AndroidUtils.IntToBytes4(rightValue);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //空桶重量
            temp=AndroidUtils.IntToBytes4(ReagentFrgm.PackWeights[0]);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            temp=AndroidUtils.IntToBytes4(ReagentFrgm.PackWeights[1]);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            byte[] send= AndroidUtils.tobeByte(CMD_Weight,data,(byte) 0x1B);
            Send(send);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //发送开始任务
    public void SendStartDisinfect()
    {
        Log.d(TAG, "SendStartDisinfect: ");
        try {

            byte[] data=new byte[52];//数据长度+6
            int cnt=0;
            //Data
            //mac
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //执行员id
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.userId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //消毒剂id
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.typeId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //目标浓度数据
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.ratioValue);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //喷洒时间
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.sprayTime);
            //1206 移动模式工作
            if(!DisinfectData.IsCarPointMode &&
                    CarPlanUtils.getInstance().WaitTimeCnt>0)
            {
                temp= AndroidUtils.IntToBytes4(-100);
            }
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //增强喷洒时间
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.strengTime);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //消毒时间
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.idleTime);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //撤离时间
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.leaveTime);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //预约时间
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.planTime);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //场景id
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.sceneId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //房间id
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.roomId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //消毒场景
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.NowRunMode);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            byte[] send= AndroidUtils.tobeByte(CMD_LocalStart,data,(byte) 0x37);
            Send(send);

            Log.d(TAG, "主动发送 终端启动机器");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void  GetCmdWebStart(byte[] data)
    {
        int index=4;
        //设备Mac地址
        String mac=new String(subBytes(data,index,8));
        index+=8;
        //执行员id
        int userId=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        // 消毒剂id
        int typeId=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        //目标浓度 数据
        int ratioValue =AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        // 喷洒时间
        int sprayTime=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        // 增强喷洒时间
        int strengTime=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        // 消毒时间
        int idleTime=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        // 撤离时间
        int leaveTime=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        // 预约时间
        int planTime=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        // 场景id
        int sceneId=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        // 房间id   网络化才使用，正常用场景ID即可
        int roomId=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        // 消毒场景
        int runMode=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;

//        //记录用户信息
//        if(NowUserId == userId){
//            NowUserId=userId;
//            NowUserRunCnt=0;
//        }else{
//            //同一个用户
//
//        }

        ParmFrgm.time_streng=strengTime;
        ParmFrgm.time_idle=idleTime;
        Log.d(TAG, "EndTime: "+ ParmFrgm.time_idle);
        ParmFrgm.time_leave=leaveTime;
        ParmFrgm.time_spray=sprayTime;

        //更新消毒任务信息
        PlanItem item=new PlanItem();
        item.mac=mac;
        item.userId=userId;
        item.InitParm(typeId,ratioValue,
                sprayTime,strengTime,idleTime,leaveTime,planTime,
                sceneId,roomId,runMode);
        DisinfectData.planItem=item;

        Log.d(TAG, "GetCmdWebStart: "+item+" SceneId:"+sceneId);
        //发送事件
        if(MainActivity.Instance!=null &&
                MainActivity.Instance.MainHandler!=null)
        {
            Message msg=MainActivity.Instance.MainHandler.obtainMessage();
            msg.what=MainActivity.MsgID_Web_StartDisinfect;
            MainActivity.Instance.MainHandler.sendMessage(msg);
        }
    }

    public void  GetCmdWebStop(byte[] data)
    {
        int index=4;
        //设备Mac地址
        String mac=new String(subBytes(data,index,8));
        index+=8;
        //消毒场景
        int runMode=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;

        DisinfectData.planItem.NowRunMode=runMode;

        Log.d(TAG, "GetCmdWebStop: "+runMode);
        //发送事件
        if(MainActivity.Instance!=null &&
                MainActivity.Instance.MainHandler!=null)
        {
            Message msg=MainActivity.Instance.MainHandler.obtainMessage();
            msg.what=MainActivity.MsgID_Web_StopDisinfect;
            MainActivity.Instance.MainHandler.sendMessage(msg);
        }
    }

    //服务器启动任务应答
    public void ReplyWebStart()
    {
        try {

            byte[] data=new byte[52];//数据长度+6
            int cnt=0;
            //Data
            //mac
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //执行员id
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.userId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //消毒剂id
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.typeId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //目标浓度数据
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.ratioValue);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //喷洒时间
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.sprayTime);
            //1206 移动模式工作
            if(!DisinfectData.IsCarPointMode &&
                    CarPlanUtils.getInstance().WaitTimeCnt>0)
            {
                temp= AndroidUtils.IntToBytes4(-100);
            }
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //增强喷洒时间
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.strengTime);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //消毒时间
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.idleTime);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //撤离时间
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.leaveTime);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //预约时间
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.planTime);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //场景id
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.sceneId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //房间id
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.roomId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //消毒场景
            temp= AndroidUtils.IntToBytes4(DisinfectData.planItem.NowRunMode);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            byte[] send= AndroidUtils.tobeByte(CMD_WebStart,data,(byte) 0x37);
            Send(send);

            Log.d(TAG, "服务器启动任务应答");
            Log.d("LXPTIME", "应答:Plan"+DisinfectData.planItem.planTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //主动发送 终端暂停机器
    public void SendStopDisinfect()
    {
        try {
            byte[] data=new byte[12];
            int cnt=0;
            //Data
            //MacAddress
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //NowRunMode
            temp=AndroidUtils.IntToBytes4(DisinfectData.planItem.NowRunMode);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            byte[] send= AndroidUtils.tobeByte(CMD_LocalStop,data,(byte) 0x0F);
            Send(send);

            Log.d(TAG, "主动发送 终端暂停机器");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //服务器暂停任务应答
    public void ReplyWebStop()
    {
        try {
            byte[] data=new byte[12];
            int cnt=0;
            //Data
            //MacAddress
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //NowRunMode
            temp=AndroidUtils.IntToBytes4(DisinfectData.planItem.NowRunMode);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            byte[] send= AndroidUtils.tobeByte(CMD_WebStop,data,(byte) 0x0F);
            Send(send);

            Log.d(TAG, "服务器暂停任务应答");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

    private int getInt(String data){
        int value=0;
        try{
            value=Integer.valueOf(data);
        }catch (Exception e){}
        return value;
    }

    //发送浓度数据
    public void SendSensort( int Sensort,int TimeAdd)
    {

        try {
            //length -> 十进制 -> -3
            byte[] data=new byte[16];
            int cnt=0;
            //Data
            //MacAddress
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //发送频率 5秒
            temp=AndroidUtils.IntToBytes4(TimeAdd);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //浓度值
            temp=AndroidUtils.IntToBytes4(Sensort);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            Log.d(TAG, "SendSensort: "+Sensort+" time:"+TimeAdd);

            byte[] send= AndroidUtils.tobeByte(CMD_Sensort,data,(byte)0x13);
            Send(send);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //回应设备工作状态
    public void ReplyWebGetHWState()
    {
        try {
            byte[] data=new byte[32];
            int cnt=0;
            //Data
            //MacAddress
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //获取 工作场景,撤离时间,喷洒时间,消毒时间
            temp=MainActivity.Instance.WebGetState();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            byte[] send= AndroidUtils.tobeByte(CMD_GetHWState,data,(byte)0x23);
            Send(send);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //发送消毒结束
    public void SendPlanOver()
    {
        try {
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();

            byte[] data= AndroidUtils.tobeByte(CMD_PlanOver,temp,(byte) 0x0B);
            Send(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //服务器启动机器底盘
    public void  GetCmdCarControl(byte[] data)
    {
        int index=4;
        //设备Mac地址
        String mac=new String(subBytes(data,index,8));
        index+=8;
        //消毒剂Id
        int typeId=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        //目标浓度Id
        int ratioId=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        //用户Id
        int userId=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));
        index+=4;
        //地图Id
        int mapId=AndroidUtils.Bytes4ToInt(subBytes(data,index,4));

        /*
        //更新消毒任务信息
        PlanItem item=new PlanItem();
        item.mac=mac;
        item.userId=userId;
        item.InitParm(typeId,ratioId,
                0,0,0,0,0,
                0,0,0);
        DisinfectData.planItem=item;
        */

        //DisinfectData.planItem=new PlanItem();
        //DisinfectData.planItem.typeId=typeId;

        CarPlanUtils.getInstance().CarChangeMap(mapId);

        ReplyCmdCarControl();

    }

    //回应设备工作状态
    public void ReplyCmdCarControl()
    {
        try {
            byte[] data=new byte[32];
            int cnt=0;
            //Data
            //MacAddress
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //获取 工作场景,撤离时间,喷洒时间,消毒时间
            temp=MainActivity.Instance.WebGetState();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            byte[] send= AndroidUtils.tobeByte(CMD_GetHWState,data,(byte)0x23);
            Send(send);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //发送机器底盘信息
    public void SendCarControlInfo(int info)
    {
        try {
            byte[] data=new byte[12];
            int cnt=0;
            //Data
            //MacAddress
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //info
            temp=AndroidUtils.IntToBytes4(info);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            byte[] send= AndroidUtils.tobeByte(CMD_CarInfo,data,(byte) 0x0F);
            Send(send);

            String str="";
            switch (info){
                case CarPlanUtils.Info_MapErr:
                    str="Info_MapErr";
                    break;
                case CarPlanUtils.Info_MapSuc:
                    str="Info_MapSuc";
                    break;
                case CarPlanUtils.Info_MoveErr:
                    str="Info_MoveErr";
                    break;
                case CarPlanUtils.Info_MoveSuc:
                    str="Info_MoveSuc";
                    break;
            }
            Log.d(TAG, "发送机器底盘信息 "+str);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //网络启动底盘回应
    public void SendWebCarStartRep(){
        //Send(WebCarRepData);
        Log.d(TAG, "启动机器底盘");
    }

    //发送本地启动底盘
    public void SendLocalCarControl(int reagentId,int ratiodId,int userId,int mapId)
    {
        try {
            byte[] data=new byte[24];
            int cnt=0;
            //Data
            //MacAddress
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            temp=AndroidUtils.IntToBytes4(reagentId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            temp=AndroidUtils.IntToBytes4(ratiodId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            temp=AndroidUtils.IntToBytes4(userId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            temp=AndroidUtils.IntToBytes4(mapId);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            Log.d(TAG, "SendLocalCarControl: "+userId+" mapId:"+mapId);

            byte[] send= AndroidUtils.tobeByte(CMD_LocalCarControl,data,(byte)0x1B);
            Send(send);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //服务器更新APK应答
    public void ReplyUpDateApk(int apkid)
    {
        try {
            byte[] data=new byte[12];
            int cnt=0;
            //Data
            //MacAddress
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }
            //apkid
            temp=AndroidUtils.IntToBytes4(apkid);
            for (int i = 0; i < temp.length; i++) {
                data[cnt++]=temp[i];
            }

            byte[] send= AndroidUtils.tobeByte(CMD_UpDateApk,data,(byte) (data.length+3));
            Send(send);

            Log.d(TAG, "服务器更新APK应答");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //发送开锁
    public void ReplyOpenLock()
    {
        try {
            byte[] temp=AndroidUtils.GetMacAddress().getBytes();

            byte[] data= AndroidUtils.tobeByte(CMD_OpenLock,temp,(byte) 0x0B);
            Send(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
