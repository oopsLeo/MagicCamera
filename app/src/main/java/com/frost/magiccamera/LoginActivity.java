package com.frost.magiccamera;


import android.annotation.SuppressLint;
import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.frost.magiccamera.http.HttpHelper;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.thekhaeng.pushdownanim.PushDownAnim;


public class LoginActivity extends AppCompatActivity {

    ImageView logo;

    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private Button mLoginButton;
    private EditText suUsernameEdit;
    private EditText suEmailEdit;
    private EditText suPasswordEdit;
    private TextView mSignUp;
    private ExoPlayer mPlayer;
    private StyledPlayerView mVideoView;

    private LinearLayout bottomSheet;
    private LinearLayout bottomRubric;
    private BottomSheetBehavior bottomSheetBehavior;


    private boolean isOpenSheet = false;

    private long lastBackPressedTime = 0;
    private static final int MAX_BACK_PRESSED_INTERVAL = 2000; // 两次返回键的最大时间间隔

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //ID获取
        getAllID();
        //绑定事件
        setListener();
        //初始化背景
        try {
            initExo();
        } catch (RawResourceDataSource.RawResourceDataSourceException e) {
            // 处理异常
            e.printStackTrace();
        }
        //初始化注册界面
        initSignUp();

    }

    private void getAllID() {

        logo = findViewById(R.id.logo);
        mUsernameEditText = findViewById(R.id.et_username);
        mPasswordEditText = findViewById(R.id.et_password);
        mLoginButton = findViewById(R.id.btn_login);
        mSignUp = findViewById(R.id.sign_up);
        bottomRubric = findViewById(R.id.bottom_rubric);
        suUsernameEdit = findViewById(R.id.et_signup_username);
        suEmailEdit = findViewById(R.id.et_signup_email);
        suPasswordEdit = findViewById(R.id.et_signup_password);
        mVideoView = findViewById(R.id.player_view);

    }
    private void setListener() {

        PushDownAnim.setPushDownAnimTo(mLoginButton);
        PushDownAnim.setPushDownAnimTo(mSignUp);
        PushDownAnim.setPushDownAnimTo(mUsernameEditText);
        PushDownAnim.setPushDownAnimTo(mPasswordEditText);
        PushDownAnim.setPushDownAnimTo(logo);

        //登录按钮事件绑定
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = mUsernameEditText.getText().toString();
                String password = mPasswordEditText.getText().toString();
                new HttpHelper.LoginTask(username, password, new HttpHelper.OnLoginResultListener() {
                    @Override
                    public void onLoginSuccess() {

                        // 登录成功，跳转到主界面
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        // 在用户成功登录后，将登录状态保存到 SharedPreferences 中
                        SharedPreferences sharedPref = getSharedPreferences("login_state", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("username", username); // 将用户名保存到SharedPreferences
                        editor.putBoolean("is_login", true);// 将登录状态保存到SharedPreferences
                        editor.apply();
                        finish();
                    }

                    @Override
                    public void onLoginFailure(String error) {

                        Toast.makeText(getApplicationContext(), error,Toast.LENGTH_SHORT).show();
                        Animation shakeRed = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.btn_shake_red);
                        mLoginButton.startAnimation(shakeRed);
                    }
                });

            }
        });

        //登陆界面的底部注册事件
        mSignUp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                //展开BS
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                suUsernameEdit.setText("");
                suEmailEdit.setText("");
                suPasswordEdit.setText("");

            }

        });

    }


    private void initExo() throws RawResourceDataSource.RawResourceDataSourceException {

        // 创建RawResourceDataSource
        RawResourceDataSource rawDataSource = new RawResourceDataSource(this);
        // 设置数据源的ID
        DataSpec dataSpec = new DataSpec(RawResourceDataSource.buildRawResourceUri(R.raw.bg_login));
        // 打开数据源
        rawDataSource.open(dataSpec);

        // 创建MediaSource
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(
                new DefaultDataSourceFactory(this, "MyApp")
        ).createMediaSource(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(R.raw.bg_login)));

        mPlayer = new ExoPlayer.Builder(this).build();
        mVideoView.setPlayer(mPlayer);
        mVideoView.setUseController(false);

        mPlayer.prepare(mediaSource);
        mPlayer.setRepeatMode(mPlayer.REPEAT_MODE_ALL);

        mPlayer.play();
    }


    @SuppressLint({"WrongViewCast", "ClickableViewAccessibility"})
    private void initSignUp() {

        // 初始化BottomSheet对象
        bottomSheet = findViewById(R.id.bottomsheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);


        /**
         EditText失去焦点收回键盘
         点击其他区域收回BS
         */
        ConstraintLayout suLayout = findViewById(R.id.login_content);
        suLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // 如果用户在BottomSheet外部触摸屏幕，就隐藏BottomSheet
                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED || bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        Rect outRect = new Rect();
                        bottomSheet.getGlobalVisibleRect(outRect);

                        if (isKeyboardActive()) {
                            hideKeyboard();
                            view.performClick();
                            return false;
                        }

                        if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                            return true;
                        }
                    }
                }
                hideKeyboard();
                return false;
            }
        });

        /**
         注册按钮事件绑定
         */
        Button btn_signup = findViewById(R.id.btn_signup);
        btn_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String username = suUsernameEdit.getText().toString();
                String email = suEmailEdit.getText().toString();
                String password = suPasswordEdit.getText().toString();



                //合法校验
                if(SignUpValid(username,email,password)){

                    new HttpHelper.SignUpTask(username, email, password, new HttpHelper.OnSignUpResultListener() {
                        @Override
                        public void onSignUpResponse(String response,boolean isSuccess) {

                            Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show();
                            if(isSuccess){
                                SignUpAutoInput(view);
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                                mUsernameEditText.clearFocus();
                            }
                        }
                    });
                }

            }


        });


        /**
         BottomSheet状态回调
         */
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        isOpenSheet = false;
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        isOpenSheet = true;
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View view, float slideOffset) {
                animView(slideOffset);
            }
        });
    }

    /**
     注册表单合法校验
     */
    private boolean SignUpValid(String username, String email, String password){


        if (TextUtils.isEmpty(username)) {
            suUsernameEdit.setError("Please enter your username");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            suEmailEdit.setError("Please enter your email");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            suPasswordEdit.setError("Please enter your password");
            return false;
        }

        //邮箱格式校验
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            suEmailEdit.setError("Incorrect email format");
            return false;
        }

        //密码长度校验
        if (password.length() < 6) {
            suPasswordEdit.setError("Password length must be at least 6 characters");
            return false;
        }

        return true;
    }

    /**
     * 注册完成信息自动填入
     */
    public void SignUpAutoInput(View view) {
        // 获取用户名
        String username = suUsernameEdit.getText().toString();
        // 传递用户名给登录页面
        mUsernameEditText.setText(username);
    }

    /**
     * 登录随BS滑动
     */
    private void animView(float offsetY) {
        // 计算屏幕高度
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels/2;
        int logoHeight = logo.getHeight();
        int textHeight = mUsernameEditText.getHeight();
        Log.i("TAG", "textHeight: "+ textHeight);

        float directLogoY = -logoHeight * (1 + offsetY);
        float directViewY = screenHeight * (1 + offsetY);

        logo.setTranslationY(directLogoY);

        mUsernameEditText.setTranslationY(directViewY);
        mPasswordEditText.setTranslationY(directViewY);
        mLoginButton.setTranslationY(directViewY);
        bottomRubric.setTranslationY(directViewY);

    }

    /**
     * 键盘是否展开
     */
    private boolean isKeyboardActive() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            View view = getCurrentFocus();
            return view != null;
        }
        return false;
    }

    /**
     * 隐藏键盘
     */
    private void hideKeyboard() {
        // 判断软件盘是否显示
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            // 然后再隐藏软键盘
            View view = getCurrentFocus();
            if (view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                view.clearFocus();
            }
        }
    }

    /**
     * 手势返回
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN && !isOpenSheet) {
            long currentBackPressedTime = System.currentTimeMillis();
            if (currentBackPressedTime - lastBackPressedTime < MAX_BACK_PRESSED_INTERVAL) {
                super.onBackPressed(); // 执行返回操作
            } else {
                Toast.makeText(this, "再按一次退出应用", Toast.LENGTH_SHORT).show();
                lastBackPressedTime = currentBackPressedTime;
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK && isOpenSheet) {
            //返回键
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mPlayer.setPlayWhenReady(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.setPlayWhenReady(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.release();
    }
}
