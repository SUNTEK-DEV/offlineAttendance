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
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * 考勤打卡服务
 */
public class AttendanceService {

    private static final String TAG = "AttendanceService";
    private static final String SOURCE_FACE = "FACE";

    private static AttendanceService instance;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRuleService ruleService;

    private AttendanceService(Context context) {
        Context appContext = context.getApplicationContext();
        this.attendanceRepository = AttendanceRepository.getInstance(appContext);
        this.employeeRepository = EmployeeRepository.getInstance(appContext);
        this.ruleService = AttendanceRuleService.getInstance(appContext);
    }

    public static synchronized AttendanceService getInstance(Context context) {
        if (instance == null) {
            instance = new AttendanceService(context.getApplicationContext());
        }
        return instance;
    }

    public Single<CheckResult> checkIn(long employeeId, String imagePath) {
        return checkIn(employeeId, imagePath, null);
    }

    public Single<CheckResult> checkIn(long employeeId, String imagePath, String source) {
        return Single.create((SingleOnSubscribe<CheckResult>) emitter -> {
            try {
                EmployeeEntity employee = employeeRepository.getEmployeeById(employeeId).blockingGet();
                if (employee == null) {
                    emitter.onSuccess(CheckResult.failed("员工不存在"));
                    return;
                }

                long today = AttendanceRepository.getTodayStart();
                AttendanceRecordEntity todayRecord = attendanceRepository.getTodayRecord(employeeId).blockingGet();
                if (todayRecord != null && todayRecord.getCheckInTime() > 0) {
                    emitter.onSuccess(CheckResult.failed("今日已完成上班打卡"));
                    return;
                }

                AttendanceRuleEntity rule = attendanceRepository.getAttendanceRule().blockingGet();
                long currentTime = System.currentTimeMillis();
                long workStartTime = getTodayBaseTime(currentTime) + rule.getMorningStartTime();
                long lateTolerance = rule.getLateTolerance() * 60 * 1000L;

                String status = currentTime <= workStartTime + lateTolerance ? "NORMAL" : "LATE";
                String message = employee.getName() + ("NORMAL".equals(status) ? " 上班打卡成功" : " 上班打卡成功(迟到)");

                AttendanceRecordEntity record = todayRecord == null
                        ? createNewRecord(employee, today)
                        : todayRecord;
                record.setCheckInTime(currentTime);
                record.setCheckInStatus(status);
                record.setCheckInImagePath(imagePath);
                record.setRemark(buildRemark(source, record.getRemark()));

                long recordId = attendanceRepository.insertOrUpdateRecord(record).blockingGet();

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

    public Single<CheckResult> checkOut(long employeeId, String imagePath) {
        return checkOut(employeeId, imagePath, null);
    }

    public Single<CheckResult> checkOut(long employeeId, String imagePath, String source) {
        return Single.create((SingleOnSubscribe<CheckResult>) emitter -> {
            try {
                EmployeeEntity employee = employeeRepository.getEmployeeById(employeeId).blockingGet();
                if (employee == null) {
                    emitter.onSuccess(CheckResult.failed("员工不存在"));
                    return;
                }

                AttendanceRecordEntity todayRecord = attendanceRepository.getTodayRecord(employeeId).blockingGet();
                if (todayRecord == null || todayRecord.getCheckInTime() <= 0) {
                    emitter.onSuccess(CheckResult.failed("请先完成上班打卡"));
                    return;
                }
                if (todayRecord.getCheckOutTime() > 0) {
                    emitter.onSuccess(CheckResult.failed("今日已完成下班打卡"));
                    return;
                }

                AttendanceRuleEntity rule = attendanceRepository.getAttendanceRule().blockingGet();
                long currentTime = System.currentTimeMillis();
                long workEndTime = getTodayBaseTime(currentTime) + rule.getAfternoonEndTime();
                long earlyLeaveTolerance = rule.getEarlyLeaveTolerance() * 60 * 1000L;

                String status = currentTime >= workEndTime - earlyLeaveTolerance ? "NORMAL" : "EARLY";
                String message = employee.getName() + ("NORMAL".equals(status) ? " 下班打卡成功" : " 下班打卡成功(早退)");

                todayRecord.setCheckOutTime(currentTime);
                todayRecord.setCheckOutStatus(status);
                todayRecord.setCheckOutImagePath(imagePath);
                todayRecord.setRemark(buildRemark(source, todayRecord.getRemark()));

                long recordId = attendanceRepository.insertOrUpdateRecord(todayRecord).blockingGet();

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

    public Single<CheckResult> checkInByFaceId(long faceId, String imagePath) {
        return checkInByFace(faceId, null, imagePath);
    }

    public Single<CheckResult> checkInByFace(long faceId, String recognizedName, String imagePath) {
        return Single.create((SingleOnSubscribe<CheckResult>) emitter -> {
            try {
                Log.d(TAG, "checkInByFace: faceId=" + faceId + ", recognizedName=" + recognizedName);
                EmployeeEntity employee = resolveEmployeeByFace(faceId, recognizedName);
                if (employee == null) {
                    String fallback = recognizedName == null || recognizedName.trim().isEmpty()
                            ? String.valueOf(faceId)
                            : recognizedName.trim();
                    emitter.onSuccess(CheckResult.failed("未找到对应员工: " + fallback));
                    return;
                }
                emitter.onSuccess(autoCheck(employee, imagePath, SOURCE_FACE).blockingGet());
            } catch (Exception e) {
                Log.e(TAG, "Check in by face failed", e);
                emitter.onSuccess(CheckResult.failed("打卡失败: " + e.getMessage()));
            }
        }).subscribeOn(Schedulers.io());
    }

    public Single<CheckResult> checkByEmployeeNo(String employeeNo, String imagePath, String source) {
        return Single.create((SingleOnSubscribe<CheckResult>) emitter -> {
            try {
                String normalizedEmployeeNo = employeeNo == null ? "" : employeeNo.trim();
                if (normalizedEmployeeNo.isEmpty()) {
                    emitter.onSuccess(CheckResult.failed("工号不能为空"));
                    return;
                }

                EmployeeEntity employee = employeeRepository.getEmployeeByNo(normalizedEmployeeNo).blockingGet();
                if (employee == null) {
                    emitter.onSuccess(CheckResult.failed("未找到对应员工: " + normalizedEmployeeNo));
                    return;
                }

                emitter.onSuccess(autoCheck(employee, imagePath, source).blockingGet());
            } catch (Exception e) {
                Log.e(TAG, "Check in by employee no failed", e);
                emitter.onSuccess(CheckResult.failed("打卡失败: " + e.getMessage()));
            }
        }).subscribeOn(Schedulers.io());
    }

    public Single<AttendanceRecordEntity> getTodayRecord(long employeeId) {
        return attendanceRepository.getTodayRecord(employeeId).toSingle();
    }

    public Single<Boolean> canCheckIn(long employeeId) {
        return Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            AttendanceRecordEntity record = attendanceRepository.getTodayRecord(employeeId).blockingGet();
            emitter.onSuccess(record == null || record.getCheckInTime() <= 0);
        }).subscribeOn(Schedulers.io());
    }

    public Single<Boolean> canCheckOut(long employeeId) {
        return Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            AttendanceRecordEntity record = attendanceRepository.getTodayRecord(employeeId).blockingGet();
            emitter.onSuccess(record != null && record.getCheckInTime() > 0 && record.getCheckOutTime() <= 0);
        }).subscribeOn(Schedulers.io());
    }

    private Single<CheckResult> autoCheck(EmployeeEntity employee, String imagePath, String source) {
        return Single.create((SingleOnSubscribe<CheckResult>) emitter -> {
            AttendanceRecordEntity record = attendanceRepository.getTodayRecord(employee.getEmployeeId()).blockingGet();
            CheckResult result;
            if (record == null || record.getCheckInTime() <= 0) {
                result = checkIn(employee.getEmployeeId(), imagePath, source).blockingGet();
            } else if (record.getCheckOutTime() <= 0) {
                result = checkOut(employee.getEmployeeId(), imagePath, source).blockingGet();
            } else {
                result = CheckResult.failed("今日已完成打卡");
            }
            emitter.onSuccess(result);
        }).subscribeOn(Schedulers.io());
    }

    private EmployeeEntity resolveEmployeeByFace(long faceId, String recognizedName) throws Exception {
        try {
            EmployeeEntity employee = employeeRepository.getEmployeeByFaceId(faceId).blockingGet();
            if (employee != null) {
                return employee;
            }
        } catch (Exception e) {
            Log.w(TAG, "No employee matched faceId directly: " + faceId);
        }

        String normalizedName = recognizedName == null ? "" : recognizedName.trim();
        if (normalizedName.isEmpty()) {
            return null;
        }

        List<EmployeeEntity> employees = employeeRepository.getAllEmployees().blockingGet();
        if (employees == null || employees.isEmpty()) {
            return null;
        }

        for (EmployeeEntity employee : employees) {
            if (normalizedName.equalsIgnoreCase(employee.getEmployeeNo())
                    || normalizedName.equalsIgnoreCase(employee.getName())) {
                bindFaceIdIfNeeded(employee, faceId);
                return employee;
            }
        }
        return null;
    }

    private void bindFaceIdIfNeeded(EmployeeEntity employee, long faceId) {
        if (employee == null || employee.getFaceId() == faceId) {
            return;
        }
        employee.setFaceId(faceId);
        employee.setUpdateTime(System.currentTimeMillis());
        try {
            employeeRepository.updateEmployee(employee).blockingGet();
            Log.i(TAG, "Updated employee faceId mapping: " + employee.getEmployeeNo() + " -> " + faceId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update employee faceId mapping", e);
        }
    }

    private AttendanceRecordEntity createNewRecord(EmployeeEntity employee, long today) {
        AttendanceRecordEntity record = new AttendanceRecordEntity();
        record.setEmployeeId(employee.getEmployeeId());
        record.setEmployeeNo(employee.getEmployeeNo());
        record.setEmployeeName(employee.getName());
        record.setDate(today);
        return record;
    }

    private long getTodayBaseTime(long currentTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTime);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String buildRemark(String source, String existingRemark) {
        if (source == null || source.trim().isEmpty()) {
            return existingRemark;
        }
        if (existingRemark == null || existingRemark.trim().isEmpty()) {
            return "SOURCE:" + source;
        }
        if (existingRemark.contains("SOURCE:" + source)) {
            return existingRemark;
        }
        return existingRemark + "; SOURCE:" + source;
    }
}