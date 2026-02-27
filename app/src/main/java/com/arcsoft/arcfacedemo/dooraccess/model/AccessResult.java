package com.arcsoft.arcfacedemo.dooraccess.model;

/**
 * 门禁访问结果
 */
public class AccessResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果: SUCCESS(成功), FAILED(失败), DENIED(拒绝)
     */
    private String result;

    /**
     * 员工ID
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
     * 访问类型: FACE(人脸), CARD(卡), PASSWORD(密码), VISITOR(访客)
     */
    private String accessType;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 访问时间戳
     */
    private long accessTime;

    /**
     * 记录ID（保存到数据库后）
     */
    private long recordId;

    public AccessResult() {
        this.accessTime = System.currentTimeMillis();
    }

    public AccessResult(boolean success, String result, String message) {
        this.success = success;
        this.result = result;
        this.message = message;
        this.accessTime = System.currentTimeMillis();
    }

    public static AccessResult success(long employeeId, String employeeNo, String employeeName) {
        AccessResult result = new AccessResult();
        result.setSuccess(true);
        result.setResult("SUCCESS");
        result.setEmployeeId(employeeId);
        result.setEmployeeNo(employeeNo);
        result.setEmployeeName(employeeName);
        result.setMessage("门禁验证成功");
        return result;
    }

    public static AccessResult failed(String failReason, String message) {
        AccessResult result = new AccessResult();
        result.setSuccess(false);
        result.setResult("FAILED");
        result.setFailReason(failReason);
        result.setMessage(message);
        return result;
    }

    public static AccessResult denied(String reason) {
        AccessResult result = new AccessResult();
        result.setSuccess(false);
        result.setResult("DENIED");
        result.setFailReason(reason);
        result.setMessage("门禁访问被拒绝");
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
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

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }

    public long getRecordId() {
        return recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    @Override
    public String toString() {
        return "AccessResult{" +
                "success=" + success +
                ", result='" + result + '\'' +
                ", employeeName='" + employeeName + '\'' +
                ", message='" + message + '\'' +
                ", failReason='" + failReason + '\'' +
                '}';
    }
}
