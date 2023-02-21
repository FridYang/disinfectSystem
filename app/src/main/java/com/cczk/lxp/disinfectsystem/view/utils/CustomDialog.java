package com.cczk.lxp.disinfectsystem.view.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cczk.lxp.disinfectsystem.R;

public class CustomDialog extends Dialog {
    private CustomDialog(Context context) {
        super(context);
    }

    private CustomDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    private CustomDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    /* Builder */
    public static class Builder {
        private TextView tvTitle;
        private Button btnCancel, btnConfirm;
        private ImageView imgCancel;

        private View mLayout;
        private View.OnClickListener mButtonCancelClickListener;
        private View.OnClickListener mButtonConfirmClickListener;

        private CustomDialog mDialog;

        public Builder(Context context) {
            mDialog = new CustomDialog(context, R.style.custom_dialog);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            // 加载布局文件
            mLayout = inflater.inflate(R.layout.dialog_custom, null, false);
            // 添加布局文件到 Dialog
            mDialog.addContentView(mLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            tvTitle = (TextView) mLayout.findViewById(R.id.dialog_tv_title);
            btnConfirm = (Button) mLayout.findViewById(R.id.dialog_btn_confirm);
            btnCancel = (Button) mLayout.findViewById(R.id.dialog_btn_cancel);
            imgCancel = (ImageView) mLayout.findViewById(R.id.dialog_img_cancel);
        }

        /**
         * 设置 Dialog 标题
         */
        public Builder setTitle(String title) {
            tvTitle.setText(title);
            tvTitle.setVisibility(View.VISIBLE);
            return this;
        }

        //隐藏按钮
        public void hideButtonCancel()
        {
            btnCancel.setVisibility(View.GONE);
        }
        public void hideButtonConfirm()
        {
            btnConfirm.setVisibility(View.GONE);
        }

        /**
         * 设置取消按钮文字和监听
         */
        public Builder setButtonCancel(String text, View.OnClickListener listener) {
            btnCancel.setText(text);
            mButtonCancelClickListener = listener;
            return this;
        }

        /**
         * 设置确认按钮文字和监听
         */
        public Builder setButtonConfirm(String text, View.OnClickListener listener) {
            btnConfirm.setText(text);
            mButtonConfirmClickListener = listener;
            return this;
        }

        public void dismiss()
        {
            if(mDialog!=null)
            {
                mDialog.dismiss();
            }
        }

        public CustomDialog create() {
            btnCancel.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDialog.dismiss();
                    if(mButtonCancelClickListener!=null)
                    {
                        mButtonCancelClickListener.onClick(view);
                    }
                }
            });
            imgCancel.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDialog.dismiss();
                    if(mButtonCancelClickListener!=null)
                    {
                        mButtonCancelClickListener.onClick(view);
                    }
                }
            });

            btnConfirm.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDialog.dismiss();
                    if(mButtonConfirmClickListener!=null)
                    {
                        mButtonConfirmClickListener.onClick(view);
                    }
                }
            });

            mDialog.setContentView(mLayout);
            mDialog.setCancelable(true);
            mDialog.setCanceledOnTouchOutside(false);
            return mDialog;
        }
    }
}