package com.cczk.lxp.disinfectsystem.utils.hw;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.cczk.lxp.disinfectsystem.test.SensorTestActivity;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by pc on 2020/5/5.
 */

public class BatteryUtils extends  SerialPortBase{
    private static final String TAG = "BatteryUtils";

    private static Handler handler=null;

    private int CmdStatue;
    //电池电量
    public int Power=0;
    public int RealPower=100;
    //充电中
    public boolean isBattery=false;
    //离线累计
    public int OffLineCnt=0;
    public int DifferCnt=0;

    public int SendCMDType=0;

    public String str1="电量：100 R:100";
    public String str2="充电：123";

    //获取单例
    private static BatteryUtils instance=null;
    public static BatteryUtils getInstance() {
        if (instance == null) {
            synchronized (BatteryUtils.class) {
                if (instance == null) {
                    instance = new BatteryUtils();
                }
            }
        }
        return instance;
    }

    public void Init(String portno,int baudrate, Handler han){
        this.handler=han;
        //打开串口
        OpenSerialPort(portno,baudrate);

        Log.d(TAG, "OpenSerialPort: "+isConnected()+" no:"+portno+" "+isConnected());
        //监听服务器
        ReceiveFun();
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
                        if(isConnected()) {
                            //串口读取数据
                            BatteryUtils.super.ReadData();
                            //直接立刻读取数据
                            CheckCmd();
                        }
                        Thread.sleep(500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public boolean isConnected(){
        return mSerialPort !=null &&
                mOutputStream != null &&
                mInputStream != null ;
    }

    // 读取数据
    public void CheckCmd()
    {
        String str="";

        byte c1;

        int CmdIn = 0;
        int CmdLen = 0;

        byte CmdBuf[] = new byte[100];
        //Log.d(TAG,"run "+GetIn +" "+ GetRead);
        while ( (GetIn != GetRead) )
        {
            c1 = Data() ;
            switch(CmdStatue)
            {
                case 0:
                    //起始符1
                    if(c1 == (byte)0xFF)
                    {
                        CmdStatue = 1;
                        CmdBuf[CmdIn] = c1;
                        CmdIn  = 1;

                        //设置指令长度
                        CmdLen = 15;
                    }
                    break;
                case 1:
                    //起始符2
                    if(c1 == 0x55)
                    {
                        CmdStatue = 2;
                        CmdBuf[CmdIn] = c1;
                        CmdIn  = 2;
                    }
                    break;
                case 2:
                    //数据
                    CmdBuf[CmdIn] = c1;
                    //校验
                    if(++CmdIn  >= CmdLen)
                    {
                        //Log.d(TAG, "Check: "+CmdIn+"-"+CmdBuf[0]);
                        //GetCmd(CmdBuf);

                        String dataStr="";
                        for (int i = 0; i < CmdLen; i++) {
                            String s=Integer.toHexString(CmdBuf[i]);
                            s=s.toUpperCase();
                            s=s.replace("FFFFFF","");
                            if(s.length()==1){
                                s="0"+s;
                            }
                            dataStr+= s +" ";
                        }

                        //Log.d(TAG, "Check: "+CmdBuf[7]+"-"+CmdBuf[8]);
                        //      00 01 02 03 04 05 06 07 08 09 10 11 12 13 14
                        // 电量 FF 55 00 01 82 07 01 16 0D 24 00 00 D2 0D 0A
                        if( CmdBuf[0]==(byte)0xFF &&
                            CmdBuf[1]==(byte)0x55 &&
                            CmdBuf[2]==(byte)0x00 &&
                            CmdBuf[3]==(byte)0x01 &&
                            CmdBuf[4]==(byte)0x82 &&
                            CmdBuf[5]==(byte)0x07 &&
                            CmdBuf[6]==(byte)0x01 &&
                            CmdBuf[7]==(byte)0x16 &&
                            CmdBuf[8]==(byte)0x0D)
                        {
                            try{
                                OffLineCnt=0;

                                String valueStr=Integer.toHexString(CmdBuf[9]);
                                RealPower= (int)Long.parseLong(valueStr,  16);
                                //初始化
                                if(Power==0){
                                    Power=RealPower;
                                }
                                //跟上一个做判断
                                //区别大于3
                                if(Math.abs(RealPower-Power)<3)
                                {
                                    Power=RealPower;
                                }else{
                                    if(DifferCnt<=0)
                                    {
                                        DifferCnt=10;
                                    }
                                }
                                str1="电量："+Power+" R:"+RealPower+" |"+ dataStr;
                                Log.d(TAG, str1);
                            }catch (Exception e){e.printStackTrace();}
                        }

                        //      00 01 02 03 04 05 06 07 08 09 10 11 12 13 14
                        // 充电 FF 55 00 01 82 07 01 16 16 87 00 01 3E 0D 0A
                        else
                            if( CmdBuf[0]==(byte)0xFF &&
                                CmdBuf[1]==(byte)0x55 &&
                                CmdBuf[2]==(byte)0x00 &&
                                CmdBuf[3]==(byte)0x01 &&
                                CmdBuf[4]==(byte)0x82 &&
                                CmdBuf[5]==(byte)0x07 &&
                                CmdBuf[6]==(byte)0x01 &&
                                CmdBuf[7]==(byte)0x16 &&
                                CmdBuf[8]==(byte)0x16)
                        {
                            //FF 55 00 01 82 07 01 16 16 0D 00 00 C4 0D 0A
                            //FF 55 00 01 82 07 01 16 16 16 00 00 CD 0D 0A
                            //FF 55 00 01 82 07 01 16 16 C7 00 01 7E 0D 0A

                            try{
                                OffLineCnt=0;
                                int type=CmdBuf[9];
                                String typeHX=Integer.toBinaryString((type & 0xFF) + 0x100).substring(1);
                                if(typeHX.charAt(1)=='0')
                                {
                                    isBattery=true;
                                }else{
                                    isBattery=false;
                                }
                                str2="充电："+isBattery+" |"+dataStr+"|"+typeHX;
                                Log.d(TAG, str2);
                            }catch (Exception e){e.printStackTrace();}
                        }

                        CmdStatue = 0;
                        CmdIn = 0;
                    }
                    break;
            }
        }
    }

    public void SendCmd(byte[] data)
    {
        if(!isConnected())return;

        try {
            mOutputStream.write(data, 0,data.length);

            String str="";
            for (int i=0;i<data.length;i++){
                str+= data[i]+" ";
            }
            //Log.v(TAG,"Send:"+str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //获取电量
    public void SendGetValue()
    {
        SendCMDType++;
        if(SendCMDType>5){
            SendCMDType=0;
        }

        switch (SendCMDType)
        {
            case 0:
            case 1:
            case 2:
                // 0D  RelativeStateOfCharge  电量
                SendCmd(new byte[]{
                        (byte)0xFF,(byte)0x55,(byte)0x00,(byte)0x01,(byte)0x02,
                        (byte)0x06,(byte)0x01,(byte)0x16,(byte)0x0D,(byte)0x02,
                        (byte)0x00,(byte)0x2F,(byte)0x0D,(byte)0x0A});
                break;
            case 3:
            case 4:
            case 5:
                // 16  BatteryStatus  充电中
                SendCmd(new byte[]{
                        (byte)0xFF,(byte)0x55,(byte)0x00,(byte)0x01,(byte)0x02,
                        (byte)0x06,(byte)0x01,(byte)0x16,(byte)0x16,(byte)0x02,
                        (byte)0x00,(byte)0x38,(byte)0x0D,(byte)0x0A});
                break;
        }
        //Log.d(TAG, "SendGetValue: "+SendCMDType);

        /*
        FF 55 00 01 02 06 01 16 0D 02 00 2F 0D 0A
        校验  00 01 02 06 01 16 0D 02 = 00 2F
        FF 55 00 01 02 06 01 16 16 02 00 38 0D 0A
        */
    }

    //线程一秒执行方法
    public void ThreadOneSecFun()
    {
        //发送传感器读取数据
        SendGetValue();

        if(DifferCnt>0){
            DifferCnt--;
            if(DifferCnt<=0){
                Power=RealPower;
            }
        }
    }
}
