package com.arcsoft.arcfacedemo.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 * API 配置
 */
public class ApiConfig {
    private static final String PREF_KEY_DEVICE_ID = "device_id";
    private static final String PREF_KEY_DEVICE_SECRET = "device_secret";
    private static final String DEFAULT_DEVICE_ID = "01KG9EYS7B9FBZSER1ACA0R4X6";
    private static final String DEFAULT_DEVICE_SECRET = "cd4637bca572ae975262e6b64b172b82e9ed40fd65c2a45ba27c74058484791f";

    // 生产环境 API 地址
    private static final String PRODUCTION_API_URL = "https://faceapi.inypc.com/api/";

    // 测试环境 API 地址
    private static final String TEST_API_URL = "https://faceapi.inypc.com/api/";

    // 当前使用的 API 地址
    private static String currentApiUrl = PRODUCTION_API_URL;

    /**
     * 获取 API 基础 URL
     */
    public static String getApiBaseUrl() {
        return currentApiUrl;
    }

    /**
     * 设置 API 环境
     */
    public static void setEnvironment(boolean isProduction) {
        currentApiUrl = isProduction ? PRODUCTION_API_URL : TEST_API_URL;
    }

    /**
     * 获取设备 ID
     */
    public static String getDeviceId(Context context) {
        if (context == null) {
            return DEFAULT_DEVICE_ID;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_KEY_DEVICE_ID, DEFAULT_DEVICE_ID);
    }

    /**
     * 获取设备密钥
     */
    public static String getDeviceSecret(Context context) {
        if (context == null) {
            return DEFAULT_DEVICE_SECRET;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_KEY_DEVICE_SECRET, DEFAULT_DEVICE_SECRET);
    }

    /**
     * 保存设备凭证
     */
    public static void saveDeviceCredentials(Context context, String deviceId, String deviceSecret) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .putString(PREF_KEY_DEVICE_ID, deviceId)
                .putString(PREF_KEY_DEVICE_SECRET, deviceSecret)
                .apply();
    }

    /**
     * 清除设备凭证
     */
    public static void clearDeviceCredentials(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .remove(PREF_KEY_DEVICE_ID)
                .remove(PREF_KEY_DEVICE_SECRET)
                .apply();
    }

    /**
     * 检查是否有设备凭证
     */
    public static boolean hasDeviceCredentials(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.contains(PREF_KEY_DEVICE_ID) && prefs.contains(PREF_KEY_DEVICE_SECRET);
    }
}
