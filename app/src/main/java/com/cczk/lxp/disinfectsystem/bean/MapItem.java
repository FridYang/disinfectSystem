package com.cczk.lxp.disinfectsystem.bean;

/**
 * Created by pc on 2020/10/21.
 */
/*
  int MarkType=jo.getInt("mLaneMarkType");
                            String MarkName=jo.getString("mLaneMarkName");
                            String MarkDescript=jo.getString("mLaneMarkDescript");

                            JSONObject MarkXYZW=jo.getJSONObject("mLaneMarkXYZW");
          */
public class MapItem {
    //坐标类型
    public int MarkType=0;
    //坐标名称
    public String MarkName="";
    //坐标消毒时间
    public int MarkWait =0;
    //坐标消毒模式  0定点模式 1移动模式
    public int MarkMode =0;
    //坐标点
    public float x=0;
    public float y=0;
    public float z=0;
    public float w=0;
    //坐标点
    public byte[] valueX;
    public byte[] valueY;
    public byte[] valueZ;
    public byte[] valueW;

    public MapItem(){}

    public MapItem(int markType, String markName, int markWait, int markMode,
                    float x, float y, float z, float w,
                    byte[] valueX, byte[] valueY, byte[] valueZ, byte[] valueW) {
        MarkType = markType;
        MarkName = markName;
        MarkWait = markWait;
        MarkMode = markMode;

        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;

        this.valueX = valueX;
        this.valueY = valueY;
        this.valueZ = valueZ;
        this.valueW = valueW;
    }
}
