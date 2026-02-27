package com.arcsoft.arcfacedemo.attendance.model;

/**
 * 考勤统计数据
 */
public class AttendanceStatistics {

    /**
     * 统计的员工ID
     */
    private long employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 统计起始日期
     */
    private long startDate;

    /**
     * 统计结束日期
     */
    private long endDate;

    /**
     * 应出勤天数
     */
    private int shouldAttendDays;

    /**
     * 实际出勤天数
     */
    private int actualAttendDays;

    /**
     * 出勤率（百分比）
     */
    private double attendanceRate;

    /**
     * 正常次数
     */
    private int normalCount;

    /**
     * 迟到次数
     */
    private int lateCount;

    /**
     * 早退次数
     */
    private int earlyLeaveCount;

    /**
     * 缺勤次数
     */
    private int absentCount;

    /**
     * 加班次数
     */
    private int overtimeCount;

    public AttendanceStatistics() {
    }

    public long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public int getShouldAttendDays() {
        return shouldAttendDays;
    }

    public void setShouldAttendDays(int shouldAttendDays) {
        this.shouldAttendDays = shouldAttendDays;
    }

    public int getActualAttendDays() {
        return actualAttendDays;
    }

    public void setActualAttendDays(int actualAttendDays) {
        this.actualAttendDays = actualAttendDays;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public int getNormalCount() {
        return normalCount;
    }

    public void setNormalCount(int normalCount) {
        this.normalCount = normalCount;
    }

    public int getLateCount() {
        return lateCount;
    }

    public void setLateCount(int lateCount) {
        this.lateCount = lateCount;
    }

    public int getEarlyLeaveCount() {
        return earlyLeaveCount;
    }

    public void setEarlyLeaveCount(int earlyLeaveCount) {
        this.earlyLeaveCount = earlyLeaveCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
    }

    public int getOvertimeCount() {
        return overtimeCount;
    }

    public void setOvertimeCount(int overtimeCount) {
        this.overtimeCount = overtimeCount;
    }
}
