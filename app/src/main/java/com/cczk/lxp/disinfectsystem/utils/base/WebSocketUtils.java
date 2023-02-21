package com.cczk.lxp.disinfectsystem.utils.base;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.test.WebSocketActivity;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WebSocketUtils {
    public static String TAG="WebSocketUtils";
    //获取单例
    private static WebSocketUtils instance=null;
    public static WebSocketUtils getInstance() {
        if (instance == null) {
            synchronized (WebSocketUtils.class) {
                if (instance == null) {
                    instance = new WebSocketUtils();
                }
            }
        }
        return instance;
    }

    private Handler handler;
    public JWebSocketClient client;
    public static final String url = "ws://192.168.1.150:6062";

//    public boolean isConnected(){
//        if(client==null){
//            return  false;
//        }
//        return client.isOpen();
//    }

    public void initWebSocket(Handler handler) {
        this.handler = handler;
        initWebSocket();
    }
    /**
     * 初始化websocket
     */
    private void initWebSocket()
    {
        Log.e(TAG, "Init:"+url);
        URI uri = URI.create(url);
        //TODO 创建websocket
        client = new JWebSocketClient(uri) {
            @Override
            public void onMessage(String message) {
                super.onMessage(message);
                if (!message.equals("Heartbeat")){
                    SendMsg(MainActivity.MsgID_Web_WSGetMsg, message);
                }
            }

            @Override
            public void onOpen(ServerHandshake handshakedata) {
                super.onOpen(handshakedata);
                SendMsg(MainActivity.MsgID_Web_WSConnect, "websocket连接成功");
            }

            @Override
            public void onError(Exception ex) {
                super.onError(ex);
                SendMsg(MainActivity.MsgID_Web_WSDiconnect,  "websocket连接错误：" + ex);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                super.onClose(code, reason, remote);
                if (code!=1000) {
                    reconnectWs();//意外断开马上重连
                }
                SendMsg(MainActivity.MsgID_Web_WSDiconnect,  "websocket断开连接：·code:" + code + "·reason:" + reason + "·remote:" + remote);
            }
        };
        //TODO 设置超时时间
        client.setConnectionLostTimeout(110 * 1000);
        //TODO 连接websocket
        new Thread() {
            @Override
            public void run() {
                try {
                    //connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
                    client.connectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void SendMsg(int what,Object obj)
    {
        if(handler!=null) {
            Message msg = Message.obtain();
            msg.what = what;
            msg.obj = obj;
            handler.sendMessage(msg);
        }
    }

    /**
     * 发送消息
     *
     * @param msg
     */
    private void sendMsg(String msg) {
        if (null != client) {
            Log.e(TAG, "发送:" + msg);
            if (client.isOpen()) {
                client.send(msg);
            }
        }
    }

    /**
     * 开启重连
     */
    private void reconnectWs() {
        mHandler.removeCallbacks(heartBeatRunnable);
        new Thread() {
            @Override
            public void run() {
                try {
                    Log.e(TAG,"开启重连");
                    client.reconnectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    /**
     * 断开连接
     */
    public void closeConnect() {
        try {
            //关闭websocket
            if (null != client) {
                client.close();
            }
            //停止心跳
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client = null;
        }
    }

    //    -------------------------------------websocket心跳检测------------------------------------------------
    private static final long HEART_BEAT_RATE = 10 * 1000;//每隔10秒进行一次对长连接的心跳检测
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (client != null) {
                if (client.isClosed()) {
                    Log.e(TAG,"心跳检测1"+client.isOpen() + "/" +url);
                    reconnectWs();//心跳机制发现断开开启重连
                } else {
                    Log.e(TAG,"心跳检测2"+client.isOpen() + "/" + url);
                    sendMsg("Heartbeat");
                }
            } else {
                Log.e(TAG,"心跳检测重连");
                //如果client已为空，重新初始化连接
                client = null;
                initWebSocket();
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            CheckHeartBeat();
        }
    };

    //开启心跳检测
    private void CheckHeartBeat()
    {
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);
    }

    public void onResume() {
        CheckHeartBeat();
        if (client == null)
        {
            Log.e(TAG, "onResume");
            initWebSocket();
        } else if (!client.isOpen())
        {
            //进入页面发现断开开启重连
            reconnectWs();
        }
    }
}
