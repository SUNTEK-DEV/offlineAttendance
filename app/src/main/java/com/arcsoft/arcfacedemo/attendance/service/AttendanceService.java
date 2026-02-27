package com.arcsoft.arcfacedemo.attendance.service;

import android.content.Context;
import android.util.Log;

import com.arcsoft.arcfacedemo.attendance.model.AttendanceRecordEntity;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceRuleEntity;
import com.arcsoft.arcfacedemo.attendance.model.CheckResult;
import com.arcsoft.arcfacedemo.attendance.repository.AttendanceRepository;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.repository.EmployeeRepository;

import java.util.Calendar;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * 考勤打卡服务
 */
public class AttendanceService {

    private static final String TAG = "AttendanceService";

    private static AttendanceService instance;

    private AttendanceRepository attendanceRepository;
    private EmployeeRepository employeeRepository;
    private AttendanceRuleService ruleService;

    private AttendanceService(Context context) {
        this.attendanceRepository = AttendanceRepository.getInstance(context);
        this.employeeRepository = EmployeeRepository.getInstance(context);
        this.ruleService = AttendanceRuleService.getInstance(context);
    }

    public static synchronized AttendanceService getInstance(Context context) {
        if (instance == null) {
            instance = new AttendanceService(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 上班打卡
     *
     * @param employeeId 员工ID
     * @param imagePath  打卡照片路径
     * @return 打卡结果
     */
    public Single<CheckResult> checkIn(long employeeId, String imagePath) {
        return Single.create((SingleOnSubscribe<CheckResult>) emitter -> {
            try {
                // 1. 获取员工信息
                EmployeeEntity employee = employeeRepository.getEmployeeById(employeeId).blockingGet();
                if (employee == null) {
                    emitter.onSuccess(CheckResult.failed("员工不存在"));
                    return;
                }

                // 2. 检查今日是否已打卡
                long today = AttendanceRepository.getTodayStart();
                AttendanceRecordEntity todayRecord = attendanceRepository.getTodayRecord(employeeId).blockingGet();

                if (todayRecord != null && todayRecord.getCheckInTime() > 0) {
                    emitter.onSuccess(CheckResult.failed("今日已上班打卡"));
                    return;
                }

                // 3. 获取考勤规则
                AttendanceRuleEntity rule = attendanceRepository.getAttendanceRule().blockingGet();
                long morningStartTime = rule.getMorningStartTime();
                long lateTolerance = rule.getLateTolerance() * 60 * 1000L; // 分钟转毫秒

                // 4. 判定打卡状态
                long currentTime = System.currentTimeMillis();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(currentTime);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                long todayBase = calendar.getTimeInMillis();
                long workStartTime = todayBase + morningStartTime;

                String status;
                String message;

                if (currentTime <= workStartTime + lateTolerance) {
                    status = "NORMAL";
                    message = "上班打卡成功 - 正常";
                } else {
                    status = "LATE";
                    message = "上班打卡成功 - 迟到";
                }

                // 5. 创建或更新考勤记录
                AttendanceRecordEntity record;
                if (todayRecord == null) {
                    record = new AttendanceRecordEntity();
                    record.setEmployeeId(employeeId);
                    record.setEmployeeNo(employee.getEmployeeNo());
                    record.setEmployeeName(employee.getName());
                    record.setDate(today);
                } else {
                    record = todayRecord;
                }

                record.setCheckInTime(currentTime);
                record.setCheckInStatus(status);
                record.setCheckInImagePath(imagePath);

                long recordId = attendanceRepository.insertOrUpdateRecord(record).blockingGet();

                // 6. 返回结果
                CheckResult result = CheckResult.success("CHECK_IN", status, message);
                result.setRecordId(recordId);
                result.setCheckTime(currentTime);

                emitter.onSuccess(result);

            } catch (Exception e) {
                Log.e(TAG, "Check in failed", e);
                emitter.onSuccess(CheckResult.failed("打卡失败: " + e.getMessage()));
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 下班打卡
     *
     * @param employeeId 员工ID
     * @param imagePath  打卡照片路径
     * @return 打卡结果
     */
    public Single<CheckResult> checkOut(long employeeId, String imagePath) {
        return Single.create((SingleOnSubscribe<CheckResult>) emitter -> {
            try {
                // 1. 获取员工信息
                EmployeeEntity employee = employeeRepository.getEmployeeById(employeeId).blockingGet();
                if (employee == null) {
                    emitter.onSuccess(CheckResult.failed("员工不存在"));
                    return;
                }

                // 2. 检查今日是否已上班打卡
                long today = AttendanceRepository.getTodayStart();
                AttendanceRecordEntity todayRecord = attendanceRepository.getTodayRecord(employeeId).blockingGet();

                if (todayRecord == null || todayRecord.getCheckInTime() <= 0) {
                    emitter.onSuccess(CheckResult.failed("请先进行上班打卡"));
                    return;
                }

                // 3. 检查是否已下班打卡
                if (todayRecord.getCheckOutTime() > 0) {
                    emitter.onSuccess(CheckResult.failed("今日已下班打卡"));
                    return;
                }

                // 4. 获取考勤规则
                AttendanceRuleEntity rule = attendanceRepository.getAttendanceRule().blockingGet();
                long afternoonEndTime = rule.getAfternoonEndTime();
                long earlyLeaveTolerance = rule.getEarlyLeaveTolerance() * 60 * 1000L; // 分钟转毫秒

                // 5. 判定打卡状态
                long currentTime = System.currentTimeMillis();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(currentTime);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                long todayBase = calendar.getTimeInMillis();
                long workEndTime = todayBase + afternoonEndTime;

                String status;
                String message;

                if (currentTime >= workEndTime - earlyLeaveTolerance) {
                    status = "NORMAL";
                    message = "下班打卡成功 - 正常";
                } else {
                    status = "EARLY";
                    message = "下班打卡成功 - 早退";
                }

                // 6. 更新考勤记录
                todayRecord.setCheckOutTime(currentTime);
                todayRecord.setCheckOutStatus(status);
                todayRecord.setCheckOutImagePath(imagePath);

                long recordId = attendanceRepository.insertOrUpdateRecord(todayRecord).blockingGet();

                // 7. 返回结果
                CheckResult result = CheckResult.success("CHECK_OUT", status, message);
                result.setRecordId(recordId);
                result.setCheckTime(currentTime);

                emitter.onSuccess(result);

            } catch (Exception e) {
                Log.e(TAG, "Check out failed", e);
                emitter.onSuccess(CheckResult.failed("打卡失败: " + e.getMessage()));
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 获取今日打卡记录
     */
    public Single<AttendanceRecordEntity> getTodayRecord(long employeeId) {
        return attendanceRepository.getTodayRecord(employeeId);
    }

    /**
     * 判断是否可以上班打卡
     */
    public Single<Boolean> canCheckIn(long employeeId) {
        return Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            AttendanceRecordEntity record = attendanceRepository.getTodayRecord(employeeId).blockingGet();

            if (record == null) {
                emitter.onSuccess(true);
                return;
            }

            boolean canCheckIn = record.getCheckInTime() <= 0;
            emitter.onSuccess(canCheckIn);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 判断是否可以下班打卡
     */
    public Single<Boolean> canCheckOut(long employeeId) {
        return Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            AttendanceRecordEntity record = attendanceRepository.getTodayRecord(employeeId).blockingGet();

            if (record == null || record.getCheckInTime() <= 0) {
                emitter.onSuccess(false);
                return;
            }

            boolean canCheckOut = record.getCheckOutTime() <= 0;
            emitter.onSuccess(canCheckOut);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 根据人脸ID打卡
     * 自动判断上班还是下班
     *
     * @param faceId    人脸ID
     * @param imagePath 打卡照片路径
     * @return 打卡结果
     */
    public Single<CheckResult> checkInByFaceId(long faceId, String imagePath) {
        return Single.create((SingleOnSubscribe<CheckResult>) emitter -> {
            try {
                Log.d(TAG, "checkInByFaceId: faceId=" + faceId);

                // 1. 根据人脸ID获取员工
                EmployeeEntity employee = employeeRepository.getEmployeeByFaceId(faceId).blockingGet();
                if (employee == null) {
                    String error = "未找到对应员工 (faceId: " + faceId + ")";
                    Log.e(TAG, error);
                    emitter.onSuccess(CheckResult.failed(error));
                    return;
                }

                Log.d(TAG, "Found employee: " + employee.getName() + " (employeeId: " + employee.getEmployeeId() + ")");

                // 2. 获取今日记录
                AttendanceRecordEntity record = attendanceRepository.getTodayRecord(employee.getEmployeeId()).blockingGet();

                // 3. 判断是上班打卡还是下班打卡
                CheckResult result;
                if (record == null || record.getCheckInTime() <= 0) {
                    // 上班打卡
                    Log.d(TAG, "Performing check-in");
                    result = checkIn(employee.getEmployeeId(), imagePath).blockingGet();
                } else if (record.getCheckOutTime() <= 0) {
                    // 下班打卡
                    Log.d(TAG, "Performing check-out");
                    result = checkOut(employee.getEmployeeId(), imagePath).blockingGet();
                } else {
                    // 已完成打卡
                    Log.d(TAG, "Already checked in and out for today");
                    result = CheckResult.failed("今日已完成打卡");
                }

                if (result != null) {
                    emitter.onSuccess(result);
                } else {
                    Log.e(TAG, "Check result is null!");
                    emitter.onSuccess(CheckResult.failed("打卡返回结果为空"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Check in by face id failed", e);
                emitter.onSuccess(CheckResult.failed("打卡失败: " + e.getMessage()));
            }
        }).subscribeOn(Schedulers.io());
    }
}
