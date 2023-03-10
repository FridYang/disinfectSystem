package com.cczk.lxp.disinfectsystem.utils.hw;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.cczk.lxp.disinfectsystem.test.NFC2TestActivity;
import com.cczk.lxp.disinfectsystem.utils.base.AndroidUtils;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;
import com.hc.reader.AndroidUSB;
import com.hc.reader.Card;

import java.util.Arrays;

public class NFC2Utils
{
    private String TAG = "NFC2Utils";
    private Handler FrgmHandler;
    public static boolean IsHaveDevice=false;

    public static Card card = null;
    private UsbManager manager = null;
    private String Device_USB = "com.android.example.USB";
    private UsbDevice usbDevice = null;

    private int ToastCnt=0;
    public static boolean IsHaveDeviceName=false;

    //θ·εεδΎ
    private static NFC2Utils instance=null;
    public static NFC2Utils getInstance() {
        if (instance == null) {
            synchronized (NFC2Utils.class) {
                if (instance == null) {
                    instance = new NFC2Utils();
                }
            }
        }
        return instance;
    }

    private void ShowToast(String str)
    {
        Log.d(TAG, str);
        //MainActivity.Instance.MyToastThread(str);
    }

    public void Init()
    {
        try {
            manager = (UsbManager) MainActivity.Instance.getApplicationContext().getSystemService(Context.USB_SERVICE);
            if (manager == null) {
                ShowToast("UsbManager is null");
                return;
            }

            card = new AndroidUSB(MainActivity.Instance.getApplicationContext(), manager);
            usbDevice = card.GetUsbReader();
            if (usbDevice == null) {
                ShowToast("UsbDevice is null.");
                return;
            }

            // ε€ζ­ζ―ε¦ζ₯ζθ―₯θ?Ύε€ηθΏζ₯ζι
            if (!manager.hasPermission(usbDevice)) {
                ShowToast("ζ²‘ζζι");

                // ε¦ζζ²‘ζεθ―·ζ±ζι
                PendingIntent mPermissionIntent = PendingIntent.getBroadcast(MainActivity.Instance.getApplicationContext(), 0,
                        new Intent(Device_USB), PendingIntent.FLAG_UPDATE_CURRENT);
                manager.requestPermission(usbDevice, mPermissionIntent);
            } else {
                GetPermission();
            }
        } catch (Exception e) {
            ShowToast(e.getMessage());
        }
    }

    private void GetPermission()
    {
        ShowToast("θ·εε°ζι");

        short st = card.OpenReader(usbDevice);
        if (st >= 0) {
            ShowToast("Connect Reader succeeded.");
        } else {
            ShowToast(card.GetErrMessage((short) 0, st));
        }

        //θ?Ύη½?ε‘η±»ε
        try {
            short type = 2;
            st = card.rf_Tag_SetConfigNdef((short) 0, type);
            if (st >= 0) {
                ShowToast("Set Tag Type success.");
            } else {
                ShowToast(card.GetErrMessage((short) 1, st));
            }
            IsHaveDevice=true;
        } catch (Exception e) {
            ShowToast(e.getMessage());
            e.printStackTrace();
        }

        //θ·εζιζε»ΆθΏ
        IsHaveDeviceName = true;
        if(FrgmHandler!=null)
        {
            DeviceOpen(FrgmHandler);
        }
    }

    private void logMsg()
    {
        try {
            //ε―»ε‘
            byte[] snr = new byte[12];
            short st = card.rf_scard((byte) 1, snr);
            if (st < 0) {
                //ShowToast("0 "+card.GetErrMessage((short) 0, st));
                return;
            }
            //ζ£ζ΅ζ―ε¦δΈΊNDEFζ ΌεΌ
            st = card.rf_Tag_CheckNdef();
            if (st < 0) {
                //ShowToast("1 "+card.GetErrMessage((short) 0, st));
                return;
            } else if (st == 0) { //ζ η­ΎδΈΊι»θ?€εε§εηΆζοΌε³δΈζ―NDEFζ ΌεΌ
                //ShowToast("2 "+"Tag is default initial state.");
                return;
            }

            //θ―»εζ°ζ?
            byte[] data = new byte[256];
            st = card.rf_Tag_ReadNdef(data);
            if (st < 0) {
                //ShowToast("3 "+card.GetErrMessage((short) 0, st));
                return;
            }

            byte[] ndefData = new byte[st];
            System.arraycopy(data, 0, ndefData, 0, st);

            //θ§£ζζ°ζ?
            NdefMessage ndefMessage = new NdefMessage(ndefData);
            NdefRecord ndefRecord = ndefMessage.getRecords()[0];

            byte[] payload = ndefRecord.getPayload();
            String type = new String(ndefRecord.getType());

            if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                //TextRecord
                String textCoding = ((payload[0] & 0x80) == 0) ? "utf-8" : "utf-16";
                int codeLength = payload[0] & 0x3f;
                String text = new String(payload, codeLength + 1, payload.length - codeLength - 1, textCoding);
                text=text.trim();

                String show="";
                show+="# Record " + ": Text record";
                show+=" | Type: " + type + "\n";
                show+=" | Encoding: " + textCoding + "\n";
                show+=" | Text: " + text;

                ShowToast(text);

                Message message=FrgmHandler.obtainMessage();
                message.what= MainActivity.MsgID_HW_NFCID;
                message.obj=text;
                FrgmHandler.sendMessage(message);

                //ζ­ζΎNFCι³ζ
                AndroidUtils.PlayNFCSound();
            }
        } catch (Exception e) {
            //ShowToast(e.getMessage());
            Log.d(TAG, "logMsg: "+e.getMessage());
        }
    }

    //BroadcastReceiver
    public void  MyReceive(Intent paramAnonymousIntent)
    {
        Log.w(TAG, "Enter the broadcast receiver.");
        String action = paramAnonymousIntent.getAction();
        // ε€ζ­εΉΏζ­η±»ε
        if (Device_USB.equals(action))
        {
            GetPermission();
        }
    }

    public void MyResume()
    {
        IntentFilter filter = new IntentFilter(Device_USB);
        MainActivity.Instance.getApplicationContext().registerReceiver(MainActivity.Instance.mUsbBroadcast, filter);
    }

    public void MyPause()
    {
        MainActivity.Instance.getApplicationContext().unregisterReceiver(MainActivity.Instance.mUsbBroadcast);
    }

    public void DeviceOpen(Handler handler)
    {
        Log.d(TAG, "DeviceOpen: "+IsHaveDevice);
        FrgmHandler = handler;
        if(IsHaveDevice) {
            IsHaveDeviceName = true;
        }else{
            if(ToastCnt<=0)
            {
                ToastCnt = 60;
                //MainActivity.Instance.MyToastThread("ζΎδΈε°NFCζ¨‘ε");
            }
        }
    }

    public void DeviceClose()
    {
        Log.d(TAG, "DeviceClose: ");
        IsHaveDeviceName=false;
    }

    public void  ThreadHalfSecFun()
    {
        if(ToastCnt>0){
            ToastCnt--;
        }

        //Log.d(TAG, "ThreadHalfSecFun: "+IsHaveDevice +" "+
        //        IsHaveDeviceName);
        if(IsHaveDevice &&
           IsHaveDeviceName)
        {
            logMsg();
        }
    }


}
