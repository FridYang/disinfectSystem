package com.cczk.lxp.disinfectsystem.bean;

//消毒数据 例如：浓度/消毒剂
public class ParmItem{
    public String name="";
    //数据库ID
    public int id = 0;
    //数据值
    public int value = 0;
    public boolean isDefault=false;

    public ParmItem(String name, int id, int value, boolean isDefault) {
        this.name = name;
        this.id = id;
        this.value = value;
        this.isDefault = isDefault;
    }
}
