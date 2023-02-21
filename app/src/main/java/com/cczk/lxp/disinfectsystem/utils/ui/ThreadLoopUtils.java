package com.cczk.lxp.disinfectsystem.utils.ui;

import android.util.Log;

import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.SocketUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.BatteryUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.CarPlanUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.CarUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.NFC2Utils;
import com.cczk.lxp.disinfectsystem.utils.hw.NFCUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.SensortUtils1;
import com.cczk.lxp.disinfectsystem.utils.hw.SensortUtils2;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by pc on 2020/5/14.
 */

public class ThreadLoopUtils {
    public static String TAG="ThreadLoopUtils";

    // 线程池
    // 为了方便管理,采用线程池进行线程管理
    public ExecutorService mThreadPool;

    //500毫秒时间戳计数
    private int mLast_500=0;
    //1秒时间戳计数
    private int mLast_1000=0;
    //5秒时间戳计数
    private int mLast_5000=0;
    //1分钟时间戳计数
    private int mLast_1min =0;

    //获取单例
    private static ThreadLoopUtils instance=null;
    public static ThreadLoopUtils getInstance() {
        if (instance == null) {
            synchronized (ThreadLoopUtils.class) {
                if (instance == null) {
                    instance = new ThreadLoopUtils();
                }
            }
        }
        return instance;
    }

    public void Init(){
        // 初始化线程池
        mThreadPool = Executors.newCachedThreadPool();

        //开启读取线程
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                ThreadLoopFun();
            }
        });
        //开启计时线程
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                ThreadMilliSecFun();
            }
        });
    }

    public void Close(){
        try {
            int awaitTime=10;
            // 向学生传达“问题解答完毕后请举手示意！”
            mThreadPool.shutdown();

            // 向学生传达“XX分之内解答不完的问题全部带回去作为课后作业！”后老师等待学生答题
            // (所有的任务都结束的时候，返回TRUE)
            if(!mThreadPool.awaitTermination(awaitTime, TimeUnit.MILLISECONDS)){
                // 超时的时候向线程池中所有的线程发出中断(interrupted)。
                mThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            // awaitTermination方法被中断的时候也中止线程池中全部的线程的执行。
            //System.out.println("awaitTermination interrupted: " + e);
            mThreadPool.shutdownNow();
        }
    }

    private void ThreadLoopFun(){
        while (MainActivity.Instance.IsRun) {

        }
    }

    private void ThreadMilliSecFun(){
        while (MainActivity.Instance.IsRun) {


            try{
                String second=System.currentTimeMillis()+"";
                second= second.substring(9);
                second= AndroidUtils.GetThreadRunTime();
                int time=Integer.parseInt(second);

                //半秒
                try{

                    if(Math.abs(mLast_500-time) >= 500){
                        mLast_500=time;

                        if(MainActivity.Instance!=null) {
                            //主界面线程运行
                            MainActivity.Instance.ThreadHalfSecFun();
                        }

                        //获取称数据
                        if(MainActivity.Instance!=null){
                            if(MainActivity.Instance.GetNowFrgmFlag()==MainActivity.flag_leave ||
                                    MainActivity.Instance.GetNowFrgmFlag()==MainActivity.flag_run)
                            {
                                ControlUtils.getInstance().SendGetdRadar();
                            }
                        }

                        //NFC
                        switch (AndroidUtils.NFCType)
                        {
                            case 0:
                                NFCUtils.getInstance().ThreadHalfSecFun();
                                break;
                            case 1:
                                NFC2Utils.getInstance().ThreadHalfSecFun();
                                break;
                        }
                    }

                }catch (Exception e){ e.printStackTrace();}


                //一秒
                try{
                    if(Math.abs(mLast_1000-time) >= 1000)
                    {
                        mLast_1000=time;
                        if(MainActivity.Instance!=null)
                        {
                            //主界面线程运行
                            MainActivity.Instance.ThreadOneSecFun();
                        }

                        if(SocketUtils.getInstance().isConnected())
                        {
                            //服务器长连接发送数据
                            SocketUtils.getInstance().ThreadOneSecFun();
                        }

                        if(CarUtils.getInstance().isConnected()){
                            //机器底盘运行
                            CarPlanUtils.getInstance().ThreadOneSecFun();
                        }

                        if(BatteryUtils.getInstance().isConnected()){
                            //智能电池运行
                            BatteryUtils.getInstance().ThreadOneSecFun();
                        }

                        //传感器读取数据
                        SensortUtils1.getInstance().ThreadOneSecFun();
                        SensortUtils2.getInstance().ThreadOneSecFun();

                        if(ControlUtils.getInstance().isConnected()){
                            ControlUtils.getInstance().ThreadOneSecFun();

                            //获取称数据
                            if(MainActivity.Instance!=null){
                                if(MainActivity.Instance.GetNowFrgmFlag()==MainActivity.flag_reagent)
                                {
                                    Log.d("ControlTest", "ControlUtils:"+ControlUtils.getInstance().Data_Weights[0]);
                                    ControlUtils.getInstance().SendGetWeight();
                                }
                            }
                        }

                        //NFC
                        switch (AndroidUtils.NFCType)
                        {
                            case 0:

                                break;
                            case 1:
                                //NFC2Utils.getInstance().ThreadHalfSecFun();
                                break;
                        }
                    }
                }catch (Exception e){ e.printStackTrace();}

                //五秒
                try{
                    if(Math.abs(mLast_5000-time) >= 5000)
                    {
                        mLast_5000=time;

                        if(ControlUtils.getInstance().isConnected()) {
                            //硬件控制板运行
                            ControlUtils.getInstance().ThreadFiveSecFun();
                        }

                        if(SocketUtils.getInstance().isConnected()) {
                            //服务器长连接发送数据
                            SocketUtils.getInstance().ThreadFiveSecFun();
                        }

                        if(MainActivity.Instance!=null)
                        {
                            //主界面线程运行
                            MainActivity.Instance.ThreadFiveSecFun();
                        }
                    }
                }catch (Exception e){ e.printStackTrace();}

                //一分钟
                try{
                    if(Math.abs(mLast_1min -time) >= 1000 * 60 * 1 )
                    {
                        mLast_1min =time;

                        if(MainActivity.Instance!=null) {
                            //主界面线程运行
                            MainActivity.Instance.ThreadOneMinFun();
                        }
                    }
                }catch (Exception e){ e.printStackTrace();}

                Thread.sleep(100);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void NewThreadDemo(){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {

            }
        });
    }
}
