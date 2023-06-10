package com.frost.magiccamera.adapter;

import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.frost.magiccamera.R;
import com.frost.magiccamera.bean.StyleBean;
import com.frost.magiccamera.utils.ImageLoader;
import com.squareup.picasso.Picasso;

import java.util.List;

public class StyleAdapter extends BaseQuickAdapter<StyleBean, BaseViewHolder> {

    public StyleAdapter(int layoutResId) {
        super(layoutResId);
    }

    public StyleAdapter(@Nullable List<StyleBean> data) {
        super(R.layout.item_style, data);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, StyleBean styleBean) {
        ImageView iv = helper.getView(R.id.style_image);
        helper.setText(R.id.style_name, styleBean.style);
        Picasso.get().load(styleBean.url).placeholder(R.drawable.placeholder).fit().into(iv);
    }
}
