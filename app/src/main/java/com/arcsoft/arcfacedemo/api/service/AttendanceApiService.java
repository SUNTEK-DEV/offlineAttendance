package com.arcsoft.arcfacedemo.api.service;

import com.arcsoft.arcfacedemo.api.model.PunchRequest;
import com.arcsoft.arcfacedemo.api.model.PunchResponse;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 考勤 API 服务接口
 */
public interface AttendanceApiService {
    /**
     * 提交打卡记录
     */
    @POST("attendance")
    Single<PunchResponse> submitPunch(@Body PunchRequest request);
}
