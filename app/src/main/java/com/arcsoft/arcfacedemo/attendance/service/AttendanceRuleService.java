package com.arcsoft.arcfacedemo.attendance.service;

import android.content.Context;

import com.arcsoft.arcfacedemo.attendance.model.AttendanceRuleEntity;
import com.arcsoft.arcfacedemo.attendance.repository.AttendanceRepository;

import java.util.Calendar;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;

/**
 * 考勤规则服务
 */
public class AttendanceRuleService {

    private static AttendanceRuleService instance;

    private AttendanceRepository attendanceRepository;

    private AttendanceRuleService(Context context) {
        this.attendanceRepository = AttendanceRepository.getInstance(context);
    }

    public static synchronized AttendanceRuleService getInstance(Context context) {
        if (instance == null) {
            instance = new AttendanceRuleService(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 获取考勤规则
     */
    public Single<AttendanceRuleEntity> getRule() {
        return attendanceRepository.getAttendanceRule();
    }

    /**
     * 更新考勤规则
     */
    public Single<Long> updateRule(AttendanceRuleEntity rule) {
        return attendanceRepository.updateAttendanceRule(rule);
    }

    /**
     * 判断是否为工作日
     */
    public Single<Boolean> isWorkDay(long date) {
        return Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            AttendanceRuleEntity rule = attendanceRepository.getAttendanceRule().blockingGet();

            // 检查是否为节假日
            Boolean isHoliday = attendanceRepository.isHoliday(date).blockingGet();
            if (isHoliday != null && isHoliday) {
                emitter.onSuccess(false);
                return;
            }

            // 检查是否为调休工作日
            Boolean isWorkday = attendanceRepository.isWorkday(date).blockingGet();
            if (isWorkday != null && isWorkday) {
                emitter.onSuccess(true);
                return;
            }

            // 检查工作日配置
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            // 转换为 bitmask: 周日=1, 周一=2, ..., 周六=64
            int dayBit = 1 << (dayOfWeek - 1);

            boolean isWorkDay = (rule.getWorkDays() & dayBit) != 0;
            emitter.onSuccess(isWorkDay);
        }).subscribeOn(io.reactivex.schedulers.Schedulers.io());
    }

    /**
     * 获取指定日期的上班开始时间
     */
    public Single<Long> getWorkStartTime(long date) {
        return Single.<Long>create(emitter -> {
            AttendanceRuleEntity rule = attendanceRepository.getAttendanceRule().blockingGet();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);

            // 获取当天的毫秒数（从0点开始）
            long startTime = rule.getMorningStartTime();

            // 组合成完整时间戳
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long result = calendar.getTimeInMillis() + startTime;
            emitter.onSuccess(result);
        }).subscribeOn(io.reactivex.schedulers.Schedulers.io());
    }

    /**
     * 获取指定日期的上班结束时间
     */
    public Single<Long> getWorkEndTime(long date) {
        return Single.<Long>create(emitter -> {
            AttendanceRuleEntity rule = attendanceRepository.getAttendanceRule().blockingGet();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);

            long endTime = rule.getAfternoonEndTime();

            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long result = calendar.getTimeInMillis() + endTime;
            emitter.onSuccess(result);
        }).subscribeOn(io.reactivex.schedulers.Schedulers.io());
    }

    /**
     * 获取上午上班开始时间
     */
    public Single<Long> getMorningStartTime(long date) {
        return getStartTime(date, true);
    }

    /**
     * 获取上午上班结束时间
     */
    public Single<Long> getMorningEndTime(long date) {
        return getEndTime(date, true);
    }

    /**
     * 获取下午上班开始时间
     */
    public Single<Long> getAfternoonStartTime(long date) {
        return getStartTime(date, false);
    }

    /**
     * 获取下午上班结束时间
     */
    public Single<Long> getAfternoonEndTime(long date) {
        return getEndTime(date, false);
    }

    private Single<Long> getStartTime(long date, boolean isMorning) {
        return Single.<Long>create(emitter -> {
            AttendanceRuleEntity rule = attendanceRepository.getAttendanceRule().blockingGet();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);

            long time = isMorning ? rule.getMorningStartTime() : rule.getAfternoonStartTime();

            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            emitter.onSuccess(calendar.getTimeInMillis() + time);
        }).subscribeOn(io.reactivex.schedulers.Schedulers.io());
    }

    private Single<Long> getEndTime(long date, boolean isMorning) {
        return Single.<Long>create(emitter -> {
            AttendanceRuleEntity rule = attendanceRepository.getAttendanceRule().blockingGet();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);

            long time = isMorning ? rule.getMorningEndTime() : rule.getAfternoonEndTime();

            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            emitter.onSuccess(calendar.getTimeInMillis() + time);
        }).subscribeOn(io.reactivex.schedulers.Schedulers.io());
    }
}
