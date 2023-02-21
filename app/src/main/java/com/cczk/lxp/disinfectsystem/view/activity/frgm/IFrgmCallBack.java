package com.cczk.lxp.disinfectsystem.view.activity.frgm;


import android.os.Message;

/**
 * Created by pc on 2020/5/5.
 */

public interface IFrgmCallBack {

    void InitData();

    void onTouchNext();
    void onTouchBack(int flag);

    void FrgmMessage(Message msg);
}
