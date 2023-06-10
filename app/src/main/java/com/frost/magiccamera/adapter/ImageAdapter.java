package com.frost.magiccamera.adapter;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.frost.magiccamera.bean.AlbumBean;
import com.frost.magiccamera.R;
import com.frost.magiccamera.utils.ImageLoader;
import java.util.List;


public class ImageAdapter extends BaseMultiItemQuickAdapter<AlbumBean, BaseViewHolder> {

    private final ImageLoader imageLoader;

    public ImageAdapter(List<AlbumBean> data) {
        super(data);
        // 绑定 layout 对应的 type
        addItemType(AlbumBean.IMAGE, R.layout.item_content);
        //addItemType(ImageBean.VIDEO, R.layout.item_image_view);

        imageLoader = new ImageLoader();
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, AlbumBean item) {

        int position = getItemPosition(item);
        // 根据返回的 type 分别设置数据
        switch (getItemViewType(position)) {

            case AlbumBean.IMAGE:
                imageLoader.loadImage(item.uri, helper.getView(R.id.iv));
                break;
            case AlbumBean.VIDEO:
               //TODO
                break;
        }
    }

}
