package com.arcsoft.arcfacedemo.attendance.service;

import android.content.Context;
import android.util.Log;

import com.arcsoft.arcfacedemo.attendance.model.AttendanceRecordEntity;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceStatistics;
import com.arcsoft.arcfacedemo.attendance.repository.AttendanceRepository;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.repository.EmployeeRepository;

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

    private final AttendanceRepository attendanceRepository;
    private final AttendanceRuleService ruleService;
    private final EmployeeRepository employeeRepository;

    private AttendanceStatisticsService(Context context) {
        Context appContext = context.getApplicationContext();
        this.attendanceRepository = AttendanceRepository.getInstance(appContext);
        this.ruleService = AttendanceRuleService.getInstance(appContext);
        this.employeeRepository = EmployeeRepository.getInstance(appContext);
    }

    public static synchronized AttendanceStatisticsService getInstance(Context context) {
        if (instance == null) {
            instance = new AttendanceStatisticsService(context);
        }
        return instance;
    }

    public Single<AttendanceStatistics> generateMonthlyReport(long employeeId, int year, int month) {
        return Single.create((SingleOnSubscribe<AttendanceStatistics>) emitter -> {
            try {
                long[] monthRange = AttendanceRepository.getMonthRange(year, month);
                long startDate = monthRange[0];
                long endDate = monthRange[1];

                EmployeeEntity employee = employeeRepository.getEmployeeById(employeeId).blockingGet();
                if (employee == null) {
                    emitter.onError(new IllegalArgumentException("员工不存在"));
                    return;
                }

                List<AttendanceRecordEntity> records =
                        attendanceRepository.getRecordsByEmployee(employeeId, startDate, endDate).blockingGet();

                int shouldAttendDays = calculateWorkDays(startDate, endDate);
                int actualAttendDays = records.size();

                AttendanceStatistics stats = new AttendanceStatistics();
                stats.setEmployeeId(employeeId);
                stats.setEmployeeName(employee.getName());
                stats.setEmployeeNo(employee.getEmployeeNo());
                stats.setStartDate(startDate);
                stats.setEndDate(endDate);
                stats.setShouldAttendDays(shouldAttendDays);
                stats.setActualAttendDays(actualAttendDays);

                int normalCount = 0;
                int lateCount = 0;
                int earlyLeaveCount = 0;
                int absentCount = 0;
                int overtimeCount = 0;

                for (AttendanceRecordEntity record : records) {
                    if ("NORMAL".equals(record.getCheckInStatus())) {
                        normalCount++;
                    } else if ("LATE".equals(record.getCheckInStatus())) {
                        lateCount++;
                    } else if ("ABSENT".equals(record.getCheckInStatus())) {
                        absentCount++;
                    }

                    if ("EARLY".equals(record.getCheckOutStatus())) {
                        earlyLeaveCount++;
                    } else if ("ABSENT".equals(record.getCheckOutStatus())) {
                        absentCount++;
                    }

                    if ("OVERTIME".equals(record.getWorkStatus())) {
                        overtimeCount++;
                    }
                }

                absentCount = Math.max(absentCount, Math.max(0, shouldAttendDays - actualAttendDays));

                stats.setNormalCount(normalCount);
                stats.setLateCount(lateCount);
                stats.setEarlyLeaveCount(earlyLeaveCount);
                stats.setAbsentCount(absentCount);
                stats.setOvertimeCount(overtimeCount);

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

    public Single<List<AttendanceStatistics>> generateAllEmployeesMonthlyReport(int year, int month) {
        return Single.create((SingleOnSubscribe<List<AttendanceStatistics>>) emitter -> {
            try {
                List<EmployeeEntity> employees = employeeRepository.getAllEmployees().blockingGet();
                if (employees == null || employees.isEmpty()) {
                    emitter.onSuccess(new ArrayList<>());
                    return;
                }

                List<AttendanceStatistics> statsList = new ArrayList<>();
                for (EmployeeEntity employee : employees) {
                    try {
                        statsList.add(generateMonthlyReport(employee.getEmployeeId(), year, month).blockingGet());
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

    public Single<List<AttendanceStatistics>> generateDailyReport(long startDate, long endDate) {
        return Single.create((SingleOnSubscribe<List<AttendanceStatistics>>) emitter -> {
            try {
                List<EmployeeEntity> employees = employeeRepository.getAllEmployees().blockingGet();
                if (employees == null || employees.isEmpty()) {
                    emitter.onSuccess(new ArrayList<>());
                    return;
                }

                int workDays = calculateWorkDays(startDate, endDate);
                List<AttendanceStatistics> statsList = new ArrayList<>();
                for (EmployeeEntity employee : employees) {
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

                    int normalCount = 0;
                    int lateCount = 0;
                    int earlyLeaveCount = 0;
                    int absentCount = Math.max(0, workDays - records.size());

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
                    stats.setAttendanceRate(workDays > 0 ? (records.size() * 100.0 / workDays) : 0);

                    statsList.add(stats);
                }

                emitter.onSuccess(statsList);
            } catch (Exception e) {
                Log.e(TAG, "Generate daily report failed", e);
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    public Single<List<AttendanceRecordEntity>> getAbnormalRecords(long startDate, long endDate) {
        return Single.create((SingleOnSubscribe<List<AttendanceRecordEntity>>) emitter -> {
            try {
                List<AttendanceRecordEntity> abnormalRecords = new ArrayList<>();
                abnormalRecords.addAll(attendanceRepository.getLateRecords(startDate, endDate).blockingGet());
                abnormalRecords.addAll(attendanceRepository.getEarlyLeaveRecords(startDate, endDate).blockingGet());
                abnormalRecords.addAll(attendanceRepository.getAbsentRecords(startDate, endDate).blockingGet());
                emitter.onSuccess(abnormalRecords);
            } catch (Exception e) {
                Log.e(TAG, "Get abnormal records failed", e);
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    private int calculateWorkDays(long startDate, long endDate) {
        int workDays = 0;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startDate);

        while (calendar.getTimeInMillis() <= endDate) {
            long currentDay = calendar.getTimeInMillis();
            try {
                Boolean isWorkDay = ruleService.isWorkDay(currentDay).blockingGet();
                if (Boolean.TRUE.equals(isWorkDay)) {
                    workDays++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Check work day failed", e);
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return workDays;
    }
}
