package com.frost.magiccamera.bean;

import android.net.Uri;

import com.chad.library.adapter.base.entity.MultiItemEntity;

public class AlbumBean implements MultiItemEntity{

    public static final int IMAGE = 0;
    public static final int VIDEO = 1;
    public static final int BUTTON = 2;
    public int itemType;
    public Uri uri;

    public AlbumBean(int type) {
        this.itemType = type;
    }
    public AlbumBean(int type, Uri uri) {
        this.itemType = type;
        this.uri = uri;
    }

    @Override
    public int getItemType() {
        return itemType;
    }
}
