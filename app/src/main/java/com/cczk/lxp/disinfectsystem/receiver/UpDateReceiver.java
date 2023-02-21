package com.cczk.lxp.disinfectsystem.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class UpDateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String localPkgName = context.getPackageName();
        //取得MyReceiver所在的App的包名
        Uri data = intent.getData();
        String installedPkgName = data.getSchemeSpecificPart();
        //取得安装的Apk的包名，只在该app覆盖安装后自启动
        if ((action.equals(Intent.ACTION_PACKAGE_ADDED) || action.equals(Intent.ACTION_PACKAGE_REPLACED)) && installedPkgName.equals(localPkgName)) {
            /**
             * 启动activity
             */
            Intent mIntent = new Intent( );
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName comp = new ComponentName("com.cczk.lxp.disinfectsystem", "com.cczk.lxp.disinfectsystem.view.activity.MainActivity");
            mIntent.setComponent(comp);
            mIntent.setAction("android.intent.action.VIEW");
            context.startActivity(mIntent);

        }
    }
}