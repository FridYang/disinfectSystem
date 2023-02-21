package com.cczk.lxp.disinfectsystem.utils.base;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Message;
import android.util.Log;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by pc on 2019/12/12.
 */

public class HttpUtils {
    private static final String TAG = "HttpUtils";
    //本地
//    public static final String HostURL = "http://192.168.1.25:82";
//    public static final String SocketURL = "192.168.1.25";

    //    //上线
    public static final String HostURL = "http://8.129.170.63:82";
    public static final String SocketURL = "8.129.170.63";

    public static String NetWork_ToKen="";
    public static String NetWork_Sign="0";
    //离线登录
    public static boolean IsOnLine=true;

    public static String PostData(String strUrlPath, Map<String, String> params_str) {
        Map<String, Integer> params=new HashMap<>();
        //防止空字符串异常
        String result=PostData(strUrlPath,params_str,params);
        if(result.isEmpty() || result =="" || result==null){
            result="{\"code\":999,\"msg\":\"网络异常\"}";
        }
        return result;
    }

    public static String PostData(String strUrlPath, Map<String, String> params_str,Map<String, Integer> params_int) {
        return PostData(strUrlPath, getRequestData(params_str, params_int).toString());
    }

    public static String PostData(String strUrlPath) {
        return PostData(strUrlPath,"");
    }

    private static String PostData(String strUrlPath,String parms) {

        byte[] data = parms.getBytes();//获得请求体
        try {
            URL url = new URL(strUrlPath);

            Log.v(TAG,"PostDataSend:"+strUrlPath+":"+parms);

            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.setConnectTimeout(3000);     //设置连接超时时间
            httpURLConnection.setDoInput(true);                  //打开输入流，以便从服务器获取数据
            httpURLConnection.setDoOutput(true);                 //打开输出流，以便向服务器提交数据
            httpURLConnection.setRequestMethod("POST");     //设置以Post方式提交数据
            httpURLConnection.setUseCaches(false);               //使用Post方式不能使用缓存
            //设置请求体的类型是文本类型
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            //设置请求体的长度
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(data.length));

            //切换语言
            if(AndroidUtils.langue==Locale.ENGLISH)
            {
                httpURLConnection.setRequestProperty("Accept-Language", "en");
            }else
            {
                httpURLConnection.setRequestProperty("Accept-Language", "zh");
            }

            if(NetWork_ToKen!="" && NetWork_ToKen!=null) {
                //附带ToKen
                httpURLConnection.setRequestProperty("token", NetWork_ToKen);
            }else{
                Log.d(TAG, "PostData: ToKen=Null "+NetWork_ToKen);
            }

            //获得输出流，向服务器写入数据
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(data);

            int response = httpURLConnection.getResponseCode();            //获得服务器的响应码
            if(response == HttpURLConnection.HTTP_OK) {
                InputStream inptStream = httpURLConnection.getInputStream();
                String result=dealResponseResult(inptStream);
                Log.v(TAG,"PostDataGet:"+result);
                return result;                     //处理服务器的响应结果
            }
        } catch (IOException e) {
            return "err: " + e.getMessage().toString();
        }
        return "-1";
    }

    public static String GetData(String strUrlPath) {
        URL url = null;
        Log.v(TAG,"GetData:"+strUrlPath);
        try {
            url = new URL(strUrlPath);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url
                    .openConnection();
            httpURLConnection.setRequestMethod("GET");
            if(NetWork_ToKen!="") {
                //附带ToKen
                httpURLConnection.setRequestProperty("token", NetWork_ToKen);
            }

            httpURLConnection.connect();
            BufferedInputStream bis = new BufferedInputStream(
                    httpURLConnection.getInputStream());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len;
            byte[] arr = new byte[1024];
            while ((len = bis.read(arr)) != -1) {
                bos.write(arr, 0, len);
                bos.flush();
            }
            bos.close();
            Log.v(TAG, bos.toString("utf-8"));
            return bos.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap GetBitmapData(String strUrlPath) {
        URL url = null;
        Bitmap bmp = null;
        Log.v(TAG,"GetBitmapData:"+strUrlPath);
        try {
            url = new URL(strUrlPath);
            HttpURLConnection conn = (HttpURLConnection) url
                    .openConnection();
            conn.setConnectTimeout(6000);//设置超时
            conn.setDoInput(true);
            conn.setUseCaches(false);//不缓存
            conn.connect();
            InputStream is = conn.getInputStream();//获得图片的数据流
            bmp = BitmapFactory.decodeStream(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bmp;
    }

    /*
     * Function  :   封装请求体信息
     * Param     :   params请求体内容，encode编码格式
     */
    private static StringBuffer getRequestData(Map<String, String> params_str,Map<String, Integer> params_int) {
        StringBuffer stringBuffer = new StringBuffer();        //存储封装好的请求体信息
        try {
            for(Map.Entry<String, String> entry : params_str.entrySet()) {
                stringBuffer
                        .append("\"")
                        .append(entry.getKey())
                        .append("\":\"")
                        //.append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                        .append(new String(entry.getValue().getBytes(),"UTF-8"))
                        .append("\",");
            }
            for(Map.Entry<String, Integer> entry : params_int.entrySet()) {
                stringBuffer
                        .append("\"")
                        .append(entry.getKey())
                        .append("\":")
                        .append(entry.getValue())
                        .append(",");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);    //删除最后的一个","
            stringBuffer.insert(0,"{");
            stringBuffer.append("}");
            Log.v(TAG,"getRequestData: "+stringBuffer.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.v(TAG,"getRequestDataErr: "+e.toString());
        }
        return stringBuffer;
    }

    /*
     * Function  :   处理服务器的响应结果（将输入流转化成字符串）
     * Param     :   inputStream服务器的响应输入流
     */
    private static String dealResponseResult(InputStream inputStream) {
        String resultData = null;      //存储处理结果
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len = 0;
        try {
            while((len = inputStream.read(data)) != -1) {
                byteArrayOutputStream.write(data, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        resultData = new String(byteArrayOutputStream.toByteArray());
        return resultData;
    }

    public static String uploadLogFile(String uploadUrl, String fileName) {
        String result = null;
        try {
            HttpClient hc = new DefaultHttpClient();
            hc.getParams().setParameter(
                    CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
            HttpPost hp = new HttpPost(uploadUrl);
            File file = new File(MainActivity.Instance.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+fileName);
            final MultipartEntity entity = new MultipartEntity();
            ContentBody contentBody = new FileBody(file);
            entity.addPart("jsonFile", contentBody);
            hp.setEntity(entity);

            /*
            List<NameValuePair> params=new ArrayList<NameValuePair>();
            //建立一个NameValuePair数组，用于存储欲传送的参数
            params.add(new BasicNameValuePair("token",NetWork_ToKen));
            //添加参数
            hp.setEntity(new UrlEncodedFormEntity(params,HTTP.UTF_8));*/

            hp.addHeader("token",NetWork_ToKen);

            HttpResponse hr = hc.execute(hp);
            HttpEntity he = hr.getEntity();
            int statusCode = hr.getStatusLine().getStatusCode();
            //if (statusCode != HttpStatus.SC_OK)
            //    throw new ServiceRulesException(Common.MSG_SERVER_ERROR);
            Log.d(TAG, "uploadLogFile: "+statusCode);
            result = EntityUtils.toString(he, HTTP.UTF_8);
            Log.d(TAG, "result: "+result);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", "⽂件上传失败！上传⽂件为：" + fileName);
            Log.e("TAG", "报错信息toString：" + e.toString());
        }
        return result;
    }

//
//    /*
//     * Function  :   封装请求体信息
//     * Param     :   params请求体内容，encode编码格式
//     */
//    private static StringBuffer getRequestData(Map<String, Long> params) {
//        StringBuffer stringBuffer = new StringBuffer();        //存储封装好的请求体信息
//        try {
//            for(Map.Entry<String, Long> entry : params.entrySet()) {
//                stringBuffer.append(entry.getKey())
//                        .append("=")
//                        .append(entry.getValue())
//                        .append("&");
//            }
//            stringBuffer.deleteCharAt(stringBuffer.length() - 1);    //删除最后的一个"&"
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return stringBuffer;
//    }
//
//    /*
//     * Function  :   封装请求体信息
//     * Param     :   params请求体内容，encode编码格式
//     */
//    private static StringBuffer getRequestData(Map<String, String> params, String encode) {
//        StringBuffer stringBuffer = new StringBuffer();        //存储封装好的请求体信息
//        try {
//            for(Map.Entry<String, String> entry : params.entrySet()) {
//                stringBuffer.append(entry.getKey())
//                        .append("=")
//                        .append(URLEncoder.encode(entry.getValue(), encode))
//                        .append("&");
//            }
//            stringBuffer.deleteCharAt(stringBuffer.length() - 1);    //删除最后的一个"&"
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return stringBuffer;
//    }
//
//
//    private static String convertStreamToString(InputStream is) {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//        StringBuilder sb = new StringBuilder();
//        String line = null;
//        try {
//            while ((line = reader.readLine()) != null) {
//                sb.append(line + "/n");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                is.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return sb.toString();
//    }
//
//    public static String PostFile(String actionUrl,String fileName) throws IOException {
//        Log.d(TAG, "actionUrl: "+actionUrl);
//        Log.d(TAG, "PostFile: "+fileName);
//
//        String BOUNDARY = java.util.UUID.randomUUID().toString();
//        String PREFIX = "--", LINEND = "\r\n";
//        String CHARSET = "UTF-8";
//        URL uri = new URL(actionUrl);
//        HttpURLConnection conn = (HttpURLConnection) uri.openConnection();
//        conn.setConnectTimeout(3000);     //设置连接超时时间
//        conn.setDoInput(true);                  //打开输入流，以便从服务器获取数据
//        conn.setDoOutput(true);                 //打开输出流，以便向服务器提交数据
//        conn.setRequestMethod("POST");     //设置以Post方式提交数据
//        conn.setUseCaches(false);               //使用Post方式不能使用缓存
//        //设置请求体的类型是文本类型
//        conn.setRequestProperty("Content-Type", "application/json");
//
//        DataOutputStream outStream = new DataOutputStream(conn
//                .getOutputStream());
//        // 发送文件数据
//
//        StringBuilder sb1 = new StringBuilder();
//        sb1.append(PREFIX);
//        sb1.append(BOUNDARY);
//        sb1.append(LINEND);
//        sb1.append("Content-Disposition: form-data; name=\"jsonFile\"; filename=\""
//                + "jsonFile" + "\"" + LINEND);
//        sb1.append("Content-Type: application/octet-stream; charset="
//                + CHARSET + LINEND);
//        sb1.append(LINEND);
//        outStream.write(sb1.toString().getBytes());
//
//        Log.d(TAG, "Data: "+sb1.toString());
//        File file = new File(MainActivity.Instance.getApplicationContext().getFilesDir().getAbsolutePath()+"/"+fileName);
///*
//                InputStream is = new FileInputStream(file);
//                byte[] buffer = new byte[1024];
//                int len = 0;
//                while ((len = is.read(buffer)) != -1) {
//                    outStream.write(buffer, 0, len);
//                }
//                is.close();
//*/
//
//
//        try {
//            FileInputStream fileInputStream = new FileInputStream(file);
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                //stringBuilder.append(line);
//                outStream.write(line.getBytes());
//                Log.d(TAG, "run:line "+line);
//            }
//            bufferedReader.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        outStream.write(LINEND.getBytes());
//
//        // 请求结束标志
//        byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();
//        outStream.write(end_data);
//        outStream.flush();
//        // 得到响应码
//        int res = conn.getResponseCode();
//        Log.d(TAG, "DataOver0:"+res);
//        InputStream in = conn.getInputStream();
//        InputStreamReader isReader = new InputStreamReader(in);
//        BufferedReader bufReader = new BufferedReader(isReader);
//        String line = null;
//        String data = "OK";
//        while ((line = bufReader.readLine()) == null)
//        {
//            data += line;
//        }
//
//        Log.d(TAG, "DataOver1:"+data);
//
//        if (res == 200) {
//            int ch;
//            StringBuilder sb2 = new StringBuilder();
//            while ((ch = in.read()) != -1) {
//                sb2.append((char) ch);
//            }
//        }
//        outStream.close();
//        conn.disconnect();
//        return in.toString();
//    }

}
