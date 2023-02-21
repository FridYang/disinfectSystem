package com.cczk.lxp.disinfectsystem.utils.hw;

import android.os.Message;
import android.os.ParcelUuid;
import android.provider.FontRequest;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android_serialport_api.SerialPort;

/**
 * Created by pc on 2020/5/5.
 */

public class SerialPortBase {
    public static final int BUFFER_LEN = 1024;
    // 数据接收处理
    byte[] GetBuf = new byte[BUFFER_LEN];
    int GetIn, GetRead;

    public SerialPort mSerialPort;
    public OutputStream mOutputStream;
    public InputStream mInputStream;

    /**
     *
     * @param portno 串口号 例如：3
     * @param baudrate 波特率 例如：9600
     */
    public void OpenSerialPort(int portno,int baudrate) {
        try
        {
            //主板打开串口
            mSerialPort = new SerialPort(new File("/dev/ttyS"+portno), baudrate, 0);

            //获取输出输入流
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param portStr 串口号 例如：3
     * @param baudrate 波特率 例如：9600
     */
    public void OpenSerialPort(String portStr,int baudrate) {
        try
        {
            //主板打开串口
            mSerialPort = new SerialPort(new File("/dev/"+portStr), baudrate, 0);

            //获取输出输入流
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void CloseSerialPort() {
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }

    // 读取数据
    public void CheckCmd()
    {

    }

    //发送数据
    public void SendCmd(byte[] data)
    {

    }

    byte[] buffer = new byte[4096];
    public void ReadData(){
        int size=0;
        try {
            if (mInputStream == null) return;
            //Log.d("ControlTest","0");
            size = mInputStream.read(buffer);
            //Log.d("ControlTest","1");
            if (size > 0) {
                onDataReceived(buffer, size);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    private void onDataReceived(final byte[] buffer, final int size) {
        int i1;
        for (i1 = 0; i1 < size; i1++) {
            GetBuf[GetIn] = buffer[i1];
            if (++GetIn >= BUFFER_LEN) {
                GetIn = 0;
            }
        }
    }

    public byte Data() {
        byte c1;
        c1 = GetBuf[GetRead];
        if (++GetRead >= BUFFER_LEN) {
            GetRead = 0;
        }
        return c1;
    }

    private int Count() {
        if (GetIn >= GetRead) {
            return (GetIn - GetRead);
        } else {
            return (BUFFER_LEN - GetRead + GetIn);
        }
    }
}
