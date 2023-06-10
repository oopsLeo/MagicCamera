package com.frost.magiccamera.http;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;


import com.frost.magiccamera.bean.StyleBean;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cn.zhxu.okhttps.HTTP;
import cn.zhxu.okhttps.HttpResult;

public class HttpHelper {
    //private static final String URL = "http://47.98.63.242";
    private static final String URL = "http://118.31.113.224";
    private static final HTTP http = HTTP.builder()
            .baseUrl(URL)    // 设置 BaseUrl
            .callbackExecutor((Runnable run) -> {
                // 实际编码中可以吧 Handler 提出来，不需要每次执行回调都重新创建
                new Handler(Looper.getMainLooper()).post(run); // 在主线程执行
            })
            .build();

    public static class LoginTask {
        public LoginTask(String username, String password, OnLoginResultListener listener) {


            http.async("/login")
                    .addBodyPara("username", username)
                    .addBodyPara("password", password)
                    .setOnResponse((HttpResult result) -> {
                        // 登录是否成功
                        Log.d("TAG", "result: " + result);
                        if (result.getStatus() == 200) {
                            // 登录成功
                            listener.onLoginSuccess();
                        } else if (result.getStatus() == 400) {
                            // 登录失败
                            listener.onLoginFailure("Please enter your username and password");
                        } else if (result.getStatus() == 401) {
                            listener.onLoginFailure("Incorrect username or password");
                        } else {
                            listener.onLoginFailure("Something went wrong,please try again");
                        }

                    })
                    .post();

        }
    }

    public static class SignUpTask {

        public SignUpTask(String username, String email, String password, OnSignUpResultListener listener) {
            http.async("/register")
                    .addBodyPara("username", username)
                    .addBodyPara("email", email)
                    .addBodyPara("password", password)
                    .setOnResponse((HttpResult result) -> {
                        // 登录是否成功
                        if (result.getStatus() == 200) {
                            listener.onSignUpResponse("Sign up success", true);
                        } else if (result.getStatus() == 400) {
                            // 登录失败
                            listener.onSignUpResponse("Please fill in all information", false);
                        } else if (result.getStatus() == 401) {
                            listener.onSignUpResponse("Username has been duplicated", false);
                        } else {
                            listener.onSignUpResponse("Something went wrong,please try again", false);
                        }

                    })
                    .post();
        }

    }

    public static class UserTask {
        public UserTask(String username, OnUserResultListener listener) {
            http.async("/user")
                    .addBodyPara("username", username)
                    .setOnResString((String str) -> {
                        // 得到响应报文体的字符串 String 对象
                        try {
                            Gson gson = new Gson();
                            JsonObject jsonObject = gson.fromJson(str, JsonObject.class);
                            String email = jsonObject.get("email").getAsString();
                            listener.onUserResponseSuccess(email);
                        } catch (Exception e) {
                            // 处理异常情况
                            listener.onUserResponseError("Something went wrong,please try again");
                        }
                    })
                    .post();
        }
    }

    public static class getImages {

        public getImages(String username, OnImageResultListener listener) {
            http.async("/images")
                    .addUrlPara("username", username)
                    .setOnResString((String str) -> {
                        // 得到响应报文体的字符串 String 对象
                        try {
                            Gson gson = new Gson();
                            JsonObject jsonObject = gson.fromJson(str, JsonObject.class);

                            JsonArray urlsArray = jsonObject.getAsJsonArray("urls");
                            if (urlsArray != null) {
                                List<String> urls = new ArrayList<>();
                                for (JsonElement element : urlsArray) {
                                    urls.add(element.getAsString());
                                }
                                listener.onImageResponse(urls);
                            } else {
                                listener.onImageResponse(null);
                            }

                        } catch (Exception e) {
                            // 处理异常情况
                        }
                    })
                    .get();
        }
    }

    public static class upload {
        public upload(String username, Context context, Uri uri) {

            // 获取 Uri 对应的文件路径
            String filePath = null;
            if (uri.getScheme().equals("file")) {
                filePath = uri.getPath();
            } else {
                String[] projection = {MediaStore.Images.Media.DATA};
                Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    filePath = cursor.getString(columnIndex);
                    cursor.close();
                }
            }
            // 创建 File 对象
            assert filePath != null;
            File file = new File(filePath);

            http.async("/upload")
                    .addBodyPara("username", username)
                    .addFilePara("file", file)
                    .post();
        }
    }

    public static class delete {
        public delete(String uri) {
            http.async("/delete")
                    .addBodyPara("url", uri)
                    .post();
        }
    }

    public static class generate {
        public generate(String style, String uri, OnGenerateResultListener listener) {
            http.async("/generate")
                    .addBodyPara("style", style)
                    .addBodyPara("url", uri)
                    .setOnResString((String str) -> {
                        // 得到响应报文体的字符串 String 对象
                        try {
                            Gson gson = new Gson();
                            JsonObject jsonObject = gson.fromJson(str, JsonObject.class);
                            String path = jsonObject.get("url").getAsString();
                            Uri image_uri = Uri.parse(URL + path);
                            listener.onGenerateResponse(image_uri);
                        } catch (Exception e) {
                            // 处理异常情况
                        }
                    })
                    .post();
        }
    }

    public static class getStyles {
        public getStyles(OnStyleResultListener listener) {
            http.async("/getStyles")
                    .setOnResString((String str) -> {
                        // 得到响应报文体的字符串 String 对象
                        try {
                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<StyleBean>>() {}.getType();
                            List<StyleBean> styleBeans = gson.fromJson(str, listType);

                            if (styleBeans != null) {
                                listener.onStyleResponse(styleBeans);
                            }
                            else {
                                listener.onStyleResponse(null);
                            }

                        } catch (Exception e) {
                            // 处理异常情况
                        }
                    })
                    .get();
        }
    }

    public interface OnStyleResultListener {
        void onStyleResponse(List<StyleBean> styles);

    }

    public interface OnGenerateResultListener {
        void onGenerateResponse(Uri uri);

    }

    public interface OnImageResultListener {
        void onImageResponse(List<String> urls);

    }

    public interface OnUserResultListener {
        void onUserResponseSuccess(String email);

        void onUserResponseError(String message);
    }

    public interface OnSignUpResultListener {
        void onSignUpResponse(String response, boolean isSuccess);
    }

    public interface OnLoginResultListener {
        void onLoginSuccess();

        void onLoginFailure(String error);
    }
}
