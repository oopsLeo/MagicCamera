package com.frost.magiccamera.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator;

import com.frost.magiccamera.R;
import com.frost.magiccamera.utils.PaddingUtils;

/*
 * Created by memfis on 7/6/16.
 * Updated by amadeu01 on 17/04/17
 */
public class RecordButton extends AppCompatImageButton {

    public interface RecordButtonListener {
        void onRecordButtonClicked();
    }

    private Drawable takePhotoDrawable;
    private Drawable startRecordDrawable;
    private Drawable stopRecordDrawable;
    private int iconPadding = 8;
    private int iconPaddingStop = 18;

    @Nullable
    private RecordButtonListener listener;

    public RecordButton(@NonNull Context context) {
        this(context, null, 0);
    }

    public RecordButton(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordButton(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        takePhotoDrawable = ContextCompat.getDrawable(context, R.drawable.take_photo_button);
        startRecordDrawable = ContextCompat.getDrawable(context, R.drawable.start_video_record_button);
        stopRecordDrawable = ContextCompat.getDrawable(context, R.drawable.stop_button_background);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            setBackground(ContextCompat.getDrawable(context, R.drawable.circle_frame_background));
        else
            setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.circle_frame_background));

        setOnClickListener(new OnClickListener() {
            private final static int CLICK_DELAY = 1000;

            private long lastClickTime = 0;

            @Override
            public void onClick(View view) {
                if (System.currentTimeMillis() - lastClickTime < CLICK_DELAY) {
                    return;
                } else lastClickTime = System.currentTimeMillis();

                if(listener != null) {
                    listener.onRecordButtonClicked();
                }
            }
        });
        setSoundEffectsEnabled(false);
        setIconPadding(iconPadding);
        initPhotoState();
    }

    public void initPhotoState() {
        setImageDrawable(takePhotoDrawable);
        setIconPadding(iconPadding);
    }

    private void setIconPadding(int paddingDP) {
        int padding = PaddingUtils.convertDipToPixels(getContext(), paddingDP);
        setPadding(padding, padding, padding, padding);
    }

    public void setRecordButtonListener(@NonNull RecordButtonListener listener) {
        this.listener = listener;
    }

    public void displayVideoRecordStateReady(){
        colorAnim(Color.parseColor("#fafafa"),Color.parseColor("#d32f2f"));
        setIconPadding(iconPadding);
    }

    public void displayVideoRecordStateInProgress(){
        setImageDrawable(stopRecordDrawable);
        setIconPadding(iconPaddingStop);
    }
    public void displayVideoRecordFinish() {
        setImageDrawable(startRecordDrawable);
        setIconPadding(iconPadding);
    }


    public void displayPhotoState(){
        colorAnim(Color.parseColor("#d32f2f"), Color.parseColor("#fafafa"));
        setIconPadding(iconPadding);
    }

    private void colorAnim(int startColor, int endColor){

        // 创建 ValueAnimator 对象
        ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);

        // 设置动画属性和时长
        colorAnimator.setDuration(500);

        // 在动画更新期间执行此操作
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int animatedValue = (int) animator.getAnimatedValue(); // 获取当前动画值（Int类型）
                setColorFilter(animatedValue, PorterDuff.Mode.SRC_ATOP); // 更新元素颜色
            }
        });

        // 启动动画对象
        colorAnimator.start();

    }

}
