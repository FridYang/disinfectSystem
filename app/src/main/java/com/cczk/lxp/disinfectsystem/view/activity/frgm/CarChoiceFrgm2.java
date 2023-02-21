package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.utils.base.WebSocketUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by pc on 2018/2/7.
 */
public class CarChoiceFrgm2 extends Fragment implements IFrgmCallBack {
    private static final String TAG = "CarChoiceFrgm2";
    private MainActivity activity;
    Handler handler;
    TextView tv;
    WebView webView;
    private final boolean isRun=true;

    //定点类型
    boolean nowPointType = true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_car2, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        InitData();
        return v;
    }

    private void InitView(View v) {
        if (isRun) {
            tv = v.findViewById(R.id.car_tv);
            webView = v.findViewById(R.id.car_webview);
            ImageView img = v.findViewById(R.id.car_img_exit);
            img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.DisinfectBtnFrgmHome();
                }
            });

            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    try {
                        switch (msg.what) {
                            case MainActivity.MsgID_Web_WSConnect:
                            case MainActivity.MsgID_Web_WSDiconnect:
                                if (msg.obj != null &&
                                        msg.obj.toString().length() > 0) {
                                    tv.setText(msg.obj.toString());
                                }
                                break;
                            case MainActivity.MsgID_Web_WSGetMsg:
                                if (msg.obj != null &&
                                        msg.obj.toString().length() > 0) {
                                    String str = tv.getText().toString();
                                    //str=msg.obj.toString();
                                    try {
                                        JSONObject jsonObject = new JSONObject(msg.obj.toString());
                                        if (jsonObject.has("taskState")) {
                                            JSONObject taskState = jsonObject.getJSONObject("taskState");
                                            Log.d(TAG, "0 handleMessage: " + msg.obj.toString());
                                            if (taskState.has("nowPoint")) {
                                                taskState = taskState.getJSONObject("nowPoint");
                                                //紫外灯开关
                                                boolean isUVlamp = false;
                                                //喷雾开关
                                                boolean isSpray = false;
                                                Log.d(TAG, "1 handleMessage: " + msg.obj.toString());
                                                JSONObject data = null;
                                                if (taskState.has("fixedPoint")) {
                                                    data = taskState.getJSONObject("fixedPoint");
                                                    if (data.has("isWork")) {
                                                        nowPointType = true;
                                                        str = "\r\n定点工作";
                                                        str += "\r\n" + data.getBoolean("isWork");
                                                        if (data.getBoolean("isWork")) {
                                                            isUVlamp = data.getBoolean("isUVlamp");
                                                            isSpray = data.getBoolean("isSpray");
                                                            int UVlampTime = data.getInt("UVlampTime");
                                                            int SprayTime = data.getInt("SprayTime");
                                                        }
                                                    }
                                                }
                                                if (taskState.has("process")) {
                                                    data = taskState.getJSONObject("process");
                                                    if (data.has("isWork")) {
                                                        nowPointType = false;
                                                        str = "\r\n行进中工作";
                                                        str += "\r\n" + data.getBoolean("isWork");
                                                        if (data.getBoolean("isWork")) {
                                                            isUVlamp = data.getBoolean("isUVlamp");
                                                            isSpray = data.getBoolean("isSpray");
                                                        }
                                                    }
                                                }
                                                if (isUVlamp) {
                                                    str += "\r\n硬件打开紫外线";
                                                    ControlUtils.getInstance().SetSwitchRays(true);
                                                } else {
                                                    str += "\r\n硬件关闭紫外线";
                                                    ControlUtils.getInstance().SetSwitchRays(false);
                                                }
                                                if (isSpray) {
                                                    str += "\r\n硬件打开喷雾";
                                                    ControlUtils.getInstance().FunStartDisinfect();
                                                } else {
                                                    str += "\r\n硬件关闭喷雾";
                                                    ControlUtils.getInstance().FunStopDisinfect();
                                                }
                                                Log.d(TAG, "NowPoint: " + str);
                                            } else if (taskState.has("nextPoint") ||
                                                    taskState.has("task_uuid")) {
                                                str = "\r\n硬件关闭紫外线";
                                                str += "\r\n硬件关闭喷雾";
                                                ControlUtils.getInstance().FunStopDisinfect();
                                                ControlUtils.getInstance().SetSwitchRays(false);
                                                Log.d(TAG, "NextPoint: " + str);
                                            }
                                        }
                                    } catch (Exception e) {
                                        //str+="\r\nErr:"+e.toString();
                                        Log.d(TAG, "Exception: " + e.toString());
                                    }
                                    tv.setText(str);
                                }
                                break;
                        }
                    } catch (Exception e) {
                    }
                }
            };

            WebSocketUtils.getInstance().initWebSocket(handler);

            WebViewInit();
            webView.loadUrl("http://192.168.1.150:9000/");
        }
    }

    void WebViewInit() {

//声明WebSettings子类
        WebSettings webSettings = webView.getSettings();

//如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        webSettings.setJavaScriptEnabled(true);

//设置自适应屏幕，两者合用
        webSettings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小

//缩放操作
        webSettings.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        webSettings.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        webSettings.setDisplayZoomControls(false); //隐藏原生的缩放控件

//其他细节操作
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); //关闭webview中缓存
        webSettings.setAllowFileAccess(true); //设置可以访问文件
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        webSettings.setLoadsImagesAutomatically(true); //支持自动加载图片

        webSettings.setDomStorageEnabled(true);

        //设置不用系统浏览器打开,直接显示在当前Webview
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "loadUrl " + url);
                view.loadUrl(url);
                return true;
            }
        });

        //设置WebChromeClient类
        webView.setWebChromeClient(new WebChromeClient() {
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
                System.out.println("标题在这里");
                Log.d(TAG, "onReceivedTitle:" + title);
                //mtitle.setText(title);
            }


            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    String progress = newProgress + "%";
                    //loading.setText(progress);
                } else if (newProgress == 100) {
                    String progress = newProgress + "%";
                    //loading.setText(progress);
                }
                Log.d(TAG, "onProgressChanged: " + newProgress);
            }
        });


        //设置WebViewClient类
        webView.setWebViewClient(new WebViewClient() {
            //设置加载前的函数
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                System.out.println("开始加载了");
                //beginLoading.setText("开始加载了");
                Log.d(TAG, "onPageStarted: 开始加载");
            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
                //endLoading.setText("结束加载了");
                Log.d(TAG, "onPageFinished: 结束加载");
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Log.d(TAG, "onPageFinished: 加载异常" + request.toString() + " -:- " + error.toString());
            }

//            @Override
//            public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
//                Log.d(TAG, "shouldOverrideKeyEvent: 加载拦截");
//            }
        });
    }


    @Override
    public void FrgmMessage(Message msg) {
        Object[] data = (Object[]) msg.obj;

        String[] List_Item;
        ArrayAdapter<String> dataAdapter;

        switch (msg.what) {
            /*
            //获取用户标识
            case MainActivity.MsgID_Web_GetUserType:
                try {
                    data= (Object[]) msg.obj;
                    if((boolean)data[0]){
                        JSONObject joData=(JSONObject) data[1];
                        int id=joData.getInt("userId");
                        //初始化任务参数
                        DisinfectData.planItem=new PlanItem();
                        DisinfectData.planItem.InitUser(id);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
                */
        }
    }

    @Override
    public void InitData() {

    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            InitData();
        }
    }

    @Override
    public void onTouchNext() {
    }

    @Override
    public void onTouchBack(int flag) {
    }

    public void BtnCarChange(View v) {
        if (tv.getVisibility() != View.VISIBLE) {
            tv.setVisibility(View.VISIBLE);
            tv.setText("--");
        } else {
            tv.setVisibility(View.GONE);
            tv.setText("");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isRun) {
            WebSocketUtils.getInstance().onResume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(isRun) {
            WebSocketUtils.getInstance().closeConnect();
        }
    }
}

