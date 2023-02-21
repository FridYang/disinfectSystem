package com.cczk.lxp.disinfectsystem.bean;

import android.util.Log;

import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.view.activity.frgm.ParmFrgm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.Guard;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pc on 2020/6/19.
 */

public class DisinfectData {
    //登录用户使用到的场景
    public static ScensItem[] scensItems;
    //所有场景
    public static ScensItem[] AllScensItems;
    public static ParmItem[] ratioItems;
    public static ParmItem[] typeItems;

    //消毒任务数据 例如：提交服务器开始任务
    public static PlanItem planItem=new PlanItem();
    //单机离线数据
    public static JSONObject localParmData=new JSONObject();
    //单机离线浓度
    public static JSONArray localSensort=new JSONArray();
    //消毒液重量
    public static int[] ReagentWeights=new int[2];

    //登录角色类型
    public static boolean UserIsAdmin=false; // 用户是否为超级管理员
    public final static String UserRoleAdmin="superAdmin"; // 管理员标识

    // 预约时间
    //2021/6/19 新增
    public static String planTimeStr="";

    //是否为Web用户登录操作
    public static boolean IsWebUser=false;

    //是否为机器底盘任务
    //是否为机器类型
    public static boolean IsDevCateCar=false;
    public static int DevCateNo=1;
    public static boolean IsCarControl=false;

    public final static int Dev_Panda=1;    // 熊猫
    public final static int Dev_Pretty=2;
    public final static int Dev_Walker=3;  // 机器人
    public final static int Dev_Little=4;  // 便携式
    public final static int Dev_Giraffe=5; // 长颈鹿 照灯
    public final static int Dev_Guard=6;   // 卫兵 喷淋
    public final static int Dev_Clear=7;   // 现在不用 AB桶
    //本机类型
    public static int HwDevCate = Dev_Little;

    //试剂是否充足
    private static boolean ReagentSuffice=false;
    //消毒所需实际容量
    public static float ReagentSceneNeed=0;
    public static boolean GetReagentSuffice()
    {
        return ReagentSuffice;
    }

    public static void SetReagentSuffice(boolean suffice,float need)
    {
        ReagentSceneNeed=need;
        if(HwDevCate==Dev_Little)
        {
            ReagentSuffice=true;
            return;
        }

        if(suffice) {
            int value1 = ControlUtils.getInstance().GetWeightRatio(0);
            int value2 = ControlUtils.getInstance().GetWeightRatio(1);

            boolean isLess = false;
            if (DisinfectData.HwDevCate == DisinfectData.Dev_Little) {
                //单桶
                isLess = value1 <= 3;
            } else {
                //双桶
                isLess = (value1 <= 3 || value2 <= 3);
            }
            if (isLess) {
                ReagentSuffice = false;
            } else {
                ReagentSuffice = true;
            }

            //1010
            if (ReagentSceneNeed > 0)
            {
                if(ReagentSceneNeed<=ControlUtils.getInstance().GetNowReagent())
                {
                    //需要的小于现在的
                    ReagentSuffice = true;
                }else{
                    //需要的大于现在的
                    ReagentSuffice = false;
                }
            }
        }else{
            ReagentSuffice=false;
        }
    }

    //定点模式 运行前逻辑选择
    public static int CarWorkMode=0;

    //定点模式 运行中逻辑判断
    public static boolean IsCarPointMode=true;

    //是否为热成像类型 !!!
    public static boolean IsDevHeatCheck=false;

    //自定义场景名称
    public final static String CustomName="custom"; // 自定义场景标识
    //自定义场景数据
    public static ScensItem scensCustom;

    public static void Init(){
        scensItems=null;
        AllScensItems=null;
        ratioItems=null;
        typeItems=null;
        IsWebUser=false;

        scensCustom=new ScensItem();
        planItem=new PlanItem();
    }

    public static void SetPlanItem(PlanItem item)
    {
        //保存缓存数据
        Map<String, String> params=new HashMap<>();
        params.put("userId", String.valueOf(item.userId));
        params.put("typeId", String.valueOf(item.typeId));
        params.put("ratioValue", String.valueOf(item.ratioValue));
        params.put("sprayTime", String.valueOf(item.sprayTime));
        params.put("strengTime", String.valueOf(item.strengTime));
        params.put("idleTime", String.valueOf(item.idleTime));
        params.put("leaveTime", String.valueOf(item.leaveTime));
        params.put("planTime", String.valueOf(item.planTime));
        params.put("sceneId", String.valueOf(item.sceneId));
        params.put("roomId", String.valueOf(item.roomId));
        params.put("runMode", String.valueOf(item.NowRunMode));
        AndroidUtils.QuickDataSet(params);
    }

    public static PlanItem GetPlanItem()
    {
        int userId=AndroidUtils.QuickDataGetInt("userId");
        int typeId=AndroidUtils.QuickDataGetInt("typeId");
        int ratioValue=AndroidUtils.QuickDataGetInt("ratioValue");
        int sprayTime=AndroidUtils.QuickDataGetInt("sprayTime");
        int strengTime=AndroidUtils.QuickDataGetInt("strengTime");
        int idleTime=AndroidUtils.QuickDataGetInt("idleTime");
        int leaveTime=AndroidUtils.QuickDataGetInt("leaveTime");
        int planTime=AndroidUtils.QuickDataGetInt("planTime");
        int sceneId=AndroidUtils.QuickDataGetInt("sceneId");
        int roomId=AndroidUtils.QuickDataGetInt("roomId");
        int runMode=AndroidUtils.QuickDataGetInt("runMode");

        PlanItem item=new PlanItem();
        item.mac=AndroidUtils.GetMacAddress();
        item.userId=userId;
        item.InitParm(typeId,ratioValue,
                sprayTime,strengTime,idleTime,leaveTime,planTime,
                sceneId,roomId,runMode);
/*
        ParmFrgm.time_spray=sprayTime;
        ParmFrgm.time_streng=strengTime;
        ParmFrgm.time_idle=idleTime;
        ParmFrgm.time_leave=leaveTime;
*/
        return item;
    }

}

