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
    public final static int flag_login = 100;            // ????????????
    public final static int flag_register = 101;         // ????????????
    public final static int flag_main = 102;            // ??????????????????
    public final static int flag_parm = 103;            // ??????????????????
    public final static int flag_reagent = 104;         // ??????????????????

    public final static int flag_switch = 105;             //  ????????????
    public final static int flag_leave = 106;              //  ????????????
    public final static int flag_run = 107;               //  ????????????
    public final static int flag_end = 108;               //  ????????????
    public final static int flag_binddev = 109;            // ????????????
    public final static int flag_bindcard = 110;           // ???????????????
    public final static int flag_webgrid = 111;            // ???????????????
    //public final static int flag_carChoice =  112;            // ????????????
    public final static int flag_carChoice2 = 112;            // ???????????????
    public final static int flag_toSettings = 113;           // ???????????????
    public final static int flag_changeOffPw = 114;        // ??????????????????

    //MsgID
    public final static int MsgID_UI_Toast = 100;         // UI ????????????
    public final static int MsgID_UI_Time = 101;          // UI ????????????
    public final static int MsgID_UI_WebOffLineInfo = 103;     // UI ????????????
    public final static int MsgID_UI_TestInfo = 104;                // ??????

    public final static int MsgID_HW_NFCID = 200;         // NFC???Id??????
    public final static int MsgID_HW_Control = 201;       // ???????????????
    public final static int MsgID_HW_SensortInfo1 = 202;       // ???????????????1??????
    public final static int MsgID_HW_SensortInfo2 = 204;       // ???????????????2??????

    public final static int MsgID_Web_Login = 300;        // ????????????
    public final static int MsgID_Web_Register = 301;     // ????????????
    public final static int MsgID_Web_UpdateNFC = 302;     // ??????NFC
    public final static int MsgID_Web_GetHos = 303;        // ????????????
    public final static int MsgID_Web_GetDep = 304;        // ????????????
    public final static int MsgID_Web_GetNowScenList = 305;  // ???????????????????????????
    public final static int MsgID_Web_ChangeScenList = 306;  // ??????????????????
    public final static int MsgID_Web_GetRatioList = 308;  // ??????????????????
    public final static int MsgID_Web_GetTypeList = 309;  // ??????????????????
    public final static int MsgID_Web_GetAllScenList = 310;  // ????????????????????????
    public final static int MsgID_Web_GetAllScenImg = 311;  // ??????????????????

    public final static int MsgID_Web_CheckNFC = 312;       // ??????????????????????????????
    public final static int MsgID_Web_NFCUsed = 313;        // ?????????????????????
    public final static int MsgID_Web_PostNFC = 314;         // ??????NFC????????????
    public final static int MsgID_Web_ReConnect = 315;     // ????????????
    public final static int MsgID_Web_StartDisinfect = 316;     // ????????????????????????
    public final static int MsgID_Web_StopDisinfect = 317;     // ????????????????????????
    public final static int MsgID_Web_GetUserType = 318;     // ??????????????????

    public final static int MsgID_Web_CheckBindDev = 320;        // ?????????????????????
    public final static int MsgID_Web_GetDevCate = 321;          // ??????????????????
    public final static int MsgID_Web_GetDevName = 322;          // ??????????????????
    public final static int MsgID_Web_BindDev = 323;             // ????????????
    public final static int MsgID_Web_GridGetNo = 324;           // ???????????????????????????
    public final static int MsgID_Web_GridGetFloor = 325;        // ???????????????????????????
    public final static int MsgID_Web_GridGetData = 326;        // ???????????????????????????

    public final static int MsgID_Web_GetCardName = 330;          // ??????????????????

    public final static int MsgID_Car_StartDisinfect = 400;     // ????????????????????????
    public final static int MsgID_Car_StopDisinfect = 401;      //  ????????????????????????
    public final static int MsgID_Car_OverPos = 403;             // ????????????????????????
    public final static int MsgID_Car_GetMap = 405;              // ??????????????????

    public final static int MsgID_Web_HeatLinkSuc1 = 500;            // ??????????????????????????????
    public final static int MsgID_Web_HeatLinkSuc2 = 501;            // ????????????????????????
    public final static int MsgID_Web_HeatErr = 503;                 // ???????????????

    public final static int MsgID_Web_ApkUpDate = 505;                 // apk

    public final static int MsgID_Web_WSConnect = 600;              // WebSocket????????????
    public final static int MsgID_Web_WSDiconnect = 601;            // WebSocket????????????
    public final static int MsgID_Web_WSGetMsg = 602;               // WebSocket????????????


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

    //?????????????????????
    public int checkHeatCnt = 0;
    //????????????
    public static int toastWaitCnt = 5;
    //????????????
    public static int soundWaitCnt = 5;
    //NFC????????????
    private int NFCOpenCnt = 0;

    //?????????
    FrameLayout layout_Tips;
    TextView tv_Tips;

    //Frgm
    //????????????????????????????????????
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
        //????????????
        EDensityUtils.setDensity(getApplication(), this);

        this.MyBundle = savedInstanceState;

        // ????????????
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.img_home);
        getWindow().getDecorView().setBackground(new BitmapDrawable(bm));
        // ??????
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // ??????
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // ?????????
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        setContentView(R.layout.activity_main);

        AndroidUtils.getUniqueId(this);

        Instance = this;
        IsRun = true;


/*

        //??????
        Intent intent=new Intent(MainActivity.this,CarTestActivity.class);
        startActivity(intent);
*/
        /*
         */

        //?????????UI
        InitView();

        //???????????????
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
                MainActivity.Instance.MyToast("????????????!");
            } else {
                MainActivity.Instance.MyToast("????????????");
            }
        }
    }

    public void InitView() {
        //??????????????????
        GetLanguage();

        //???????????????
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

        //?????????
        layout_Tips = findViewById(R.id.main_layout_tips);
        tv_Tips = findViewById(R.id.main_tv_tips);

        //Handler
        MainHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                try {
                    Object[] data;
                    switch (msg.what) {
                        // ------------ ??????UI ------------
                        case MsgID_UI_Time:
                            if (!DisinfectData.IsDevHeatCheck) {
                                //??????
                                String str = msg.obj.toString();
                                //str=testShow+" "+DisinfectData.IsCarPointMode;
                                tv_time.setText(str);
                                //tv_time.setText(BatteryUtils.getInstance().str1+"\r\n"+BatteryUtils.getInstance().str2);
                            } else {
//                                tv_time.setText(CarPlanUtils.getInstance().WaitTimeCnt + "");
                                int Cnt = HeatUtils.getInstance().ClearSceneCnt;
                                tv_time.setText((Cnt < 20 ? "??????" : "??????") + " " + Cnt + " " + checkHeatCnt);
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
                                            //????????????
                                            NFC2Utils.getInstance().Init();
                                            NFCOpen();
                                            break;
                                    }
                                }
                            }

                            if (HttpUtils.IsOnLine) {
                                //????????????
                                CheckWifiType();
                                CheckNetWorkInit();
                            }

                            //????????????
                            CheckBattery();
                            //?????????
                            CheckHeat();

                            //????????? ?????????????????????
                            if (GetNowFrgmFlag() == flag_main) {
                                DisinfectData.SetReagentSuffice(true, 0);
                            }

                            //????????????
                            if (toastWaitCnt > 0) {
                                toastWaitCnt--;
                            }
                            //????????????
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
                            //MyToast("???????????????");
                            break;
                        case MsgID_UI_TestInfo:
                            //if(DisinfectData.IsDevHeatCheck)
                        {
                            //????????????
                            //tv_test.setText(msg.obj.toString());
                        }
                        break;

                        // ------------ ???????????????????????? ???????????? ------------
                        case MsgID_Web_GetAllScenImg:
                            mainFrgm.FrgmMessage(msg);
                            break;

                        // ------------ ???????????? ------------
                        case MsgID_Web_ReConnect:
                            //????????????
                            WebReConnect();
                            break;
                        case MsgID_Web_StartDisinfect:
                            DisinfectData.IsWebUser = true;
                            //????????????
                            WebStartDisinfect();
                            break;
                        case MsgID_Web_StopDisinfect:
                            DisinfectData.IsWebUser = true;
                            //????????????
                            WebStopDisinfect();
                            break;

                        // ------------ ???????????? ------------

                        case MsgID_Car_StartDisinfect:
                            //CarPlanUtils.getInstance().AddInfoList("Car_StartDisinfect");

                            int time = (int) msg.obj;
                            //????????????
                            CarStartDisinfect(time);
                            break;
                        case MsgID_Car_StopDisinfect:
                            //CarPlanUtils.getInstance().AddInfoList("Car_StopDisinfect");

                            //????????????
                            CarStopDisinfect();
                            break;
                        case MsgID_Car_OverPos:
                            //????????????
                            CarOverPos();
                            break;

                        // ------------ ??????????????? ------------
                        case MsgID_Web_HeatLinkSuc1:
                            Log.d("HeatUtils", "MsgID_Web_HeatLinkSuc1");
                            // ??????????????????????????????
                            HeatUtils.getInstance().LinkSuc1SendClient();
                            break;
                        case MsgID_Web_HeatLinkSuc2:
                            Log.d("HeatUtils", "MsgID_Web_HeatLinkSuc1");
                            // ????????????????????????
                            HeatUtils.getInstance().LinkSuc2ChangeServer();
                            break;
                        case MsgID_Web_HeatErr:
                            Log.d("HeatUtils", "MsgID_Web_HeatErr");
                            // ???????????????
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
        //?????????NFC??????
        //NFCOpenCnt=3;
        switch (AndroidUtils.NFCType) {
            case 0:
                NFCUtils.getInstance().Init(MainActivity.this);
                break;
            case 1:
                //????????????
                NFC2Utils.getInstance().Init();
                break;
        }

        //??????????????????
        ThreadLoopUtils.getInstance().Init();

        //??????????????????
        ControlUtils.getInstance().Init("ttysWK2", 115200, MainHandler);

        //????????????ttyS4 ?????????ttyS1
        //?????????????????? 1-7??? 8?????????
        //??????????????????
        //????????????
        SensortUtils1.getInstance().Init("ttyS4", 9600, MainHandler);
        //??????????????????
        //????????????
        SensortUtils2.getInstance().Init("ttyS1", 9600, MainHandler);

        //????????????????????????
        SocketInit();

        //?????????????????????
        CarUtils.getInstance().Init("ttysWK1", 115200, MainHandler);
        CarPlanUtils.getInstance().CarStop();

        //?????????????????????
        BatteryUtils.getInstance().Init("ttysWK3", 9600, MainHandler);

        //????????????????????????
        if (DisinfectData.IsDevHeatCheck) {
            HeatUtils.getInstance().Open();
            img_heat.setVisibility(View.VISIBLE);
        }

        //?????????????????????
        AndroidUtils.SoundPoolInit();
    }

    public void SocketInit() {
        Log.d(TAG, "SocketInit");
        SocketUtils.getInstance().Open(HttpUtils.SocketURL, 8001, MainHandler, 0);
    }

    public void InitFrgm() {
        //Frgm?????? ????????????
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
            //???????????????switchFragment()???currentFragment???null????????????????????????
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
        //?????????????????? sdk2.3
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
        //????????????
        DisinfectData.planTimeStr = AndroidUtils.QuickDataGet("planTimeStr");
        //Log.d("LXPTIME","QuickDataGet: "+DisinfectData.planTimeStr);

        //????????????
        ControlUtils.getInstance().AllHWStop();

        Log.d(TAG, "InitData MAC: " + AndroidUtils.GetMacAddress());


        //??????????????????
        NetWorkGetDevCate(AndroidUtils.GetMacAddress());

        //??????????????????
        CheckNetWorkInit();

        //????????????????????????
        DisinfectData.ReagentWeights[0] = AndroidUtils.QuickDataGetInt("ReagentWeightA");
        DisinfectData.ReagentWeights[1] = AndroidUtils.QuickDataGetInt("ReagentWeightB");
    }

    //????????????UI
    public void ShowTopView() {
        layout_top.setVisibility(View.VISIBLE);
    }

    //????????????UI
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

    //WIFI??????
    private void CheckWifiType() {
        int wifi = -100;
        if (isWifiConnect()) {
            //Log.d(TAG, "Logo Wifi:True "+wifi);
            WifiManager mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
            wifi = mWifiInfo.getRssi();//??????wifi????????????
//            if (wifi > -50 && wifi < 0) {//??????
//                Log.e(TAG, "??????");
//            } else if (wifi > -70 && wifi < -50) {//??????
//                Log.e(TAG, "??????");
//            } else if (wifi > -80 && wifi < -70) {//??????
//                Log.e(TAG, "??????");
//            } else if (wifi > -100 && wifi < -80) {//??????
//                Log.e(TAG, "??????");
//            }

            if (wifi > -70) {
                //Log.e(TAG, "??????");
                img_wifi.setImageBitmap(bm_Wifi2);

            } else {
                //Log.e(TAG, "??????");
                img_wifi.setImageBitmap(bm_Wifi1);
            }
        } else {
            //Log.d(TAG, "Logo Wifi:Falise "+wifi);
            //Log.e(TAG, "???wifi??????");
            img_wifi.setImageBitmap(bm_Wifi0);
        }

        /*
        if(!SocketUtils.getInstance().isOnLine){
            //?????????
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
     * ????????????????????????
     *
     */
    private void CheckNetWorkInit() {
        //??????telephonyManager
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//????????????
        mListener = new PhoneStatListener();
//??????????????????
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
        //??????????????????????????????????????????
        mTelephonyManager.listen(mListener, PhoneStatListener.LISTEN_NONE);
        if (AndroidUtils.NFCType == 1) {
            NFC2Utils.getInstance().MyPause();
        }
    }

    private class PhoneStatListener extends PhoneStateListener {
        //??????????????????
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            //img_network.setImageBitmap(bm_Network0);
//????????????????????????
//??????0-4???5????????????????????????????????????,??????api23???????????????
            int level = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                level = signalStrength.getLevel();
            }
            int gsmSignalStrength = signalStrength.getGsmSignalStrength();
//??????????????????
            int netWorkType = getNetWorkType(MainActivity.this);
            switch (netWorkType) {
                case NETWORKTYPE_WIFI:
                    //mTextView.setText("???????????????wifi,??????????????????" + gsmSignalStrength);
                    break;
//                case NETWORKTYPE_2G:
//                    //mTextView.setText("???????????????2G????????????,??????????????????" + gsmSignalStrength);
//                    break;
//                case NETWORKTYPE_4G:
//                    //mTextView.setText("???????????????4G????????????,??????????????????" + gsmSignalStrength);
//                    break;
//                case NETWORKTYPE_NONE:
//                    //mTextView.setText("??????????????????,??????????????????" + gsmSignalStrength);
//                    break;
//                case -1:
//                    //mTextView.setText("??????????????????,??????????????????" + gsmSignalStrength);
//                    break;
                case NETWORKTYPE_2G:
                case NETWORKTYPE_4G:
                    /*
                    if(!SocketUtils.getInstance().isOnLine){
                        //?????????
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
            mNetWorkType = NETWORKTYPE_NONE;//????????????
        }
        return mNetWorkType;
    }

    /**??????????????????*/
    private static boolean isFastMobileNetwork(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
//????????????????????????????????????????????????4G???????????????????????????????????????????????????
            return true;
        }
        return false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        IsRun=false;
        ControlUtils.getInstance().AllHWStop();

        //???????????????
        if(DisinfectData.IsDevHeatCheck)
        {
            if(HeatUtils.getInstance().isStart)
            {
                HeatUtils.getInstance().Close();
            }
        }

//        IsRun=false;
//
//        //??????NFC??????
//        NFCUtils.getInstance().onDestroy();
//
//        //??????????????????
//        ControlUtils.getInstance().CloseSerialPort();
//
//        //?????????????????????
//        SensortUtils1.getInstance().CloseSerialPort();
//
//        //??????????????????
//        ThreadLoopUtils.getInstance().Close();
//
//        //?????????????????????
//        SocketUtils.getInstance().Close();
    }

    public void ComeGoFrame (int id){
        //Frgm?????? ????????????
        FragmentTransaction ft= GetComeTransaction();
        //??????????????????????????? ?????????
        Log.d("FrgmChangeList", "ComeGoFrame:"+GetNowFrgmFlag());
        if(NowFrgmId!=id)
        {
            NowFrgmId=id;
            goFrame(id, ft);

            //???????????????
            AndroidUtils.HideInput(this);
        }
        else{
            Log.d("FrgmChangeList", "????????????????????? "+GetNowFrgmFlag()+" "+id);
            frgmCallBack.InitData();
        }
    }

    private void  BackGoFrame(int id){
        FragmentTransaction ft= GetBackTransaction();
        //??????????????????????????? ?????????
        Log.d("FrgmChangeList", "BackGoFrame:"+GetNowFrgmFlag());
        if(NowFrgmId != id ) {
            NowFrgmId=id;
            goFrame(id, ft);
        }else{
            Log.d("FrgmChangeList", "????????????????????? "+GetNowFrgmFlag()+" "+id);
            frgmCallBack.InitData();
        }
    }

    //??????????????????????????????
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
                //????????????
                ControlUtils.getInstance().FunStopDisinfect();

                frgmCallBack=loginFrgm;
                targetFragment=loginFrgm;
                break;
            case flag_register:
                frgmCallBack=registerFrgm;
                targetFragment=registerFrgm;
                break;
            case flag_main:
                //????????????
                ControlUtils.getInstance().FunStopDisinfect();

                frgmCallBack=mainFrgm;
                targetFragment=mainFrgm;

//                //?????????????????? ????????????
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

//                //?????????????????? ????????????
//                if(DisinfectData.IsDevHeatCheck)
//                {
//                    HeatUtils.getInstance().SetListen(true);
//                }
                break;

            case flag_end:
            case EndFrgm.flag_sub_idle:
                frgmCallBack=endFrgm;
                targetFragment=endFrgm;

//                //?????????????????? ????????????
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
            //???????????????switchFragment()???currentFragment???null????????????????????????
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

        //???????????????
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

    // ====================================== ?????? ======================================

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
                                //??????
                                Log.v(TAG,"Next");
                                frgmCallBack.onTouchNext();
                            }
                        }else if(nowX-TouchPointX>offset){
                            //??????
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

    //?????????????????????
    public void AllBtnFrgmHome()
    {
        //????????????
        ControlUtils.getInstance().FunStopDisinfect();

        ClearFrgmList();

        boolean isBackHome=true;
        if(!DisinfectData.IsWebUser) {
            isBackHome=true;
        }else{
            isBackHome=false;
        }

        //????????????????????????
        if(DisinfectData.IsDevCateCar)
        {
            isBackHome=false;
        }

        if(isBackHome)
        {
            //??????????????????
            SetFrgmChange(flag_login);
            BackGoFrame(flag_main);
        }else{
            //??????????????????
            BackGoFrame(flag_login);
        }
    }

    //?????????????????????
    public void BtnFrgmLogin()
    {
        //????????????
        ControlUtils.getInstance().FunStopDisinfect();

        ClearFrgmList();
        //Web????????????????????????
        BackGoFrame(flag_login);
    }

    //??????????????????????????????
    public void DisinfectBtnFrgmHome()
    {
        /*
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("?????????????????????")
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {//??????"Yes"??????
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //?????????????????????????????????
                        DisinfectData.planItem.NowRunMode= PlanItem.RunMode_End;
                        SocketUtils.getInstance().SendStopDisinfect();

                        //??????????????????
                        if(DisinfectData.IsDevCateCar)
                        {
                            Message msg = MainActivity.Instance.MainHandler.obtainMessage();
                            msg.what = MainActivity.MsgID_Car_OverPos;
                            MainActivity.Instance.MainHandler.sendMessage(msg);
                        }

                        AllBtnFrgmHome();
                    }
                })
                .setNegativeButton("??????",null)
                .create();
        alertDialog.show();
        */


        View.OnClickListener onConfimClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //?????????????????????????????????
                DisinfectData.planItem.NowRunMode= PlanItem.RunMode_End;
                SocketUtils.getInstance().SendStopDisinfect();

//                //??????????????????
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
        builder.setTitle("?????????????????????");
        builder.setButtonConfirm( "??????", onConfimClickListener);
        builder.setButtonCancel( "??????", null);

        customDialog = builder.create();
        customDialog.show();
    }

    public void FrgmGoBack(){
        GetNowFrgmFlag();

        Log.d("FrgmChangeList", "Now:"+GetNowFrgmFlag()+" "+FrgmChangeList);
        int size=FrgmChangeList.size();
        if(size>=2 && GetNowFrgmFlag()!=flag_login) {
            int lastFlag = FrgmChangeList.get(size-2);
            //??????????????????
            if(GetNowFrgmFlag()==flag_register){
                ClearFrgmList();
                BackGoFrame(flag_login);
                return;
            }
            //?????????????????????
            if(GetNowFrgmFlag()==flag_leave ||
                    GetNowFrgmFlag()==flag_run ||
                    GetNowFrgmFlag()==flag_end ||
                    GetNowFrgmFlag()==EndFrgm.flag_sub_idle ||
                    GetNowFrgmFlag()==EndFrgm.flag_sub_end)
            {
                return;
            }

            Log.d("FrgmChangeList", "Go: "+lastFlag);

            //?????????????????? ???????????????????????????
            FrgmChangeList.remove(size-1);

            //?????????
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
        //???????????????????????????
        if(toastWaitCnt<=0)
        {
            Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
            //???????????????
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

    //????????????
    public void CheckBattery()
    {
        int TempFlag=GetNowFrgmFlag();
        //????????????
        int Power=BatteryUtils.getInstance().Power;
        //????????????
        int CarPower=CarUtils.getInstance().Power;

        String showText="";
        int color=Color.GREEN;

        int Num=20;
        //??????????????????
        //Log.d("BatteryUtils", BatteryUtils.getInstance().OffLineCnt+" "+BatteryUtils.getInstance().isConnected());
        if(BatteryUtils.getInstance().OffLineCnt<30)//5
        {
            //????????????????????? ?????????????????????
            BatteryUtils.getInstance().OffLineCnt++;
        }

        if(BatteryUtils.getInstance().OffLineCnt<=30){
            showText=Power+"";

            if(Power<Num)
            {
                color=Color.RED;
            /*
            tv_Tips.setText(AndroidUtils.GetStr(R.string.lowpower));

            //???????????????
            if(TempFlag!=flag_login)
            {
                //????????????
                ControlUtils.getInstance().FunStopDisinfect();
                if(runFrgm.timeCnt>0 ||
                    RunFrgm.timeCanRun) {
                    //?????????????????????????????????
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

        //??????
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
        //??????????????????
        if(CarPower<Num)
        {
            tv_Tips.setText(AndroidUtils.GetStr(R.string.carlowpower));

            //???????????????
            if(TempFlag!=flag_login)
            {
                //????????????
                ControlUtils.getInstance().FunStopDisinfect();
                CarPlanUtils.getInstance().CarOver();
                if(runFrgm.timeCnt>0 ||
                        RunFrgm.timeCanRun) {
                    //?????????????????????????????????
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

    //???????????????
    public void CheckHeat()
    {
        //????????????????????????
        if(DisinfectData.IsDevHeatCheck)
        {
            if(!HeatUtils.getInstance().isStart)
            {
                //???????????????
                img_heat.setImageBitmap(bm_Heat0);
            }
            else
            {
                //????????????
                img_heat.setImageBitmap(bm_Heat1);
                /*
                //????????????
                if (HeatUtils.getInstance().GetNowClearScene())
                {
                    //????????????
                    img_heat.setImageBitmap(bm_Heat1);
                } else
                {
                    //????????????
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

    // ====================================== Thread ???????????? ======================================

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
        //????????????
        try
        {
            Message msg;
            //??????UI??????
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

        //??????????????????
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
//                    Log.v("LXPTIME", "??????|" + temp);
//                }

                if(nowTime.equals(planTime) && nowSec+1 >= planSec)
                {
                    Log.v("LXPTIME","????????? "+temp);

                    DisinfectData.planTimeStr="";
                    //??????????????????
                    Map<String, String> params=new HashMap<>();
                    params.put("planTimeStr",DisinfectData.planTimeStr);
                    AndroidUtils.QuickDataSet(params);

                    //????????????????????????
                    if(NowFrgmId==flag_leave ||
                       NowFrgmId==flag_run)
                    {
                        Log.v("LXPTIME","????????????");
                    }else{
                        //??????????????????????????????
                        WebUserLogout();

                        //Socket??????
                        DisinfectData.planItem.planTime=0;
                        SocketUtils.getInstance().ReplyWebStart();

                        //????????????
                        DisinfectData.planItem=DisinfectData.GetPlanItem();
                        ParmFrgm.time_spray=DisinfectData.planItem.sprayTime;
                        ParmFrgm.time_streng=DisinfectData.planItem.strengTime;
                        ParmFrgm.time_leave=DisinfectData.planItem.leaveTime;
                        ParmFrgm.time_idle=DisinfectData.planItem.idleTime;

                        //????????????
                        ControlUtils.getInstance().FunStopDisinfect();

                        leaveFrgm.WebControlStart();
                        ComeGoFrame(MainActivity.flag_leave);
                    }
                }
                if(nowDay>planDay)
                {
                    Log.v("LXPTIME","?????? "+temp);

                    DisinfectData.planTimeStr="";
                    //??????????????????
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

    //?????????????????????
    public void ThreadOneMinFun()
    {
        Map<String, String> sets=new HashMap<>();
        sets.put("ReagentWeightA",String.valueOf(DisinfectData.ReagentWeights[0]));
        sets.put("ReagentWeightB",String.valueOf(DisinfectData.ReagentWeights[1]));
        AndroidUtils.QuickDataSet(sets);
    }

    private void GetLanguage(){
        // ??????res????????????
        Resources resources = this.getResources();
        // ??????????????????????????????????????????????????????
        DisplayMetrics metrics = resources.getDisplayMetrics();
        // ??????????????????
        Configuration config = resources.getConfiguration();

        AndroidUtils.langue=config.locale;
    }

    //????????????
    public void ChangeLanguage(Locale target){
        // ??????res????????????
        Resources resources = this.getResources();
        // ??????????????????????????????????????????????????????
        DisplayMetrics metrics = resources.getDisplayMetrics();
        // ??????????????????
        Configuration config = resources.getConfiguration();

        AndroidUtils.langue=config.locale;

        if( config.locale != target) {
            config.locale = target;
            AndroidUtils.langue=config.locale;
            resources.updateConfiguration(config, metrics);

            //??????????????????
            FragmentTransaction ft= GetComeTransaction();
            ReplaceFrgm(ft, loginFrgm);
            loginFrgm.InitData();
        }
    }


    // ====================================== ???????????? ======================================

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
     * ?????????????????????????????????????????????????????????????????????????????????????????????bug
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
        // ????????????????????????????????????????????????TouchEvent???
        if (getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }

    public boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] leftTop = {0, 0};
            //????????????????????????location??????
            v.getLocationInWindow(leftTop);
            int left = leftTop[0];
            int top = leftTop[1];
            int bottom = top + v.getHeight();
            int right = left + v.getWidth();
            if (event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom) {
                // ??????????????????????????????????????????EditText?????????
                return false;
            } else {
                return true;
            }
        }
        return false;
    }


    // ====================================== ???????????? ======================================

    //????????????????????????
    private void WebStartDisinfect()
    {
        if(IsApkUpDate){
            return;
        }

        CarPlanUtils.getInstance().AddInfoList("????????????????????????");
        //??????????????????????????????
        WebUserLogout();

        ParmFrgm.time_spray=DisinfectData.planItem.sprayTime;
        ParmFrgm.time_streng=DisinfectData.planItem.strengTime;
        ParmFrgm.time_leave=DisinfectData.planItem.leaveTime;
        ParmFrgm.time_idle=DisinfectData.planItem.idleTime;

        //????????????
        int plan=DisinfectData.planItem.planTime;
        //??????????????????
        if(plan>0)
        {
            AddPlanTimeMission(plan);
        }
        else{
            //????????????
            switch (DisinfectData.planItem.NowRunMode) {
                case PlanItem.RunMode_Init:
                case PlanItem.RunMode_Leave:
                    //????????????
                    ControlUtils.getInstance().FunStopDisinfect();

                    leaveFrgm.WebControlStart();
                    ComeGoFrame(MainActivity.flag_leave);
                    break;
                case PlanItem.RunMode_Spray:
                    runFrgm.WebControlStart();
                    GoRunFrgm(true);
                    break;
                case PlanItem.RunMode_Idle:
                    //????????????
                    ControlUtils.getInstance().FunStopDisinfect();

                    ComeGoFrame(MainActivity.flag_end);
                    endFrgm.ChangeIdleMode();
                    break;
                case PlanItem.RunMode_End:
                    //????????????
                    ControlUtils.getInstance().FunStopDisinfect();

                    endFrgm.ChangeEndMode();
                    ComeGoFrame(MainActivity.flag_end);
                    //??????????????????????????????
                    if (DisinfectData.IsCarControl) {
                        ComeGoFrame(MainActivity.flag_login);
                    }
                    break;
            }
        }

        //?????????????????????
        //?????????????????????????????? ?????????????????????
        if(DisinfectData.planItem.sceneId==0 &&
           !DisinfectData.IsDevCateCar)
        {
            DisinfectData.DevCateNo=0;
        }
    }

    //????????????????????????
    private void WebStopDisinfect()
    {
        if(IsApkUpDate){
            return;
        }

        String info=String.format("WebStopDisinfect: ???????????????%d",
                DisinfectData.planItem.NowRunMode);
        Log.d(TAG, info);

        CarPlanUtils.getInstance().AddInfoList("????????????????????????");
        //????????????
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
                //????????????
                ControlUtils.getInstance().FunStopDisinfect();

                ComeGoFrame(MainActivity.flag_end);
                break;
            case PlanItem.RunMode_End:
                //????????????
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

    //????????????
    private void WebReConnect(){
        MyToastThread("??????????????????");
        if(frgmCallBack != null){
            //???????????????????????????
            if(GetNowFrgmFlag()==flag_login ||
                    GetNowFrgmFlag()==flag_register ||
                    GetNowFrgmFlag()==flag_main)
            {
                frgmCallBack.InitData();
            }
        }
    }

    //?????????????????? ??????????????????
    public void CheckWebOffLine()
    {
        //0512 ??????
        if(HttpUtils.IsOnLine) {
            if (ControlUtils.getInstance().isConnected()) {
                if (!SocketUtils.getInstance().isConnected()) {
                    Log.d(TAG, "run: ??????????????? ------------------------------");

                    SocketInit();

                    Message msg = MainHandler.obtainMessage();
                    msg.what = MsgID_UI_WebOffLineInfo;
                    MainHandler.sendMessage(msg);
                }
            }
        }

        //????????????????????????
        if(DisinfectData.IsDevHeatCheck)
        {
            //????????????????????????
            Log.d("HeatUtils", HeatUtils.getInstance().isStart?"??????":"??????");
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

    //????????????
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
                            //??????
                            Log.v(TAG,data);
                        }else{
                            //??????
                            Log.v(TAG,data);
                        }

                        //??????ToKen
                        HttpUtils.NetWork_ToKen="";
                        //???????????????????????????
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

    //?????????????????????????????????
    private int testShow=0;
    public byte[] WebGetState()
    {
        int state=4;//???????????????
        int leaveTime=DisinfectData.planItem.leaveTime;
        int sprayTime=DisinfectData.planItem.sprayTime+DisinfectData.planItem.strengTime;
        int strengTime=0;
        int idleTime=DisinfectData.planItem.idleTime;
        int runType=0;  //0????????????1????????? 2??????????????????????????????

        //?????????????????? ??? ????????????
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
        //??????????????????2
        if(DisinfectData.IsCarControl){
            state=2;
            //0113
            if(runFrgm.timeCanRun){
                runType=1;
            }
            if(GetNowFrgmFlag()!=flag_run){
                runType=0;
            }

            //1206 ??????????????????
            if(!DisinfectData.IsCarPointMode &&
               CarPlanUtils.getInstance().WaitTimeCnt>0)
            {
                sprayTime=-100;
            }
            testShow=sprayTime;

            //Log.d(TAG, "CarWebGetState:???????????? "+DisinfectData.IsCarControl);
            String test="??????:"+runType+" ??????:"+DisinfectData.planItem.sprayTime;
            //TestCarBug(test);
        }
        //???????????????
        if(DisinfectData.IsDevHeatCheck){
            //?????????????????????
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
        Log.d(TAG, "????????????????????????: "+state);
        return data;
    }

    //??????????????????
    public void NetWorkGetDevCate(final String mac){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/device/cate?macAdd="+mac;
                    Log.d(TAG, "?????????????????? url:"+url);
                    String result= HttpUtils.GetData(url);

                    Log.d(TAG, "?????????????????? data:"+result);
                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String msgStr= jsonObject.getString("msg");
                    Integer data= jsonObject.getInt("data");
                    DisinfectData.DevCateNo=data;
                    if(code==0){
                        //??????
                        /*
                        Panda == 1  //??????
                        Pretty == 2
                        Walker == 3  //?????????
                        Little == 4  //?????????
                        Giraffe == 5 //????????? ??????
                        Guard == 6   //??????
                        */

                        if(data!=3){
                            DisinfectData.IsDevCateCar=false;
                        }else{
                            DisinfectData.IsDevCateCar=true;
                        }

                    }else{
                        //??????

                    }
                    Log.v(TAG,"?????????????????? "+msgStr+" "+data);
                    //MyToastThread("?????????????????????"+data);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    //????????????
    public void WebApkUpDate(final int apkid)
    {
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/apk/id/"+apkid;
                    String result= HttpUtils.GetData(url);

                    Log.d(TAG, "???????????? data:"+result);
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

    // ====================================== ???????????? ======================================

    //??????????????????
    private void CarStartDisinfect(int time)
    {
        ParmFrgm.time_spray =time;
        ParmFrgm.time_streng=0;

        GoRunFrgm(false);
        runFrgm.CarStartDisinfect(time);
    }

    //??????????????????
    private void CarStopDisinfect(){
        ParmFrgm.time_spray =0;
        ParmFrgm.time_streng=0;

        GoRunFrgm(false);
        runFrgm.CarStopDisinfect();
    }

    //????????????????????????
    private void CarOverPos(){
        CarPlanUtils.getInstance().CarOver();

        //????????????
        ControlUtils.getInstance().FunStopDisinfect();

        ParmFrgm.time_spray =0;
        ParmFrgm.time_streng=0;

        DisinfectData.IsCarControl=false;
        Log.d(TAG, "????????????CarOverPos: "+DisinfectData.IsCarControl);

        //??????????????????????????????
        ComeGoFrame(MainActivity.flag_login);
    }


    //0622
    // ====================================== ?????????????????? ======================================

    public void AddPlanTimeMission(int plan)
    {
        //Log.v("LXPTIME","??????????????????0:"+plan);
        String day=AndroidUtils.GetFormatTime("dd");
        int hour =(int)(plan / 3600f);
        plan -= hour * 3600;
        int min = (int)(plan / 60f);
        plan -= min * 60;
        int sec = plan;
        DisinfectData.planTimeStr=day+":"+String.format("%02d",hour)+":"+String.format("%02d",min)+":"+String.format("%02d",sec);
        Log.v("LXPTIME","??????????????????1:"+DisinfectData.planTimeStr);

        //????????????????????????
        DisinfectData.SetPlanItem(DisinfectData.planItem);

        //????????????????????????
        Map<String, String> params=new HashMap<>();
        params.put("planTimeStr",DisinfectData.planTimeStr);
        AndroidUtils.QuickDataSet(params);

        String info=String.format("WebStartDisinfect: ?????????%d,???????????????%d,???????????????%d,???????????????%d",
                DisinfectData.planItem.NowRunMode,ParmFrgm.time_leave,(ParmFrgm.time_spray+ParmFrgm.time_streng),ParmFrgm.time_idle);
        Log.d(TAG, info);

        //?????????????????????
            /*
            //????????????
            ControlUtils.getInstance().FunStopDisinfect();
            //????????????
            ComeGoFrame(MainActivity.flag_switch);
            swithFrgm.ShowPlanDialog(String.format("%02d",hour)+":"+String.format("%02d",min)+":"+String.format("%02d",sec));
            */
    }


    //????????????
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

    //????????????
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
