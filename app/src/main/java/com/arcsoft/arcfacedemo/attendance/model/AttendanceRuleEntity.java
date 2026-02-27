package com.arcsoft.arcfacedemo.attendance.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 考勤规则实体
 * 单例模式，只有一条记录
 */
@Entity(tableName = "attendance_rule")
public class AttendanceRuleEntity implements Parcelable {

    @PrimaryKey
    private long id = 1;

    // ==================== 工作时间 ====================

    /**
     * 工作模式: FIXED(固定时间), FLEXIBLE(弹性时间)
     */
    @ColumnInfo(name = "work_mode")
    private String workMode;

    /**
     * 工作日 bitmask (1=周一, 2=周二, 4=周三, 8=周四, 16=周五, 32=周六, 64=周日)
     * 默认周一到周五: 1+2+4+8+16 = 31
     */
    @ColumnInfo(name = "work_days")
    private int workDays;

    /**
     * 上午上班时间 (毫秒，表示当天的毫秒数，如 8:30 = 8*3600*1000 + 30*60*1000)
     */
    @ColumnInfo(name = "morning_start_time")
    private long morningStartTime;

    /**
     * 上午下班时间
     */
    @ColumnInfo(name = "morning_end_time")
    private long morningEndTime;

    /**
     * 下午上班时间
     */
    @ColumnInfo(name = "afternoon_start_time")
    private long afternoonStartTime;

    /**
     * 下午下班时间
     */
    @ColumnInfo(name = "afternoon_end_time")
    private long afternoonEndTime;

    // ==================== 判定规则 ====================

    /**
     * 迟到容差(分钟)
     * 在容差时间内打卡不算迟到
     */
    @ColumnInfo(name = "late_tolerance")
    private int lateTolerance;

    /**
     * 早退容差(分钟)
     */
    @ColumnInfo(name = "early_leave_tolerance")
    private int earlyLeaveTolerance;

    /**
     * 加班判定阈值(分钟)
     * 超过这个时间的加班才算加班
     */
    @ColumnInfo(name = "overtime_threshold")
    private int overtimeThreshold;

    // ==================== 其他设置 ====================

    /**
     * 是否需要拍照
     */
    @ColumnInfo(name = "require_photo")
    private boolean requirePhoto;

    /**
     * 是否需要活体检测
     */
    @ColumnInfo(name = "require_liveness")
    private boolean requireLiveness;

    /**
     * 更新时间
     */
    @ColumnInfo(name = "update_time")
    private long updateTime;

    public AttendanceRuleEntity() {
        // 默认值
        this.workMode = "FIXED";
        this.workDays = 31; // 周一到周五
        // 8:30
        this.morningStartTime = 8 * 3600 * 1000L + 30 * 60 * 1000L;
        // 12:00
        this.morningEndTime = 12 * 3600 * 1000L;
        // 13:30
        this.afternoonStartTime = 13 * 3600 * 1000L + 30 * 60 * 1000L;
        // 18:00
        this.afternoonEndTime = 18 * 3600 * 1000L;
        this.lateTolerance = 5; // 5分钟容差
        this.earlyLeaveTolerance = 5;
        this.overtimeThreshold = 60; // 1小时以上才算加班
        this.requirePhoto = true;
        this.requireLiveness = true;
        this.updateTime = System.currentTimeMillis();
    }

    @Ignore
    protected AttendanceRuleEntity(Parcel in) {
        id = in.readLong();
        workMode = in.readString();
        workDays = in.readInt();
        morningStartTime = in.readLong();
        morningEndTime = in.readLong();
        afternoonStartTime = in.readLong();
        afternoonEndTime = in.readLong();
        lateTolerance = in.readInt();
        earlyLeaveTolerance = in.readInt();
        overtimeThreshold = in.readInt();
        requirePhoto = in.readByte() != 0;
        requireLiveness = in.readByte() != 0;
        updateTime = in.readLong();
    }

    public static final Creator<AttendanceRuleEntity> CREATOR = new Creator<AttendanceRuleEntity>() {
        @Override
        public AttendanceRuleEntity createFromParcel(Parcel in) {
            return new AttendanceRuleEntity(in);
        }

        @Override
        public AttendanceRuleEntity[] newArray(int size) {
            return new AttendanceRuleEntity[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getWorkMode() {
        return workMode;
    }

    public void setWorkMode(String workMode) {
        this.workMode = workMode;
    }

    public int getWorkDays() {
        return workDays;
    }

    public void setWorkDays(int workDays) {
        this.workDays = workDays;
    }

    public long getMorningStartTime() {
        return morningStartTime;
    }

    public void setMorningStartTime(long morningStartTime) {
        this.morningStartTime = morningStartTime;
    }

    public long getMorningEndTime() {
        return morningEndTime;
    }

    public void setMorningEndTime(long morningEndTime) {
        this.morningEndTime = morningEndTime;
    }

    public long getAfternoonStartTime() {
        return afternoonStartTime;
    }

    public void setAfternoonStartTime(long afternoonStartTime) {
        this.afternoonStartTime = afternoonStartTime;
    }

    public long getAfternoonEndTime() {
        return afternoonEndTime;
    }

    public void setAfternoonEndTime(long afternoonEndTime) {
        this.afternoonEndTime = afternoonEndTime;
    }

    public int getLateTolerance() {
        return lateTolerance;
    }

    public void setLateTolerance(int lateTolerance) {
        this.lateTolerance = lateTolerance;
    }

    public int getEarlyLeaveTolerance() {
        return earlyLeaveTolerance;
    }

    public void setEarlyLeaveTolerance(int earlyLeaveTolerance) {
        this.earlyLeaveTolerance = earlyLeaveTolerance;
    }

    public int getOvertimeThreshold() {
        return overtimeThreshold;
    }

    public void setOvertimeThreshold(int overtimeThreshold) {
        this.overtimeThreshold = overtimeThreshold;
    }

    public boolean isRequirePhoto() {
        return requirePhoto;
    }

    public void setRequirePhoto(boolean requirePhoto) {
        this.requirePhoto = requirePhoto;
    }

    public boolean isRequireLiveness() {
        return requireLiveness;
    }

    public void setRequireLiveness(boolean requireLiveness) {
        this.requireLiveness = requireLiveness;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(workMode);
        dest.writeInt(workDays);
        dest.writeLong(morningStartTime);
        dest.writeLong(morningEndTime);
        dest.writeLong(afternoonStartTime);
        dest.writeLong(afternoonEndTime);
        dest.writeInt(lateTolerance);
        dest.writeInt(earlyLeaveTolerance);
        dest.writeInt(overtimeThreshold);
        dest.writeByte((byte) (requirePhoto ? 1 : 0));
        dest.writeByte((byte) (requireLiveness ? 1 : 0));
        dest.writeLong(updateTime);
    }
}
