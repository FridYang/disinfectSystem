package com.cczk.lxp.disinfectsystem.test;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import com.hc.reader.AndroidUSB;
import com.hc.reader.Card;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

public class NFC2TestActivity  extends AppCompatActivity {
    private String TAG = NFC2TestActivity.class.getSimpleName();
    public static Card card = null;
    private UsbManager manager = null;
    private String Device_USB = "com.android.example.USB";
    private UsbDevice usbDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testnfc2);

    }

    private void ShowToast(String str)
    {
        Toast.makeText(NFC2TestActivity.this, str, Toast.LENGTH_SHORT).show();
    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
/*
        if(requestCode == 1){

            if(permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                File file = new File(AndroidUtils.getSDPath()+ "/Download", "test.txt");
                if(file.exists()){
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
                MainActivity.Instance.MyToast("创建成功!");
            }else{
                MainActivity.Instance.MyToast("授权失败");
            }
        }
 */

    }


    public void Btn1(View v)
    {
        try {
                manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                if (manager == null) {
                    Log.e(TAG, "UsbManager is null.");
                    ShowToast("UsbManager is null");
                    return;
                }

                card = new AndroidUSB(this, manager);
                usbDevice = card.GetUsbReader();
                if (usbDevice == null) {
                    ShowToast("No reader was scanned.");
                    return;
                }

                short st=0;
                // 判断是否拥有该设备的连接权限
                if (!manager.hasPermission(usbDevice)) {
                    ShowToast("没有权限");

                    // 如果没有则请求权限
                    PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0,
                            new Intent(Device_USB), PendingIntent.FLAG_UPDATE_CURRENT);
                    manager.requestPermission(usbDevice, mPermissionIntent);

                    ShowToast("更新配置重启应用");
                    Handler handler=new Handler();
                    handler.postDelayed(new Runnable(){
                        @Override
                        public void run() {
                            final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    },1000);

                }else{
                    st = card.OpenReader(usbDevice);
                    if (st >= 0) {
                        ShowToast("Connect Reader succeeded.");

                        try {
                            short type = 2;
                            st = card.rf_Tag_SetConfigNdef((short) 0, type);
                            if (st < 0) {
                                ShowToast(card.GetErrMessage((short) 1, st));
                            } else {
                                ShowToast("Set Tag Type success.");
                            }
                        } catch (Exception e) {
                            ShowToast(e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        ShowToast(card.GetErrMessage((short) 0, st));
                    }
                }
        } catch (Exception e) {
            ShowToast(e.getMessage());
        }
    }

    public void Btn2(View v)
    {
        try {
        //寻卡
        byte[] snr = new byte[12];
        short st = card.rf_scard((byte) 1, snr);
        if (st < 0) {
            ShowToast(card.GetErrMessage((short) 0, st));
            return;
        }
        //检测是否为NDEF格式
        st = card.rf_Tag_CheckNdef();
        if (st < 0) {
            ShowToast(card.GetErrMessage((short) 0, st));
            return;
        } else if (st == 0) { //标签为默认初始化状态，即不是NDEF格式
            ShowToast("Tag is default initial state.");
            return;
        }

        //读取数据
        byte[] data = new byte[256];
        st = card.rf_Tag_ReadNdef(data);
        if (st < 0) {
            ShowToast(card.GetErrMessage((short) 0, st));
            return;
        }

        byte[] ndefData = new byte[st];
        System.arraycopy(data, 0, ndefData, 0, st);

        //解析数据
        NdefMessage ndefMessage = new NdefMessage(ndefData);
        NdefRecord ndefRecord = ndefMessage.getRecords()[0];

        byte[] payload = ndefRecord.getPayload();
        String type = new String(ndefRecord.getType());

        if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
            //TextRecord
            String textCoding = ((payload[0] & 0x80) == 0) ? "utf-8" : "utf-16";
            int codeLength = payload[0] & 0x3f;
            String text = new String(payload, codeLength + 1, payload.length - codeLength - 1, textCoding);

            String show="";
            show+="# Record " + ": Text record";
            show+=" | Type: " + type + "\n";
            show+=" | Encoding: " + textCoding + "\n";
            show+=" | Text: " + text;

            ShowToast(text);
        }
    } catch (Exception e) {
        ShowToast(e.getMessage());
    }

    }
}