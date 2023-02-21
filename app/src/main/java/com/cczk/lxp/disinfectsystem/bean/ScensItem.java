package com.cczk.lxp.disinfectsystem.bean;

import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by pc on 2020/6/12.
 */

//场景信息 例如：救护车/门诊室
public class ScensItem {
    public ScensItem(){}
    public static ScensItem Empty=new ScensItem();
    //[{"id":9,"userId":34,"scenesId":4,"length":10,"width":10,"height":10,"scenesName":"[场景]04辐射台","createTime":"2020-06-12 10:55:07","updateTime":"2020-06-12 10:55:07"}]
    public int id=0;
    public int userId=0;
    public int scenesId=0;
    public String icon="";
    public int length=0;
    public int width=0;
    public int height=0;
    public String scenesName="";
    public String createTime="";
    public String updateTime="";
    //场景图片
    public Bitmap bm=null;

    public ScensItem(int id, int userId, int scenesId, String icon, int length, int width, int height, String scenesName, String createTime, String updateTime) {
        this.id = id;
        this.userId = userId;
        this.scenesId = scenesId;
        this.icon = icon;
        this.length = length;
        this.width = width;
        this.height = height;
        this.scenesName = scenesName;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public ScensItem(JSONObject jsonData) {
        try {
            this.scenesName = jsonData.getString("scenesName");

            if(jsonData.has("id")){
                this.id = jsonData.getInt("id");
            }else{
                //不是使用中的场景
                this.id= -1;
            }
            if(jsonData.has("userId")){
                this.userId = jsonData.getInt("userId");
            }else{
                //不是使用中的场景
                this.userId = -1;
            }

            //场景名称对称自定义
            if(!scenesName.equals(DisinfectData.CustomName)){
                this.icon = jsonData.getString("icon");
            }else{
                //自定义场景
                this.icon = "";
            }

            this.scenesId = jsonData.getInt("scenesId");

            this.length =(int) jsonData.getInt("length");
            this.width = (int)jsonData.getInt("width");
            this.height =(int)jsonData.getInt("height");
            this.createTime = jsonData.getString("createTime");
            this.updateTime = jsonData.getString("updateTime");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //Log.d("ScensItem", "ScensItem: "+scenesName);
    }

}
