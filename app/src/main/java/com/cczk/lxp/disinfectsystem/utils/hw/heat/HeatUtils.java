package com.cczk.lxp.disinfectsystem.utils.hw.heat;

import android.os.Message;
import android.util.Log;

import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.CarPlanUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by pc on 2021/2/21.
 */

public class HeatUtils {
    public static String TAG = "HeatUtils";

    //是否与热成像连接上 (是否启动检测)
    public boolean isStart=false;
    //无人场景累计
    public int ClearSceneCnt=0;
    //默认值 0为默认有人 100为默认无人
    public int ClearSceneDefault=100;
    ExecutorService exec = Executors.newCachedThreadPool();

    //安卓板 IP
    String AndroidIP = "192.168.1.105";
    //主机  IP
    String HeatIP = "192.168.1.106";
    //DM20  IP
    String Dm20IP1 = "192.168.1.107";
    String Dm20IP2 = "192.168.1.108";
    String Dm20IP3 = "192.168.1.109";

    //获取单例
    private static HeatUtils instance=null;
    public static HeatUtils getInstance() {
        if (instance == null) {
            synchronized (HeatUtils.class) {
                if (instance == null) {
                    instance = new HeatUtils();
                }
            }
        }
        return instance;
    }

    //获取当前是否无人
    public boolean GetNowClearScene(){
        //无人延时检测0.5秒
        if(HeatUtils.getInstance().ClearSceneCnt>=8)
        {
            return true;
        }else{
            return false;
        }
    }

    public void Open(){
        //直到最后成功连接才是成功
        isStart=false;

        //GetTarget=false;

        //1020 打开后不能走动
        //ClearSceneCnt=0;
        ClearSceneCnt=ClearSceneDefault;

        //首先 自己作为客户端 TcpClient 与热成像主机服务端 进行连接
        //

        //连接热成像主机服务端
        int port = 12315;
        TcpClient.getInstance().connect(HeatIP, port);
        exec.execute(TcpClient.getInstance());

        //等待连接成功后 发送数据SendClient
        Log.d(TAG, "Open: "+ AndroidUtils.getLocalIpAddress());

        CarPlanUtils.getInstance().AddInfoList("开启TcpClient");

        LinkSuc1SendClient();
    }

    public void SetListen(boolean b){
        TcpServer.getInstance().SetListen(b);
        ClearSceneCnt=ClearSceneDefault;
    }

    public void LinkSuc1SendClient(){
        //热成像连接成功后 发送Dm20数据
            CarPlanUtils.getInstance().AddInfoList("连接TcpClient");
        //安卓板的IP
        AndroidIP= AndroidUtils.getLocalIpAddress();

        //连接后立刻发送数据
        String data="<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<MESSAGE Verison=\"1.0\">\n" +
                "\t<HEADER MsgType=\"VideoHwpeople_multipath_Req\" MsgSeq=\"\"/>\n" +
                "\t<ParamList>\n" +
                "\t\t<Item>\n" +
                "\t\t\t<Videocapture>rtsp://"+Dm20IP1+"/ONVIFMedia</Videocapture>\n" +
                "\t\t</Item>\n" +
                "\t\t<Item>\n" +
                "\t\t\t<Videocapture>rtsp://"+Dm20IP2+"/ONVIFMedia</Videocapture>\n" +
                "\t\t</Item>\n" +
                "\t\t<Item>\n" +
                "\t\t\t<Videocapture>rtsp://"+Dm20IP3+"/ONVIFMedia</Videocapture>\n" +
                "\t\t</Item>\n" +
                "\t\t<PortID>12320</PortID>\n" +
                "\t\t<HostIP>"+AndroidIP+"</HostIP>\n" +
                "\t</ParamList>\n" +
                "</MESSAGE>";
        TcpClient.getInstance().sendStrCmds(data,1001);

        //延时发送后 关闭客户端 开启服务端
        new Thread(){
            @Override
            public void run() {
                try{
                    Thread.sleep(500);

                    if(MainActivity.Instance!=null &&
                            MainActivity.Instance.MainHandler!=null )
                    {
                        Message msg=new Message();
                        msg.what= MainActivity.MsgID_Web_HeatLinkSuc2;
                        MainActivity.Instance.MainHandler.sendMessage(msg);
                    }
                }catch (Exception e){}
            }
        }.start();

        Log.d(TAG, "SendClient: ");
    }

    public void LinkSuc2ChangeServer(){
        TcpClient.getInstance().disconnect();

        int port = 12320;
        TcpServer.getInstance().connect(port);
        exec.execute(TcpServer.getInstance());

        Log.d(TAG, "ChangeServer: 启动后开启检测");
        SetListen(true);
        CarPlanUtils.getInstance().AddInfoList("连接TcpServer");
    }

    public void Close(){
        CarPlanUtils.getInstance().AddInfoList("热成像关闭");
        isStart=false;

        //GetTarget=false;
        ClearSceneCnt=ClearSceneDefault;

        TcpClient.getInstance().disconnect();
        TcpServer.getInstance().disconnect();

        Log.d(TAG, "Close: ");
    }

    public void UIThreadOneSecFun() {
        //每秒调用一次
        ClearSceneCnt++;
    }

}


