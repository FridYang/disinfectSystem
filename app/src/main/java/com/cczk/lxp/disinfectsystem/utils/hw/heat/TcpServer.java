package com.cczk.lxp.disinfectsystem.utils.hw.heat;

/**
 * Created by pc on 2021/2/2.
 */

import android.os.Message;
import android.util.Log;

import com.cczk.lxp.disinfectsystem.utils.hw.CarPlanUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.PortUnreachableException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by Lenovo on 2018/2/22.
 */
public class TcpServer implements Runnable{
    private String TAG = "HeatUtils";
    private int port = 1234;
    private boolean isListen = true;   //线程监听标志位
    public String testShow="";

    private static TcpServer mSocketServer = null;
    public static TcpServer getInstance(){
        if(mSocketServer == null){
            synchronized (TcpServer.class) {
                mSocketServer = new TcpServer();
            }
        }
        return mSocketServer;
    }

    public ArrayList<ServerSocketThread> SST = new ArrayList<ServerSocketThread>();
    public void connect(int port){
        this.port = port;
    }

    public void SetListen(boolean b){
        isListen = b;
        for (ServerSocketThread s : SST){
            s.isRun = false;
        }
        SST.clear();
    }

    public void disconnect(){
        isListen = false;
        for (ServerSocketThread s : SST){
            s.isRun = false;
        }
        SST.clear();
    }

    private Socket getSocket(ServerSocket serverSocket){
        try {
            return serverSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "run: 监听超时");
            return null;
        }
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(5000);
            while (MainActivity.Instance.IsRun){
                //成功连接
                if(!HeatUtils.getInstance().isStart){
                    HeatUtils.getInstance().isStart=true;
                }

                if(isListen)
                //if(true)
                {
                    Log.i(TAG, "run: 开始监听...");

                    Socket socket = getSocket(serverSocket);
                    if (socket != null){
                        new ServerSocketThread(socket);
                    }

                    //延时等待下一个
                    try{
                        Thread.sleep(2000);
                    }catch (Exception e){}
                }else{
                    //不运行时延时等待
                    try{
                        Thread.sleep(2000);
                    }catch (Exception e){}
                }

            }
//            while (isListen){
//                Log.i(TAG, "run: 开始监听...");
//
//                Socket socket = getSocket(serverSocket);
//                if (socket != null){
//                    new ServerSocketThread(socket);
//                }
//            }

            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ServerSocketThread extends Thread{
        Socket socket = null;
        private PrintWriter pw;
        private InputStream is = null;
        private OutputStream os = null;
        private String ip = null;
        private boolean isRun = true;

        ServerSocketThread(Socket socket){
            this.socket = socket;
            ip = socket.getInetAddress().toString();
            Log.i(TAG, "ServerSocketThread:检测到新的客户端联入,ip:" + ip);

            try {
                socket.setSoTimeout(5000);
                os = socket.getOutputStream();
                is = socket.getInputStream();
                pw = new PrintWriter(os,true);
                start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void send(String msg){
            pw.println(msg);
            pw.flush(); //强制送出数据
        }

        @Override
        public void run() {
            byte buff[]  = new byte[4096];
            String rcvMsg;
            int rcvLen;
            SST.add(this);
            while (isRun && !socket.isClosed() && !socket.isInputShutdown()){
                try {
                    if ((rcvLen = is.read(buff)) != -1 ){
                        rcvMsg = new String(buff,0,rcvLen);
                        Log.i(TAG, "run:收到消息: " + rcvMsg);

                        /*
                        Intent intent =new Intent();
                        intent.setAction("tcpServerReceiver");
                        intent.putExtra("tcpServerReceiver",rcvMsg);
                        MainActivity.context.sendBroadcast(intent);//将消息发送给主界面
                        */

//                        if(MainActivity.Instance!=null &&
//                           MainActivity.Instance.MainHandler!=null )
//                        {
//                            Message msg=new Message();
//                            msg.obj=rcvMsg;
//                            msg.what= MainActivity.MsgID_Web_HeatGetTarget;
//                            MainActivity.Instance.MainHandler.sendMessage(msg);
//                        }

                        if(rcvMsg.length()>0){
                            testShow=rcvMsg.length()+" "+rcvMsg;
                        }

                        if(rcvMsg.indexOf("VideoHwpeople_multipath_Res")!=-1)
                        {
                            //HeatUtils.getInstance().GetTarget=true;
                            HeatUtils.getInstance().ClearSceneCnt=0;
                            //CarPlanUtils.getInstance().AddInfoList(rcvMsg+"有人");

                            testShow=rcvMsg.length()+" "+rcvMsg+"有人";
                        }
                        CarPlanUtils.getInstance().AddInfoList("get:"+rcvMsg.length()+" "+rcvMsg);
                        Log.d(TAG, "get:"+rcvMsg.length()+" "+rcvMsg);

                        //ServerActivity.activity.handler.sendMessage(msg);

                        if (rcvMsg.equals("QuitServer")){
                            isRun = false;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                socket.close();
                SST.clear();
                Log.i(TAG, "run: 断开连接");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}