package com.cczk.lxp.disinfectsystem.view.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.bean.DisinfectData;
import com.cczk.lxp.disinfectsystem.bean.ScensItem;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.base.HttpUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by pc on 2020/7/1.
 */

public class ScenesAddView {
    private final String TAG="ScenesAddView";
    private MainActivity activity;

    private FrameLayout layout_Add;
    private LinearLayout layout_Content;

    private ImageView[] imgs_Add;

    public ScenesAddView(MainActivity activity, View v){
        this.activity=activity;
        //获取场景添加布局
        layout_Add =v.findViewById(R.id.main_layout_add);
        layout_Content =v.findViewById(R.id.main_layout_add_content);

        Hide();
    }

    public void  Show(){
        Log.d(TAG, "Show: 1");
        layout_Add.setVisibility(View.VISIBLE);

        //更新是否已使用
        ScensItem[] AllScensItems=DisinfectData.AllScensItems;
        if(imgs_Add!=null && AllScensItems!=null) {
            if (imgs_Add.length == AllScensItems.length) {
                List<Integer> UsedSceneId = new ArrayList<>();
                ScensItem[] scensItems = DisinfectData.scensItems;
                for (int i = 0; i < scensItems.length; i++) {
                    UsedSceneId.add(scensItems[i].scenesId);
                }

                int ColorHide = Color.argb(150, 0, 0, 0);
                int ColorShow = Color.argb(0, 0, 0, 0);
                for (int i = 0; i < AllScensItems.length; i++) {
                    if (UsedSceneId.contains(AllScensItems[i].scenesId)) {
                        imgs_Add[i].setColorFilter(ColorHide);
                        imgs_Add[i].setEnabled(false);
                    } else {
                        imgs_Add[i].setColorFilter(ColorShow);
                        imgs_Add[i].setEnabled(true);
                    }
                }
            }
        }
    }

    public void  Hide(){
        layout_Add.setVisibility(View.GONE);
    }

    public void Init(ScensItem[] scensItems){
        layout_Content.removeAllViews();

        //添加图片框
        int count=scensItems.length;
        addView(count);

        //添加场景按钮事件
        for (int i = 0; i < imgs_Add.length; i++) {
            final int finalI = i;
            imgs_Add[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UIBtnViewAddFun(finalI);
                }
            });
        }
    }

    private void addView(int count)
    {
        int sizeW = (int)(MainActivity.Instance.getResources().getDimension(R.dimen.mainScenWidth)*1.2f);
        int sizeH = (int) (MainActivity.Instance.getResources().getDimension(R.dimen.mainScenWidth)*0.8f);

        //是否为单数
        boolean isSingle=false;
        if(count%2==1){
            count++;
            isSingle=true;
        }

        imgs_Add=new ImageView[count];
        int round=count/2;
        for (int i = 0; i < round; i++) {
            LinearLayout Hlist = new LinearLayout(activity);
            LinearLayout.LayoutParams parms;
            Hlist.setOrientation(LinearLayout.HORIZONTAL);

            ImageView img=new ImageView(activity);
            img.setImageBitmap(activity.bm_SceneEmpty);
            img.setScaleType(ImageView.ScaleType.FIT_XY);
            parms = new LinearLayout.LayoutParams(
                    //500,LinearLayout.LayoutParams.MATCH_PARENT );
                    sizeW,sizeH);
            parms.setMargins(5,0,5,10);
            imgs_Add[i*2+0]=img;
            Hlist.addView(img, parms);

            img=new ImageView(activity);
            img.setImageBitmap(activity.bm_SceneEmpty);
            img.setScaleType(ImageView.ScaleType.FIT_XY);
            parms = new LinearLayout.LayoutParams(
                    //500,LinearLayout.LayoutParams.MATCH_PARENT );
                    sizeW,sizeH);
            parms.setMargins(0,0,5,0);

            //最后如果为单数 隐藏
            if(i==round-1 && isSingle){
                img.setVisibility(View.GONE);
            }

            imgs_Add[i*2+1]=img;
            Hlist.addView(img, parms);

            parms = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layout_Content.addView(Hlist, parms);
        }
    }

    //更新场景图片
    public void UpDateSceneImage(int id){
        //获取当前使用场景
        ScensItem[] scensItems= DisinfectData.AllScensItems;
        Bitmap bm=scensItems[id].bm;

        imgs_Add[id].setImageBitmap(bm);
    }

    //添加场景按钮
    private void UIBtnViewAddFun(int no){
        NetWorkAddList(String.valueOf(DisinfectData.AllScensItems[no].scenesId), AndroidUtils.GetMacAddress());
        //activity.MyToast("Image "+no+" -"+DisinfectData.AllScensItems[no].scenesId);
    }

    //网络添加场景
    private void NetWorkAddList(final String scenId,final String macAdd){
        //public void NetWorkLogin(final String name, final String password,final String nfc){
        ThreadLoopUtils.getInstance().
                mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String url= HttpUtils.HostURL+"/user/scenes/create";
                    Map<String, String> params=new HashMap<>();
                    params.put("scenesId",scenId);
                    params.put("macAdd",macAdd);
                    String result= HttpUtils.PostData(url,params);

                    Message msg=activity.FrgmHandler.obtainMessage();
                    msg.what= MainActivity.MsgID_Web_ChangeScenList;
                    Object[] msgData=new Object[2];

                    JSONObject jsonObject=new JSONObject(result);
                    Integer code= jsonObject.getInt("code");
                    String data= jsonObject.getString("msg");
                    if(code==0){
                        //成功
                        msgData[0]=true;
                        msgData[1]=data;
                        Log.v(TAG,data);
                    }else{
                        //失败
                        msgData[0]=false;
                        msgData[1]=data;
                        Log.v(TAG,data);
                    }

                    msg.obj=msgData;
                    activity.FrgmHandler.sendMessage(msg);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
