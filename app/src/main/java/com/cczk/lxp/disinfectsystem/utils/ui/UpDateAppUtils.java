package com.cczk.lxp.disinfectsystem.utils.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by pc on 2021/3/16.
 */

public class UpDateAppUtils {
    public final String TAG="UpDateAppUtils";
    public TextView tv;
    public static final String ACTION_REQUEST_SHUTDOWN = "android.intent.action.ACTION_REQUEST_SHUTDOWN";
    public static final String ACTION_REBOOT = "android.intent.action.REBOOT";

    //获取单例
    private static UpDateAppUtils instance=null;
    public static UpDateAppUtils getInstance() {
        if (instance == null) {
            synchronized (UpDateAppUtils.class) {
                if (instance == null) {
                    instance = new UpDateAppUtils();
                }
            }
        }
        return instance;
    }

        //  进度
        private int mProgress;
        //  文件保存路径
        private String mSavePath;
        /**
         * 下载APk
         * @param apk_file_url
         */
        public void downloadAPK(final String apk_file_url,TextView tv) {
            this.tv=tv;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                        {
                            //文件保存路径
                            mSavePath = AndroidUtils.getSDPath()+ "/Download";
                            File dir = new File(mSavePath);
                            if (!dir.exists()){
                                dir.mkdir();
                            }
                            // 下载文件
                            HttpURLConnection conn = (HttpURLConnection) new URL(apk_file_url).openConnection();
                            conn.connect();
                            InputStream is = conn.getInputStream();
                            int length = conn.getContentLength();

                            mSavePath+="/update.apk";
                            File apkFile = new File(mSavePath);
                            FileOutputStream fos = new FileOutputStream(apkFile);

                            int count = 0;
                            byte[] buffer = new byte[1024];
                            //while (!mIsCancel){
                            while (true){
                                int numread = is.read(buffer);
                                count += numread;
                                // 计算进度条的当前位置
                                mProgress = (int) (((float)count/length) * 100);
                                // 更新进度条
                                mUpdateProgressHandler.sendEmptyMessage(1);
                                Log.d(TAG, "更新进度条 "+mProgress);

                                // 下载完成
                                if (numread < 0){
                                    mUpdateProgressHandler.sendEmptyMessage(2);
                                    Log.d(TAG, "下载完成 "+mProgress);
                                    break;
                                }
                                fos.write(buffer, 0, numread);
                            }
                            fos.close();
                            is.close();
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();

        }
        /**
         * 接收消息
         */
        @SuppressLint("HandlerLeak")
        private Handler mUpdateProgressHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case 1:
                        // 设置进度条
                        if(tv!=null) {
                            tv.setText("更新Apk "+mProgress + "%");
                            //proBar.setProgress(mProgress);
                        }
                        break;
                    case 2:
                        // 隐藏当前下载对话框
                        //dialog.dismiss();

                        // 安装 APK 文件
                        installApp(mSavePath);

                        /*
                        //延时重启
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(ACTION_REBOOT);
                                intent.putExtra("EXTRA_KEY_CONFIRM", false);
                                //当中false换成true,会弹出是否关机的确认窗体
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                MainActivity.Instance.startActivity(intent);
                            }
                        }, 60000);
                        */
                }
            };
        };

    //静默安装
    private boolean installApp(String apkPath) {
        Log.d(TAG, "installApp: "+apkPath);
        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder errorMsg = new StringBuilder();
        try {
            process = Runtime.getRuntime().exec("su");
            process = new ProcessBuilder("pm", "install","-i","com.cczk.lxp.disinfectsystem", "-r", apkPath).start();
            successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }
        } catch (Exception e) {
            Log.d(TAG, "installAppE0: "+e.toString());
        } finally {
            try {
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (Exception e) {
                Log.d(TAG, "installAppE1: "+e.toString());
            }
            if (process != null) {
                process.destroy();
            }
        }
        Log.e(TAG,""+errorMsg.toString());
        MainActivity.Instance.MyToast(errorMsg.toString()+"  "+successMsg);
        //如果含有“success”单词则认为安装成功
        MainActivity.Instance.AppReStart(0);
        return successMsg.toString().equalsIgnoreCase("success");
    }

    /*
    public void Test()
    {
        String mSavePath = AndroidUtils.getSDPath()+ "/Download/update.apk";
        installApp(mSavePath);
    }
*/
    /*
        //普通安装
        private void InstallApk(String path){
            File apk = new File(path);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(MainActivity.Instance, "com.cczk.lxp.disinfectsystem.fileprovider", apk);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            MainActivity.Instance. startActivity(intent);
        }
        */

}
