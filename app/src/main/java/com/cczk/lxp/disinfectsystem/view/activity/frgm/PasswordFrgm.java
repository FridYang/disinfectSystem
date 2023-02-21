package com.cczk.lxp.disinfectsystem.view.activity.frgm;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.R;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

/**
 * Created by pc on 2018/2/7.
 */
public class PasswordFrgm extends Fragment {
    private MainActivity activity;
    private static int SuccessFrgmFlag;
    //private static SettingsFrgm SuperFrgm;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.frgm_password, container, false);
        activity = (MainActivity) this.getActivity();
        InitView(v);
        return v;
    }

    public static void Init(int flag){
        //SuperFrgm=frgm;
        SuccessFrgmFlag=flag;
    }

    private void InitView(View v) {
        ImageView img =  v.findViewById(R.id.subpassword_img_exit);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //activity.AllBtnFrgmHome();
            }
        });

        TextView tv =  v.findViewById(R.id.subpassword_tv_btn12);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //SuperFrgm.ComeGoFrame(SuccessFrgmFlag);
            }
        });
    }

}

