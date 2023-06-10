package com.frost.magiccamera.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.frost.magiccamera.R;


/*
 * Created by memfis on 7/6/16.
 * Updated by amadeu01 on 17/04/17
 */
public class FlashSwitchView extends AppCompatImageButton {

    private Drawable flashOnDrawable;
    private Drawable flashOffDrawable;
    private Drawable flashAutoDrawable;

    public FlashSwitchView(@NonNull Context context) {
        this(context, null);
    }

    public FlashSwitchView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        flashOnDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_on_white_24dp);
        flashOffDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_off_white_24dp);
        flashAutoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_auto_white_24dp);
        init();
    }

    private void init() {
        setBackgroundColor(Color.TRANSPARENT);
        displayFlashAuto();
    }

    public void displayFlashOff() {
        setImageDrawable(flashOffDrawable);
    }

    public void displayFlashOn() {
        setImageDrawable(flashOnDrawable);
    }

    public void displayFlashAuto() {
        setImageDrawable(flashAutoDrawable);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            if (enabled) {
                setAlpha(1f);
            } else {
                setAlpha(0.5f);
            }
        }
    }

}
