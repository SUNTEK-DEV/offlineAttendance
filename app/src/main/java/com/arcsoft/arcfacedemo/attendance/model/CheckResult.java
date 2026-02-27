package com.arcsoft.arcfacedemo.attendance.model;

/**
 * 打卡结果
 */
public class CheckResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 打卡类型: CHECK_IN(上班), CHECK_OUT(下班)
     */
    private String checkType;

    /**
     * 打卡状态: NORMAL(正常), LATE(迟到), EARLY(早退), ABSENT(缺勤)
     */
    private String status;

    /**
     * 打卡时间戳
     */
    private long checkTime;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 考勤记录ID（如果成功）
     */
    private long recordId;

    /**
     * 错误信息（如果失败）
     */
    private String error;

    public CheckResult() {
    }

    public CheckResult(boolean success, String checkType, String status, String message) {
        this.success = success;
        this.checkType = checkType;
        this.status = status;
        this.message = message;
        this.checkTime = System.currentTimeMillis();
    }

    public static CheckResult success(String checkType, String status, String message) {
        return new CheckResult(true, checkType, status, message);
    }

    public static CheckResult failed(String error) {
        CheckResult result = new CheckResult();
        result.setSuccess(false);
        result.setError(error);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(long checkTime) {
        this.checkTime = checkTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getRecordId() {
        return recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "CheckResult{" +
                "success=" + success +
                ", checkType='" + checkType + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
