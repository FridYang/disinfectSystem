package com.cczk.lxp.disinfectsystem.test;

import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.bean.MapItem;
import com.cczk.lxp.disinfectsystem.bean.ParmItem;
import com.cczk.lxp.disinfectsystem.utils.hw.CarPlanUtils;
import com.cczk.lxp.disinfectsystem.utils.hw.CarUtils;
import com.cczk.lxp.disinfectsystem.utils.ui.ThreadLoopUtils;

import java.util.ArrayList;
import java.util.List;

public class CarTestActivity extends AppCompatActivity {
    private static final String TAG = "CarTestActivity";
    public static CarTestActivity instance=null;
    TextView tv;
    TextView tv_Show;
    public Handler handler=null;
    ListView lv_map;
    ListView lv_mapPos;

    public static final int flag_info=0;
    public static final int flag_map=1;
    public static final int flag_mapPos=2;
    public static final int flag_infoShow=3;

    private boolean isStartPlan=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance=this;

        // 全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_testcar);

        lv_map =findViewById(R.id.testcar_listMap);
        lv_map.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                List<ParmItem> mapItems= CarPlanUtils.getInstance().mapItems;
                if(mapItems != null && mapItems.size()>position)
                {
                    //更换地图
                    String Name=mapItems.get(position).name;
                    String[] list=Name.split("-");
                    String mapName=list[0]+"-"+list[1];
                    Log.d(TAG, "onItemClick: " + mapName);

                    CarPlanUtils.getInstance().CarChangeMap(position);
                }
            }
        });

        lv_mapPos =findViewById(R.id.testcar_listMapPos);
        lv_mapPos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                List<MapItem> mapItems= CarPlanUtils.getInstance().mapPosItems;
                if(mapItems != null && mapItems.size()>position)
                {
                    //更换位置点
                    String name=mapItems.get(position).MarkName;
                    Log.d(TAG, "onItemClick: " + name);

                    float x=mapItems.get(position).x;
                    float y=mapItems.get(position).y;
                    float z=mapItems.get(position).z;
                    float w=mapItems.get(position).w;

                    byte[] valueX= mapItems.get(position).valueX;
                    byte[] valueY= mapItems.get(position).valueY;
                    byte[] valueZ= mapItems.get(position).valueZ;
                    byte[] valueW= mapItems.get(position).valueW;
                    CarUtils.getInstance().SendMovePos(
                            x,y,z,w,
                            valueX,valueY,valueZ,valueW);
                }
            }
        });

        ThreadLoopUtils.getInstance().Init();


        tv=findViewById(R.id.testcar_tv_info);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv.setText("");
            }
        });
        tv_Show=findViewById(R.id.testcar_tv_show);

        handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                try{
                    switch (msg.what){
                        case flag_info:
                            String data=msg.obj.toString();
                            data=tv.getText().toString()+"\r\n"+data;
                            tv.setText(data);
                            break;
                        case flag_infoShow:
                            tv_Show.setText(msg.obj.toString());
                            break;
                        case flag_map:
                            SetMapListView();
                            break;
                        case flag_mapPos:
                            SetMapPosListView();
                            break;
                    }
                }catch (Exception e){}
            }
        };


        CarUtils.getInstance().Init("ttysWK1",115200,handler);
        CarPlanUtils.getInstance().NetWorkGetMapIdList();

        //CarPlanUtils.getInstance().NetWorkGetMapInfoList(10);
    }

    public void SetMapListView(){
        //地图
        List<String> List_Name=new ArrayList<>();
        for (int i = 0; i <CarPlanUtils.getInstance().mapItems.size(); i++) {
            String Name=CarPlanUtils.getInstance().mapItems.get(i).name;
//            String[] list=Name.split("-");
//            String mapName=list[0]+"-"+list[1];
            List_Name.add(Name);
        }
        //初始化选框
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(CarTestActivity.this,
                android.R.layout.simple_spinner_item, List_Name);
        dataAdapter.setDropDownViewResource(R.layout.str_dropdown);
        lv_map.setAdapter(dataAdapter);
    }

    public void SetMapPosListView(){
        //位置点
        List<String> List_PosName=new ArrayList<>();
        for (int i = 0; i <CarPlanUtils.getInstance().mapPosItems.size(); i++) {
            List_PosName.add(CarPlanUtils.getInstance().mapPosItems.get(i).MarkName);
        }
        //初始化选框
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(CarTestActivity.this,
                android.R.layout.simple_spinner_item, List_PosName);
        dataAdapter.setDropDownViewResource(R.layout.str_dropdown);
        lv_mapPos.setAdapter(dataAdapter);
    }

    public void GoFront(View v) {
        CarUtils.getInstance().SendGoFront();
    }

    public void GoFollow(View v) {
        CarUtils.getInstance().SendFollow();
        //CarUtils.getInstance().SendGetCarPos();
    }

    public void StopMove(View v) {
        CarUtils.getInstance().SendStop();
    }

    public void PlanA(View v) {
        CarUtils.getInstance().SendMovePos3();
    }

    public void PlanB(View v) {
        CarUtils.getInstance().SendMovePos4();
    }

    public void PlanC(View v) {
        byte[] valueX=new byte[]{(byte)0x3F,(byte)0xA5,(byte)0x3F,(byte)0x7C};
        byte[] valueY=new byte[]{(byte)0x3E,(byte)0x0C,(byte)0x49,(byte)0xBA};
        byte[] valueA=new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        byte[] valueW=new byte[]{(byte)0x3F,(byte)0x80,(byte)0x00,(byte)0x00};
        CarUtils.getInstance().SendMovePos(0,0,0,0,valueX,valueY,valueA,valueW);
    }


}
