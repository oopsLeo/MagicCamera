package com.frost.magiccamera.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.frost.magiccamera.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

public class ImageLoader {
    int targetWidth = 300;
    int targetHeight = 0; // 0表示根据长宽比自动计算高度
    public void loadImage(Uri imageUri, ImageView imageView) {


        Picasso.get()
                .load(imageUri)
                .placeholder(R.drawable.placeholder)
                .resize(targetWidth, targetHeight)
                .into(imageView);

    }

}
