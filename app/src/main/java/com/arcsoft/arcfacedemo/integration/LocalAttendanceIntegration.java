package com.arcsoft.arcfacedemo.integration;

import android.content.Context;
import android.util.Log;

import com.arcsoft.arcfacedemo.attendance.model.CheckResult;
import com.arcsoft.arcfacedemo.attendance.service.AttendanceService;
import com.arcsoft.arcfacedemo.attendance.service.AttendanceStatisticsService;
import com.arcsoft.arcfacedemo.dooraccess.model.AccessResult;
import com.arcsoft.arcfacedemo.dooraccess.service.DoorAccessService;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.service.EmployeeService;
import com.arcsoft.arcfacedemo.ui.model.CompareResult;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

/**
 * 本地考勤和门禁集成服务
 * 将人脸识别结果集成到本地考勤系统
 */
public class LocalAttendanceIntegration {

    private static final String TAG = "LocalAttendanceIntegration";

    private static LocalAttendanceIntegration instance;

    private AttendanceService attendanceService;
    private DoorAccessService doorAccessService;
    private EmployeeService employeeService;
    private Context context;

    private LocalAttendanceIntegration(Context context) {
        this.context = context.getApplicationContext();
        this.attendanceService = AttendanceService.getInstance(this.context);
        this.doorAccessService = DoorAccessService.getInstance(this.context);
        this.employeeService = EmployeeService.getInstance(this.context);
    }

    public static synchronized LocalAttendanceIntegration getInstance(Context context) {
        if (instance == null) {
            instance = new LocalAttendanceIntegration(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 处理人脸识别结果，进行本地打卡
     *
     * @param compareResult 人脸识别结果
     * @param callback      回调接口
     */
    public void processFaceRecognition(CompareResult compareResult, ProcessCallback callback) {
        if (compareResult == null || compareResult.getFaceEntity() == null) {
            callback.onError("人脸识别结果无效");
            return;
        }

        long faceId = compareResult.getFaceEntity().getFaceId();
        String userName = compareResult.getFaceEntity().getUserName();

        Log.i(TAG, "Processing face recognition - faceId: " + faceId + ", userName: " + userName);

        // 生成打卡图片路径（如果需要）
        String imagePath = generateImagePath(userName);

        // 调用本地打卡服务
        attendanceService.checkInByFace(faceId, userName, imagePath)
                .subscribe(new SingleObserver<CheckResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "Check in subscribe");
                    }

                    @Override
                    public void onSuccess(CheckResult result) {
                        Log.i(TAG, "Check in result: " + result.toString());
                        if (result != null && result.isSuccess()) {
                            callback.onAttendanceResult(result);
                        } else if (result != null) {
                            callback.onError(result.getError());
                        } else {
                            callback.onError("打卡失败: result is null");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Check in failed", e);
                        callback.onError("打卡失败: " + e.getMessage());
                    }
                });
    }

    /**
     * 处理人脸识别结果，进行门禁验证
     *
     * @param compareResult 人脸识别结果
     * @param callback      回调接口
     */
    public void processDoorAccess(CompareResult compareResult, String imagePath, DoorAccessCallback callback) {
        if (compareResult == null || compareResult.getFaceEntity() == null) {
            callback.onAccessFailed("人脸识别结果无效");
            return;
        }

        long faceId = compareResult.getFaceEntity().getFaceId();

        Log.i(TAG, "Processing door access - faceId: " + faceId);

        // 调用门禁服务
        doorAccessService.verifyAccessByFace(faceId, imagePath)
                .subscribe(new SingleObserver<AccessResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "Door access subscribe");
                    }

                    @Override
                    public void onSuccess(AccessResult result) {
                        Log.i(TAG, "Door access result: " + result.toString());
                        if (result.isSuccess()) {
                            callback.onAccessGranted(result);
                        } else {
                            callback.onAccessDenied(result);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Door access failed", e);
                        callback.onAccessFailed("门禁验证失败: " + e.getMessage());
                    }
                });
    }

    /**
     * 根据人脸ID获取员工信息
     */
    public Single<EmployeeEntity> getEmployeeByFaceId(long faceId) {
        return employeeService.getEmployeeByFaceId(faceId);
    }

    /**
     * 生成图片保存路径
     */
    private String generateImagePath(String userName) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "checkin_" + userName + "_" + timestamp + ".jpg";

        // 保存到应用的Pictures目录
        File directory = new File(context.getExternalFilesDir("Pictures"), "checkin");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        return new File(directory, fileName).getAbsolutePath();
    }

    /**
     * 处理回调接口
     */
    public interface ProcessCallback {
        /**
         * 打卡成功
         */
        void onAttendanceResult(CheckResult result);

        /**
         * 处理失败
         */
        void onError(String error);
    }

    /**
     * 门禁回调接口
     */
    public interface DoorAccessCallback {
        /**
         * 门禁验证通过
         */
        void onAccessGranted(AccessResult result);

        /**
         * 门禁验证拒绝
         */
        void onAccessDenied(AccessResult result);

        /**
         * 验证失败
         */
        void onAccessFailed(String error);
    }

    /**
     * 获取当前工作模式
     */
    public WorkMode getWorkMode() {
        // 默认为考勤模式
        return WorkMode.ATTENDANCE;
    }

    /**
     * 设置工作模式
     */
    public void setWorkMode(WorkMode mode) {
        Log.i(TAG, "Work mode set to: " + mode);
    }

    /**
     * 工作模式枚举
     */
    public enum WorkMode {
        /**
         * 考勤模式
         */
        ATTENDANCE,

        /**
         * 门禁模式
         */
        DOOR_ACCESS,

        /**
         * 混合模式（同时打卡和门禁）
         */
        HYBRID
    }
}
