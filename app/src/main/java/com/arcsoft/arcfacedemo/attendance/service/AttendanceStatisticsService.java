package com.arcsoft.arcfacedemo.attendance.service;

import android.content.Context;
import android.util.Log;

import com.arcsoft.arcfacedemo.attendance.model.AttendanceRecordEntity;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceStatistics;
import com.arcsoft.arcfacedemo.attendance.repository.AttendanceRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * 考勤统计服务
 */
public class AttendanceStatisticsService {

    private static final String TAG = "AttendanceStatisticsService";

    private static AttendanceStatisticsService instance;

    private AttendanceRepository attendanceRepository;
    private AttendanceRuleService ruleService;

    private AttendanceStatisticsService(Context context) {
        this.attendanceRepository = AttendanceRepository.getInstance(context);
        this.ruleService = AttendanceRuleService.getInstance(context);
    }

    public static synchronized AttendanceStatisticsService getInstance(Context context) {
        if (instance == null) {
            instance = new AttendanceStatisticsService(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 生成员工月度统计
     *
     * @param employeeId 员工ID
     * @param year       年份
     * @param month      月份 (1-12)
     * @return 统计数据
     */
    public Single<AttendanceStatistics> generateMonthlyReport(long employeeId, int year, int month) {
        return Single.create((SingleOnSubscribe<AttendanceStatistics>) emitter -> {
            try {
                // 1. 获取月份范围
                long[] monthRange = AttendanceRepository.getMonthRange(year, month);
                long startDate = monthRange[0];
                long endDate = monthRange[1];

                // 2. 获取员工信息
                com.arcsoft.arcfacedemo.employee.repository.EmployeeRepository empRepo =
                        com.arcsoft.arcfacedemo.employee.repository.EmployeeRepository.getInstance(null);
                com.arcsoft.arcfacedemo.employee.model.EmployeeEntity employee =
                        empRepo.getEmployeeById(employeeId).blockingGet();

                if (employee == null) {
                    emitter.onError(new IllegalArgumentException("员工不存在"));
                    return;
                }

                // 3. 获取考勤记录
                List<AttendanceRecordEntity> records =
                        attendanceRepository.getRecordsByEmployee(employeeId, startDate, endDate).blockingGet();

                // 4. 计算应出勤天数
                int shouldAttendDays = calculateWorkDays(startDate, endDate);

                // 5. 统计数据
                AttendanceStatistics stats = new AttendanceStatistics();
                stats.setEmployeeId(employeeId);
                stats.setEmployeeName(employee.getName());
                stats.setEmployeeNo(employee.getEmployeeNo());
                stats.setStartDate(startDate);
                stats.setEndDate(endDate);
                stats.setShouldAttendDays(shouldAttendDays);

                int normalCount = 0;
                int lateCount = 0;
                int earlyLeaveCount = 0;
                int absentCount = 0;
                int overtimeCount = 0;

                for (AttendanceRecordEntity record : records) {
                    // 统计上班打卡
                    if ("NORMAL".equals(record.getCheckInStatus())) {
                        normalCount++;
                    } else if ("LATE".equals(record.getCheckInStatus())) {
                        lateCount++;
                    } else if ("ABSENT".equals(record.getCheckInStatus())) {
                        absentCount++;
                    }

                    // 统计下班打卡
                    if ("EARLY".equals(record.getCheckOutStatus())) {
                        earlyLeaveCount++;
                    } else if ("ABSENT".equals(record.getCheckOutStatus())) {
                        absentCount++;
                    }

                    // 统计加班
                    if ("OVERTIME".equals(record.getWorkStatus())) {
                        overtimeCount++;
                    }
                }

                stats.setNormalCount(normalCount);
                stats.setLateCount(lateCount);
                stats.setEarlyLeaveCount(earlyLeaveCount);
                stats.setAbsentCount(absentCount);
                stats.setOvertimeCount(overtimeCount);

                // 计算实际出勤天数
                int actualAttendDays = records.size();
                stats.setActualAttendDays(actualAttendDays);

                // 计算出勤率
                double attendanceRate = shouldAttendDays > 0
                        ? (actualAttendDays * 100.0 / shouldAttendDays)
                        : 0;
                stats.setAttendanceRate(attendanceRate);

                emitter.onSuccess(stats);

            } catch (Exception e) {
                Log.e(TAG, "Generate monthly report failed", e);
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 生成所有员工月度统计
     *
     * @param year  年份
     * @param month 月份
     * @return 统计列表
     */
    public Single<List<AttendanceStatistics>> generateAllEmployeesMonthlyReport(int year, int month) {
        return Single.create((SingleOnSubscribe<List<AttendanceStatistics>>) emitter -> {
            try {
                // 1. 获取所有员工
                com.arcsoft.arcfacedemo.employee.repository.EmployeeRepository empRepo =
                        com.arcsoft.arcfacedemo.employee.repository.EmployeeRepository.getInstance(null);
                List<com.arcsoft.arcfacedemo.employee.model.EmployeeEntity> employees =
                        empRepo.getAllEmployees().blockingGet();

                if (employees == null || employees.isEmpty()) {
                    emitter.onSuccess(new ArrayList<>());
                    return;
                }

                // 2. 为每个员工生成统计
                List<AttendanceStatistics> statsList = new ArrayList<>();
                for (com.arcsoft.arcfacedemo.employee.model.EmployeeEntity employee : employees) {
                    try {
                        AttendanceStatistics stats = generateMonthlyReport(
                                employee.getEmployeeId(), year, month
                        ).blockingGet();
                        statsList.add(stats);
                    } catch (Exception e) {
                        Log.e(TAG, "Generate stats for employee " + employee.getEmployeeNo() + " failed", e);
                    }
                }

                emitter.onSuccess(statsList);

            } catch (Exception e) {
                Log.e(TAG, "Generate all employees report failed", e);
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 生成日期范围统计（用于日报表）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 统计列表
     */
    public Single<List<AttendanceStatistics>> generateDailyReport(long startDate, long endDate) {
        return Single.create((SingleOnSubscribe<List<AttendanceStatistics>>) emitter -> {
            try {
                // 1. 获取所有员工
                com.arcsoft.arcfacedemo.employee.repository.EmployeeRepository empRepo =
                        com.arcsoft.arcfacedemo.employee.repository.EmployeeRepository.getInstance(null);
                List<com.arcsoft.arcfacedemo.employee.model.EmployeeEntity> employees =
                        empRepo.getAllEmployees().blockingGet();

                if (employees == null || employees.isEmpty()) {
                    emitter.onSuccess(new ArrayList<>());
                    return;
                }

                // 2. 计算工作日
                int workDays = calculateWorkDays(startDate, endDate);

                // 3. 为每个员工生成统计
                List<AttendanceStatistics> statsList = new ArrayList<>();
                for (com.arcsoft.arcfacedemo.employee.model.EmployeeEntity employee : employees) {
                    List<AttendanceRecordEntity> records =
                            attendanceRepository.getRecordsByEmployee(employee.getEmployeeId(), startDate, endDate).blockingGet();

                    AttendanceStatistics stats = new AttendanceStatistics();
                    stats.setEmployeeId(employee.getEmployeeId());
                    stats.setEmployeeName(employee.getName());
                    stats.setEmployeeNo(employee.getEmployeeNo());
                    stats.setStartDate(startDate);
                    stats.setEndDate(endDate);
                    stats.setShouldAttendDays(workDays);
                    stats.setActualAttendDays(records.size());

                    // 统计各项数据
                    int normalCount = 0;
                    int lateCount = 0;
                    int earlyLeaveCount = 0;
                    int absentCount = workDays - records.size(); // 缺勤 = 应出勤 - 实际出勤

                    for (AttendanceRecordEntity record : records) {
                        if ("NORMAL".equals(record.getCheckInStatus())) {
                            normalCount++;
                        } else if ("LATE".equals(record.getCheckInStatus())) {
                            lateCount++;
                        }
                        if ("EARLY".equals(record.getCheckOutStatus())) {
                            earlyLeaveCount++;
                        }
                    }

                    stats.setNormalCount(normalCount);
                    stats.setLateCount(lateCount);
                    stats.setEarlyLeaveCount(earlyLeaveCount);
                    stats.setAbsentCount(absentCount);

                    double attendanceRate = workDays > 0 ? (records.size() * 100.0 / workDays) : 0;
                    stats.setAttendanceRate(attendanceRate);

                    statsList.add(stats);
                }

                emitter.onSuccess(statsList);

            } catch (Exception e) {
                Log.e(TAG, "Generate daily report failed", e);
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 获取异常记录（迟到、早退、缺勤）
     */
    public Single<List<AttendanceRecordEntity>> getAbnormalRecords(long startDate, long endDate) {
        return Single.create((SingleOnSubscribe<List<AttendanceRecordEntity>>) emitter -> {
            try {
                List<AttendanceRecordEntity> abnormalRecords = new ArrayList<>();

                // 获取迟到记录
                List<AttendanceRecordEntity> lateRecords =
                        attendanceRepository.getLateRecords(startDate, endDate).blockingGet();
                abnormalRecords.addAll(lateRecords);

                // 获取早退记录
                List<AttendanceRecordEntity> earlyRecords =
                        attendanceRepository.getEarlyLeaveRecords(startDate, endDate).blockingGet();
                abnormalRecords.addAll(earlyRecords);

                // 获取缺勤记录
                List<AttendanceRecordEntity> absentRecords =
                        attendanceRepository.getAbsentRecords(startDate, endDate).blockingGet();
                abnormalRecords.addAll(absentRecords);

                emitter.onSuccess(abnormalRecords);

            } catch (Exception e) {
                Log.e(TAG, "Get abnormal records failed", e);
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 计算日期范围内的实际工作日
     * 考虑工作日配置、节假日、调休
     */
    private int calculateWorkDays(long startDate, long endDate) {
        int workDays = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startDate);

        long endDateTime = endDate;

        while (calendar.getTimeInMillis() <= endDateTime) {
            long currentDay = calendar.getTimeInMillis();
            try {
                Boolean isWorkDay = ruleService.isWorkDay(currentDay).blockingGet();
                if (isWorkDay != null && isWorkDay) {
                    workDays++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Check work day failed", e);
            }

            // 下一天
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return workDays;
    }
}
