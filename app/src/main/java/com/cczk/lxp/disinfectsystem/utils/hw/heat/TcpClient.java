package com.cczk.lxp.disinfectsystem.utils.hw.heat;

import android.os.Message;
import android.util.Log;

import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by pc on 2021/2/1.
 */

public class TcpClient implements Runnable {
    private String ip = "";
    private int port = 1234;

    /**
     * single instance TcpClient
     * */
    private static TcpClient mSocketClient = null;
    private TcpClient(){}
    public static TcpClient getInstance(){
        if(mSocketClient == null){
            synchronized (TcpClient.class) {
                mSocketClient = new TcpClient();
            }
        }
        return mSocketClient;
    }


    String TAG = "HeatUtils";
    private Socket mSocket;

    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private SocketThread mSocketThread;
    private boolean isStop = false;//thread flag

    /**
     * 128 - 数据按照最长接收，一次性
     * */
    private class SocketThread extends Thread {

        private String ip;
        private int port;
        public SocketThread(String ip, int port){
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            Log.d(TAG,"SocketThread start ");
            super.run();

            //connect ...
            try {
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }

                InetAddress ipAddress = InetAddress.getByName(ip);
                mSocket = new Socket(ipAddress, port);

                //设置不延时发送
                //mSocket.setTcpNoDelay(true);
                //设置输入输出缓冲流大小
                //mSocket.setSendBufferSize(8*1024);
                //mSocket.setReceiveBufferSize(8*1024);

                if(isConnect())
                {
                    mOutputStream = mSocket.getOutputStream();
                    mInputStream = mSocket.getInputStream();

                    isStop = false;

                    //uiHandler.sendEmptyMessage(1);
                    //onConnectSuccess
                    if(MainActivity.Instance!=null &&
                            MainActivity.Instance.MainHandler!=null )
                    {
                        Message msg=new Message();
                        msg.what= MainActivity.MsgID_Web_HeatLinkSuc1;
                        MainActivity.Instance.MainHandler.sendMessage(msg);
                    }
                }
                /* 此处这样做没什么意义不大，真正的socket未连接还是靠心跳发送，等待服务端回应比较好，一段时间内未回应，则socket未连接成功 */
                else{
                    //uiHandler.sendEmptyMessage(-1);
                    if(MainActivity.Instance!=null &&
                            MainActivity.Instance.MainHandler!=null )
                    {
                        Message msg=new Message();
                        msg.what= MainActivity.MsgID_Web_HeatErr;
                        MainActivity.Instance.MainHandler.sendMessage(msg);
                    }

                    Log.e(TAG,"SocketThread connect fail");
                    return;
                }

            }
            catch (IOException e) {
                //uiHandler.sendEmptyMessage(-1);
                if(MainActivity.Instance!=null &&
                        MainActivity.Instance.MainHandler!=null )
                {
                    Message msg=new Message();
                    msg.what= MainActivity.MsgID_Web_HeatErr;
                    MainActivity.Instance.MainHandler.sendMessage(msg);
                }

                Log.e(TAG,"SocketThread connect io exception = "+e.getMessage());
                e.printStackTrace();
                return;
            }
            Log.d(TAG,"SocketThread connect over ");

            //0204 此应用不需要读取
            /*
            //read ...
            while (isConnect() && !isStop && !isInterrupted()) {

                int size;
                try {
                    byte[] buffer = new byte[1024];
                    if (mInputStream == null) return;
                    size = mInputStream.read(buffer);//null data -1 , zrd serial rule size default 10
                    if (size > 0) {
                        Message msg = new Message();
                        msg.what = 100;
                        Bundle bundle = new Bundle();
                        bundle.putByteArray("data",buffer);
                        bundle.putInt("size",size);
                        bundle.putInt("requestCode",requestCode);
                        msg.setData(bundle);
                        uiHandler.sendMessage(msg);
                    }
                    Log.i(TAG, "SocketThread read listening");
                    //Thread.sleep(100);//log eof
                }
                catch (IOException e) {
                    uiHandler.sendEmptyMessage(-1);
                    Log.e(TAG,"SocketThread read io exception = "+e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }*/
        }
    }



    //==============================socket connect============================
    /**
     * connect socket in thread
     * Exception : android.os.NetworkOnMainThreadException
     * */
    public void connect(String ip, int port){
        this.ip=ip;
        this.port=port;
        Log.d(TAG, "connect: "+ip+" "+port);
    }

    @Override
    public void run() {
        mSocketThread = new SocketThread(ip, port);
        mSocketThread.start();
    }

    /**
     * socket is connect
     * */
    public boolean isConnect(){
        boolean flag = false;
        if (mSocket != null) {
            flag = mSocket.isConnected();
        }
        return flag;
    }

    /**
     * socket disconnect
     * */
    public void disconnect() {
        isStop = true;
        try {
            if (mOutputStream != null) {
                mOutputStream.close();
            }

            if (mInputStream != null) {
                mInputStream.close();
            }

            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mSocketThread != null) {
            mSocketThread.interrupt();//not intime destory thread,so need a flag
        }
    }



    /**
     * send byte[] cmd
     * Exception : android.os.NetworkOnMainThreadException
     * */
    public void sendByteCmd(final byte[] mBuffer,int requestCode) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mOutputStream != null) {
                        mOutputStream.write(mBuffer);
                        mOutputStream.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }


    /**
     * send string cmd to serial
     */
    public void sendStrCmds(String cmd, int requestCode) {
        byte[] mBuffer = cmd.getBytes();
        sendByteCmd(mBuffer,requestCode);
    }


    /**
     * send prt content cmd to serial
     */
    public void sendChsPrtCmds(String content, int requestCode) {
        try {
            byte[] mBuffer = content.getBytes("GB2312");
            sendByteCmd(mBuffer,requestCode);
        }
        catch (UnsupportedEncodingException e1){
            e1.printStackTrace();
        }
    }

/*
    Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Message RMsg=new Message();
            switch(msg.what){
                //connect error
                case -1:
                        //onDataReceiveListener.onConnectFail();
                    if(MainActivity.Instance!=null &&
                            MainActivity.Instance.MainHandler!=null )
                    {

                        msg.what= MainActivity.MsgID_Web_HeatGetTarget;
                        MainActivity.Instance.MainHandler.sendMessage(msg);
                    }
                        disconnect();
                    break;

                //connect success
                case 1:
                        //onDataReceiveListener.onConnectSuccess();

                    break;

                //receive data
                case 100:
                    Bundle bundle = msg.getData();
                    byte[] buffer = bundle.getByteArray("data");
                    int size = bundle.getInt("size");
                    int mequestCode = bundle.getInt("requestCode");
                    //onDataReceiveListener.onDataReceive(buffer, size, mequestCode);
                    break;

            }
        }
    };
    */
}