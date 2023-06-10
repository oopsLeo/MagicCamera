package com.frost.magiccamera.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.frost.magiccamera.R;
import com.frost.magiccamera.utils.PaddingUtils;


/*
 * Created by memfis on 6/24/16.
 * Updated by amadeu01 on 17/04/17
 */
public class MediaActionSwitchView extends AppCompatImageButton {

    @Nullable
    private OnMediaActionStateChangeListener onMediaActionStateChangeListener;

    public interface OnMediaActionStateChangeListener {
        void switchAction();
    }

    private Drawable photoDrawable;
    private Drawable videoDrawable;
    private int padding = 5;

    private boolean isPhoto = true;

    public MediaActionSwitchView(Context context) {
        this(context, null);
    }

    public MediaActionSwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView();
    }

    public MediaActionSwitchView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    private void initializeView() {
        Context context = getContext();

        photoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_photo_camera_white_24dp);
        photoDrawable = DrawableCompat.wrap(photoDrawable);
        DrawableCompat.setTintList(photoDrawable.mutate(), ContextCompat.getColorStateList(context, R.color.switch_camera_mode_selector));

        videoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_videocam_white_24dp);
        videoDrawable = DrawableCompat.wrap(videoDrawable);
        DrawableCompat.setTintList(videoDrawable.mutate(), ContextCompat.getColorStateList(context, R.color.switch_camera_mode_selector));

        setBackgroundResource(R.drawable.circle_frame_background_dark);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(onMediaActionStateChangeListener != null) {
                    onMediaActionStateChangeListener.switchAction();
                }
            }
        });

        padding = PaddingUtils.convertDipToPixels(context, padding);
        setPadding(padding, padding, padding, padding);

        displayActionWillSwitchVideo();
    }

    public void toggleMode(){
        if(isPhoto) displayActionWillSwitchPhoto();
        else displayActionWillSwitchVideo();
        isPhoto =  !isPhoto;
    }
    public void displayActionWillSwitchPhoto(){
        setImageDrawable(photoDrawable);
    }

    public void displayActionWillSwitchVideo(){
        setImageDrawable(videoDrawable);
    }

    public void setOnMediaActionStateChangeListener(@Nullable OnMediaActionStateChangeListener onMediaActionStateChangeListener) {
        this.onMediaActionStateChangeListener = onMediaActionStateChangeListener;
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
