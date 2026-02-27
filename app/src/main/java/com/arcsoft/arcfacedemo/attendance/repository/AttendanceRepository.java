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

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * 考勤仓库
 */
public class AttendanceRepository {

    private static AttendanceRepository instance;

    private AttendanceRecordDao attendanceRecordDao;
    private AttendanceRuleDao attendanceRuleDao;
    private HolidayDao holidayDao;

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

    // ==================== 考勤规则 ====================

    /**
     * 获取考勤规则
     */
    public Single<AttendanceRuleEntity> getAttendanceRule() {
        return Single.create((SingleOnSubscribe<AttendanceRuleEntity>) emitter -> {
            AttendanceRuleEntity rule = attendanceRuleDao.getRule();
            if (rule == null) {
                // 如果规则不存在，创建默认规则
                rule = new AttendanceRuleEntity();
                long id = attendanceRuleDao.insertOrUpdate(rule);
                rule.setId(id);
            }
            emitter.onSuccess(rule);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 更新考勤规则
     */
    public Single<Long> updateAttendanceRule(AttendanceRuleEntity rule) {
        return Single.fromCallable(() -> {
            rule.setUpdateTime(System.currentTimeMillis());
            return attendanceRuleDao.insertOrUpdate(rule);
        }).subscribeOn(Schedulers.io());
    }

    // ==================== 考勤记录 ====================

    /**
     * 获取今日考勤记录
     */
    public Single<AttendanceRecordEntity> getTodayRecord(long employeeId) {
        return Single.create((SingleOnSubscribe<AttendanceRecordEntity>) emitter -> {
            long today = getTodayStart();
            AttendanceRecordEntity record = attendanceRecordDao.getTodayRecord(employeeId, today);
            // getTodayRecord 可能返回 null，Single 允许发射 null
            emitter.onSuccess(record);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 插入或更新考勤记录
     */
    public Single<Long> insertOrUpdateRecord(AttendanceRecordEntity record) {
        return Single.fromCallable(() -> {
            record.setUpdateTime(System.currentTimeMillis());
            return attendanceRecordDao.insertOrUpdate(record);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 根据日期范围获取记录
     */
    public Single<List<AttendanceRecordEntity>> getRecordsByDateRange(long startDate, long endDate) {
        return Single.fromCallable(() ->
                attendanceRecordDao.getRecordsByDateRange(startDate, endDate)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 根据员工获取记录
     */
    public Single<List<AttendanceRecordEntity>> getRecordsByEmployee(long employeeId, long startDate, long endDate) {
        return Single.fromCallable(() ->
                attendanceRecordDao.getRecordsByEmployee(employeeId, startDate, endDate)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 获取迟到记录
     */
    public Single<List<AttendanceRecordEntity>> getLateRecords(long startDate, long endDate) {
        return Single.fromCallable(() ->
                attendanceRecordDao.getLateRecords(startDate, endDate)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 获取早退记录
     */
    public Single<List<AttendanceRecordEntity>> getEarlyLeaveRecords(long startDate, long endDate) {
        return Single.fromCallable(() ->
                attendanceRecordDao.getEarlyLeaveRecords(startDate, endDate)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 获取缺勤记录
     */
    public Single<List<AttendanceRecordEntity>> getAbsentRecords(long startDate, long endDate) {
        return Single.fromCallable(() ->
                attendanceRecordDao.getAbsentRecords(startDate, endDate)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 分页获取记录
     */
    public Single<List<AttendanceRecordEntity>> getRecords(int limit, int offset) {
        return Single.fromCallable(() ->
                attendanceRecordDao.getRecords(limit, offset)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 获取记录总数
     */
    public Single<Integer> getRecordCount() {
        return Single.fromCallable(attendanceRecordDao::getRecordCount)
                .subscribeOn(Schedulers.io());
    }

    // ==================== 节假日 ====================

    /**
     * 获取所有节假日
     */
    public Single<List<HolidayEntity>> getAllHolidays() {
        return Single.fromCallable(holidayDao::getAllHolidays)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 判断是否为节假日
     */
    public Single<Boolean> isHoliday(long date) {
        return Single.fromCallable(() -> {
            HolidayEntity holiday = holidayDao.isHoliday(date);
            return holiday != null;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 判断是否为调休工作日
     */
    public Single<Boolean> isWorkday(long date) {
        return Single.fromCallable(() -> {
            HolidayEntity workday = holidayDao.isWorkday(date);
            return workday != null;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 批量插入节假日
     */
    public Single<Object> insertHolidays(List<HolidayEntity> holidays) {
        return Single.fromCallable(() -> {
            holidayDao.insertAll(holidays);
            return new Object();
        }).subscribeOn(Schedulers.io());
    }

    // ==================== 工具方法 ====================

    /**
     * 获取今天0点的时间戳
     */
    public static long getTodayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取今天23:59:59的时间戳
     */
    public static long getTodayEnd() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取月份范围
     */
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

    /**
     * 获取星期几 (1=周一, 7=周日)
     */
    public static int getDayOfWeek(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        // 转换为周一=1, 周日=7
        return day == Calendar.SUNDAY ? 7 : day - 1;
    }
}
