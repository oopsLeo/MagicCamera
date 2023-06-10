package com.frost.magiccamera.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.dd.CircularProgressButton;

import at.markushi.ui.CircleButton;

public class AsyncImageSave extends AsyncTask {

    private Context mContext;

    private CircularProgressButton mButton;

    private ImageView mImageView;

    public AsyncImageSave (Context context, ImageView cardImage, CircularProgressButton btn_save){
        mContext = context;
        mImageView = cardImage;
        mButton = btn_save;
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        // 在后台线程中执行保存图片任务
        Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
        String filename = System.currentTimeMillis() + ".jpeg";
        Uri uri = ImageExtKt.saveToAlbum(bitmap,mContext,filename,"MagicCamera",75);

        if (uri != null) {
            mButton.setProgress(100);
            mButton.setClickable(false);
        } else {
            mButton.setProgress(-1);
        }
        return null;

    }
}
