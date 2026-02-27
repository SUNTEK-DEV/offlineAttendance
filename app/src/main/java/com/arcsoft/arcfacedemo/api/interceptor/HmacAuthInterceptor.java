package com.arcsoft.arcfacedemo.api.interceptor;

import android.util.Log;

import com.arcsoft.arcfacedemo.util.HmacAuthUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * HMAC 认证拦截器
 * 自动为请求添加 HMAC 签名头
 */
public class HmacAuthInterceptor implements Interceptor {
    private static final String TAG = "HmacAuthInterceptor";

    private HmacAuthUtil hmacAuthUtil;

    public HmacAuthInterceptor(HmacAuthUtil hmacAuthUtil) {
        this.hmacAuthUtil = hmacAuthUtil;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // 读取请求体
        String bodyJson = null;
        RequestBody requestBody = originalRequest.body();
        if (requestBody != null) {
            MediaType contentType = requestBody.contentType();
            if (contentType != null && contentType.subtype() != null &&
                    contentType.subtype().contains("json")) {
                bodyJson = bodyToString(requestBody);
            }
        }

        // 生成 HMAC 头
        Map<String, String> hmacHeaders = hmacAuthUtil.generateHeaders(bodyJson);

        // 构建新请求，添加 HMAC 头
        Request.Builder requestBuilder = originalRequest.newBuilder();
        for (Map.Entry<String, String> entry : hmacHeaders.entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        Log.d(TAG, "Added HMAC headers to request: " + originalRequest.url());
        Request newRequest = requestBuilder.build();

        return chain.proceed(newRequest);
    }

    /**
     * 将 RequestBody 转换为字符串
     */
    private String bodyToString(RequestBody request) {
        try {
            Buffer buffer = new Buffer();
            request.writeTo(buffer);
            return buffer.readString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read request body", e);
            return null;
        }
    }
}
