package com.cczk.lxp.disinfectsystem.bean;

import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;

//消毒任务数据 例如：提交服务器开始任务
public class PlanItem{
    //设备Mac地址
    public String mac="";
    //执行员id
    public int userId=0;
    // 消毒剂id
    public int typeId=0;
    //目标浓度 数据
    public int ratioValue = 0;
    // 喷洒时间
    public int sprayTime=0;
    // 增强喷洒时间
    public int strengTime=0;
    // 消毒时间
    public int idleTime=0;
    // 撤离时间
    public int leaveTime=0;
    // 预约时间
    public int planTime=0;
    // 场景id
    public int sceneId=0;
    // 房间id   网络化才使用，正常用场景ID即可
    public int roomId=0;
    // 消毒场景 1：撤离，2：喷洒，3：结束
    public int NowRunMode=0;

    public final static int RunMode_Init=0;
    public final static int RunMode_Leave=1;
    public final static int RunMode_Spray=2;
    public final static int RunMode_Idle = 3;
    public final static int RunMode_End = 4;
    public final static int RunMode_CarStop = 6;
    //0120 终端暂停机器人任务
    public final static int RunMode_CarPlanStop = 7;

    //初始化用户
    public void InitUser(int userId) {
        mac= AndroidUtils.GetMacAddress();
        this.userId=userId;
    }

    //初始化消毒数据
    public void InitParm(int typeId, int ratioValue, int sprayTime, int strengTime, int idleTime, int leaveTime, int planTime, int sceneId, int roomId,int runMode) {
        this.typeId = typeId;
        this.ratioValue = ratioValue;
        this.sprayTime = sprayTime;
        this.strengTime = strengTime;
        this.idleTime = idleTime;
        this.leaveTime = leaveTime;
        this.planTime = planTime;
        this.sceneId = sceneId;
        this.roomId = roomId;
        this.NowRunMode=runMode;
    }
}
