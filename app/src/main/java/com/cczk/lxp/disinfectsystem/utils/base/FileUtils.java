package com.cczk.lxp.disinfectsystem.utils.base;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class FileUtils {
    private static final String TAG = "FileUtils";
    public static final String SplitKey="itmeData:";

     /*
             * 此方法为android程序写入sd文件文件，用到了android-annotation的支持库@
     *
             * @param buffer   写入文件的内容
     * @param folder   保存文件的文件夹名称,如log；可为null，默认保存在sd卡根目录
     * @param fileName 文件名称，默认app_log.txt
     * @param append   是否追加写入，true为追加写入，false为重写文件
     * @param autoLine 针对追加模式，true为增加时换行，false为增加时不换行
     */
    public synchronized static void wirteTxt(@NonNull final String str, @Nullable final String fileName, final boolean append, final boolean autoLine)
    {
        Log.d(TAG, "wirteTxt: "+fileName);
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(MainActivity.Instance.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+fileName);

                RandomAccessFile raf = null;
                FileOutputStream out = null;
                byte[] buffer=str.getBytes();
                try {
                    if (append) {
                        //如果为追加则在原来的基础上继续写文件
                        raf = new RandomAccessFile(file, "rw");
                        raf.seek(file.length());
                        raf.write(buffer);
                        if (autoLine) {
                            raf.write("\n".getBytes());
                        }

                        //Log.d(TAG, "run:追加 "+str);
                    } else {
                        //重写文件，覆盖掉原来的数据
                        out = new FileOutputStream(file);
                        out.write(buffer);
                        out.flush();

                        //Log.d(TAG, "run:覆盖 "+str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (raf != null) {
                            raf.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 读取内容
     *
     * @param fileName
     * @return
     */
    public static String readTxt(@Nullable final String fileName)
    {
        Log.d(TAG, "readTxt: "+fileName);
        FileInputStream fileInputStream;
        BufferedReader bufferedReader;
        StringBuilder stringBuilder = new StringBuilder();

        File file = new File(MainActivity.Instance.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+fileName);
        if (file.exists())
        {
            try {
                fileInputStream = new FileInputStream(file);
                bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                    //Log.d(TAG, "run:line "+line);
                }
                bufferedReader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        //Log.d(TAG, "run:line "+stringBuilder.toString());
        return stringBuilder.toString();
    }

    public static boolean existsTxt(String fileName)
    {
        File file = new File(MainActivity.Instance.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+fileName);
        return file.exists();
    }

    public static void delTxt(String fileName)
    {
        Log.d(TAG, "delTxt: "+fileName);
        File file = new File(MainActivity.Instance.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+fileName);
        if (file.exists())
        {
            file.delete();
        }
    }

    public static ArrayList<String> getTxtFile()
    {
        File file = new File(MainActivity.Instance.getApplicationContext().getFilesDir().getAbsolutePath());
        File[] files=file.listFiles();
        if (files == null){
            return null;
        }
        ArrayList<String> list = new ArrayList<>();
        for(int i =0;i<files.length;i++){
            list.add(files[i].getName());
        }
        return list;
    }
}