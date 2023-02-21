package com.cczk.lxp.disinfectsystem.utils.hw;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.test.HWTestActivity;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.ReagentFrgm;

import java.io.File;
import java.io.IOException;

import android_serialport_api.SerialPort;

/**
 * Created by pc on 2020/5/5.
 */

public class ControlUtils extends  SerialPortBase{
    private static final String TAG = "ControlUtils";

    private int CmdStatue;

    private final byte CMD_Address = (byte)0x0A;         // 地址
    private final byte CMD_GetdRadar = (byte)0x45;        // 获取雷达
    private final byte CMD_GetWeight = (byte)0x46;        // 获取重量
    private final byte CMD_SetSwitch = (byte)0x47;        // 控制继电器
    private final byte CMD_GetSwitch = (byte)0x48;        // 获取继电器
    private final byte CMD_SetMotor = (byte)0x49;         // 控制马达
    private final byte CMD_GetMotor = (byte)0x50;         // 获取马达
    private final byte CMD_SetMotorDir = (byte)0x51;      // 控制马达方向
    private final byte CMD_SetMotorSpeed = (byte)0x52;   // 控制马达速度

    private Handler handler=null;

    //两个称的重量
    public int[] Data_Weights=new int[2];
    //继电器控制状态
    private byte[] Data_Switch=new byte[11];
    //雷达状态  11为遥控器
    private boolean[] RadarData=new boolean[15];

    //记录是否消毒中
    public boolean isStartDisinfect=false;
    //命令变更倒计时
    private int MissionDelay=0;
    //是否有命令变更
    public boolean MissionChange=false;

    private int LedRunMode=0;     //0不操作 1闪烁绿灯 2闪烁黄灯 3闪烁红灯
    private int LedBlinkTemp=0;   //闪烁计数

    public boolean isConnected(){
        return handler!=null &&
                mSerialPort !=null &&
                mOutputStream != null &&
                mInputStream != null ;
    }

    //获取单例
    private static ControlUtils instance=null;
    public static ControlUtils getInstance() {
        if (instance == null) {
            synchronized (ControlUtils.class) {
                if (instance == null) {
                    instance = new ControlUtils();
                    //默认继电器关闭
                    for (int i = 0; i < instance.Data_Switch.length; i++) {
                        instance.Data_Switch[i]=0;
                    }
                }
            }
        }
        return instance;
    }

    public void Init(String portno,int baudrate, Handler han){
        this.handler=han;
        try
        {
            //主板打开串口
            mSerialPort = new SerialPort(new File("/dev/"+portno), baudrate, 0);

            //获取输出输入流
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

            Log.d("ControlTest","OpenSerialPort");
            if(isConnected()){
                ReceiveFun();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //-------------------------------------- 解析命令

    // 读取数据
    public void CheckCmd()
    {
        String str="";

        byte c1;

        int CmdIn = 0;
        int CmdLen = 0;

        byte CmdBuf[] = new byte[100];
        byte retBuf[] = new byte[100];

        //for (int i = 0; i < 20; i++)
        while ( (GetIn != GetRead) )
        {
            //c1 = TestData() ;
            c1 = Data() ;
            switch(CmdStatue)
            {
                case 0:
                    //地址
                    if(CMD_Address == c1)
                    {
                        CmdBuf[0] = c1;
                        CmdIn  = 1;
                        CmdStatue = 1;
                    }
                    break;
                case 1:
                    //功能码
                    CmdBuf[1] = c1;
                    CmdIn  = 2;
                    CmdStatue = 2;
                    break;
                case 2:
                    //子功能码
                    CmdBuf[2] = c1;
                    CmdIn  = 3;
                    CmdStatue = 3;
                    break;
                case 3:
                    //数据长度
                    CmdBuf[3] = c1;
                    CmdIn  = 4;
                    CmdStatue = 4;
                    CmdLen = (byte)(c1+5);
                    break;
                case 4:
                    //数据
                    int verify;
                    int readverify;

                    CmdBuf[CmdIn] = c1;
                    //校验
                    if(++CmdIn  >= CmdLen)
                    {
                        // readverify
                        readverify = CmdBuf[CmdIn-1] & 0xff;

                        // verify
                        verify=CalcDataVerify(CmdBuf,CmdIn);

                        str="";
                        for (int i = 0; i < CmdIn-1; i++) {
                            str+=CmdBuf[i]+" ";
                        }
                        if(verify == readverify)
                        {
                            GetCmd(CmdBuf);

                            if(handler!=null) {
                                //校验成功
                                Message msg = handler.obtainMessage();
                                msg.what = MainActivity.MsgID_HW_Control;
                                msg.obj = CmdBuf;
                                //handler.sendMessage(msg);
                                //MainActivity.instance.ShowInfo(MainActivity.Type_HW,"主板通信收到："+str);
                            }
                        }

                        CmdStatue = 0;
                        CmdIn = 0;
                    }
                    break;
            }
        }
    }

    //-------------------------------------- 处理命令
    private void GetCmd(byte[] data){
        int cmd=data[1];
        int cnt=0;
        int DataCnt=0;
        String info="";

        //Log.d(TAG, "GetCmd: "+cmd);

        switch (cmd){
            case CMD_GetdRadar :
                info="CMD_GetdRadar(雷达) ";

                RadarData=new boolean[15];
                DataCnt=4;
                for (int i = 0; i < RadarData.length; i++) {
                    RadarData[cnt++]=GetBool(data[DataCnt++]);
                    String add=RadarData[cnt-1]+" ";
                    if(RadarData[cnt-1]){
                        add="0 ";
                    }else{
                        add="_ ";
                    }
                    info+=add;
                    //Log.v(TAG,"CMD_GetdRadar="+RadarData[cnt-1]);
                }
                break;
            case CMD_GetWeight :
                /* 十进制数
                State=4 c1=-1
                State=4 c1=-1
                State=4 c1=-2
                State=4 c1=87

                State=4 c1=0
                State=4 c1=0
                State=4 c1=0
                State=4 c1=33*/
                Log.v(TAG,"WeightData0= "+AndroidUtils.BytesToString(data));
                Log.v(TAG,"WeightData1="+data[4]+":"+data[5]+":"+data[6]+":"+data[7]);
                Log.v(TAG,"WeightData2="+data[8]+":"+data[9]+":"+data[10]+":"+data[11]);

                info="CMD_GetWeight(称) ";

                int[] WeightData=new int[2];
                DataCnt=4;
                byte[] temp;
                temp=new byte[]{data[DataCnt++],data[DataCnt++],
                        data[DataCnt++],data[DataCnt++]};
                WeightData[0]= AndroidUtils.Bytes4ToInt(temp);

                temp=new byte[]{data[DataCnt++],data[DataCnt++],
                        data[DataCnt++],data[DataCnt++]};
                WeightData[1]= AndroidUtils.Bytes4ToInt(temp);

                info+=WeightData[0]+" "+WeightData[1];

                //取绝对值
                Data_Weights[0]=Math.abs(WeightData[0]);
                Data_Weights[1]=Math.abs(WeightData[1]);
                Log.v(TAG,"CMD_GetWeight="+Data_Weights[0]+" "+Data_Weights[1]);
                break;
            case CMD_GetSwitch :
                info="CMD_GetSwitch(开关) ";

                boolean[] SwitchData=new boolean[11];
                DataCnt=4;
                for (int i = 0; i < SwitchData.length; i++) {
                    SwitchData[cnt++]=GetBool(data[DataCnt++]);
                    info+=SwitchData[cnt-1]+" ";
                    Log.v(TAG,"CMD_GetSwitch="+SwitchData[cnt-1]);
                }

                ActivityMsg("CMD_GetSwitch ");
                break;
            case CMD_GetMotor :
                info="CMD_GetMotor(马达) ";

                byte[] MotorData=new byte[12];
                DataCnt=4;
                for (int i = 0; i < MotorData.length; i++) {
                    MotorData[cnt++]=data[DataCnt++];
                    info+=MotorData[cnt-1]+" ";
                    Log.v(TAG,"CMD_GetMotor="+MotorData[cnt-1]);
                }

                ActivityMsg("CMD_GetMotor ");
                break;
            default:
                info="协议号 "+cmd;
                break;
        }
        if(cmd!=CMD_GetWeight) {
            ActivityMsg(info);
        }
    }

    public void ActivityMsg(String data){
        Message msg = handler.obtainMessage();
        msg.obj = data;
        if(handler == HWTestActivity.handler) {
            handler.sendMessage(msg);
        }
        //Log.d(TAG, "ActivityMsg: "+data);
    }

    private boolean GetBool(byte b){
        if(b==0){
            return false;
        }else{
            return true;
        }
    }

//    int GetTestRead=0;
//    byte[] GetTestBuf=new byte[]{00,46,00,8,00,00,00,00,00,00,00,00,0x4E};
//    private byte TestData() {
//        byte c1;
//        c1=GetTestBuf[GetTestRead++];
//        if(GetTestRead>=GetTestBuf.length){
//            GetTestRead=0;
//        }
//        return c1;
//    }

    //必须发0x45 单发45会变成2D
    public void SendCmd(final byte[] data)
    {
        if(!isConnected())return;

        try {
            mOutputStream.write(data);

            String str="";
            for (int i=0;i<data.length;i++){
                str+= data[i]+" ";
            }
            //Log.v(TAG,"Send:"+str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 发送数据
    // 函数内转换成十进制
    public void SendCmd(String[] list)
    {
        byte[] data=new byte[list.length];
        try {
            for (int i = 0; i < list.length; i++) {
                data[i]=(byte)Integer.parseInt(list[i],16);
                //data[i]=(byte)(Integer.parseInt(list[i]));
            }
        }catch (Exception e){
            Log.e(TAG, "SendCmd: ",e );
            return;
        }

        SendCmd(data);
    }

    public void SendCmdTest(byte[] data){
        try {
            mOutputStream.write(data);
            Log.d(TAG, "SendCmdTest: "+data.length);
        }catch (Exception e){}
    }

    //监听服务器返回数据
    //会一直阻塞 必须放到新线程循环
    public void ReceiveFun(){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                while (MainActivity.Instance.IsRun) {

                    //Log.d("ControlTest","run");
                    try {

                        //串口读取数据
                        ControlUtils.super.ReadData();
                        //直接立刻读取数据
                        CheckCmd();

                        Thread.sleep(500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void ThreadOneSecFun()
    {
        if(MissionChange){
            if(MissionDelay>0){
                MissionDelay--;
                if(MissionDelay<=0){
                    MissionChange=false;
                    //执行延时操作
                    if(isStartDisinfect)
                    {
                        //3秒后打开马达
                        ThreadLoopUtils.getInstance().
                                mThreadPool.execute(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("启动", "SceneId:"+DisinfectData.DevCateNo);
                                try {
                                    //普通类型使用电机抽液
                                    for (int i = 0; i < 2; i++) {
                                        ControlUtils.getInstance().SendSetMotor(new byte[]{1,1,1,1});
                                        Thread.sleep(650);
                                        //流动泵速率 22/0919                                      7,6,6,6
                                        //         23/0209                                      4,3,3,3
                                        ControlUtils.getInstance().SendSetMotorSpeed(new byte[]{3,3,3,3});
                                        Thread.sleep(650);
                                    }

                                    if(DisinfectData.DevCateNo==0){
                                        //网络化房间使用阀门抽液
                                        for (int i = 0; i < 2; i++) {
                                            ControlUtils.getInstance().SetSwitchWaterValve(true);
                                            Thread.sleep(500);
                                        }
                                        Log.d("使用阀门抽液", "SceneId:"+DisinfectData.DevCateNo);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                    }else
                    {
                        //5秒后停止风机
                        ThreadLoopUtils.getInstance().
                                mThreadPool.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    for (int i = 0; i < 2; i++) {
                                        ControlUtils.getInstance().SendSwitchSpray(false,false,false,false);
                                        Thread.sleep(500);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }
        }

        //RGB灯闪烁
        switch (LedRunMode){
            case 1:
                if(LedBlinkTemp==0){
                    LedBlinkTemp=1;

                    SetSwitchRGB(false,false,false);
                }else{
                    LedBlinkTemp=0;

                    SetSwitchRGB(false,true,false);
                }
                break;
            case 2:
                if(LedBlinkTemp==0){
                    LedBlinkTemp=1;

                    SetSwitchRGB(false,false,false);
                }else{
                    LedBlinkTemp=0;

                    SetSwitchRGB(true,true,false);
                }
                break;
            case 3:
                if(LedBlinkTemp==0){
                    LedBlinkTemp=1;

                    SetSwitchRGB(false,false,false);
                }else{
                    LedBlinkTemp=0;

                    SetSwitchRGB(true,false,false);
                }
                break;
        }
    }

    public void ThreadFiveSecFun(){
        SendGetWeight();
    }

    //计算数据校验码
    // ！！ 注意 函数主动忽略最后一位数据
    public int CalcDataVerify(byte[] data,int len){
        int verify = 0;
        for(int c1=0; c1<(len-1); c1++)
        {
            int add=0;
            //十进制转十六进制
            //BigInteger bi = new BigInteger(String.valueOf( data[c1]), 16);
            //add= bi.intValue();

            //add= Integer.parseInt(String.valueOf(data[c1]),16);
            add= Integer.parseInt(String.valueOf(data[c1]));

            verify+=add;
            //Log.v(TAG,"CalcVerify "+c1+" "+ data[c1]+" ="+add);
        }
        verify &= 0xff;
        //Log.v(TAG,"CalcVerify End "+verify);
        return verify;
    }

    public byte CalcDataVerify(byte[] data){
        return (byte) CalcDataVerify(data,data.length);
    }

    //-------------------------------------- 发送命令
/*
    CMD_Address = (by
            CMD_GetdRadar = (
            CMD_GetWeight = (
                    CMD_SetSwitch = (
                            CMD_GetSwitch = (
                                    CMD_GetMotor = (b
    CMD_SetMotor = (b
            CMD_SetMotorDir =
            CMD_SetMotorSpeed
            */

    //获取雷达状态
    public void SendGetdRadar(){
        byte[] data=new byte[]{
                CMD_Address,
                CMD_GetdRadar,0,0,0};
        data[data.length-1]=CalcDataVerify(data);
        SendCmd(data);
    }

    //获取称状态
    public void SendGetWeight(){
        Log.d(TAG, "SendGetWeight: ");
        byte[] data=new byte[]{
                CMD_Address,
                CMD_GetWeight ,0,0,0};
        data[data.length-1]=CalcDataVerify(data);
        SendCmd(data);
    }

    //4路风机
    private void SetSwitchSpray(boolean isOn1, boolean isOn2, boolean isOn3, boolean isOn4) {
        if (isOn1) {
            Data_Switch[0] = 1;
        } else {
            Data_Switch[0] = 0;
        }
        if (isOn2) {
            Data_Switch[1] = 1;
        } else {
            Data_Switch[1] = 0;
        }
        if (isOn3) {
            Data_Switch[2] = 1;
        } else {
            Data_Switch[2] = 0;
        }
        if (isOn4) {
            Data_Switch[3] = 1;
        } else {
            Data_Switch[3] = 0;
        }
    }

    //4路风机
    private void SendSwitchSpray(boolean isOn1, boolean isOn2, boolean isOn3, boolean isOn4){
        SetSwitchSpray(isOn1,isOn2,isOn3,isOn4);
        SendSetSwitch();
    }

    //开机时绿色
    public void OpenRGBGreen(){
        LedRunMode=0;
        SetSwitchRGB(false,true,false);
    }

    //运行时黄色
    public void OpenRGBYellow(){
        LedRunMode=0;
        SetSwitchRGB(true,true,false);
    }

    //结束时绿灯闪烁
    public void OpenRGBGreedBlink(){
        LedRunMode=1;
        SetSwitchRGB(false,true,false);
    }

    //撤离时黄灯闪烁
    public void OpenRGBYellowBlink(){
        LedRunMode=2;
        SetSwitchRGB(true,true,false);
    }

    public void OpenRGBRedBlink(){
        LedRunMode=3;
        SetSwitchRGB(true,false,false);
    }

    //RGB 6R 5G 4B
    public void SetSwitchRGB(boolean isOnR,boolean isOnG,boolean isOnB){
        if(isOnR){
            Data_Switch[6]=1;
        }else{
            Data_Switch[6]=0;
        }
        if(isOnG){
            Data_Switch[5]=1;
        }else{
            Data_Switch[5]=0;
        }
        if(isOnB){
            Data_Switch[4]=1;
        }else{
            Data_Switch[4]=0;
        }
        SendSetSwitch();
    }

    //门锁 8门锁1 7门锁2
    public void SetSwitchLock(boolean isOn1,boolean isOn2){
        if(isOn1){
            Data_Switch[8]=1;
        }else{
            Data_Switch[8]=0;
        }
        if(isOn2){
            Data_Switch[7]=1;
        }else{
            Data_Switch[7]=0;
        }
        SendSetSwitch();

        Data_Switch[8]=0;
        Data_Switch[7]=0;
    }

    //紫外灯
    public void SetSwitchRays(boolean isOn){
        if(isOn){
            Data_Switch[9]=1;
        }else{
            Data_Switch[9]=0;
        }
        SendSetSwitch();
    }

    //水阀门
    public void SetSwitchWaterValve(boolean isOn){
        if(isOn){
            Data_Switch[10]=1;
        }else{
            Data_Switch[10]=0;
        }
        SendSetSwitch();
    }

    //设置继电器状态 11路
    private void SendSetSwitch(){
        byte[] SwitchList=Data_Switch;
        if(SwitchList.length==11) {
            byte[] data = new byte[16];
            int cnt = 0;
            data[cnt++] = CMD_Address;
            data[cnt++] = CMD_SetSwitch;
            data[cnt++] = 0;
            data[cnt++] = 0x0B;
            for (int i = 0; i < SwitchList.length; i++) {
                data[cnt++] = SwitchList[i];
            }
            data[data.length - 1] = CalcDataVerify(data);

            String str="";
            for (int i = 0; i < data.length; i++) {
                str+=data[i]+" ";
            }
            Log.v(TAG, "SendSetSwitch " + str);

            SendCmd(data);
        }else{
            Log.v(TAG, "数据异常 长度" + SwitchList.length);
        }
    }

    //打开所有继电器
    public void SendOpenSwitch(){
        for (int i = 0; i < Data_Switch.length; i++) {
            Data_Switch[i] = 1;
        }
        SendSetSwitch();
    }

    //关闭所有继电器
    public void SendCloseSwitch(){
        for (int i = 0; i < Data_Switch.length; i++) {
            Data_Switch[i] = 0;
        }
        SendSetSwitch();
    }

    //获取继电器状态
    public void SendGetSwitch(){
        byte[] data=new byte[]{
                CMD_Address,
                CMD_GetSwitch ,0,0,0};
        data[data.length-1]=CalcDataVerify(data);
        SendCmd(data);
    }

    //写马达控制
    public void SendSetMotor(byte[] MotorList){
        byte[] data=new byte[9];
        int cnt=0;
        data[cnt++]=CMD_Address;
        data[cnt++]=CMD_SetMotor;
        data[cnt++]=0;
        data[cnt++]=0x04;
        for (int i = 0; i < MotorList.length; i++) {
            data[cnt++]=MotorList[i];
        }
        data[data.length-1]=CalcDataVerify(data);
        Log.v(TAG,"SendSetMotor "+ data[data.length-1]);
        SendCmd(data);
    }

    //读马达状态
    public void SendGetGetMotor(){
        byte[] data=new byte[]{
                CMD_Address,
                CMD_GetMotor ,0,0,0};
        data[data.length-1]=CalcDataVerify(data);
        SendCmd(data);
    }

    //设置马达转动方向
    public void SendSetMotorDir(byte[] MotorList){
        byte[] data=new byte[9];
        int cnt=0;
        data[cnt++]=CMD_Address;
        data[cnt++]=CMD_SetMotorDir;
        data[cnt++]=0;
        data[cnt++]=0x04;
        for (int i = 0; i < MotorList.length; i++) {
            data[cnt++]=MotorList[i];
        }
        data[data.length-1]=CalcDataVerify(data);
        Log.v(TAG,"SendSetMotorDir "+ data[data.length-1]);
        SendCmd(data);
    }

    //设置马达转动方向
    public void SendSetMotorSpeed(byte[] MotorList){
        byte[] data=new byte[9];
        int cnt=0;
        data[cnt++]=CMD_Address;
        data[cnt++]=CMD_SetMotorSpeed;
        data[cnt++]=0;
        data[cnt++]=0x04;
        for (int i = 0; i < MotorList.length; i++) {
            data[cnt++]=MotorList[i];
        }
        data[data.length-1]=CalcDataVerify(data);
        Log.v(TAG,"SendSetMotorSpeed "+ data[data.length-1]);
        SendCmd(data);
    }

    //直接方法 开始消毒
    public void FunStartDisinfect(){
        isStartDisinfect=true;

        MissionChange=true;
        MissionDelay=3;

        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 2; i++) {

                        //切换后跳出循环
                        if(!isStartDisinfect){
                            return;
                        }
                        //风机
                        ControlUtils.getInstance().SendSwitchSpray(true,true,true,true);
                        Thread.sleep(300);

                        //切换后跳出循环
                        if(!isStartDisinfect){
                            return;
                        }
                        //紫外灯
                        ControlUtils.getInstance().SetSwitchRays(true);
                        Thread.sleep(200);

                        //消毒液电机延时打开
                        //...
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //直接方法 停止消毒 延时关闭
    public void FunStopDisinfect(){
        isStartDisinfect=false;
        MissionChange=true;
        MissionDelay=5;
        //网络化房间延长几秒
        if(DisinfectData.DevCateNo==0){
            MissionDelay+=1;
        }

        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 3; i++) {

                        //切换后跳出循环
                        if(isStartDisinfect){
                            return;
                        }

                        //消毒液电机关闭
                        ControlUtils.getInstance().SendSetMotor(new byte[]{0,0,0,0});
                        Thread.sleep(300);

                        //切换后跳出循环
                        if(isStartDisinfect){
                            return;
                        }

                        if(DisinfectData.DevCateNo==0)
                        {
                            //阀门关闭
                            ControlUtils.getInstance().SetSwitchWaterValve(false);
                            Thread.sleep(300);
                        }


                        //切换后跳出循环
                        if(isStartDisinfect){
                            return;
                        }
                        //紫外灯
                        ControlUtils.getInstance().SetSwitchRays(false);
                        Thread.sleep(300);

                        //风机延时关闭
                        //...
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //直接方法 停止消毒 遥控立刻关闭
    public void FunRemoteStopDisinfect(){
        isStartDisinfect=false;
        MissionChange=true;
        MissionDelay=5;

        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 3; i++) {
                        //风机立刻关闭
                        ControlUtils.getInstance().SetSwitchSpray(false, false, false, false);

                        //切换后跳出循环
                        if(isStartDisinfect){
                            return;
                        }

                        //消毒液电机关闭
                        ControlUtils.getInstance().SendSetMotor(new byte[]{0,0,0,0});
                        Thread.sleep(300);

                        //切换后跳出循环
                        if(isStartDisinfect){
                            return;
                        }

                        if(DisinfectData.DevCateNo==0){
                            //阀门关闭
                            ControlUtils.getInstance().SetSwitchWaterValve(false);
                            Thread.sleep(300);
                        }

                        //切换后跳出循环
                        if(isStartDisinfect){
                            return;
                        }
                        //紫外灯
                        ControlUtils.getInstance().SetSwitchRays(false);
                        Thread.sleep(300);

                        //风机延时关闭
                        //...
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //立刻停止硬件
    public void AllHWStop(){
        SendCloseSwitch();
        ControlUtils.getInstance().SendSetMotor(new byte[]{0,0,0,0});
    }

    public boolean GetRetomeData(){
        return RadarData[11];
    }

    //返回溶剂比例  0~100
    public int GetWeightRatio(int no){
        //空桶重  20600
        //满桶重  23400  24200
        //溶液重  2800   3600
        int PackWeight=0;
        PackWeight= ReagentFrgm.PackWeights[no];
        float[] FullWeight=new float[]{2800f,3600f};
        float Reagent=Data_Weights[no];
        Reagent-=PackWeight;
        if(Reagent<0){
            Reagent=0;
        }
        Log.d(TAG, "GetWeightRatio: "+
                "no:"+no+" "+
                "value:"+Data_Weights[no]+"  ("+
                Reagent+"="+
                (Reagent/FullWeight[no]));
        Reagent=Reagent/FullWeight[no];
        Reagent*=100;
        if(Reagent>100){
            Reagent=100;
        }
        return (int)Reagent;
    }

    //获取硬件喷洒速率
    public int GetHareWareRate()
    {
        //速率 每分钟喷x毫升
        switch (DisinfectData.HwDevCate){
            case DisinfectData.Dev_Little:
                return 20;//40
            case DisinfectData.Dev_Panda:
            case DisinfectData.Dev_Pretty:
            case DisinfectData.Dev_Walker:
            case DisinfectData.Dev_Guard:
                return 40;//80
            case DisinfectData.Dev_Giraffe:
                return 160;
        }
        return 40;
    }

    //获取硬件现在溶液量 单位毫升
    public float GetNowReagent()
    {
        float ratio1= ControlUtils.getInstance().GetWeightRatio(0)/100f;
        float ratio2=ControlUtils.getInstance().GetWeightRatio(1)/100f;

        /*
        //测试
        try {
            ratio1 = Integer.parseInt(MainActivity.Instance.parmFrgm.edit_planT_hour.getText().toString().trim());
            ratio1 /= 100f;
            ratio2 = Integer.parseInt(MainActivity.Instance.parmFrgm.edit_planT_minute.getText().toString().trim());
            ratio2 /= 100f;
        }catch (Exception e){e.printStackTrace();}
         */

        float now= ratio1*1000f;
        switch (DisinfectData.HwDevCate)
        {
            case DisinfectData.Dev_Panda:
            case DisinfectData.Dev_Pretty:
                //2.5L * 2桶
                now = ratio1*2500;
                now+= ratio2*2500;
                break;
            case DisinfectData.Dev_Little:
                //1L * 1桶
                now= ratio1*1000f;
                break;
        }
        return now;
    }

}
