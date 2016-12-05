package com.junyou.hbks;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.junyou.hbks.luckydraw.AngleUtil;
import com.junyou.hbks.luckydraw.LuckyDrawLayout;
import com.junyou.hbks.luckydraw.RotatePlate;

public class LuckyDraw extends AppCompatActivity implements RotatePlate.AnimationEndListener{

    private RotatePlate mRotateP;
    private LuckyDrawLayout mLuckyDrawL;
    private ImageView mGoBtn;
    private String[] strs = {"华为手机","谢谢惠顾","iPhone 6s","mac book","魅族手机","小米手机"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lucky_draw);

        mLuckyDrawL = (LuckyDrawLayout) findViewById(R.id.lucky_draw_layout);
        mLuckyDrawL.startLuckLight();
//        mLuckyDrawL.setVisibility(View.INVISIBLE);
        mRotateP = (RotatePlate) findViewById(R.id.lucky_draw_plate_layout);
        mRotateP.setAnimationEndListener(this);

        mGoBtn = (ImageView)findViewById(R.id.go_rotate_btn);

        mLuckyDrawL.post(new Runnable() {
            @Override
            public void run() {
                int height =  getWindow().getDecorView().getHeight();
                int width = getWindow().getDecorView().getWidth();

                int backHeight = 0;

                int MinValue = Math.min(width,height);
                MinValue -= AngleUtil.dip2px(LuckyDraw.this,10)*2;
                backHeight = MinValue/2;

                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mLuckyDrawL.getLayoutParams();
                lp.width = MinValue;
                lp.height = MinValue;

                mLuckyDrawL.setLayoutParams(lp);

                MinValue -= AngleUtil.dip2px(LuckyDraw.this,28)*2;
                lp = (RelativeLayout.LayoutParams) mRotateP.getLayoutParams();
                lp.height = MinValue;
                lp.width = MinValue;

                mRotateP.setLayoutParams(lp);

                lp = (RelativeLayout.LayoutParams) mGoBtn.getLayoutParams();
                lp.topMargin += backHeight;
                lp.topMargin -= (mGoBtn.getHeight()/2);
                mGoBtn.setLayoutParams(lp);

                getWindow().getDecorView().requestLayout();
            }
        });
    }

    @Override
    public void endAnimation(int position) {
        mGoBtn.setEnabled(true);
        mLuckyDrawL.setDelayTime(500);
        Toast.makeText(this,"Position = "+position+","+strs[position],Toast.LENGTH_SHORT).show();
    }

    public void rotationClick(View view){
        Log.i("TAG","转...");
        mRotateP.startRotate(-1);
        mLuckyDrawL.setDelayTime(100);
        mGoBtn.setEnabled(false);
    }
}
