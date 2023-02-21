package com.cczk.lxp.disinfectsystem.test;

import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.utils.hw.ControlUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;

import java.util.Objects;
import java.util.Random;

public class HWTestActivity extends AppCompatActivity {
    TextView tv;
    EditText edit_speed;
    public static Handler handler;

    private boolean LedR=false;
    private boolean LedG=false;
    private boolean LedB=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_testhw);
        tv=findViewById(R.id.testhw_tv_info);
        edit_speed=findViewById(R.id.testhw_edit_speed);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv.setText("");
            }
        });

        handler=new Handler(Objects.requireNonNull(Looper.myLooper())){
            @Override
            public void handleMessage(Message msg) {
                try{
                    //if(msg.what==1)
                    {
                        String data=msg.obj.toString();
                        data=tv.getText().toString()+"\r\n"+data;
                        tv.setText(data);
                    }
                }catch (Exception e){}
            }
        };


        ThreadLoopUtils.getInstance().Init();

        ControlUtils.getInstance().Init("ttysWK2",115200,handler);
        //ControlUtils.getInstance().Init("ttyS1",115200,handler);

    }

    public void SendR(View v){
        LedR=!LedR;
        ControlUtils.getInstance().SetSwitchRGB(LedR,LedG,LedB);
    }

    public void SendG(View v){
        LedG=!LedG;
        ControlUtils.getInstance().SetSwitchRGB(LedR,LedG,LedB);
    }

    public void SendB(View v){
        LedB=!LedB;
        ControlUtils.getInstance().SetSwitchRGB(LedR,LedG,LedB);
    }

    public void GetdRadar(View v){
        ControlUtils.getInstance().SendGetdRadar();
    }

    public void GetWeight(View v){
        ControlUtils.getInstance().SendGetWeight();
    }

    public void GetSwitch(View v){
        ControlUtils.getInstance().SendGetSwitch();
    }

    public void GetMotor(View v){
        ControlUtils.getInstance().SendGetGetMotor();
    }

    //

    public void OpenSwitch(View v){
        ControlUtils.getInstance().SendOpenSwitch();
    }

    public void CloseSwitch(View v){
        ControlUtils.getInstance().SendCloseSwitch();
    }

    public void OpenMotor(View v){
         //ControlUtils.getInstance().SendSetMotor(new byte[]{1,1,1,1});
        ControlUtils.getInstance().SetSwitchWaterValve(true);
    }

    public void CloseMotor(View v){
        //ControlUtils.getInstance().SendSetMotor(new byte[]{0,0,0,0});
        ControlUtils.getInstance().SetSwitchWaterValve(false);
    }

    /*
    public void SetMotorDir(View v){
        //ControlUtils.getInstance().SendSetMotorDir(new byte[]{1,1,1,1});
        ControlUtils.getInstance().SetSwitchWaterValve(true);
    }
    */

    public void SetMotorSpeedALL(View v){
        int speed=1;
        try{
            speed=Integer.valueOf(edit_speed.getText().toString().trim() );
        }catch (Exception e){}
        ControlUtils.getInstance().SendSetMotorSpeed(new byte[]{(byte) speed,(byte) speed,(byte) speed,(byte) speed});
    }

    public void SetMotorSpeed1(View v){
        int speed=1;
        try{
            speed=Integer.valueOf(edit_speed.getText().toString().trim() );
        }catch (Exception e){}
        ControlUtils.getInstance().SendSetMotorSpeed(new byte[]{(byte) speed,(byte) 0,(byte) 0,(byte) 0});
    }

    public void SetMotorSpeed2(View v){
        int speed=1;
        try{
            speed=Integer.valueOf(edit_speed.getText().toString().trim() );
        }catch (Exception e){}
        ControlUtils.getInstance().SendSetMotorSpeed(new byte[]{(byte) 0,(byte) speed,(byte) 0,(byte) 0});
    }

    public void SetMotorSpeed3(View v){
        int speed=1;
        try{
            speed=Integer.valueOf(edit_speed.getText().toString().trim() );
        }catch (Exception e){}
        ControlUtils.getInstance().SendSetMotorSpeed(new byte[]{(byte) 0,(byte) 0,(byte) speed,(byte) 0});
    }

    public void SetMotorSpeed4(View v){
        int speed=1;
        try{
            speed=Integer.valueOf(edit_speed.getText().toString().trim() );
        }catch (Exception e){}
        ControlUtils.getInstance().SendSetMotorSpeed(new byte[]{(byte) 0,(byte) 0,(byte) 0,(byte) speed});
    }

    public void StartDisinfect(View v) {
        ControlUtils.getInstance().FunStartDisinfect();
    }

    public void StopDisinfect(View v) {
        ControlUtils.getInstance().FunRemoteStopDisinfect();
    }

}
