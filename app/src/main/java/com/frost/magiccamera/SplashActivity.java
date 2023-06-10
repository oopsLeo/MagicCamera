package com.frost.magiccamera;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.dinuscxj.shootrefreshview.ShootRefreshView;
import com.royrodriguez.transitionbutton.TransitionButton;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 1000; //启动画面的展示时间

    private TransitionButton transitionButton;
    private ShootRefreshView mShootRefreshView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //获取SharedPreferences对象
        SharedPreferences sharedPreferences = getSharedPreferences("login_state", Context.MODE_PRIVATE);

        //判断用户是否已经登录
        boolean isLogin = sharedPreferences.getBoolean("is_login", false);
        Intent intent;
        if (isLogin) {
            //如果用户已经登录，跳转到主页面
            intent = new Intent(this, MainActivity.class);
        } else {
            //如果用户未登录，跳转到登录页面
            intent = new Intent(this, LoginActivity.class);
        }

        transitionButton = findViewById(R.id.transition_button);
        mShootRefreshView = findViewById(R.id.shoot_logo);

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1, 0);
        valueAnimator.setDuration(2000);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mShootRefreshView.pullProgress(0, value);
            }
        });

        //初始刷新状态
        mShootRefreshView.refreshing();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mShootRefreshView.reset();
                valueAnimator.start();
                transitionButton.performClick();
            }
        }, SPLASH_TIME_OUT);

        transitionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        boolean isSuccessful = true;
                        transitionButton.startAnimation();

                        if (isSuccessful) {
                            transitionButton.stopAnimation(TransitionButton.StopAnimationStyle.EXPAND, new TransitionButton.OnAnimationStopEndListener() {
                                @Override
                                public void onAnimationStopEnd() {
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        } else {
                            transitionButton.stopAnimation(TransitionButton.StopAnimationStyle.SHAKE, null);
                        }
                    }
                }, 2000);
            }
        });
    }


}