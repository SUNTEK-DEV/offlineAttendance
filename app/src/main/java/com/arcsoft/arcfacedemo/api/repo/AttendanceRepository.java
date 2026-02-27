package com.arcsoft.arcfacedemo.api.repo;

import android.content.Context;
import android.util.Log;

import com.arcsoft.arcfacedemo.api.ApiConfig;
import com.arcsoft.arcfacedemo.api.interceptor.HmacAuthInterceptor;
import com.arcsoft.arcfacedemo.api.model.PunchRequest;
import com.arcsoft.arcfacedemo.api.model.PunchResponse;
import com.arcsoft.arcfacedemo.api.service.AttendanceApiService;
import com.arcsoft.arcfacedemo.util.HmacAuthUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 打卡 API 仓库
 * 单例模式
 */
public class AttendanceRepository {
    private static final String TAG = "AttendanceRepository";
    private static AttendanceRepository instance;

    private AttendanceApiService apiService;
    private Context context;

    private AttendanceRepository(Context context) {
        this.context = context.getApplicationContext();
        initRetrofit();
    }

    /**
     * 获取单例实例
     */
    public static synchronized AttendanceRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AttendanceRepository(context);
        }
        return instance;
    }

    /**
     * 初始化 Retrofit
     */
    private void initRetrofit() {
        // 日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
            Log.d(TAG, "API: " + message);
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 获取设备凭证
        String deviceId = ApiConfig.getDeviceId(context);
        String deviceSecret = ApiConfig.getDeviceSecret(context);

        // 创建 HMAC 认证工具
        HmacAuthUtil hmacAuthUtil = new HmacAuthUtil(deviceId, deviceSecret);

        // HMAC 认证拦截器
        HmacAuthInterceptor hmacInterceptor = new HmacAuthInterceptor(hmacAuthUtil);

        // OkHttp 客户端
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(hmacInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // 配置 Gson，不序列化 null 值
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .create();

        // Retrofit 实例
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiConfig.getApiBaseUrl())
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build();

        apiService = retrofit.create(AttendanceApiService.class);
        Log.i(TAG, "Retrofit initialized with base URL: " + ApiConfig.getApiBaseUrl());
        Log.i(TAG, "HMAC auth enabled for device: " + deviceId);
    }

    /**
     * 提交打卡
     */
    public Single<PunchResponse> submitPunch(PunchRequest request) {
        Log.i(TAG, "Submitting punch: userId=" + request.getUserId() + ", username=" + request.getUsername());
        return apiService.submitPunch(request);
    }
}
