package com.cczk.lxp.disinfectsystem.view.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.PlanItem;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.EDensityUtils;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.base.SocketUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.BatteryUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.CarPlanUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.CarUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.NFC2Utils;
import com.cczk.lxp.disinfectsystem.utils.hw.NFCUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.SensortUtils1;
import com.cczk.lxp.disinfectsystem.utils.hw.SensortUtils2;
import com.cczk.lxp.disinfectsystem.utils.hw.heat.HeatUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.BindCardFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.BindDevFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.CarChoiceFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.CarChoiceFrgm2;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.ChangeOffPwFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.EndFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.IFrgmCallBack;
import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.LeaveFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.LoginFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.MainFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.ParmFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.ReagentFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.RegisterFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.RunFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.SwitchFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.ToSettingsFrgm;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.WebGridFrgm;
import com.cczk.lxp.disinfectsystem.view.utils.CustomDialog;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public String TAG = "MainActivity";
    public static MainActivity Instance;
    public Bundle MyBundle;

    public Handler MainHandler;
    public Handler FrgmHandler;
    public static boolean IsRun = true;
    public boolean IsApkUpDate = false;
    private int TouchPointX = 0;

    //Flag
    public final static int flag_login = 100;            // 登录界面
    public final static int flag_register = 101;         // 注册界面
    public final static int flag_main = 102;            // 使用场景界面
    public final static int flag_parm = 103;            // 消毒参数界面
    public final static int flag_reagent = 104;         // 补充溶液界面

    public final static int flag_switch = 105;             //  选择界面
    public final static int flag_leave = 106;              //  撤离界面
    public final static int flag_run = 107;               //  运行界面
    public final static int flag_end = 108;               //  结束界面
    public final static int flag_binddev = 109;            // 绑定设备
    public final static int flag_bindcard = 110;           // 绑定用户卡
    public final static int flag_webgrid = 111;            // 网络化房间
    //public final static int flag_carChoice =  112;            // 底盘界面
    public final static int flag_carChoice2 = 112;            // 新底盘界面
    public final static int flag_toSettings = 113;           // 主设置界面
    public final static int flag_changeOffPw = 114;        // 更改离线密码

    //MsgID
    public final static int MsgID_UI_Toast = 100;         // UI 显示信息
    public final static int MsgID_UI_Time = 101;          // UI 获取时间
    public final static int MsgID_UI_WebOffLineInfo = 103;     // UI 离线信息
    public final static int MsgID_UI_TestInfo = 104;                // 调试

    public final static int MsgID_HW_NFCID = 200;         // NFC卡Id信息
    public final static int MsgID_HW_Control = 201;       // 硬件控制板
    public final static int MsgID_HW_SensortInfo1 = 202;       // 气体传感器1详情
    public final static int MsgID_HW_SensortInfo2 = 204;       // 气体传感器2详情

    public final static int MsgID_Web_Login = 300;        // 网络登录
    public final static int MsgID_Web_Register = 301;     // 网络注册
    public final static int MsgID_Web_UpdateNFC = 302;     // 修改NFC
    public final static int MsgID_Web_GetHos = 303;        // 获取医院
    public final static int MsgID_Web_GetDep = 304;        // 获取科室
    public final static int MsgID_Web_GetNowScenList = 305;  // 获取用到的用户场景
    public final static int MsgID_Web_ChangeScenList = 306;  // 变更用户场景
    public final static int MsgID_Web_GetRatioList = 308;  // 获取浓度列表
    public final static int MsgID_Web_GetTypeList = 309;  // 获取药剂列表
    public final static int MsgID_Web_GetAllScenList = 310;  // 获取所有用户场景
    public final static int MsgID_Web_GetAllScenImg = 311;  // 获取场景图片

    public final static int MsgID_Web_CheckNFC = 312;       // 检查消毒剂是否可以用
    public final static int MsgID_Web_NFCUsed = 313;        // 消毒剂已经使用
    public final static int MsgID_Web_PostNFC = 314;         // 录入NFC到系统中
    public final static int MsgID_Web_ReConnect = 315;     // 网络重连
    public final static int MsgID_Web_StartDisinfect = 316;     // 网络运行消毒任务
    public final static int MsgID_Web_StopDisinfect = 317;     // 网络暂停消毒任务
    public final static int MsgID_Web_GetUserType = 318;     // 获取用户类型

    public final static int MsgID_Web_CheckBindDev = 320;        // 初始化绑定设备
    public final static int MsgID_Web_GetDevCate = 321;          // 获取设备类型
    public final static int MsgID_Web_GetDevName = 322;          // 获取设备名称
    public final static int MsgID_Web_BindDev = 323;             // 绑定设备
    public final static int MsgID_Web_GridGetNo = 324;           // 网络化房间获取楼号
    public final static int MsgID_Web_GridGetFloor = 325;        // 网络化房间获取层号
    public final static int MsgID_Web_GridGetData = 326;        // 网络化房间获取数据

    public final static int MsgID_Web_GetCardName = 330;          // 获取绑卡名称

    public final static int MsgID_Car_StartDisinfect = 400;     // 底盘运行消毒任务
    public final static int MsgID_Car_StopDisinfect = 401;      //  底盘暂停消毒任务
    public final static int MsgID_Car_OverPos = 403;             // 底盘结束消毒任务
    public final static int MsgID_Car_GetMap = 405;              // 获取地图回调

    public final static int MsgID_Web_HeatLinkSuc1 = 500;            // 热成像创建客户端成功
    public final static int MsgID_Web_HeatLinkSuc2 = 501;            // 热成像创建服务端
    public final static int MsgID_Web_HeatErr = 503;                 // 热成像异常

    public final static int MsgID_Web_ApkUpDate = 505;                 // apk

    public final static int MsgID_Web_WSConnect = 600;              // WebSocket连接成功
    public final static int MsgID_Web_WSDiconnect = 601;            // WebSocket断开连接
    public final static int MsgID_Web_WSGetMsg = 602;               // WebSocket获取信息


    //View
    LinearLayout layout_top;
    ImageView img_network;
    ImageView img_wifi;
    ImageView img_heat;
    ImageView img_battery;
    TextView tv_time;
    TextView tv_test;
    TextView tv_battery;
    TextView tv_battery2;

    //Bitmap
    public Bitmap bm_BtnPlay;
    public Bitmap bm_BtnStop;
    public Bitmap bm_SceneEmpty;

    public Bitmap bm_Network0;
    public Bitmap bm_Network1;
    public Bitmap bm_Network2;
    public Bitmap bm_Wifi0;
    public Bitmap bm_Wifi1;
    public Bitmap bm_Wifi2;
    public Bitmap bm_Heat0;
    public Bitmap bm_Heat1;
    public Bitmap bm_Heat2;
    public Bitmap bm_Battery0;
    public Bitmap bm_Battery1;
    public Bitmap bm_Battery2;
    public Bitmap bm_Battery3;
    public Bitmap bm_reagent1;
    public Bitmap bm_reagent2;
    public Bitmap bm_refresh1;
    public Bitmap bm_refresh2;

    public Bitmap bm_RoomSelect;
    public Bitmap bm_RoomNoSelect;

    public Bitmap bm_leaveBg;
    public Bitmap bm_planBg;
    public Bitmap bm_pandaBg;
    public Bitmap bm_prettyBg;
    public Bitmap bm_littleBg;

    public Bitmap bm_txtLessA;
    public Bitmap bm_txtLessB;
    public Bitmap bm_txtLessAB;
    public Bitmap bm_txtLessScen;
    public Bitmap bm_txtLessAr;
    public Bitmap bm_txtLessBr;
    public Bitmap bm_txtLessABr;
    public Bitmap bm_txtLessScenr;
    public Bitmap bm_txtOpen;
    public Bitmap bm_txtScanning;

    public int color_tvB;
    public int color_tv;
    public int color_tvLess;
    public int batteryNo = 0;

    //热成像检查开启
    public int checkHeatCnt = 0;
    //提示等待
    public static int toastWaitCnt = 5;
    //音效等待
    public static int soundWaitCnt = 5;
    //NFC延时开启
    private int NFCOpenCnt = 0;

    //提示框
    FrameLayout layout_Tips;
    TextView tv_Tips;

    //Frgm
    //只用于判断当前是哪个界面
    private int NowFrgmId = 0;

    private IFrgmCallBack frgmCallBack;
    public ArrayList<Integer> FrgmChangeList;
    Fragment currentFragment = null;

    private LoginFrgm loginFrgm;
    private RegisterFrgm registerFrgm;
    public MainFrgm mainFrgm;
    private ReagentFrgm reagentFrgm;
    public ParmFrgm parmFrgm;
    private SwitchFrgm swithFrgm;
    public LeaveFrgm leaveFrgm;
    public RunFrgm runFrgm;
    private EndFrgm endFrgm;
    private BindDevFrgm bindDevFrgm;
    private BindCardFrgm bindCardFrgm;
    public WebGridFrgm webGridFrgm;
    private CarChoiceFrgm2 carChoiceFrgm;
    private ToSettingsFrgm toSettingsFrgm;
    private ChangeOffPwFrgm changeOffPwFrgm;

    public CustomDialog customDialog;

    public static String mainTest = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //适配屏幕
        EDensityUtils.setDensity(getApplication(), this);

        this.MyBundle = savedInstanceState;

        // 启动图片
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.img_home);
        getWindow().getDecorView().setBackground(new BitmapDrawable(bm));
        // 全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // 输入法
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        setContentView(R.layout.activity_main);

        AndroidUtils.getUniqueId(this);

        Instance = this;
        IsRun = true;


/*

        //启动
        Intent intent=new Intent(MainActivity.this,CarTestActivity.class);
        startActivity(intent);
*/
        /*
         */

        //初始化UI
        InitView();

        //初始化工具
        InitUtils();

        InitFrgm();

        InitData();

        Log.d(TAG, "Open: " + AndroidUtils.getLocalIpAddress());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int writePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (writePermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                File file = new File(AndroidUtils.getSDPath() + "/Download", "test.txt");
                if (file.exists()) {
                    file.delete();
                }

                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    String info = "test";
                    fos.write(info.getBytes("utf-8"));
                    fos.close();
                } catch (Exception e) {

                    e.printStackTrace();

                }
                MainActivity.Instance.MyToast("创建成功!");
            } else {
                MainActivity.Instance.MyToast("授权失败");
            }
        }
    }

    public void InitView() {
        //获取语言类型
        GetLanguage();

        //数值初始化
        FrgmChangeList = new ArrayList<Integer>();

        //Bitmap
        bm_BtnPlay = BitmapFactory.decodeResource(getResources(), R.drawable.btn_play);
        bm_BtnStop = BitmapFactory.decodeResource(getResources(), R.drawable.btn_stop);
        bm_SceneEmpty = BitmapFactory.decodeResource(getResources(), R.drawable.ico_empty);
        bm_RoomSelect = BitmapFactory.decodeResource(getResources(), R.drawable.grid_btnon);
        bm_RoomNoSelect = BitmapFactory.decodeResource(getResources(), R.drawable.grid_btnoff);

        bm_Network0 = BitmapFactory.decodeResource(getResources(), R.drawable.ico_network0);
        bm_Network1 = BitmapFactory.decodeResource(getResources(), R.drawable.ico_network1);
        bm_Network2 = BitmapFactory.decodeResource(getResources(), R.drawable.ico_network2);
        bm_Wifi0 = BitmapFactory.decodeResource(getResources(), R.drawable.ico_wifi0);
        bm_Wifi1 = BitmapFactory.decodeResource(getResources(), R.drawable.ico_wifi1);
        bm_Wifi2 = BitmapFactory.decodeResource(getResources(), R.drawable.ico_wifi2);
        bm_Heat0 = BitmapFactory.decodeResource(getResources(), R.drawable.ico_heat0);
        bm_Heat1 = BitmapFactory.decodeResource(getResources(), R.drawable.ico_heat1);
        bm_Heat2 = BitmapFactory.decodeResource(getResources(), R.drawable.ico_heat2);
        bm_Battery0 = BitmapFactory.decodeResource(getResources(), R.drawable.battery);
        bm_Battery1 = BitmapFactory.decodeResource(getResources(), R.drawable.battery1);
        bm_Battery2 = BitmapFactory.decodeResource(getResources(), R.drawable.battery2);
        bm_Battery3 = BitmapFactory.decodeResource(getResources(), R.drawable.battery3);
        bm_reagent1 = BitmapFactory.decodeResource(getResources(), R.drawable.ico_reagent);
        bm_reagent2 = BitmapFactory.decodeResource(getResources(), R.drawable.ico_reagent2);
        bm_refresh1 = BitmapFactory.decodeResource(getResources(), R.drawable.img_refresh);
        bm_refresh2 = BitmapFactory.decodeResource(getResources(), R.drawable.img_refresh2);

        bm_leaveBg = BitmapFactory.decodeResource(getResources(), R.drawable.full_leave);
        bm_planBg = BitmapFactory.decodeResource(getResources(), R.drawable.full_plan);
        bm_pandaBg = BitmapFactory.decodeResource(getResources(), R.drawable.full_panda);
        bm_prettyBg = BitmapFactory.decodeResource(getResources(), R.drawable.full_pretty);
        bm_littleBg = BitmapFactory.decodeResource(getResources(), R.drawable.full_little);

        bm_txtLessA = BitmapFactory.decodeResource(getResources(), R.drawable.text001);
        bm_txtLessB = BitmapFactory.decodeResource(getResources(), R.drawable.text002);
        bm_txtLessAB = BitmapFactory.decodeResource(getResources(), R.drawable.text003);
        bm_txtLessScen = BitmapFactory.decodeResource(getResources(), R.drawable.text004);
        bm_txtLessAr = BitmapFactory.decodeResource(getResources(), R.drawable.text001r);
        bm_txtLessBr = BitmapFactory.decodeResource(getResources(), R.drawable.text002r);
        bm_txtLessABr = BitmapFactory.decodeResource(getResources(), R.drawable.text003r);
        bm_txtLessScenr = BitmapFactory.decodeResource(getResources(), R.drawable.text004r);
        bm_txtOpen = BitmapFactory.decodeResource(getResources(), R.drawable.text005);
        bm_txtScanning = BitmapFactory.decodeResource(getResources(), R.drawable.text006);

        //View
        tv_time = findViewById(R.id.main_title_time);
        tv_test = findViewById(R.id.main_tv_test);
        tv_battery = findViewById(R.id.main_tv_battery);
        tv_battery2 = findViewById(R.id.main_tv_battery2);
        layout_top = findViewById(R.id.main_layout_top);
        img_network = findViewById(R.id.main_img_network);
        img_wifi = findViewById(R.id.main_img_wifi);
        img_heat = findViewById(R.id.main_img_heat);
        img_battery = findViewById(R.id.main_img_battery);
        img_heat.setVisibility(View.GONE);

        img_network.setImageBitmap(bm_Network0);
        img_wifi.setImageBitmap(bm_Wifi0);
        img_heat.setImageBitmap(bm_Heat0);

        //提示框
        layout_Tips = findViewById(R.id.main_layout_tips);
        tv_Tips = findViewById(R.id.main_tv_tips);

        //Handler
        MainHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                try {
                    Object[] data;
                    switch (msg.what) {
                        // ------------ 界面UI ------------
                        case MsgID_UI_Time:
                            if (!DisinfectData.IsDevHeatCheck) {
                                //时间
                                String str = msg.obj.toString();
                                //str=testShow+" "+DisinfectData.IsCarPointMode;
                                tv_time.setText(str);
                                //tv_time.setText(BatteryUtils.getInstance().str1+"\r\n"+BatteryUtils.getInstance().str2);
                            } else {
//                                tv_time.setText(CarPlanUtils.getInstance().WaitTimeCnt + "");
                                int Cnt = HeatUtils.getInstance().ClearSceneCnt;
                                tv_time.setText((Cnt < 20 ? "有人" : "无人") + " " + Cnt + " " + checkHeatCnt);
                            }

                            //NFC
                            if (NFCOpenCnt > 0) {
                                NFCOpenCnt--;
                                if (NFCOpenCnt <= 0) {
                                    switch (AndroidUtils.NFCType) {
                                        case 0:
                                            NFCUtils.getInstance().Init(MainActivity.this);
                                            break;
                                        case 1:
                                            //延时开启
                                            NFC2Utils.getInstance().Init();
                                            NFCOpen();
                                            break;
                                    }
                                }
                            }

                            if (HttpUtils.IsOnLine) {
                                //网络图标
                                CheckWifiType();
                                CheckNetWorkInit();
                            }

                            //电池信息
                            CheckBattery();
                            //热成像
                            CheckHeat();

                            //消毒液 主界面默认足够
                            if (GetNowFrgmFlag() == flag_main) {
                                DisinfectData.SetReagentSuffice(true, 0);
                            }

                            //提示等待
                            if (toastWaitCnt > 0) {
                                toastWaitCnt--;
                            }
                            //音效等待
                            if (soundWaitCnt > 0) {
                                soundWaitCnt--;
                            }
                            break;
                        case MsgID_UI_Toast:
                            MyToast(msg.obj.toString());
                            break;

                            /*
                        case MsgID_UI_NetWork:
                            if((boolean)msg.obj){
                                TopUISetNetWork(true);
                            }else{
                                TopUISetNetWork(false);
                            }
                            break;
                            */

                        case MsgID_UI_WebOffLineInfo:
                            //MyToast("网络不稳定");
                            break;
                        case MsgID_UI_TestInfo:
                            //if(DisinfectData.IsDevHeatCheck)
                        {
                            //调试信息
                            //tv_test.setText(msg.obj.toString());
                        }
                        break;

                        // ------------ 下载图片延时操作 用作中转 ------------
                        case MsgID_Web_GetAllScenImg:
                            mainFrgm.FrgmMessage(msg);
                            break;

                        // ------------ 网络部分 ------------
                        case MsgID_Web_ReConnect:
                            //网络重连
                            WebReConnect();
                            break;
                        case MsgID_Web_StartDisinfect:
                            DisinfectData.IsWebUser = true;
                            //网络启动
                            WebStartDisinfect();
                            break;
                        case MsgID_Web_StopDisinfect:
                            DisinfectData.IsWebUser = true;
                            //网络暂停
                            WebStopDisinfect();
                            break;

                        // ------------ 底盘部分 ------------

                        case MsgID_Car_StartDisinfect:
                            //CarPlanUtils.getInstance().AddInfoList("Car_StartDisinfect");

                            int time = (int) msg.obj;
                            //底盘启动
                            CarStartDisinfect(time);
                            break;
                        case MsgID_Car_StopDisinfect:
                            //CarPlanUtils.getInstance().AddInfoList("Car_StopDisinfect");

                            //底盘暂停
                            CarStopDisinfect();
                            break;
                        case MsgID_Car_OverPos:
                            //底盘结束
                            CarOverPos();
                            break;

                        // ------------ 热成像部分 ------------
                        case MsgID_Web_HeatLinkSuc1:
                            Log.d("HeatUtils", "MsgID_Web_HeatLinkSuc1");
                            // 热成像创建客户端成功
                            HeatUtils.getInstance().LinkSuc1SendClient();
                            break;
                        case MsgID_Web_HeatLinkSuc2:
                            Log.d("HeatUtils", "MsgID_Web_HeatLinkSuc1");
                            // 热成像创建服务端
                            HeatUtils.getInstance().LinkSuc2ChangeServer();
                            break;
                        case MsgID_Web_HeatErr:
                            Log.d("HeatUtils", "MsgID_Web_HeatErr");
                            // 热成像异常
                            HeatUtils.getInstance().Close();
                            break;

                        case MsgID_Web_ApkUpDate:
                            if (!IsApkUpDate) {
                                IsApkUpDate = true;
                                BtnFrgmLogin();
                                loginFrgm.StartApkUpDate(msg.obj.toString());
                            }
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        FrgmHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                try {
                    frgmCallBack.FrgmMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
    }

    public void InitUtils() {
        //初始化NFC读卡
        //NFCOpenCnt=3;
        switch (AndroidUtils.NFCType) {
            case 0:
                NFCUtils.getInstance().Init(MainActivity.this);
                break;
            case 1:
                //延时开启
                NFC2Utils.getInstance().Init();
                break;
        }

        //开启循环线程
        ThreadLoopUtils.getInstance().Init();

        //开启硬件控制
        ControlUtils.getInstance().Init("ttysWK2", 115200, MainHandler);

        //靠近圆环ttyS4 另一个ttyS1
        //！！拨码开关 1-7下 8上！！
        //初始化传感器
        //过氧化氢
        SensortUtils1.getInstance().Init("ttyS4", 9600, MainHandler);
        //初始化传感器
        //过氧化氯
        SensortUtils2.getInstance().Init("ttyS1", 9600, MainHandler);

        //初始化网络长连接
        SocketInit();

        //初始化机器底盘
        CarUtils.getInstance().Init("ttysWK1", 115200, MainHandler);
        CarPlanUtils.getInstance().CarStop();

        //初始化智能电池
        BatteryUtils.getInstance().Init("ttysWK3", 9600, MainHandler);

        //如果是热成像类型
        if (DisinfectData.IsDevHeatCheck) {
            HeatUtils.getInstance().Open();
            img_heat.setVisibility(View.VISIBLE);
        }

        //初始化音效播放
        AndroidUtils.SoundPoolInit();
    }

    public void SocketInit() {
        Log.d(TAG, "SocketInit");
        SocketUtils.getInstance().Open(HttpUtils.SocketURL, 8001, MainHandler, 0);
    }

    public void InitFrgm() {
        //Frgm卡顿 图片过大
        loginFrgm = new LoginFrgm();
        registerFrgm = new RegisterFrgm();
        mainFrgm = new MainFrgm();
        reagentFrgm = new ReagentFrgm();
        parmFrgm = new ParmFrgm();
        swithFrgm = new SwitchFrgm();
        leaveFrgm = new LeaveFrgm();
        runFrgm = new RunFrgm();
        endFrgm = new EndFrgm();
        bindDevFrgm = new BindDevFrgm();
        bindCardFrgm = new BindCardFrgm();
        webGridFrgm = new WebGridFrgm();
        carChoiceFrgm = new CarChoiceFrgm2();
        toSettingsFrgm = new ToSettingsFrgm();
        changeOffPwFrgm = new ChangeOffPwFrgm();

        FragmentTransaction ft = GetComeTransaction();

        ComeGoFrame(flag_register);
        ComeGoFrame(flag_main);
        ComeGoFrame(flag_reagent);
        ComeGoFrame(flag_parm);
        ComeGoFrame(flag_switch);
        ComeGoFrame(flag_leave);
        ComeGoFrame(flag_run);
        ComeGoFrame(flag_end);
        ComeGoFrame(flag_binddev);
        ComeGoFrame(flag_webgrid);
        ComeGoFrame(flag_carChoice2);
        ComeGoFrame(flag_toSettings);

        ComeGoFrame(flag_login);
    }

    public void GoRunFrgm(boolean NeedRun) {
        runFrgm.NeedStartHW = NeedRun;
        ComeGoFrame(MainActivity.flag_run);
    }

    public void InitAddFrgm(FragmentTransaction ft, Fragment frgm) {
        if (!frgm.isAdded()) {
            //第一次使用switchFragment()时currentFragment为null，所以要判断一下
            if (currentFragment != null) {
                ft.hide(currentFragment);
            }
            ft.add(R.id.main_frame, frgm, frgm.getClass().getName());
            ft.hide(frgm);
        }
    }

    public void ReplaceFrgm(FragmentTransaction ft, Fragment frgm) {
        ft.replace(R.id.main_frame, frgm, frgm.getClass().getName());
        ft.commit();
    }

    public void InitData() {
        AndroidUtils.GetMacAddress();

        //Color
        //如果版本小于 sdk2.3
        Log.d(TAG, "InitView: " + Build.VERSION.SDK_INT + " " + Build.VERSION_CODES.LOLLIPOP_MR1);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            color_tvB = getResources().getColor(R.color.uitvcolorB);
            color_tv = getResources().getColor(R.color.uitvcolor);
            color_tvLess = getResources().getColor(R.color.uitvless);
        } else {
            color_tvB = getColor(R.color.uitvcolorB);
            color_tv = getColor(R.color.uitvcolor);
            color_tvLess = getColor(R.color.uitvless);
        }

        loginFrgm.cacheHos = AndroidUtils.QuickDataGet("cacheHos");
        loginFrgm.cacheDep = AndroidUtils.QuickDataGet("cacheDep");
        loginFrgm.cacheName = AndroidUtils.QuickDataGet("cacheName");
        //预约时间
        DisinfectData.planTimeStr = AndroidUtils.QuickDataGet("planTimeStr");
        //Log.d("LXPTIME","QuickDataGet: "+DisinfectData.planTimeStr);

        //停止硬件
        ControlUtils.getInstance().AllHWStop();

        Log.d(TAG, "InitData MAC: " + AndroidUtils.GetMacAddress());


        //获取设备类型
        NetWorkGetDevCate(AndroidUtils.GetMacAddress());

        //获取网络状态
        CheckNetWorkInit();

        //获取消毒液默认值
        DisinfectData.ReagentWeights[0] = AndroidUtils.QuickDataGetInt("ReagentWeightA");
        DisinfectData.ReagentWeights[1] = AndroidUtils.QuickDataGetInt("ReagentWeightB");
    }

    //显示顶部UI
    public void ShowTopView() {
        layout_top.setVisibility(View.VISIBLE);
    }

    //隐藏顶部UI
    public void HideTopView() {
        layout_top.setVisibility(View.GONE);
    }

    /*
    private void TopUISetNetWork(boolean isOnLine){
        if(isOnLine){
            img_network.setImageBitmap(bm_Network);
        }else{
            img_network.setImageBitmap(bm_NotNetwork);
        }
    }*/

    //WIFI状态
    private void CheckWifiType() {
        int wifi = -100;
        if (isWifiConnect()) {
            //Log.d(TAG, "Logo Wifi:True "+wifi);
            WifiManager mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
            wifi = mWifiInfo.getRssi();//获取wifi信号强度
//            if (wifi > -50 && wifi < 0) {//最强
//                Log.e(TAG, "最强");
//            } else if (wifi > -70 && wifi < -50) {//较强
//                Log.e(TAG, "较强");
//            } else if (wifi > -80 && wifi < -70) {//较弱
//                Log.e(TAG, "较弱");
//            } else if (wifi > -100 && wifi < -80) {//微弱
//                Log.e(TAG, "微弱");
//            }

            if (wifi > -70) {
                //Log.e(TAG, "最强");
                img_wifi.setImageBitmap(bm_Wifi2);

            } else {
                //Log.e(TAG, "微弱");
                img_wifi.setImageBitmap(bm_Wifi1);
            }
        } else {
            //Log.d(TAG, "Logo Wifi:Falise "+wifi);
            //Log.e(TAG, "无wifi连接");
            img_wifi.setImageBitmap(bm_Wifi0);
        }

        /*
        if(!SocketUtils.getInstance().isOnLine){
            //无连接
            img_wifi.setImageBitmap(bm_Wifi0);
        }*/
        //Log.d(TAG, "Logo Wifi:"+wifi+" Net:"+SocketUtils.getInstance().isOnLine);
    }

    private boolean isWifiConnect() {
        ConnectivityManager connManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifiInfo.isConnected();
    }

    public TelephonyManager mTelephonyManager;
    public PhoneStatListener mListener;
    private static final int NETWORKTYPE_WIFI = 0;
    private static final int NETWORKTYPE_4G = 1;
    private static final int NETWORKTYPE_2G = 2;
    private static final int NETWORKTYPE_NONE = 3;

    /**
     * 网络信号强度监听
     *
     */
    private void CheckNetWorkInit() {
        //获取telephonyManager
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//开始监听
        mListener = new PhoneStatListener();
//监听信号强度
        mTelephonyManager.listen(mListener, PhoneStatListener.LISTEN_SIGNAL_STRENGTHS);
    }

    /**
     * Create a broadcast receiver.
     */
    public final BroadcastReceiver mUsbBroadcast = new BroadcastReceiver() {
        public void onReceive(Context paramAnonymousContext, Intent paramAnonymousIntent) {
            if (AndroidUtils.NFCType == 1) {
                NFC2Utils.getInstance().MyReceive(paramAnonymousIntent);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mTelephonyManager.listen(mListener, PhoneStatListener.LISTEN_SIGNAL_STRENGTHS);
        if (AndroidUtils.NFCType == 1) {
            NFC2Utils.getInstance().MyResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //用户不在当前页面时，停止监听
        mTelephonyManager.listen(mListener, PhoneStatListener.LISTEN_NONE);
        if (AndroidUtils.NFCType == 1) {
            NFC2Utils.getInstance().MyPause();
        }
    }

    private class PhoneStatListener extends PhoneStateListener {
        //获取信号强度
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            //img_network.setImageBitmap(bm_Network0);
//获取网络信号强度
//获取0-4的5种信号级别，越大信号越好,但是api23开始才能用
            int level = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                level = signalStrength.getLevel();
            }
            int gsmSignalStrength = signalStrength.getGsmSignalStrength();
//获取网络类型
            int netWorkType = getNetWorkType(MainActivity.this);
            switch (netWorkType) {
                case NETWORKTYPE_WIFI:
                    //mTextView.setText("当前网络为wifi,信号强度为：" + gsmSignalStrength);
                    break;
//                case NETWORKTYPE_2G:
//                    //mTextView.setText("当前网络为2G移动网络,信号强度为：" + gsmSignalStrength);
//                    break;
//                case NETWORKTYPE_4G:
//                    //mTextView.setText("当前网络为4G移动网络,信号强度为：" + gsmSignalStrength);
//                    break;
//                case NETWORKTYPE_NONE:
//                    //mTextView.setText("当前没有网络,信号强度为：" + gsmSignalStrength);
//                    break;
//                case -1:
//                    //mTextView.setText("当前网络错误,信号强度为：" + gsmSignalStrength);
//                    break;
                case NETWORKTYPE_2G:
                case NETWORKTYPE_4G:
                    /*
                    if(!SocketUtils.getInstance().isOnLine){
                        //无连接
                        img_network.setImageBitmap(bm_Network0);
                    }*/
                    break;
                case NETWORKTYPE_NONE:
                case -1:
                    //img_network.setImageBitmap(bm_Network0);
                    break;
            }

            if (level >= 3) {
                img_network.setImageBitmap(bm_Network2);
            } else if (level >= 1) {
                img_network.setImageBitmap(bm_Network1);
            } else {
                img_network.setImageBitmap(bm_Network0);
            }
            //Log.d(TAG, "Logo Network:"+level+" Net:"+SocketUtils.getInstance().isOnLine);
        }
    }

    public static int getNetWorkType(Context context) {
        int mNetWorkType = -1;
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String type = networkInfo.getTypeName();
            if (type.equalsIgnoreCase("WIFI")) {
                mNetWorkType = NETWORKTYPE_WIFI;
            } else if (type.equalsIgnoreCase("MOBILE")) {
                return isFastMobileNetwork(context) ? NETWORKTYPE_4G : NETWORKTYPE_2G;
            }
        } else {
            mNetWorkType = NETWORKTYPE_NONE;//没有网络
        }
        return mNetWorkType;
    }

    /**判断网络类型*/
    private static boolean isFastMobileNetwork(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
//这里只简单区分两种类型网络，认为4G网络为快速，但最终还需要参考信号值
            return true;
        }
        return false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        IsRun=false;
        ControlUtils.getInstance().AllHWStop();

        //停止热成像
        if(DisinfectData.IsDevHeatCheck)
        {
            if(HeatUtils.getInstance().isStart)
            {
                HeatUtils.getInstance().Close();
            }
        }

//        IsRun=false;
//
//        //关闭NFC读卡
//        NFCUtils.getInstance().onDestroy();
//
//        //关闭硬件控制
//        ControlUtils.getInstance().CloseSerialPort();
//
//        //关闭传感器数据
//        SensortUtils1.getInstance().CloseSerialPort();
//
//        //关闭循环线程
//        ThreadLoopUtils.getInstance().Close();
//
//        //关闭网络长连接
//        SocketUtils.getInstance().Close();
    }

    public void ComeGoFrame (int id){
        //Frgm卡顿 图片过大
        FragmentTransaction ft= GetComeTransaction();
        //如果已经是当前页面 不跳转
        Log.d("FrgmChangeList", "ComeGoFrame:"+GetNowFrgmFlag());
        if(NowFrgmId!=id)
        {
            NowFrgmId=id;
            goFrame(id, ft);

            //隐藏输入法
            AndroidUtils.HideInput(this);
        }
        else{
            Log.d("FrgmChangeList", "已经是当前页面 "+GetNowFrgmFlag()+" "+id);
            frgmCallBack.InitData();
        }
    }

    private void  BackGoFrame(int id){
        FragmentTransaction ft= GetBackTransaction();
        //如果已经是当前页面 不跳转
        Log.d("FrgmChangeList", "BackGoFrame:"+GetNowFrgmFlag());
        if(NowFrgmId != id ) {
            NowFrgmId=id;
            goFrame(id, ft);
        }else{
            Log.d("FrgmChangeList", "已经是当前页面 "+GetNowFrgmFlag()+" "+id);
            frgmCallBack.InitData();
        }
    }

    //其他界面跳转到主界面
    public void OtherFrgmComeMain()
    {
        if(!DisinfectData.IsDevCateCar)
        {
            ComeGoFrame(MainActivity.flag_main);
        }else
        {
            ComeGoFrame(MainActivity.flag_carChoice2);
            //ComeGoFrame(MainActivity.flag_main);
            //ComeGoFrame(MainActivity.flag_login);
        }
    }

    private void goFrame(int id, FragmentTransaction ft)
    {
        Fragment targetFragment=null;
        switch (id) {
            case flag_login:
                //停止硬件
                ControlUtils.getInstance().FunStopDisinfect();

                frgmCallBack=loginFrgm;
                targetFragment=loginFrgm;
                break;
            case flag_register:
                frgmCallBack=registerFrgm;
                targetFragment=registerFrgm;
                break;
            case flag_main:
                //停止硬件
                ControlUtils.getInstance().FunStopDisinfect();

                frgmCallBack=mainFrgm;
                targetFragment=mainFrgm;

//                //如果是热成像 停止监测
//                if(DisinfectData.IsDevHeatCheck)
//                {
//                    HeatUtils.getInstance().SetListen(false);
//                }
                break;
            case flag_parm:
                frgmCallBack=parmFrgm;
                targetFragment=parmFrgm;
                break;
            case flag_reagent:
                frgmCallBack=reagentFrgm;
                targetFragment=reagentFrgm;
                break;

            case flag_switch:
                frgmCallBack=swithFrgm;
                targetFragment=swithFrgm;
                break;

            case flag_leave:
                frgmCallBack=leaveFrgm;
                targetFragment=leaveFrgm;
                break;
            case flag_run:
                frgmCallBack=runFrgm;
                targetFragment=runFrgm;

//                //如果是热成像 开启监测
//                if(DisinfectData.IsDevHeatCheck)
//                {
//                    HeatUtils.getInstance().SetListen(true);
//                }
                break;

            case flag_end:
            case EndFrgm.flag_sub_idle:
                frgmCallBack=endFrgm;
                targetFragment=endFrgm;

//                //如果是热成像 停止监测
//                if(DisinfectData.IsDevHeatCheck)
//                {
//                    HeatUtils.getInstance().SetListen(false);
//                }
                break;
            case EndFrgm.flag_sub_end:
                endFrgm.ChangeEndMode();

                frgmCallBack=endFrgm;
                targetFragment=endFrgm;
                break;
            case flag_binddev:
                frgmCallBack=bindDevFrgm;
                targetFragment=bindDevFrgm;
                break;
            case flag_bindcard:
                frgmCallBack=bindCardFrgm;
                targetFragment=bindCardFrgm;
                break;
            case flag_changeOffPw:
                frgmCallBack=changeOffPwFrgm;
                targetFragment=changeOffPwFrgm;
                break;
            case flag_webgrid:
                frgmCallBack= webGridFrgm;
                targetFragment= webGridFrgm;
                break;
            case flag_carChoice2:
                frgmCallBack= carChoiceFrgm;
                targetFragment= carChoiceFrgm;
                break;
            case flag_toSettings:
                frgmCallBack= toSettingsFrgm;
                targetFragment= toSettingsFrgm;
                break;

            default:
                frgmCallBack=loginFrgm;
                targetFragment=loginFrgm;
                break;
        }

        if (!targetFragment.isAdded()) {
            //第一次使用switchFragment()时currentFragment为null，所以要判断一下
            if (currentFragment != null) {
                ft.hide(currentFragment);
            }
            ft.add(R.id.main_frame, targetFragment,targetFragment.getClass().getName());
        } else {
            ft
                    .hide(currentFragment)
                    .show(targetFragment);
        }
        currentFragment = targetFragment;
        ft.commit();

//        View currentFocus = this.getCurrentFocus();
//        if (currentFocus != null) {
//            currentFocus.clearFocus();
//        }

        //跳转完记录
        SetFrgmChange(id);
    }

    public FragmentTransaction GetComeTransaction(){
        FragmentManager fm =getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.setCustomAnimations((int)R.anim.slide_in_left,(int)R.anim.slide_out_right,
                (int)R.anim.slide_in_left,(int)R.anim.slide_out_right);
        return ft;
    }

    public FragmentTransaction GetBackTransaction(){
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.setCustomAnimations((int)R.anim.slide_in_right,(int)R.anim.slide_out_left,
                (int)R.anim.slide_in_right,(int)R.anim.slide_out_left);
        return ft;
    }

    public void SetFrgmChange(int flag){
        if(flag==flag_run ||
                flag==flag_end)
        {
            HideTopView();
        }else{
            ShowTopView();
        }

        if(flag==flag_login){
            ClearFrgmList();
        }

        int size=FrgmChangeList.size();
        if(size>0) {
            int lastFlag = FrgmChangeList.get(size - 1);
            Log.d("FrgmChangeList", lastFlag+"-"+flag);
            if(lastFlag!=flag) {
                FrgmChangeList.add(flag);
                Log.d("FrgmChangeList", "SetFrgmChange: "+FrgmChangeList);
            }
        }else{
            FrgmChangeList.add(flag);
            Log.d("FrgmChangeList", "SetFrgmChange: "+FrgmChangeList);
        }

        GetNowFrgmFlag();
    }

    public int GetNowFrgmFlag(){
        int size=FrgmChangeList.size();
        if(size>0) {
            return FrgmChangeList.get(size - 1);
        }else{
            return -1;
        }
    }

    public void ClearFrgmList()
    {
        FrgmChangeList.clear();
        GetNowFrgmFlag();
    }

    // ====================================== 手势 ======================================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int fingerCount =event.getPointerCount();
        if (event.getPointerCount() == fingerCount)
        {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    if (TouchPointX==0  ) {
                        TouchPointX = (int) event.getX(0);
                        //Log.v(TAG,"Move "+TouchPointX);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    int offset=200;
                    int nowX= (int) event.getX(fingerCount-1);
                    if (TouchPointX !=0 ) {
                        if(TouchPointX>nowX){
                            if(TouchPointX-nowX>offset){
                                //前进
                                Log.v(TAG,"Next");
                                frgmCallBack.onTouchNext();
                            }
                        }else if(nowX-TouchPointX>offset){
                            //后退
                            //Log.v(TAG,"Back");

                            FrgmGoBack();
                        }
                        //Log.v(TAG,"Up "+TouchPointX);
                    }
                    TouchPointX=0;
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

//    public void FrgmGoMain() {
//        ComeGoFrame(flag_main);
//        ClearFrgmList();
//    }

    //点击返回初始页
    public void AllBtnFrgmHome()
    {
        //停止硬件
        ControlUtils.getInstance().FunStopDisinfect();

        ClearFrgmList();

        boolean isBackHome=true;
        if(!DisinfectData.IsWebUser) {
            isBackHome=true;
        }else{
            isBackHome=false;
        }

        //底盘退回到登录页
        if(DisinfectData.IsDevCateCar)
        {
            isBackHome=false;
        }

        if(isBackHome)
        {
            //退回到主页面
            SetFrgmChange(flag_login);
            BackGoFrame(flag_main);
        }else{
            //退回到登陆页
            BackGoFrame(flag_login);
        }
    }

    //点击返回登录页
    public void BtnFrgmLogin()
    {
        //停止硬件
        ControlUtils.getInstance().FunStopDisinfect();

        ClearFrgmList();
        //Web用户退回到登陆页
        BackGoFrame(flag_login);
    }

    //消毒中点击返回初始页
    public void DisinfectBtnFrgmHome()
    {
        /*
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("立刻停止任务？")
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //服务器发送终端停止任务
                        DisinfectData.planItem.NowRunMode= PlanItem.RunMode_End;
                        SocketUtils.getInstance().SendStopDisinfect();

                        //底盘发送停止
                        if(DisinfectData.IsDevCateCar)
                        {
                            Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                            msg.what = MainActivity.MsgID_Car_OverPos;
                            MainActivity.Instance.MainHandler.sendMessage(msg);
                        }

                        AllBtnFrgmHome();
                    }
                })
                .setNegativeButton("取消",null)
                .create();
        alertDialog.show();
        */


        View.OnClickListener onConfimClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //服务器发送终端停止任务
                DisinfectData.planItem.NowRunMode= PlanItem.RunMode_End;
                SocketUtils.getInstance().SendStopDisinfect();

//                //底盘发送停止
//                if(DisinfectData.IsDevCateCar)
//                {
//                    Message msg = MainActivity.Instance.MainHandler.obtainMessage();
//                    msg.what = MainActivity.MsgID_Car_OverPos;
//                    MainActivity.Instance.MainHandler.sendMessage(msg);
//                }

                AllBtnFrgmHome();
            }
        };

        CustomDialog.Builder builder = new CustomDialog.Builder(MainActivity.Instance);
        builder.setTitle("立刻停止任务？");
        builder.setButtonConfirm( "确认", onConfimClickListener);
        builder.setButtonCancel( "取消", null);

        customDialog = builder.create();
        customDialog.show();
    }

    public void FrgmGoBack(){
        GetNowFrgmFlag();

        Log.d("FrgmChangeList", "Now:"+GetNowFrgmFlag()+" "+FrgmChangeList);
        int size=FrgmChangeList.size();
        if(size>=2 && GetNowFrgmFlag()!=flag_login) {
            int lastFlag = FrgmChangeList.get(size-2);
            //不退回到注册
            if(GetNowFrgmFlag()==flag_register){
                ClearFrgmList();
                BackGoFrame(flag_login);
                return;
            }
            //消毒开始不退回
            if(GetNowFrgmFlag()==flag_leave ||
                    GetNowFrgmFlag()==flag_run ||
                    GetNowFrgmFlag()==flag_end ||
                    GetNowFrgmFlag()==EndFrgm.flag_sub_idle ||
                    GetNowFrgmFlag()==EndFrgm.flag_sub_end)
            {
                return;
            }

            Log.d("FrgmChangeList", "Go: "+lastFlag);

            //等跳转完成后 再删除最后一条记录
            FrgmChangeList.remove(size-1);

            //主界面
            if(lastFlag < 200 ){
                BackGoFrame(lastFlag);
            }else{
                frgmCallBack.onTouchBack(lastFlag);
            }

            GetNowFrgmFlag();
        }else if(size==1){
            int lastFlag = FrgmChangeList.get(size-1);
            BackGoFrame(lastFlag);
        }
    }

    public void MyToast(String str){
        //初始化等待后再提示
        if(toastWaitCnt<=0)
        {
            Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
            //不能太频繁
            toastWaitCnt=2;
        }else{
            Log.d(TAG, "MyToast: "+str+" "+toastWaitCnt);
        }
    }

    public void MyToastThread(String str){
        Message msg=MainHandler.obtainMessage();
        msg.what=MsgID_UI_Toast;
        msg.obj=str;
        MainHandler.sendMessage(msg);
    }

    //检查电池
    public void CheckBattery()
    {
        int TempFlag=GetNowFrgmFlag();
        //本机电量
        int Power=BatteryUtils.getInstance().Power;
        //底盘电量
        int CarPower=CarUtils.getInstance().Power;

        String showText="";
        int color=Color.GREEN;

        int Num=20;
        //本机电量过低
        //Log.d("BatteryUtils", BatteryUtils.getInstance().OffLineCnt+" "+BatteryUtils.getInstance().isConnected());
        if(BatteryUtils.getInstance().OffLineCnt<30)//5
        {
            //获取信息时阻塞 放到主线程累加
            BatteryUtils.getInstance().OffLineCnt++;
        }

        if(BatteryUtils.getInstance().OffLineCnt<=30){
            showText=Power+"";

            if(Power<Num)
            {
                color=Color.RED;
            /*
            tv_Tips.setText(AndroidUtils.GetStr(R.string.lowpower));

            //返回登录页
            if(TempFlag!=flag_login)
            {
                //停止硬件
                ControlUtils.getInstance().FunStopDisinfect();
                if(runFrgm.timeCnt>0 ||
                    RunFrgm.timeCanRun) {
                    //服务器发送终端停止任务
                    DisinfectData.planItem.NowRunMode = PlanItem.RunMode_Spray;
                    SocketUtils.getInstance().SendStopDisinfect();
                }

                ClearFrgmList();
                BackGoFrame(flag_login);
            }
            */
            }else{
                color=Color.GREEN;
                color=Color.rgb(150,255,83);
            }
        }else{
            showText="x";
            color=Color.RED;
        }

        //充电
        if(!BatteryUtils.getInstance().isBattery)
        {
            img_battery.setImageBitmap(bm_Battery0);
            if(tv_battery.getVisibility()==View.GONE){
                tv_battery.setVisibility(View.VISIBLE);
            }
            if(tv_battery2.getVisibility()==View.VISIBLE){
                tv_battery2.setVisibility(View.GONE);
            }
        }else{
            batteryNo++;
            if(batteryNo>2){
                batteryNo=0;
            }
            switch (batteryNo){
                case 0:
                    img_battery.setImageBitmap(bm_Battery1);
                    break;
                case 1:
                    img_battery.setImageBitmap(bm_Battery2);
                    break;
                case 2:
                    img_battery.setImageBitmap(bm_Battery3);
                    break;
            }
            if(tv_battery.getVisibility()==View.VISIBLE){
                tv_battery.setVisibility(View.GONE);
            }
            if(tv_battery2.getVisibility()==View.GONE){
                tv_battery2.setVisibility(View.VISIBLE);
            }
        }
        if(tv_battery.getVisibility()==View.VISIBLE){
            tv_battery.setText(showText);
            tv_battery.setTextColor(color);
        }
        if(tv_battery2.getVisibility()==View.VISIBLE){
            tv_battery2.setText(showText);
            tv_battery2.setTextColor(color);
        }

/*
        //底盘电量过低
        if(CarPower<Num)
        {
            tv_Tips.setText(AndroidUtils.GetStr(R.string.carlowpower));

            //返回登录页
            if(TempFlag!=flag_login)
            {
                //停止硬件
                ControlUtils.getInstance().FunStopDisinfect();
                CarPlanUtils.getInstance().CarOver();
                if(runFrgm.timeCnt>0 ||
                        RunFrgm.timeCanRun) {
                    //服务器发送终端停止任务
                    DisinfectData.planItem.NowRunMode = PlanItem.RunMode_CarPlanStop;
                    SocketUtils.getInstance().SendStopDisinfect();
                }

                ClearFrgmList();
                BackGoFrame(flag_login);
            }
        }

        if(Power<Num ||
            CarPower>Num )
        {
            layout_Tips.setVisibility(View.VISIBLE);
            tv_Tips.setTextColor(Color.RED);
        }else{
            layout_Tips.setVisibility(View.GONE);
        }
        */
        layout_Tips.setVisibility(View.GONE);
    }

    //检查热成像
    public void CheckHeat()
    {
        //如果是热成像类型
        if(DisinfectData.IsDevHeatCheck)
        {
            if(!HeatUtils.getInstance().isStart)
            {
                //没启动检测
                img_heat.setImageBitmap(bm_Heat0);
            }
            else
            {
                //当前无人
                img_heat.setImageBitmap(bm_Heat1);
                /*
                //启动检测
                if (HeatUtils.getInstance().GetNowClearScene())
                {
                    //当前无人
                    img_heat.setImageBitmap(bm_Heat1);
                } else
                {
                    //当前有人
                    img_heat.setImageBitmap(bm_Heat2);
                }
                 */
            }
        }
    }

    // ====================================== NFC ======================================

    public void NFCOpen(){
        switch (AndroidUtils.NFCType){
            case 0:
                if(NFCUtils.IsHaveDeviceName) {
                    NFCUtils.getInstance().DeviceOpen(FrgmHandler);
                }
                break;
            case 1:
                if(NFCOpenCnt<=0)
                {
                    NFC2Utils.getInstance().DeviceOpen(FrgmHandler);
                }
                break;
        }
    }

    public void NFCClose(){
        switch (AndroidUtils.NFCType){
            case 0:
                if(NFCUtils.IsHaveDeviceName) {
                    NFCUtils.getInstance().DeviceClose();
                }
                break;
            case 1:
                if(NFC2Utils.IsHaveDevice) {
                    NFC2Utils.getInstance().DeviceClose();
                }
                break;
        }
    }

    // ====================================== Thread 线程循环 ======================================

    public void ThreadHalfSecFun() {
        try{
            switch (GetNowFrgmFlag())
            {
                case flag_leave:
                    leaveFrgm.ThreadHalfSecFun();
                    break;
                case flag_run:
                    runFrgm.ThreadHalfSecFun();
                    break;

            }
        }catch (Exception e){}
    }

    public void ThreadOneSecFun()
    {
        //更新界面
        try
        {
            Message msg;
            //更新UI时间
            msg=MainHandler.obtainMessage();
            msg.what=MsgID_UI_Time;
            msg.obj= AndroidUtils.GetSystemTime();
            MainHandler.sendMessage(msg);

            switch (GetNowFrgmFlag()){
                case flag_login:
                    loginFrgm.ThreadOneSecFun();
                    break;
                case flag_main:
                    mainFrgm.ThreadOneSecFun();
                    break;
                case flag_parm:
                    parmFrgm.ThreadOneSecFun();
                    break;
                case flag_leave:
                    leaveFrgm.ThreadOneSecFun();
                    break;
                case flag_run:
                    runFrgm.ThreadOneSecFun();
                    break;
                case flag_reagent:
                    reagentFrgm.ThreadOneSecFun();
                    break;
                case flag_end:
                case EndFrgm.flag_sub_end:
                    endFrgm.ThreadOneSecFun();
                    break;
                case flag_webgrid:
                    webGridFrgm.ThreadOneSecFun();
                    break;
            }
        }catch (Exception e){}

        //检查预约时间
        try
        {
            if(DisinfectData.planTimeStr!="")
            {
                String nowTime=AndroidUtils.GetFormatTime("dd:HH:mm:ss");
                String planTime=DisinfectData.planTimeStr;

                int nowDay=Integer.parseInt( nowTime.split(":")[0]);
                int planDay=Integer.parseInt( planTime.split(":")[0]);

                int nowSec=Integer.parseInt( nowTime.split(":")[3]);
                int planSec=Integer.parseInt( planTime.split(":")[3]);

                nowTime=nowTime.substring(0,8);
                planTime=planTime.substring(0,8);

                String temp=String.format("now:%s(%s) plan:%s(%s)",nowTime,nowSec,planTime,planSec);
//                if(nowTime.equals(planTime)) {
//                    Log.v("LXPTIME", "检查|" + temp);
//                }

                if(nowTime.equals(planTime) && nowSec+1 >= planSec)
                {
                    Log.v("LXPTIME","时间到 "+temp);

                    DisinfectData.planTimeStr="";
                    //保存缓存数据
                    Map<String, String> params=new HashMap<>();
                    params.put("planTimeStr",DisinfectData.planTimeStr);
                    AndroidUtils.QuickDataSet(params);

                    //判断当前是否空闲
                    if(NowFrgmId==flag_leave ||
                       NowFrgmId==flag_run)
                    {
                        Log.v("LXPTIME","已经运行");
                    }else{
                        //确定上个用户退出登录
                        WebUserLogout();

                        //Socket应答
                        DisinfectData.planItem.planTime=0;
                        SocketUtils.getInstance().ReplyWebStart();

                        //读取数据
                        DisinfectData.planItem=DisinfectData.GetPlanItem();
                        ParmFrgm.time_spray=DisinfectData.planItem.sprayTime;
                        ParmFrgm.time_streng=DisinfectData.planItem.strengTime;
                        ParmFrgm.time_leave=DisinfectData.planItem.leaveTime;
                        ParmFrgm.time_idle=DisinfectData.planItem.idleTime;

                        //停止硬件
                        ControlUtils.getInstance().FunStopDisinfect();

                        leaveFrgm.WebControlStart();
                        ComeGoFrame(MainActivity.flag_leave);
                    }
                }
                if(nowDay>planDay)
                {
                    Log.v("LXPTIME","超时 "+temp);

                    DisinfectData.planTimeStr="";
                    //保存缓存数据
                    Map<String, String> params=new HashMap<>();
                    params.put("planTimeStr",DisinfectData.planTimeStr);
                    AndroidUtils.QuickDataSet(params);
                }
            }
        }catch (Exception e){}

        CheckWebOffLine();
    }

    public void ThreadFiveSecFun()
    {
            switch (GetNowFrgmFlag()) {
//                case flag_main:
//                    mainFrgm.ThreadFiveSecFun();
//                    break;
                case flag_run:
                    runFrgm.ThreadFiveSecFun();
                    break;
            }
    }

    //一分钟保存一次
    public void ThreadOneMinFun()
    {
        Map<String, String> sets=new HashMap<>();
        sets.put("ReagentWeightA",String.valueOf(DisinfectData.ReagentWeights[0]));
        sets.put("ReagentWeightB",String.valueOf(DisinfectData.ReagentWeights[1]));
        AndroidUtils.QuickDataSet(sets);
    }

    private void GetLanguage(){
        // 获得res资源对象
        Resources resources = this.getResources();
        // 获得屏幕参数：主要是分辨率，像素等。
        DisplayMetrics metrics = resources.getDisplayMetrics();
        // 获得配置对象
        Configuration config = resources.getConfiguration();

        AndroidUtils.langue=config.locale;
    }

    //更换语言
    public void ChangeLanguage(Locale target){
        // 获得res资源对象
        Resources resources = this.getResources();
        // 获得屏幕参数：主要是分辨率，像素等。
        DisplayMetrics metrics = resources.getDisplayMetrics();
        // 获得配置对象
        Configuration config = resources.getConfiguration();

        AndroidUtils.langue=config.locale;

        if( config.locale != target) {
            config.locale = target;
            AndroidUtils.langue=config.locale;
            resources.updateConfiguration(config, metrics);

            //刷新注册界面
            FragmentTransaction ft= GetComeTransaction();
            ReplaceFrgm(ft, loginFrgm);
            loginFrgm.InitData();
        }
    }


    // ====================================== 回退按键 ======================================

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //moveTaskToBack(false);
            FrgmGoBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    /***
     * 2021/02/02
     * 解决软键盘弹出时任务栏不隐藏和单击输入框以外区域输入法不隐藏的bug
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideInput(v, ev)) {
                //fullScreen();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
            return super.dispatchTouchEvent(ev);
        }
        // 必不可少，否则所有的组件都不会有TouchEvent了
        if (getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }

    public boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] leftTop = {0, 0};
            //获取输入框当前的location位置
            v.getLocationInWindow(leftTop);
            int left = leftTop[0];
            int top = leftTop[1];
            int bottom = top + v.getHeight();
            int right = left + v.getWidth();
            if (event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom) {
                // 点击的是输入框区域，保留点击EditText的事件
                return false;
            } else {
                return true;
            }
        }
        return false;
    }


    // ====================================== 网络相关 ======================================

    //网络开启消毒任务
    private void WebStartDisinfect()
    {
        if(IsApkUpDate){
            return;
        }

        CarPlanUtils.getInstance().AddInfoList("网络开启消毒任务");
        //确定上个用户退出登录
        WebUserLogout();

        ParmFrgm.time_spray=DisinfectData.planItem.sprayTime;
        ParmFrgm.time_streng=DisinfectData.planItem.strengTime;
        ParmFrgm.time_leave=DisinfectData.planItem.leaveTime;
        ParmFrgm.time_idle=DisinfectData.planItem.idleTime;

        //预约时间
        int plan=DisinfectData.planItem.planTime;
        //设置预约时间
        if(plan>0)
        {
            AddPlanTimeMission(plan);
        }
        else{
            //立刻执行
            switch (DisinfectData.planItem.NowRunMode) {
                case PlanItem.RunMode_Init:
                case PlanItem.RunMode_Leave:
                    //停止硬件
                    ControlUtils.getInstance().FunStopDisinfect();

                    leaveFrgm.WebControlStart();
                    ComeGoFrame(MainActivity.flag_leave);
                    break;
                case PlanItem.RunMode_Spray:
                    runFrgm.WebControlStart();
                    GoRunFrgm(true);
                    break;
                case PlanItem.RunMode_Idle:
                    //消毒场景
                    ControlUtils.getInstance().FunStopDisinfect();

                    ComeGoFrame(MainActivity.flag_end);
                    endFrgm.ChangeIdleMode();
                    break;
                case PlanItem.RunMode_End:
                    //停止硬件
                    ControlUtils.getInstance().FunStopDisinfect();

                    endFrgm.ChangeEndMode();
                    ComeGoFrame(MainActivity.flag_end);
                    //机器人模式跳回登录页
                    if (DisinfectData.IsCarControl) {
                        ComeGoFrame(MainActivity.flag_login);
                    }
                    break;
            }
        }

        //网络化房间标识
        //如果设备是网络化房间 并且不是机器人
        if(DisinfectData.planItem.sceneId==0 &&
           !DisinfectData.IsDevCateCar)
        {
            DisinfectData.DevCateNo=0;
        }
    }

    //网络暂停消毒任务
    private void WebStopDisinfect()
    {
        if(IsApkUpDate){
            return;
        }

        String info=String.format("WebStopDisinfect: 停止页面：%d",
                DisinfectData.planItem.NowRunMode);
        Log.d(TAG, info);

        CarPlanUtils.getInstance().AddInfoList("网络暂停消毒任务");
        //停止硬件
        ControlUtils.getInstance().FunStopDisinfect();

        if(DisinfectData.IsDevCateCar){
            if(DisinfectData.planItem.NowRunMode!=PlanItem.RunMode_CarStop)
            {
                DisinfectData.planItem.NowRunMode=PlanItem.RunMode_Spray;
            }
        }

        switch (DisinfectData.planItem.NowRunMode){
            case PlanItem.RunMode_Init:
            case PlanItem.RunMode_Leave:
                ParmFrgm.time_leave=leaveFrgm.timeCnt;
                if(ParmFrgm.time_leave==0){
                    ParmFrgm.time_leave=1;
                }

                leaveFrgm.WebControlStop();
                ComeGoFrame(MainActivity.flag_leave);
                break;
            case PlanItem.RunMode_Spray:
                ParmFrgm.time_spray=runFrgm.timeCnt;
                ParmFrgm.time_streng=0;
                if(ParmFrgm.time_spray==0){
                    ParmFrgm.time_spray=1;
                }

                runFrgm.WebControlStop();
                GoRunFrgm(false);
                break;

            case PlanItem.RunMode_Idle:
                //消毒场景
                ControlUtils.getInstance().FunStopDisinfect();

                ComeGoFrame(MainActivity.flag_end);
                break;
            case PlanItem.RunMode_End:
                //停止硬件
                ControlUtils.getInstance().FunStopDisinfect();

                endFrgm.WebControlEnd();
                ComeGoFrame(MainActivity.flag_end);
                break;
            case PlanItem.RunMode_CarStop:
                CarOverPos();
                break;

//            case PlanItem.RunMode_End:
//                ComeGoFrame(MainActivity.flag_login);
//                break;
//            case PlanItem.RunMode_Login:
//                ComeGoFrame(MainActivity.flag_login);
//                break;
        }
    }

    //网络重连
    private void WebReConnect(){
        MyToastThread("网络重连成功");
        if(frgmCallBack != null){
            //只能在以下界面使用
            if(GetNowFrgmFlag()==flag_login ||
                    GetNowFrgmFlag()==flag_register ||
                    GetNowFrgmFlag()==flag_main)
            {
                frgmCallBack.InitData();
            }
        }
    }

    //检查离线重连 每秒运行一次
    public void CheckWebOffLine()
    {
        //0512 离线
        if(HttpUtils.IsOnLine) {
            if (ControlUtils.getInstance().isConnected()) {
                if (!SocketUtils.getInstance().isConnected()) {
                    Log.d(TAG, "run: 重新初始化 ------------------------------");

                    SocketInit();

                    Message msg = MainHandler.obtainMessage();
                    msg.what = MsgID_UI_WebOffLineInfo;
                    MainHandler.sendMessage(msg);
                }
            }
        }

        //如果是热成像类型
        if(DisinfectData.IsDevHeatCheck)
        {
            //一直没有开启的话
            Log.d("HeatUtils", HeatUtils.getInstance().isStart?"连接":"断开");
            if(!HeatUtils.getInstance().isStart){
                checkHeatCnt++;
                if(checkHeatCnt>=30)//20
                {
                    checkHeatCnt=0;
                    HeatUtils.getInstance().Open();
                }
            }
        }
    }

    //用户退出
    public void WebUserLogout(){
        if(!HttpUtils.NetWork_ToKen.isEmpty()){
            ThreadLoopUtils.getInstance().
                    mThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try{
                        String url= HttpUtils.HostURL+"/logout";
                        String result= HttpUtils.PostData(url);

                        JSONObject jsonObject=new JSONObject(result);
                        Integer code= jsonObject.getInt("code");
                        String data= jsonObject.getString("msg");
                        if(code==0){
                            //成功
                            Log.v(TAG,data);
                        }else{
                            //失败
                            Log.v(TAG,data);
                        }

                        //清除ToKen
                        HttpUtils.NetWork_ToKen="";
                        //初始化所有消毒数据
                        DisinfectData.Init();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /*
    public void WebSetNetWork(boolean isOnLine)
    {
        Message msg = MainHandler.obtainMessage();
        msg.what = MsgID_UI_NetWork;
        msg.obj=isOnLine;
        MainHandler.sendMessage(msg);
    }*/

    //服务器查询设备工作状态
    private int testShow=0;
    public byte[] WebGetState()
    {
        int state=4;//默认值空闲
        int leaveTime=DisinfectData.planItem.leaveTime;
        int sprayTime=DisinfectData.planItem.sprayTime+DisinfectData.planItem.strengTime;
        int strengTime=0;
        int idleTime=DisinfectData.planItem.idleTime;
        int runType=0;  //0：暂停，1：启动 2：热成像有人（暂停）

        //赋值当前场景 并 调整时间
        switch (GetNowFrgmFlag()){
            case flag_leave:
                state=1;

                leaveTime=leaveFrgm.timeCnt;
                if(leaveFrgm.timeCanRun){
                    runType=1;
                }
                break;
            case flag_run:
                state=2;

                leaveTime=0;
                sprayTime=runFrgm.timeCnt;
                if(runFrgm.timeCanRun){
                    runType=1;
                }
                break;
            case flag_end:
            case EndFrgm.flag_sub_idle:
                state=3;

                leaveTime=0;
                sprayTime=0;
                idleTime=EndFrgm.time_idle;
                runType=1;
                break;
        }
        //限制底盘回应2
        if(DisinfectData.IsCarControl){
            state=2;
            //0113
            if(runFrgm.timeCanRun){
                runType=1;
            }
            if(GetNowFrgmFlag()!=flag_run){
                runType=0;
            }

            //1206 移动模式工作
            if(!DisinfectData.IsCarPointMode &&
               CarPlanUtils.getInstance().WaitTimeCnt>0)
            {
                sprayTime=-100;
            }
            testShow=sprayTime;

            //Log.d(TAG, "CarWebGetState:底盘回应 "+DisinfectData.IsCarControl);
            String test="状态:"+runType+" 时间:"+DisinfectData.planItem.sprayTime;
            //TestCarBug(test);
        }
        //热成像模式
        if(DisinfectData.IsDevHeatCheck){
            //如果检测到有人
            if(runFrgm.IsHeatCheck){
                runType=2;
            }
        }

        byte[] data=new byte[24];
        int cnt=0;
        byte[] temp=AndroidUtils.IntToBytes4(state);
        for (int i = 0; i < temp.length; i++) {
            data[cnt++]=temp[i];
        }
        temp=AndroidUtils.IntToBytes4(leaveTime);
        for (int i = 0; i < temp.length; i++) {
            data[cnt++]=temp[i];
        }
        temp=AndroidUtils.IntToBytes4(sprayTime);
        for (int i = 0; i < temp.length; i++) {
            data[cnt++]=temp[i];
        }
        temp=AndroidUtils.IntToBytes4(strengTime);
        for (int i = 0; i < temp.length; i++) {
            data[cnt++]=temp[i];
        }
        temp=AndroidUtils.IntToBytes4(idleTime);
        for (int i = 0; i < temp.length; i++) {
            data[cnt++]=temp[i];
        }
        temp=AndroidUtils.IntToBytes4(runType);
        for (int i = 0; i < temp.length; i++) {
            data[cnt++]=temp[i];
        }
        Log.d(TAG, "回应设备工作状态: "+state);
        return data;
    }

    //获取设备类型
    public void NetWorkGetDevCate(final String mac){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/device/cate?macAdd="+mac;
                    Log.d(TAG, "获取设备类型 url:"+url);
                    String result= HttpUtils.GetData(url);

                    Log.d(TAG, "获取设备类型 data:"+result);
                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String msgStr= jsonObject.getString("msg");
                    Integer data= jsonObject.getInt("data");
                    DisinfectData.DevCateNo=data;
                    if(code==0){
                        //成功
                        /*
                        Panda == 1  //熊猫
                        Pretty == 2
                        Walker == 3  //机器人
                        Little == 4  //便携式
                        Giraffe == 5 //长颈鹿 照灯
                        Guard == 6   //喷淋
                        */

                        if(data!=3){
                            DisinfectData.IsDevCateCar=false;
                        }else{
                            DisinfectData.IsDevCateCar=true;
                        }

                    }else{
                        //失败

                    }
                    Log.v(TAG,"获取设备类型 "+msgStr+" "+data);
                    //MyToastThread("设备类型编号："+data);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    //网络更新
    public void WebApkUpDate(final int apkid)
    {
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/apk/id/"+apkid;
                    String result= HttpUtils.GetData(url);

                    Log.d(TAG, "网络更新 data:"+result);
                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    JSONObject joData=jsonObject.getJSONObject("data");
                    url= joData.getString("uri");
                    if(code==0){
                        Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                        msg.what = MainActivity.MsgID_Web_ApkUpDate;
                        msg.obj=url;
                        MainActivity.Instance.MainHandler.sendMessage(msg);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    // ====================================== 底盘相关 ======================================

    //底盘开启消毒
    private void CarStartDisinfect(int time)
    {
        ParmFrgm.time_spray =time;
        ParmFrgm.time_streng=0;

        GoRunFrgm(false);
        runFrgm.CarStartDisinfect(time);
    }

    //底盘暂停消毒
    private void CarStopDisinfect(){
        ParmFrgm.time_spray =0;
        ParmFrgm.time_streng=0;

        GoRunFrgm(false);
        runFrgm.CarStopDisinfect();
    }

    //底盘结束消毒任务
    private void CarOverPos(){
        CarPlanUtils.getInstance().CarOver();

        //停止硬件
        ControlUtils.getInstance().FunStopDisinfect();

        ParmFrgm.time_spray =0;
        ParmFrgm.time_streng=0;

        DisinfectData.IsCarControl=false;
        Log.d(TAG, "底盘结束CarOverPos: "+DisinfectData.IsCarControl);

        //机器人模式跳回登录页
        ComeGoFrame(MainActivity.flag_login);
    }


    //0622
    // ====================================== 预约时间相关 ======================================

    public void AddPlanTimeMission(int plan)
    {
        //Log.v("LXPTIME","设置预约时间0:"+plan);
        String day=AndroidUtils.GetFormatTime("dd");
        int hour =(int)(plan / 3600f);
        plan -= hour * 3600;
        int min = (int)(plan / 60f);
        plan -= min * 60;
        int sec = plan;
        DisinfectData.planTimeStr=day+":"+String.format("%02d",hour)+":"+String.format("%02d",min)+":"+String.format("%02d",sec);
        Log.v("LXPTIME","设置预约时间1:"+DisinfectData.planTimeStr);

        //保存预约消毒数据
        DisinfectData.SetPlanItem(DisinfectData.planItem);

        //保存预约时间数据
        Map<String, String> params=new HashMap<>();
        params.put("planTimeStr",DisinfectData.planTimeStr);
        AndroidUtils.QuickDataSet(params);

        String info=String.format("WebStartDisinfect: 模式：%d,撤离时间：%d,运行时间：%d,消毒时间：%d",
                DisinfectData.planItem.NowRunMode,ParmFrgm.time_leave,(ParmFrgm.time_spray+ParmFrgm.time_streng),ParmFrgm.time_idle);
        Log.d(TAG, info);

        //界面不需要变动
            /*
            //停止硬件
            ControlUtils.getInstance().FunStopDisinfect();
            //跳转等待
            ComeGoFrame(MainActivity.flag_switch);
            swithFrgm.ShowPlanDialog(String.format("%02d",hour)+":"+String.format("%02d",min)+":"+String.format("%02d",sec));
            */
    }


    //关机操作
    public void AppClose(final int dely)
    {
        new Thread(){
            @Override
            public void run() {
                try{
                    Thread.sleep(dely);

                    System.exit(0);
                }catch (Exception e){}
            }
        }.start();
    }

    //重启操作
    public void AppReStart(final int dely)
    {
        Log.d("UpDateAppUtils", "AppReStart");
        Handler handler=new Handler();
        handler.postDelayed(new Runnable(){
            @Override
            public void run() {
                final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Log.d("UpDateAppUtils", "Start");
                startActivity(intent);
            }
        },dely);
    }

}
