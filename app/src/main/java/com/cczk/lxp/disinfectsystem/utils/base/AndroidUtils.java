package com.cczk.lxp.disinfectsystem.utils.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * Created by pc on 2020/5/11.
 */

public class AndroidUtils {
    private  static final String TAG="AndroidUtils";
;
    //唯一标识
    private static String NetWork_Mac="";

    //当前语言
    public static Locale langue;

    {
        //Ver:9  Data:20-12-28 19:40  更新喷洒时间计算 机器人消毒时间显示
        //Ver:10 Data:21-01-04 14:55  更换消毒机接口
        //Ver:11 Data:21-01-12 14:25  添加传感器 更新Wifi状态信息
        //Ver:12 Data:21-01-14 09:00  修复底盘延时
        //Ver:13 Data:21-01-15 16:15  添加可调整传感器
        //Ver:15 Data:21-02-02 09:40  添加绑定界面Mac地址
        //Ver:16 Data:21-02-02 09:40  电池信息
        //Ver:17 Data:21-02-21 15:55  添加网络化房间阀门
        //Ver:18 Data:21-03-12 10:50  修改阀门
        //Ver:19 Data:21-07-05 10:10  隐藏刷新图标
        //Ver:20 Data:21-07-15 16:55  启动画面黑
        //Ver:21 Data:21-07-29 15:10  启动画面蓝
        //Ver:23 Data:21-08-10 11:10  新功能添加
        //Ver:25 Data:21-08-23 09:45  喷洒时间恢复
        //Ver:26 Data:21-08-31 10:55  新功能修改 充电
        //Ver:27 Data:21-09-02 17:40  充电动画
        //Ver:28 Data:21-09-16 17:15  流动泵
        //Ver:29 Data:21-10-06 15:00  添加场景
        //Ver:30 Data:21-10-08 11:20  场景大小
        //Ver:31 Data:21-10-18 15:00  新添加NFC
        //Ver:32 Data:21-11-04 15:00  调整溶液桶
        //Ver:33 Data:21-11-17 14:45  充电状态
        //Ver:35 Data:22-05-07 16:00  分辨率
    }
    //Ver:36 Data:22-09-30 15:20  功能修改
    //Ver:37 Data:22-10-10 16:55  功能修改
    //Ver:38 Data:22-10-22 10:55  功能修改
    public static final String Version="0.0.38";

    //Type 0 (2020/01/01)
    //Type 1 (2021/10/18)
    public static final int NFCType=1;

    //获取Mac地址
    public static String GetMacAddress(){
        if(NetWork_Mac==""){
            //NetWork_Mac =Settings.System.getString(MainActivity.Instance.getContentResolver(), Settings.Secure.ANDROID_ID);
            //c367ea27f4c38a1a
            NetWork_Mac =getUniqueId(MainActivity.Instance);
            //取后八位
            int len=NetWork_Mac.length();
            if(len>=8){
                NetWork_Mac = NetWork_Mac.substring(len-8,len);
            }else{
                NetWork_Mac="00000000";
            }
            //Log.d(TAG, "getUniqueId:Build.toMD5 "+NetWork_Mac+" "+NetWork_Mac.substring(len-8,len));
        }
        //f4c38a1a
        //aeff6eb1

        return NetWork_Mac;
    }

    //获取系统时间文本
    public static String GetSystemTime()
    {
        return GetFormatTime("yyyy.MM.dd\r\nHH:mm:ss");
    }

    //获取系统时间文本
    public static String GetThreadRunTime()
    {
        return GetFormatTime("HHmmssSSS");
    }

    public static String GetFormatTime(String pattern)
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        //获取当前时间
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }

    //Byte转Int
    public final static int Bytes4ToInt(byte[] bytes) {
        int res = 0;
        //Log.d(TAG, "Bytes4ToInt: "+bytes[0]+":"+bytes[1]+":"+bytes[2]+":"+bytes[3]);
        for(int i = bytes.length - 1; i >= 0; i--){
//			res |= (0xff << (i- bytes.length + 1) * 8) & (bytes[i] << (i- bytes.length + 1) * 8);
            res |= (0xff << (bytes.length- i - 1) * 8) & (bytes[i] << (bytes.length- i - 1) * 8);
        }
        return res;
    }

    //Int转4Byte
    public final static byte[] IntToBytes4(int val){
        byte[] b = new byte[4];
        b[3] = (byte)(val & 0xff);
        b[2] = (byte)((val >> 8) & 0xff);
        b[1] = (byte)((val >> 16) & 0xff);
        b[0] = (byte)((val >> 24) & 0xff);
        return b;
    }

    //Byte转Int文本
    public static String BytesToString(byte[] data){
        String result="";
        for (int i = 0; i < data.length; i++) {
            String str=Integer.toHexString(data[i]);
            if(str.length()==1){
                str="0"+str;
            }
            str=str.replace("ffffff","");
            str=str.toUpperCase();
            str+=" ";
            result+=str;;
        }
        result+=" len:"+data.length;
        return result;
    }

    //Byte数组转16进制文本
    public static String BytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    //Int秒数转为时间文本
    public static String UIGetTimeStr(int time){
        int hour=0;
        int min = 0;
        int sec=0;
        //计算小时
        hour=time/3600;
        time-=hour*3600;
        //计算分钟
        min=time/60;
        time-=min*60;
        //计算秒
        sec=time;

        String result=String.format("%02d",hour)+" : ";
        result+=String.format("%02d",min)+" : ";
        result+=String.format("%02d",sec);
        return result;
    }

    //缓存数据设置
    private static SharedPreferences sharedPreferences;
    public static void QuickDataSet(Map<String, String> params){
        //步骤1：创建一个SharedPreferences对象
        if(sharedPreferences==null){
         sharedPreferences=MainActivity.Instance.getSharedPreferences("QuickData", Context.MODE_PRIVATE);
        }
        //步骤2： 实例化SharedPreferences.Editor对象
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //步骤3：将获取过来的值放入文件
        for(Map.Entry<String, String> entry : params.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue());
            Log.d(TAG, "QuickDataSet: "+entry.getKey()+" "+entry.getValue());
        }
        //步骤4：提交
        editor.commit();
    }

    //缓存数据读取
    public static int QuickDataGetInt(String name){
        String str=QuickDataGet(name);
        int result=0;
        try{
            result=Integer.parseInt(str);
        }catch (Exception e){}
        return  result;
    }

    //缓存数据读取
    public static int QuickDataTryGetInt(String name)
    {
        int value=0;
        try
        {
            String str=QuickDataGet(name,"");
            if(str!=""){
                value=Integer.parseInt(str);
            }
        }catch (Exception e){e.printStackTrace();}
        return value;
    }

    //缓存数据读取
    public static String QuickDataGet(String name){
        return QuickDataGet(name,"");
    }

    //缓存数据读取
    public static String QuickDataGet(String name,String def){
        if(sharedPreferences==null){
            sharedPreferences=MainActivity.Instance.getSharedPreferences("QuickData", Context.MODE_PRIVATE);
        }
        Log.d(TAG, "QuickDataGet: "+name+" "+sharedPreferences.getString(name,""));
        return sharedPreferences.getString(name,def);
    }

    //获取values文字 自动中英文
    public static String GetStr(int id) {
        return MainActivity.Instance.getResources().getString(id);
    }

    //"12340000-28ee-3eab-ffff-ffffe9374e72";
    //"pc637de8be0945f87b";
    public static String getUniqueId(Context context){
        @SuppressLint("HardwareIds")
        // ANDROID_ID是设备第一次启动时产生和存储的64bit的一个数，当设备被wipe后该数重置。
        String androidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        //Log.d(TAG, "getUniqueId:androidID "+androidID);

        @SuppressLint("HardwareIds")
        String id = androidID + Build.SERIAL; // +硬件序列号
        //Log.d(TAG, "getUniqueId:Build.SERIAL "+Build.SERIAL);

        try {
            String result=toMD5(id);
            //Log.d(TAG, "getUniqueId:Build.toMD5 "+result);
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return id;
        }
    }

    public static String toMD5(String text) throws NoSuchAlgorithmException {
        //获取摘要器 MessageDigest
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        //通过摘要器对字符串的二进制字节数组进行hash计算
        byte[] digest = messageDigest.digest(text.getBytes());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            //循环每个字符 将计算结果转化为正整数;
            int digestInt = digest[i] & 0xff;
            //将10进制转化为较短的16进制
            String hexString = Integer.toHexString(digestInt);
            //转化结果如果是个位数会省略0,因此判断并补0
            if (hexString.length() < 2) {
                sb.append(0);
            }
            //将循环结果添加到缓冲区
            sb.append(hexString);
        }
        //返回整个结果
        return sb.toString().substring(8,24);
    }

    // ====================================== 异或 ======================================

    /**
     * Byte封装
     */
    public final static byte[] tobeByte(byte cmd, byte[] data, byte length){
        //校验失败 在线BCC校验
        byte[] res = new byte[]{(byte) 0xAA,length, 0x02, cmd};
        byte[] bytes = new byte[]{length, 0x02 , cmd};
        bytes = ArrayUtils.addAll(bytes, data);
        byte[] bcc16 = bcc16Bytes(bytes);
        res = ArrayUtils.addAll(res, data);
        res = ArrayUtils.addAll(res, bcc16);
        res = ArrayUtils.addAll(res, new byte[]{(byte) 0xDD});
        return res;
    }

    private static byte[] bcc16Bytes(byte[] bytes){
        return hexStrToByteArray(getBCC(bytes));
    }

    private static byte[] hexStrToByteArray(String str)
    {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return new byte[0];
        }
        byte[] byteArray = new byte[str.length() / 2];
        for (int i = 0; i < byteArray.length; i++){
            String subStr = str.substring(2 * i, 2 * i + 2);
            byteArray[i] = ((byte)Integer.parseInt(subStr, 16));
        }
        return byteArray;
    }

    private static String getBCC(byte[] data) {
        String ret = "";
        byte BCC[]= new byte[1];
        for(int i=0;i<data.length;i++){
            BCC[0]=(byte) (BCC[0] ^ data[i]);
        }
        String hex = Integer.toHexString(BCC[0] & 0xFF);
        if (hex.length() == 1) {
            hex = '0' + hex;
        }
        ret += hex.toUpperCase();
        return ret;
    }

    //Byte数组截取
    public static byte[] SubBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

    public static void CloseApp(Context context) {
        System.exit(0);
    }

    public static void ReStartApp(Context context) {
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static int GetJsonInt(JSONObject jo,String name){
        int data=0;
        try {
        if(jo.has(name)&& jo.isNull(name) == false)
        {
                data = jo.getInt(name);
        }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }

    //隐藏输入法
    public static void HideInput(Activity activity){
        try{
            ((InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }catch (Exception e){}
    }

    //GPRS连接下的ip
    public static String getLocalIpAddress() {
        String ip=null;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() &&
                            inetAddress instanceof Inet4Address)
                    {
                        String temp=inetAddress.getHostAddress().toString();
                        Log.d("HeatUtils", "getLocalIpAddress: "+temp);
                        if(temp.indexOf("192.168.1.")!=-1)
                        {
                            ip=temp;
                            //return temp;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.d("LocalIpAddress", ex.toString());
        }
        return ip;
    }

    public static String getSDPath(){
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if(sdCardExist)
        {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        }
        return sdDir.toString();
    }

    /**
     * 获取当前本地apk的版本
     *
     * @param mContext
     * @return
     */
    public static int getVersionCode(Context mContext) {
        int versionCode = 0;
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            //versionCode = mContext.getPackageManager().
            //        getPackageInfo(mContext.getPackageName(), 0).versionCode;
            String[] list=Version.split("\\.");
            versionCode=Integer.parseInt(list[2]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 创建SoundPool ，注意 api 等级
     */
    static SoundPool mSoundPool;
    private static int soundNFC=0;
    public static void SoundPoolInit() {
        if (mSoundPool == null) {
            // 5.0 及 之后
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = null;
                audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();

                mSoundPool = new SoundPool.Builder()
                        .setMaxStreams(16)
                        .setAudioAttributes(audioAttributes)
                        .build();
            } else { // 5.0 以前
                mSoundPool = new SoundPool(16, AudioManager.STREAM_MUSIC, 0);  // 创建SoundPool
            }
            soundNFC=mSoundPool.load(MainActivity.Instance, com.cczk.lxp.disinfectsystem.R.raw.nfc,1);
            //mSoundPool.setOnLoadCompleteListener(this);  // 设置加载完成监听
        }
    }

    public static void PlayNFCSound()
    {
        //Log.d(TAG, "PlayNFCSound: "+(mSoundPool!=null) + soundNFC);
        if(mSoundPool!=null && soundNFC!=0 &&
            MainActivity.Instance.soundWaitCnt<=0 )
        {
            MainActivity.Instance.soundWaitCnt=3;
            mSoundPool.play(soundNFC, 1, 1, 1, 0, 1);
        }
    }

}
