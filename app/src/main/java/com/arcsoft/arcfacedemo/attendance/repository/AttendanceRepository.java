package com.arcsoft.arcfacedemo.attendance.repository;

import android.content.Context;

import com.arcsoft.arcfacedemo.attendance.dao.AttendanceRecordDao;
import com.arcsoft.arcfacedemo.attendance.dao.AttendanceRuleDao;
import com.arcsoft.arcfacedemo.attendance.dao.HolidayDao;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceRecordEntity;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceRuleEntity;
import com.arcsoft.arcfacedemo.attendance.model.HolidayEntity;
import com.arcsoft.arcfacedemo.facedb.FaceDatabase;

import java.util.Calendar;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * 考勤仓库
 */
public class AttendanceRepository {

    private static AttendanceRepository instance;

    private final AttendanceRecordDao attendanceRecordDao;
    private final AttendanceRuleDao attendanceRuleDao;
    private final HolidayDao holidayDao;

    private AttendanceRepository(Context context) {
        FaceDatabase database = FaceDatabase.getInstance(context);
        this.attendanceRecordDao = database.attendanceRecordDao();
        this.attendanceRuleDao = database.attendanceRuleDao();
        this.holidayDao = database.holidayDao();
    }

    public static synchronized AttendanceRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AttendanceRepository(context.getApplicationContext());
        }
        return instance;
    }

    public Single<AttendanceRuleEntity> getAttendanceRule() {
        return Single.fromCallable(() -> {
            AttendanceRuleEntity rule = attendanceRuleDao.getRule();
            if (rule == null) {
                rule = new AttendanceRuleEntity();
                long id = attendanceRuleDao.insertOrUpdate(rule);
                rule.setId(id);
            }
            return rule;
        }).subscribeOn(Schedulers.io());
    }

    public Single<Long> updateAttendanceRule(AttendanceRuleEntity rule) {
        return Single.fromCallable(() -> {
            rule.setUpdateTime(System.currentTimeMillis());
            return attendanceRuleDao.insertOrUpdate(rule);
        }).subscribeOn(Schedulers.io());
    }

    public Maybe<AttendanceRecordEntity> getTodayRecord(long employeeId) {
        return Maybe.fromCallable(() -> attendanceRecordDao.getTodayRecord(employeeId, getTodayStart()))
                .subscribeOn(Schedulers.io());
    }

    public Single<Long> insertOrUpdateRecord(AttendanceRecordEntity record) {
        return Single.fromCallable(() -> {
            record.setUpdateTime(System.currentTimeMillis());
            return attendanceRecordDao.insertOrUpdate(record);
        }).subscribeOn(Schedulers.io());
    }

    public Single<List<AttendanceRecordEntity>> getRecordsByDateRange(long startDate, long endDate) {
        return Single.fromCallable(() -> attendanceRecordDao.getRecordsByDateRange(startDate, endDate))
                .subscribeOn(Schedulers.io());
    }

    public Single<List<AttendanceRecordEntity>> getRecordsByEmployee(long employeeId, long startDate, long endDate) {
        return Single.fromCallable(() -> attendanceRecordDao.getRecordsByEmployee(employeeId, startDate, endDate))
                .subscribeOn(Schedulers.io());
    }

    public Single<List<AttendanceRecordEntity>> getLateRecords(long startDate, long endDate) {
        return Single.fromCallable(() -> attendanceRecordDao.getLateRecords(startDate, endDate))
                .subscribeOn(Schedulers.io());
    }

    public Single<List<AttendanceRecordEntity>> getEarlyLeaveRecords(long startDate, long endDate) {
        return Single.fromCallable(() -> attendanceRecordDao.getEarlyLeaveRecords(startDate, endDate))
                .subscribeOn(Schedulers.io());
    }

    public Single<List<AttendanceRecordEntity>> getAbsentRecords(long startDate, long endDate) {
        return Single.fromCallable(() -> attendanceRecordDao.getAbsentRecords(startDate, endDate))
                .subscribeOn(Schedulers.io());
    }

    public Single<List<AttendanceRecordEntity>> getRecords(int limit, int offset) {
        return Single.fromCallable(() -> attendanceRecordDao.getRecords(limit, offset))
                .subscribeOn(Schedulers.io());
    }

    public Single<Integer> getRecordCount() {
        return Single.fromCallable(attendanceRecordDao::getRecordCount)
                .subscribeOn(Schedulers.io());
    }

    public Single<List<HolidayEntity>> getAllHolidays() {
        return Single.fromCallable(holidayDao::getAllHolidays)
                .subscribeOn(Schedulers.io());
    }

    public Single<Boolean> isHoliday(long date) {
        return Single.fromCallable(() -> holidayDao.isHoliday(date) != null)
                .subscribeOn(Schedulers.io());
    }

    public Single<Boolean> isWorkday(long date) {
        return Single.fromCallable(() -> holidayDao.isWorkday(date) != null)
                .subscribeOn(Schedulers.io());
    }

    public Single<Object> insertHolidays(List<HolidayEntity> holidays) {
        return Single.fromCallable(() -> {
            holidayDao.insertAll(holidays);
            return new Object();
        }).subscribeOn(Schedulers.io());
    }

    public static long getTodayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static long getTodayEnd() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public static long[] getMonthRange(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long start = calendar.getTimeInMillis();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long end = calendar.getTimeInMillis();

        return new long[]{start, end};
    }

    public static int getDayOfWeek(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SUNDAY ? 7 : day - 1;
    }
}