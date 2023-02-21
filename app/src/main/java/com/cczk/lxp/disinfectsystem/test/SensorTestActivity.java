package com.cczk.lxp.disinfectsystem.test;

import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.utils.hw.SensortUtils1;
import com.cczk.lxp.disinfectsystem.utils.hw.SensortUtils2;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;

public class SensorTestActivity extends AppCompatActivity {
    TextView tv;
    public static Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testsensor);
        tv=findViewById(R.id.ws_text_data);

        handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                    String data=msg.obj.toString();
                    tv.setText(data);

            }
        };

        //靠近圆环ttyS4 另一个ttyS1
        //！！拨码开关 1-7下 8上！！
        ThreadLoopUtils.getInstance().Init();
        SensortUtils1.getInstance().Init("ttyS4",9600,handler);
        SensortUtils2.getInstance().Init("ttyS1",9600,handler);

    }
}
