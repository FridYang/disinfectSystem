package com.cczk.lxp.disinfectsystem.utils.hw;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by pc on 2020/5/5.
 */

public class SensortUtils1 extends  SerialPortBase{
    private static final String TAG = "SensortUtils1";
    public String temp;

    private static Handler handler=null;

    public static int Data_Value=0;
    //分子式
    public static String CheckType="";

    //获取单例
    private static SensortUtils1 instance=null;
    public static SensortUtils1 getInstance() {
        if (instance == null) {
            synchronized (SensortUtils1.class) {
                if (instance == null) {
                    instance = new SensortUtils1();
                }
            }
        }
        return instance;
    }

    //1是过氧化氢
    //2是过氧化氯
    //靠近圆环ttyS4 另一个ttyS1
    //！！拨码开关 1-7下 8上！！
    public void Init(String portno,int baudrate, Handler han){
        this.handler=han;
        //打开串口
        OpenSerialPort(portno,baudrate);

        Log.d(TAG, "OpenSerialPort: "+isConnected()+" no:"+portno);
        //监听服务器
        if(isConnected()){
            ReceiveFun();
        }
    }

    //监听服务器返回数据
    //会一直阻塞 必须放到新线程循环
    public void ReceiveFun(){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                while (MainActivity.Instance.IsRun)
                {
                    try {
                        //串口读取数据
                        SensortUtils1.super.ReadData();
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

    public boolean isConnected(){
        return handler!=null &&
                mSerialPort !=null &&
                mOutputStream != null &&
                mInputStream != null ;
    }

    // 读取数据
    public void CheckCmd()
    {
        byte c1;
        String str="";
        String strHex="";

        ArrayList<Object> DataList=new ArrayList<Object>();
        while ( (GetIn  != GetRead) )
        {
            c1 = Data() ;

            str+=c1+" ";
            String Hex =  Integer.toHexString(c1);
            Hex=Hex.replace("ffffff","");
            Hex=Hex.toUpperCase();
            if(Hex.length()<2){
                Hex="0"+Hex;
            }
            strHex+=Hex+" ";

            DataList.add(Hex);
        }
        //Log.v(TAG,"Get:"+str);
        //Log.v(TAG,"GetHex:"+strHex+" size:"+DataList.size());

        String ShowData="";
        int no=0;
        if(DataList.size()==32)
        {
            ShowData+="起始码：  "+DataList.get(no++)+" "+DataList.get(no++)+"\r\n";
            ShowData+="有效数据字节数：  "+DataList.get(no++)+"\r\n\n";

            CheckType=DataList.get(3).toString()+DataList.get(4).toString();
            ShowData+="气体分子式：  "+DataList.get(no++)+" "+DataList.get(no++)+" "+DataList.get(no++)+" "+
                                     DataList.get(no++)+" "+DataList.get(no++)+" "+DataList.get(no++)+"\r\n";

            ShowData+="气体分子量：  "+DataList.get(no++)+" "+DataList.get(no++)+"\r\n";
            int dec=Integer.parseInt((String) DataList.get(12));
            ShowData+="浮点数小数点位数：  "+DataList.get(no++)+" "+DataList.get(no++)+"\r\n";

            ShowData+="标定单位：  "+DataList.get(no++)+" "+DataList.get(no++)+"\r\n";

            String showValue="";
            try {
                long num1=Long.parseLong((String) DataList.get(15),  16);
                long num2=Long.parseLong((String) DataList.get(16),  16);
                long num3=Long.parseLong((String) DataList.get(17),  16);
                long num4=Long.parseLong((String) DataList.get(18),  16);
                //高低位转换
                int value = (int)(num4 << 0);
                value |= (int)(num3 << 8);
                value |= (int)(num2 << 16);
                value |= (int)(num1 << 24);
                //浓度 * 10 ^ 小数位数
                value*=Math.pow(10,dec);
                Data_Value=(int)(value/Math.pow(10,dec));
                showValue=String.valueOf(Data_Value);

                //Log.d(TAG, "CheckCmd: "+Data_Value);
            }catch (Exception e){}

            ShowData+="当前浓度：  "+DataList.get(no++)+" "+DataList.get(no++)+" "+
                                      DataList.get(no++)+" "+DataList.get(no++)+"    ( "+showValue+" )\r\n";


            ShowData+="当前浓度ADC值：  "+DataList.get(no++)+" "+DataList.get(no++)+" "+
                                           DataList.get(no++)+" "+DataList.get(no++)+"\r\n";
            ShowData+="当前报警状态：  "+DataList.get(no++)+" "+DataList.get(no++)+"\r\n";
            ShowData+="量程：  "+DataList.get(no++)+" "+DataList.get(no++)+" "+
                                  DataList.get(no++)+" "+DataList.get(no++)+"\r\n";

            ShowData+="当前温度：  "+DataList.get(no++)+" "+DataList.get(no++)+"\r\n";
            ShowData+="当前湿度：  "+DataList.get(no++)+" ";

            temp=ShowData;

//            if(handler!=null) {
//                Message msg = handler.obtainMessage();
//                msg.what = MainActivity.MsgID_HW_Sensort;
//                msg.obj =showValue;
//                handler.sendMessage(msg);
//            }
        }if(DataList.size()==5)
        {
            ShowData+=DataList.get(no++)+"\r\n\n";
            ShowData+="保留：  "+DataList.get(no++)+" "+DataList.get(no++)+"\r\n";
            ShowData+="校验：  "+DataList.get(no++)+" "+DataList.get(no++)+"\r\n";

            //发送详情
            if(handler!=null) {
                Message msg = handler.obtainMessage();
                msg.what = MainActivity.MsgID_HW_SensortInfo1;
                msg.obj = temp + ShowData;
                handler.sendMessage(msg);
            }
            //Log.d(TAG, "CheckCmd: "+ temp + ShowData);
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

    // 发送数据 参数使用十六进制
    // 函数内转换成十进制
    public void SendCmd(String[] list)
    {
        byte[] data=new byte[list.length];
        try {
            for (int i = 0; i < list.length; i++) {
                data[i]=(byte)Integer.parseInt(list[i],16);
            }
        }catch (Exception e){
            //Log.e(TAG, "SendCmd: ",e );
            return;
        }

        SendCmd(data);
    }

    //线程一秒执行方法
    public void ThreadOneSecFun() {
            //发送传感器读取数据
            SendCmd(new byte[]{01, 03, 96, 00, 00, 64, 90, 58});
    }
}
