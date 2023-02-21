package com.cczk.lxp.disinfectsystem.utils.hw;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.MapItem;
import com.cczk.lxp.disinfectsystem.test.CarTestActivity;
import com.cczk.lxp.disinfectsystem.utils.base.SocketUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import java.io.File;
import java.io.IOException;

import android_serialport_api.SerialPort;

/**
 * Created by pc on 2020/5/5.
 */

public class CarUtils extends  SerialPortBase{
    private static final String TAG = "CarUtils";

    private int CmdStatue;
    public int CarMoveCnt=0;
    //电池电量
    public int Power=100;
    //目前位置点
    public MapItem NowPos=new MapItem();
    //目标位置点
    private MapItem NewPos =new MapItem();

    private final byte CMD_GoSpeed = (byte)0x02;         // 速度
    private final byte CMD_GoPlan  = (byte)0x03;         // 导航
    private final byte CMD_Power   = (byte)0x0A;          // 电量

    private Handler handler=null;

    //遥控启动底盘
    public boolean isRemoteStart=false;

    public boolean isConnected(){
        return handler!=null &&
                mSerialPort !=null &&
                mOutputStream != null &&
                mInputStream != null ;
    }

    //获取单例
    private static CarUtils instance=null;
    public static CarUtils getInstance() {
        if (instance == null) {
            synchronized (CarUtils.class) {
                if (instance == null) {
                    instance = new CarUtils();
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

        //for (int i = 0; i < 50; i++)
        while ( (GetIn != GetRead) )
        {
            //c1 = TestData() ;
            c1 = Data() ;
            //Log.v(TAG,"State="+CmdStatue+" c1="+c1+" "+CmdBuf[0]);

            //初定数据大小
            CmdLen=28;

            switch(CmdStatue)
            {
                case 0:
                    //地址
                    if(c1 == 0x55)
                    {
                        CmdStatue = 1;

                        CmdBuf[0] = c1;
                        CmdIn  = 1;
                    }
                    break;
                case 1:
                    //地址
                    if(c1 == 0x77)
                    {
                        CmdStatue = 2;

                        CmdBuf[1] = c1;
                        CmdIn  = 2;
                    }
                    break;
                case 2:
                    //CMD 命令
                    CmdStatue = 3;

                    CmdBuf[2] = c1;
                    CmdIn  = 3;
                    break;
                case 3:
                    //Num 子命令
                    CmdStatue = 4;

                    CmdBuf[3] = c1;
                    CmdIn  = 4;
                    break;
                case 4:
                    //数据
                    CmdBuf[CmdIn] = c1;
                    //校验
                    if(++CmdIn  >= CmdLen)
                    {
                        //Log.d(TAG, "Check: "+CmdIn+"-"+CmdBuf[0]);
                        GetCmd(CmdBuf);

                        if(handler!=null) {
                            //校验成功
                            Message msg = handler.obtainMessage();
                            msg.what = MainActivity.MsgID_HW_Control;
                            msg.obj = CmdBuf;
                            //handler.sendMessage(msg);
                            //MainActivity.instance.ShowInfo(MainActivity.Type_HW,"主板通信收到："+str);
                        }

                        CmdStatue = 0;
                        CmdIn = 0;
                    }
                    break;
            }
        }
    }

    //-------------------------------------- 处理命令
    private void GetCmd(byte[] data)
    {
        String str = "";
        for (int i = 0; i < data.length; i++) {
            str += Integer.toHexString(data[i]) + " ";
        }
        //Log.d(TAG, "Info: " + str);

        byte cmd = data[3];
        String info = "";
        byte[] tempByte;
        if(data[2]==0x03)
        {
            //Log.d(TAG, "GetCmd: " + cmd);
            switch (cmd) {
                case CMD_GoSpeed:
                    info = "获取当前速度 ";

                    tempByte=new byte[]{data[7],data[6],data[5],data[4]};
                    float speed =Bytes4ToData(tempByte);

                    tempByte=new byte[]{data[15],data[14],data[13],data[12]};
                    float angle =Bytes4ToData(tempByte);

                    info+=" 速度:"+speed;
                    info+=" 角度:"+angle;

                    if(speed !=0 || angle != 0){
                        Log.d(TAG, "GetCmd: "+info);
                        CarMoveCnt=5;//7
                        CarPlanUtils.getInstance().CarKeepMove();
                    }
                    break;
                case CMD_GoPlan:
                    info = "导航目标 ";
                    //85 119 3 3 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0

                    switch (data[4]){
                        //接着移动
                        case 0:
                        case 1:
                            Log.d(TAG, "导航信息返回 开始执行任务");

                            //CarPlanUtils.getInstance().AddInfoList("导航信息返回 开始执行任务");
                            break;
                        //停止移动
                        default:
                            switch (data[4]) {
                                case 3:
                                     Log.d(TAG, "导航信息返回 成功到达");
                                    CarPlanUtils.getInstance().CarWait();
                                    break;
                                case 4:
                                     Log.d(TAG, "导航信息返回 判定无法到达");
                                    break;
                                case 9:
                                     Log.d(TAG, "导航信息返回 判定目标丢失");
                                    break;
                                default:
                                     Log.d(TAG, "导航信息返回 收到取消或无法到达");
                                    break;
                            }
                            break;
                    }
                    info+=" "+str;

                    Log.d(TAG,"CMD_GoPlan:"+ info);
                    break;

                    //85 119 1 7 1 0 0 0 50 48 50 48 49 48 50 50 49 48 50 57 51 54 0 0 0 0 0 0

                case (byte)0x0C:
                    info = "切换地图 ";

                    switch (data[4]){
                        case (byte)0x10:
                            info+="切换地图成功";
                             CarPlanUtils.getInstance().CarChangeMapOver();
                            break;
                        case (byte)0x11:
                            info+="已经选择此地图";
                            CarPlanUtils.getInstance().CarChangeMapOver();
                            break;
                        case (byte)0x12:
                            info+="存在多个当前名字的地图";
                            break;
                        case (byte)0x13:
                            info+="没有当前地图";
                            break;
                        default:
                            info+="其他情况";
                            break;
                    }

                    Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                    msg.what = MainActivity.MsgID_UI_Toast;
                    msg.obj=info;
                    MainActivity.Instance.MainHandler.sendMessage(msg);
                    break;

                case (byte)0x01:
                    info = "获取位置信息 "+str;
                    int cnt=4;
                    tempByte=new byte[]{data[cnt+0],data[cnt+1],data[cnt+2],data[cnt+3]};
                    float x =Bytes4ToData(tempByte);
                    cnt+=4;
                    tempByte=new byte[]{data[cnt+0],data[cnt+1],data[cnt+2],data[cnt+3]};
                    float y =Bytes4ToData(tempByte);
                    cnt+=4;
                    tempByte=new byte[]{data[cnt+0],data[cnt+1],data[cnt+2],data[cnt+3]};
                    float z =Bytes4ToData(tempByte);
                    cnt+=4;
                    tempByte=new byte[]{data[cnt+0],data[cnt+1],data[cnt+2],data[cnt+3]};
                    float w =Bytes4ToData(tempByte);

                    x = Math.abs(((int) (x * 1000f)) / 1000f);
                    y = Math.abs(((int) (y * 1000f)) / 1000f);
                    z = Math.abs(((int) (z * 1000f)) / 1000f);
                    w = Math.abs(((int) (w * 1000f)) / 1000f);
                    info = x+" : "+y+" : "+z+" : "+w+"  | ";

//                    float dicX=((int)(Math.abs(NewPos.x-x)* 100f)) / 100f;
//                    float dicY=((int)(Math.abs(NewPos.y-y)* 100f)) / 100f;
//                    float dicZ=((int)(Math.abs(NewPos.z-z)* 100f)) / 100f;
//                    float dicW=((int)(Math.abs(NewPos.w-w)* 100f)) / 100f;
//
//                    info+=dicX+" : ";
//                    info+=dicY+" : ";
//                    info+=dicZ+" : ";
//                    info+=dicW;

                    //info += NewPos.x+" : "+NewPos.y+" : "+NewPos.z+" : "+NewPos.w;

                    NowPos.x=x;
                    NowPos.y=y;
                    NowPos.z=z;
                    NowPos.w=w;

                    NowPos.valueX=DataToBytes4(NowPos.x) ;
                    NowPos.valueY=DataToBytes4(NowPos.y) ;
                    NowPos.valueZ=DataToBytes4(NowPos.z) ;
                    NowPos.valueW=DataToBytes4(NowPos.w) ;

                    str=NowPos.x+" : "+NowPos.y+" : "+NowPos.z+" : "+NowPos.w;

//                    Message msg = handler.obtainMessage();
//                    msg.what=CarTestActivity.flag_infoShow;
//                    msg.obj =str;
//                    handler.sendMessage(msg);

                    // Log.d(TAG, info);


                    //设置初始位置
                    if(isSetInitPos){
                        isSetInitPos=false;
                        SendSetInitPos();
                    }

                    //取消判断 0112
//                    if(isRunCheckPos) {
//                        CarPlanUtils.getInstance().CarCheckPos();
//                    }
                    break;

                case CMD_Power:
                    if(data[4]==0x04){
                        //info = "获取电量 ";
                        //Log.d(TAG, "获取电量 "+data[6]);
                        String valueStr=Integer.toHexString(data[6]);
                        Power= (int)Long.parseLong(valueStr,  16);
                    }
                    break;
                default:
                    Log.v(TAG, "协议号=" + Integer.toHexString(cmd));
                    info = "协议号 " + Integer.toHexString(cmd);
                    break;
            }
        }

        if(cmd != CMD_GoSpeed &&
           cmd != CMD_Power)
        {
            /*
            String test="导航信息返回 ";
            test+="Cmd:"+Integer.toHexString(data[2])+" ";
            test+="Num:"+Integer.toHexString(data[3])+" ";
            test+="Data:"+Integer.toHexString(data[4])+" ";

            CarPlanUtils.getInstance().AddInfoList(test);
            */

            //Log.d(TAG, "GetCmd: " + cmd);
            // Log.d(TAG, info+"  "+str);
        }
    }


    /*
    //是否到达终点
    public String InPlaceStr="";
    public boolean InPlace(){
        float dicX=((int)(Math.abs(NewPos.x-NowPos.x)* 10f)) / 10f;
        float dicY=((int)(Math.abs(NewPos.y-NowPos.y)* 10f)) / 10f;
        float dicZ=((int)(Math.abs(NewPos.z-NowPos.z)* 10f)) / 10f;
        float dicW=((int)(Math.abs(NewPos.w-NowPos.w)* 10f)) / 10f;

        String str="";
        str="X1: "+NewPos.x+"   X2: "+NowPos.x+"\r\n";
        str+="Z1: "+NewPos.z+"   Z2: "+NowPos.z+"\r\n";
        str+="X: "+dicX+"   Y: "+dicY+"   Z: "+dicZ+"   W:"+dicW;

        InPlaceStr=str;

        Message msg = handler.obtainMessage();
        msg.what=CarTestActivity.flag_infoShow;
        msg.obj =str;
        handler.sendMessage(msg);

        if(dicX<0.15f && dicY<0.5f && dicZ<0.15f && dicW<0.5f){
            return true;
        }else{
            return false;
        }
    }*/

    public void SendCmd(final byte[] data)
    {
        if(!isConnected())return;

        if(!DisinfectData.IsDevCateCar)return;

        try {
            mOutputStream.write(data);

            String str="";
            for (int i=0;i<data.length;i++){
                str+= data[i]+" ";
            }
            Log.d(TAG, "SendCmd: "+str);
        } catch (IOException e) {
            e.printStackTrace();
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

                    try {
                        //串口读取数据
                        CarUtils.super.ReadData();
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

    /*
    public void ThreadOneSecFun()
    {

    }

    public void ThreadFiveSecFun(){

    }
*/

    //发送前进
    public void SendGoFront(){
        final byte[] data=new byte[]{
                (byte)0x55,(byte)0x77,(byte)0x01,(byte)0x01,
                (byte)0x95,(byte)0x43,(byte)0x0B,(byte)0x3D,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};

        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 20; i++) {
                    try {
                        Thread.sleep(100);

                        SendCmd(data);
                    } catch (InterruptedException e) {}
                }
            }
        });
    }

    //移动点1
    public void SendMovePos1(){
        float x =-0.012f;
        float y = 0.012f;
        float z = 0.000f;
        float w = 1.000f;

        byte[] valueX=new byte[]{(byte)0xBC,(byte)0x44,(byte)0x9B,(byte)0xA0};
        byte[] valueY=new byte[]{(byte)0x3C,(byte)0x44,(byte)0x9B,(byte)0xA0};
        byte[] valueA=new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        byte[] valueW=new byte[]{(byte)0x3F,(byte)0x80,(byte)0x00,(byte)0x00};

        SendMovePos(x,y,z,w,valueX,valueY,valueA,valueW);
    }

    //移动点2
    public void SendMovePos2(){
        float x =  1.291f;
        float y =  0.137f;
        float z =  0.000f;
        float w =  1.000f;

        byte[] valueX=new byte[]{(byte)0x3F,(byte)0xA5,(byte)0x3F,(byte)0x7C};
        byte[] valueY=new byte[]{(byte)0x3E,(byte)0x0C,(byte)0x49,(byte)0xBA};
        byte[] valueA=new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        byte[] valueW=new byte[]{(byte)0x3F,(byte)0x80,(byte)0x00,(byte)0x00};

        SendMovePos(x,y,z,w,valueX,valueY,valueA,valueW);
    }


    //移动点3
    public void SendMovePos3(){
        float x = -0.573f;
        float y =  6.774f;
        float z =  0.000f;
        float w =  1.000f;

        byte[] valueX=new byte[]{(byte)0xBF,(byte)0x12,(byte)0xB0,(byte)0x20};
        byte[] valueY=new byte[]{(byte)0x40,(byte)0xD8,(byte)0xC4,(byte)0x9B};
        byte[] valueA=new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        byte[] valueW=new byte[]{(byte)0x3F,(byte)0x80,(byte)0x00,(byte)0x00};

        SendMovePos(x,y,z,w,valueX,valueY,valueA,valueW);
    }

    //移动点4
    public void SendMovePos4(){
        float x = -1.360f;
        float y =  6.784f;
        float z =  0.000f;
        float w =  1.000f;

        byte[] valueX=new byte[]{(byte)0xBF,(byte)0xAE,(byte)0x14,(byte)0x7A};
        byte[] valueY=new byte[]{(byte)0x40,(byte)0xD9,(byte)0x16,(byte)0x87};
        byte[] valueA=new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        byte[] valueW=new byte[]{(byte)0x3F,(byte)0x80,(byte)0x00,(byte)0x00};

        SendMovePos(x,y,z,w,valueX,valueY,valueA,valueW);
    }

    //移动点
    public void SendMovePos(float x,float y,float z,float w,
                              byte[] valueX,byte[] valueY,byte[] valueZ,byte[] valueW){

        CarPlanUtils.getInstance().ExecuteCnt=60;

        NewPos.x=Math.abs(x);
        NewPos.y=Math.abs(y);
        NewPos.z=Math.abs(z);
        NewPos.w=Math.abs(w);
        Log.d(TAG, "SendMovePos  X = " + NewPos.x+" Z = "+NewPos.z);

        if(valueX==null ||valueX.length<4)
            valueX=new byte[4];
        if(valueY==null ||valueY.length<4)
            valueY=new byte[4];
        if(valueZ==null ||valueZ.length<4)
            valueZ=new byte[4];
        if(valueW==null ||valueW.length<4)
            valueW=new byte[4];

        try{
            final byte[] data=new byte[]{
                    (byte)0x55,(byte)0x77,(byte)0x01,(byte)0x02,
                    valueX[3],valueX[2],valueX[1],valueX[0],
                    valueY[3],valueY[2],valueY[1],valueY[0],
                    valueZ[3],valueZ[2],valueZ[1],valueZ[0],
                    valueW[3],valueW[2],valueW[1],valueW[0],
                    (byte)0x0A,(byte)0xD7,(byte)0x83,(byte)0x40,
                    (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};

            SendCmd(data);

            String test="Send:";
            for (int i = 0; i < data.length; i++) {
                test+=Integer.toHexString(data[i])+" ";
            }
            //MainActivity.Instance.TestCarBug(test);
        }catch (Exception e){e.printStackTrace();}
    }

    //跟随
    public void SendFollow(){
        final byte[] data=new byte[]{
                (byte)0x55,(byte)0x77,(byte)0x01,(byte)0x05,
                (byte)0x01,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        SendCmd(data);
    }

    //停止
    public void SendStop(){
        final byte[] data=new byte[]{
                (byte)0x55,(byte)0x77,(byte)0x01,(byte)0x04,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        SendCmd(data);
    }

    //获取当前地图信息
    public void SendGetMap(){
        final byte[] data=new byte[]{
                (byte)0x55,(byte)0x77,(byte)0x02,(byte)0x05,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};

        SendCmd(data);
    }

    int GetTestRead=0;
    byte[] GetTestBuf=new byte[]{0x55,0x77,0x03,0x02,
            0x3D,0x79, (byte) 0x98,0x3E,
            0x00,0x00,0x00,0x00,
            (byte) 0xB5,(byte) 0xD8,0x12,0x3D,
            0x00,0x00};
    private byte TestData() {
        byte c1;
        c1=GetTestBuf[GetTestRead++];
        if(GetTestRead>=GetTestBuf.length){
            GetTestRead=0;
        }
        return c1;
    }

    //切换地图信息
    private void SendChangeMap(byte[] time){
        //55 77 01 07
        // 01 00 00 00
        // 32 50 31 37
        // 50 33 50 32
        // 31 36 33 50
        // 31 34 00 00
        // 00 00 00 00

        // map-201022-20201022-102936

        CarPlanUtils.getInstance().ExecuteCnt=60;

        final byte[] data=new byte[]{
                (byte)0x55,(byte)0x77,(byte)0x01,(byte)0x07,
                (byte)0x01,(byte)0x00,(byte)0x00,(byte)0x00,
                //年
                time[0],time[1],time[2],time[3],
                //日期
                time[4], time[5], time[6], time[7],
                //时间
                time[8], time[9], time[10], time[11],
                time[12], time[13],(byte)0x00,(byte)0x00,

                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};

        SendCmd(data);
    }

    //获取位置信息
    private void SendGetCarPos(){
        final byte[] data=new byte[]{
                (byte)0x55,(byte)0x77,(byte)0x02,(byte)0x01,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};

        SendCmd(data);
    }

    //切换地图信息
    public void SendChangeMap(String mapName){
        Log.d(TAG, "SendChangeMap: "+mapName);
        String[] list=mapName.split("-");
        byte[] data=new byte[14];

        if(list.length==4 &&
                list[2].length()==8 &&
                list[3].length()==6)
        {
            char[] tempChar = list[2].toCharArray();
            int cnt = 0;
            for (int i = 0; i < tempChar.length; i++) {
                data[cnt++] = (byte) ((int) tempChar[i]);
                //Log.d(TAG, "data: " + tempChar[i] + "-" + (Integer.toHexString(data[cnt-1])));
            }

            tempChar = list[3].toCharArray();
            for (int i = 0; i < tempChar.length; i++) {
                data[cnt++] = (byte) ((int) tempChar[i]);
                //Log.d(TAG, "data: " + tempChar[i] + "-" + (Integer.toHexString(data[cnt-1])));
            }

            SendChangeMap(data);
        }else{
            Log.e(TAG, "SendChangeMap: 数据异常");
        }
    }


    public byte[] DataToBytes4(float f) {
        //Float f=0.850f;
        String TempStr=Integer.toHexString(Float.floatToIntBits(f));
        byte[] dataList=new byte[4];
        String HexStr="";
        try{
            if(TempStr.length()==8) {
                char[] list=TempStr.toCharArray();
                for (int i = 0; i <4; i++) {
                    int no=7-i*2;
                    String data=list[no-1]+""+list[no];
                    HexStr+=data+" ";

                    dataList[3-i]=(byte)Integer.parseInt(data,16);
                }
            }
        }catch (Exception e){}
        return  dataList;
    }

    public float Bytes4ToData(byte[] b) {
        int index=0;

        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }

    boolean isSetInitPos=false;
    public void SetNowIsInitPos(){
        isSetInitPos=true;
        SendGetCarPos();
    }

    boolean isRunCheckPos =false;
    public void RunCheckPos(){
        isRunCheckPos =true;
        SendGetCarPos();
    }

    //设置初始位置
    public void SendSetInitPos(){
        if(CarPlanUtils.getInstance().mapPosItems.size()>0)
        {
            MapItem item = CarPlanUtils.getInstance().mapPosItems.get(0);
            //设置000为初始位置
            byte[] valueX = item.valueX;
            byte[] valueY = item.valueY;
            byte[] valueZ = item.valueZ;
            byte[] valueW = item.valueW;

            Log.d(TAG, "SendSetInitPos: " + valueX.length);
            final byte[] data = new byte[]{
                    (byte) 0x55, (byte) 0x77, (byte) 0x01, (byte) 0x03,
                    valueX[3], valueX[2], valueX[1], valueX[0],
                    valueY[3], valueY[2], valueY[1], valueY[0],
                    valueZ[3], valueZ[2], valueZ[1], valueZ[0],
                    valueW[3], valueW[2], valueW[1], valueW[0],
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

            SendCmd(data);
        }
    }


    //设置返回充电桩
    public void SendGoBatteryPos(byte[] valueX,byte[] valueY,byte[] valueZ,byte[] valueW){
        final byte[] data=new byte[]{
                (byte)0x55,(byte)0x77,(byte)0x01,(byte)0x0B,
                valueX[3],valueX[2],valueX[1],valueX[0],
                valueY[3],valueY[2],valueY[1],valueY[0],
                valueZ[3],valueZ[2],valueZ[1],valueZ[0],
                valueW[3],valueW[2],valueW[1],valueW[0],
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};

        SendCmd(data);
    }

    //获取电量
    public void SendGetPower(){
        final byte[] data=new byte[]{
                (byte)0x55,(byte)0x77,(byte)0x02,(byte)0x0A,
                (byte)0x01,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};

        SendCmd(data);
    }

}
