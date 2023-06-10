package com.frost.magiccamera;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.cc.library.BaseSmartDialog;
import com.cc.library.BindViewListener;
import com.cc.library.SmartDialog;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemChildClickListener;
import com.dd.CircularProgressButton;
import com.frost.magiccamera.bean.AlbumBean;
import com.frost.magiccamera.adapter.ImageAdapter;
import com.frost.magiccamera.http.HttpHelper;
import com.frost.magiccamera.utils.AsyncImageSave;
import com.frost.magiccamera.widgets.MyHeaderView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.material.appbar.AppBarLayout;
import com.mxn.soul.flowingdrawer_core.ElasticDrawer;
import com.mxn.soul.flowingdrawer_core.FlowingDrawer;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import at.markushi.ui.CircleButton;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

public class MainActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {
    private Context mContext;
    private View mParentView;
    protected RecyclerView rvContent;
    protected FlowingDrawer mDrawer;
    protected MyHeaderView toolbarMyHeaderView;
    protected AppBarLayout appBarLayout;
    protected Toolbar toolbar;
    protected SwipeRefreshLayout mSwipeRefreshLayout;
    protected BlurView mBlurView;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private FloatingActionsMenu fabMenu;
    private FloatingActionButton fabGallery;
    private FloatingActionButton fabCamera;
    private ImageAdapter imageAdapter;
    private int mPosition;
    private boolean isHideToolbarView = false;

    private String username;

    // 封装数据源
    private List<AlbumBean> images = new ArrayList<AlbumBean>();
    private long lastBackPressedTime = 0;
    private static final int MAX_BACK_PRESSED_INTERVAL = 2000; // 两次返回键的最大时间间隔

    private boolean isLoading;  // 是否正在加载数据
    private boolean popupIsOpen = false;  // 是否正在加载数据

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_main);
        mContext = this;
        mParentView = ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);

        initAll();
        freeUp();
        initReceiver();
        initUser();

        mSwipeRefreshLayout.setRefreshing(true);

        //images.add(new AlbumBean(AlbumBean.IMAGE, Uri.parse("http://47.98.63.242/images/admin/ae2757f9-f3dd-4a68-a83f-f3fcb4f1a8e6.jpg")));
         /*for (int i = 0; i < 3; i++) {
            images.add(new AlbumBean(AlbumBean.IMAGE, Uri.parse("https://pica.zhimg.com/80/v2-a264bd68f98030a1b47c6fa1f7d1e1c4_720w.webp?source=1940ef5c")));
            images.add(new AlbumBean(AlbumBean.IMAGE, Uri.parse("https://pic3.zhimg.com/80/v2-d8e1c695f6f11a6ba6e1d9c465a541c6_720w.webp")));
            images.add(new AlbumBean(AlbumBean.IMAGE, Uri.parse("https://pic4.zhimg.com/80/v2-7db75659e0d54bac8dd5d79adf4011f3_720w.webp")));
            images.add(new AlbumBean(AlbumBean.IMAGE, Uri.parse("https://picx.zhimg.com/80/v2-8393b5677528d221966b9dc7f100ddf0_720w.webp?source=1940ef5c")));
            images.add(new AlbumBean(AlbumBean.IMAGE, Uri.parse("https://pic4.zhimg.com/80/v2-ac20cbd32d97d1803a0d703e66db2d33_720w.webp")));

        }*/


        initToolBar();

        setDrawer();

        setContent();

        initRefresh();

        initFab();

    }

    private void getImages() {
        mSwipeRefreshLayout.setRefreshing(true);
        new HttpHelper.getImages(username, new HttpHelper.OnImageResultListener() {
            @Override
            public void onImageResponse(List<String> urls) {
                if(urls!=null) {
                    images.clear();
                    // 对 urls 进行进一步操作和处理
                    for (String url : urls) {
                        images.add(new AlbumBean(AlbumBean.IMAGE, Uri.parse(url)));
                    }
                    imageAdapter.notifyItemRangeChanged(0, images.size());
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }


    //初始化菜单和主界面
    private void initUser() {
        //获取SharedPreferences对象
        SharedPreferences sharedPreferences = getSharedPreferences("login_state", Context.MODE_PRIVATE);
        // 获取用户名，默认为空字符串
        username = sharedPreferences.getString("username", "");
        new HttpHelper.UserTask(username, new HttpHelper.OnUserResultListener() {
            @Override
            public void onUserResponseSuccess(String email) {
                TextView profileName = findViewById(R.id.profile_name);
                profileName.setText(username);
                TextView profileEmail1 = findViewById(R.id.profile_email);
                profileEmail1.setText(email);
            }

            @Override
            public void onUserResponseError(String message) {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initFab() {
        // Registers a photo picker activity launcher in single-select mode.
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {

            // photo picker.
            if (uri != null) {
                //new HttpHelper.upload(username, mContext, uri);
                Intent intent = new Intent(mContext, EditActivity.class);
                intent.setData(uri);
                startActivity(intent);
            } else {
                Log.d("PhotoPicker", "No media selected");
            }
        });

        fabGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }
        });

        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });


    }

    private void initAll() {
        mBlurView = findViewById(R.id.blurView);
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appbar);
        toolbarMyHeaderView = findViewById(R.id.toolbar_header_view);
        mDrawer = findViewById(R.id.drawerlayout);
        rvContent = findViewById(R.id.rvContent);
        fabMenu = findViewById(R.id.fab_menu);
        fabGallery = findViewById(R.id.fab_gallery);
        fabCamera = findViewById(R.id.fab_cam);
    }

    /**
     * 初始化滑动ToolBar
     */
    private void initToolBar() {

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawer.openMenu();
            }
        });

        appBarLayout.addOnOffsetChangedListener(this);
        toolbarMyHeaderView.bindTo("MagicCamera");

//        toolbarHeaderView.bindTo("MagicCamera", "Last seen today at 7.00PM");
//        floatHeaderView.bindTo("Larry Page", "Last seen today at 7.00PM");
    }

    /**
     * 初始化侧边栏
     */
    private void setDrawer() {

        //Drawer
        mDrawer.setTouchMode(ElasticDrawer.TOUCH_MODE_FULLSCREEN);

        TextView nav_logout = findViewById(R.id.nav_logout);
        nav_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 在退出登录时，清除保存在 SharedPreferences 中的登录状态
                SharedPreferences sharedPref = getSharedPreferences("login_state", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.remove("is_login"); // 移除登录状态
                editor.remove("username"); // 移除用户名
                editor.apply();

                // 跳转到登录界面
                Intent intent = new Intent(mContext, LoginActivity.class);
                startActivity(intent);
                finish();

            }
        });


        mDrawer.setOnDrawerStateChangeListener(new ElasticDrawer.OnDrawerStateChangeListener() {
            @Override
            public void onDrawerStateChange(int oldState, int newState) {
//                if (newState == ElasticDrawer.STATE_CLOSED) {
//                    Log.i("MainActivity", "Drawer STATE_CLOSED");
//                }
            }

            @Override
            public void onDrawerSlide(float openRatio, int offsetPixels) {
//                Log.i("MainActivity", "openRatio=" + openRatio + " ,offsetPixels=" + offsetPixels);
            }
        });


    }

    /**
     * 初始化RecyclerView
     */
    private void setContent() {

        //模糊
        final Drawable windowBackground = getWindow().getDecorView().getBackground();
        mBlurView.setupWith(findViewById(R.id.content), new RenderScriptBlur(this))
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(25f);

        getImages();
        //RecyclerView布局
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        rvContent.setLayoutManager(layoutManager);
        imageAdapter = new ImageAdapter(images);
        rvContent.setAdapter(imageAdapter);
        imageAdapter.setEmptyView(R.layout.item_empty);


        // 设置点击事件
        // 先注册需要点击的子控件id
        imageAdapter.addChildClickViewIds(R.id.iv, R.id.btn_delete);

        // 设置子控件点击监听
        imageAdapter.setOnItemChildClickListener(new OnItemChildClickListener() {
            @Override
            public void onItemChildClick(@NonNull BaseQuickAdapter adapter, @NonNull View view, int position) {
                if (fabMenu.isExpanded()) {
                    fabMenu.collapse();
                } else {
                    if (!popupIsOpen) {
                        mPosition = position;
                        if (view.getId() == R.id.iv) {
                            popupPhoto(images.get(position).uri);
                            popupIsOpen = true;
                        }
                        if (view.getId() == R.id.btn_delete) {
                            new SmartDialog().init(mContext).layoutRes(R.layout.dialog_layout)
                                    .backgroundRes(R.drawable.bg_dialog_radius)
                                    // 为自定义布局的子控件设监听
                                    .bindViewListener(new BindViewListener() {
                                        @Override
                                        public void bind(View dialogView, final BaseSmartDialog dialog) {
                                            dialogView.findViewById(R.id.dialog_btn_confirmed).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    dialog.cancel();
                                                    deleteItem(imageAdapter, mPosition, images.get(position).uri);
                                                }
                                            });

                                            dialogView.findViewById(R.id.dialog_btn_cancel).setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    dialog.cancel();
                                                }
                                            });
                                        }
                                    })
                                    .display();
                        }
                    }
                }
            }
        });


        rvContent.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != 0 && fabMenu.isExpanded()) {
                    fabMenu.collapse();
                }
            }
        });
    }

    /**
     * 图片详情卡片
     */
    public void popupPhoto(Uri uri) {

        View popupView = getLayoutInflater().inflate(R.layout.popup_card, null);
        PopupWindow mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setAnimationStyle(R.style.PopupAnimation);

        // 在PopupWindow中设置背景颜色
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 为背景颜色设置点击事件
        mPopupWindow.getContentView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 点击背景后关闭PopupWindow
                mPopupWindow.dismiss();
            }
        });


        CircleButton btn_delete = popupView.findViewById(R.id.popup_delete);
        CircleButton btn_edit = popupView.findViewById(R.id.popup_edit);
        CircleButton btn_exit = popupView.findViewById(R.id.popup_exit);
        ImageView cardImage = popupView.findViewById(R.id.card_image);
        CircularProgressButton btn_save = popupView.findViewById(R.id.popup_save);

        /*
         * AI会话
         * */
        cardImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AIActivity.class);
                intent.setData(uri);
                startActivity(intent);
                mPopupWindow.dismiss();
            }
        });


        /*
         * 使用Picasso加载图片
         * */
        try {
            Picasso.get().load(uri).into(cardImage, new Callback() {
                @Override
                public void onSuccess() {

                    fabMenu.setVisibility(View.GONE);

                    // 获取图片的宽高比例
                    float aspectRatio = (float) cardImage.getDrawable().getIntrinsicWidth() / (float) cardImage.getDrawable().getIntrinsicHeight();

                    // 计算在PopupWindow中应该显示的大小
                    int maxWidth = getResources().getDisplayMetrics().widthPixels - 200;
                    int maxHeight = getResources().getDisplayMetrics().heightPixels - 900;
                    int width = Math.min(maxWidth, (int) (maxHeight * aspectRatio));
                    int height = Math.min(maxHeight, (int) (maxWidth / aspectRatio));

                    // 设置ImageView的LayoutParams
                    cardImage.setLayoutParams(new LinearLayout.LayoutParams(width, height));
                    cardImage.setClipToOutline(true);

                    // 显示PopupWindow
                    mPopupWindow.showAtLocation(mParentView, Gravity.CENTER, 0, 0);
                    //模糊效果
                    startFadeAnim(mBlurView, true, 500);
                }

                @Override
                public void onError(Exception e) {
                    // 加载图片出错的处理
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Loading", Toast.LENGTH_SHORT).show();
            Log.e("TAG", e.getMessage());
        }

        /*
         * 删除按钮
         * */
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new SmartDialog().init(mContext).layoutRes(R.layout.dialog_layout)
                        .backgroundRes(R.drawable.bg_dialog_radius)
                        // 为自定义布局的子控件设监听
                        .bindViewListener(new BindViewListener() {
                            @Override
                            public void bind(View dialogView, final BaseSmartDialog dialog) {
                                dialogView.findViewById(R.id.dialog_btn_confirmed).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        dialog.cancel();
                                        mPopupWindow.dismiss();
                                        deleteItem(imageAdapter, mPosition, uri);
                                    }
                                });

                                dialogView.findViewById(R.id.dialog_btn_cancel).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        dialog.cancel();
                                    }
                                });
                            }
                        })
                        .display();

            }
        });

        /*
         * 编辑按钮
         * */
        btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, EditActivity.class);
                intent.setData(uri);
                startActivity(intent);
                mPopupWindow.dismiss();

            }
        });

        /*
         * 保存按钮
         * */
        btn_save.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onClick(View view) {

                btn_save.setIndeterminateProgressMode(true); //设置为不确定进度模式
                btn_save.setProgress(1);

                AsyncImageSave asyncImageSave = new AsyncImageSave(mContext, cardImage, btn_save);
                asyncImageSave.execute();

            }
        });

        /*
         * 退出按钮
         * */
        btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupWindow.dismiss();
            }
        });

        /*
         * 监听window退出
         * */
        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                //关闭模糊
                startFadeAnim(mBlurView, false, 500);
                fabMenu.setVisibility(View.VISIBLE);
                popupIsOpen = false;
            }
        });

    }

    /**
     * 渐入渐出
     */
    private void startFadeAnim(View view, boolean isFadeIn, int duration) {
        ObjectAnimator animator = null;
        if (isFadeIn) {
            animator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);

        } else {
            animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        }
        animator.setDuration(duration); // 设置动画持续时间，单位为毫秒
        animator.start(); // 启动动画
    }

    /**
     * 刷新事件
     */
    private void initRefresh() {
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getImages();
            }
        });
    }

    /**
     * 删除图片的复用
     */
    private void deleteItem(ImageAdapter mAdapter, int position, Uri uri) {

        new HttpHelper.delete(uri.toString());

        images.remove(position);
        mAdapter.notifyItemRemoved(position);
    }

    /**
     * ToolBar滑动优化
     */
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(offset) / (float) maxScroll;

        if (percentage == 1f && isHideToolbarView) {
            toolbarMyHeaderView.setVisibility(View.VISIBLE);
            isHideToolbarView = !isHideToolbarView;
        } else if (percentage < 1f && !isHideToolbarView) {
            toolbarMyHeaderView.setVisibility(View.GONE);
            isHideToolbarView = !isHideToolbarView;
        }
    }

    /**
     * 返回事件监听判断
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            long currentBackPressedTime = System.currentTimeMillis();
            if (currentBackPressedTime - lastBackPressedTime < MAX_BACK_PRESSED_INTERVAL) {
                super.onBackPressed(); // 执行返回操作
            } else {
                Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show();
                lastBackPressedTime = currentBackPressedTime;
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 清理缓存
     */
    private void freeUp() {
        File tempDir = new File(getFilesDir(), "temp");
        File[] files = tempDir.listFiles();

        try {
            for (File file : files) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // 处理接收到的广播
            if (intent.getAction().equals("UPDATE_IMAGES")) {
                // 调用需要执行的函数
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("TAG", "run: getImages");
                        getImages();
                    }
                },1000);
            }
        }
    };

    private void initReceiver(){
        // 注册广播接收器
        IntentFilter filter = new IntentFilter("UPDATE_IMAGES");
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        unregisterReceiver(receiver);
    }


}