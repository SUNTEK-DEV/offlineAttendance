package com.arcsoft.arcfacedemo.attendance.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * 考勤记录实体
 */
@Entity(
        tableName = "attendance_record",
        foreignKeys = {
                @ForeignKey(
                        entity = com.arcsoft.arcfacedemo.employee.model.EmployeeEntity.class,
                        parentColumns = "employee_id",
                        childColumns = "employee_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"employee_id", "date"}, unique = true),
                @Index("date"),
                @Index("check_in_time"),
                @Index("check_out_time")
        }
)
public class AttendanceRecordEntity implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private long recordId;

    /**
     * 员工ID
     */
    @ColumnInfo(name = "employee_id")
    private long employeeId;

    /**
     * 工号 (冗余，方便查询)
     */
    @ColumnInfo(name = "employee_no")
    private String employeeNo;

    /**
     * 姓名 (冗余)
     */
    @ColumnInfo(name = "employee_name")
    private String employeeName;

    /**
     * 考勤日期 (yyyy-MM-DD 0点的时间戳)
     */
    @ColumnInfo(name = "date")
    private long date;

    /**
     * 上班打卡时间
     */
    @ColumnInfo(name = "check_in_time")
    private long checkInTime;

    /**
     * 下班打卡时间
     */
    @ColumnInfo(name = "check_out_time")
    private long checkOutTime;

    /**
     * 上班状态: NORMAL(正常), LATE(迟到), ABSENT(缺勤)
     */
    @ColumnInfo(name = "check_in_status")
    private String checkInStatus;

    /**
     * 下班状态: NORMAL(正常), EARLY(早退), ABSENT(缺勤)
     */
    @ColumnInfo(name = "check_out_status")
    private String checkOutStatus;

    /**
     * 上班打卡照片路径
     */
    @ColumnInfo(name = "check_in_image_path")
    private String checkInImagePath;

    /**
     * 下班打卡照片路径
     */
    @ColumnInfo(name = "check_out_image_path")
    private String checkOutImagePath;

    /**
     * 工作状态: NORMAL(正常), OVERTIME(加班), LEAVE(请假)
     */
    @ColumnInfo(name = "work_status")
    private String workStatus;

    /**
     * 备注
     */
    @ColumnInfo(name = "remark")
    private String remark;

    /**
     * 创建时间
     */
    @ColumnInfo(name = "create_time")
    private long createTime;

    /**
     * 更新时间
     */
    @ColumnInfo(name = "update_time")
    private long updateTime;

    public AttendanceRecordEntity() {
        this.workStatus = "NORMAL";
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
    }

    @Ignore
    protected AttendanceRecordEntity(Parcel in) {
        recordId = in.readLong();
        employeeId = in.readLong();
        employeeNo = in.readString();
        employeeName = in.readString();
        date = in.readLong();
        checkInTime = in.readLong();
        checkOutTime = in.readLong();
        checkInStatus = in.readString();
        checkOutStatus = in.readString();
        checkInImagePath = in.readString();
        checkOutImagePath = in.readString();
        workStatus = in.readString();
        remark = in.readString();
        createTime = in.readLong();
        updateTime = in.readLong();
    }

    public static final Creator<AttendanceRecordEntity> CREATOR = new Creator<AttendanceRecordEntity>() {
        @Override
        public AttendanceRecordEntity createFromParcel(Parcel in) {
            return new AttendanceRecordEntity(in);
        }

        @Override
        public AttendanceRecordEntity[] newArray(int size) {
            return new AttendanceRecordEntity[size];
        }
    };

    public long getRecordId() {
        return recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    public long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(long checkInTime) {
        this.checkInTime = checkInTime;
    }

    public long getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(long checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getCheckInStatus() {
        return checkInStatus;
    }

    public void setCheckInStatus(String checkInStatus) {
        this.checkInStatus = checkInStatus;
    }

    public String getCheckOutStatus() {
        return checkOutStatus;
    }

    public void setCheckOutStatus(String checkOutStatus) {
        this.checkOutStatus = checkOutStatus;
    }

    public String getCheckInImagePath() {
        return checkInImagePath;
    }

    public void setCheckInImagePath(String checkInImagePath) {
        this.checkInImagePath = checkInImagePath;
    }

    public String getCheckOutImagePath() {
        return checkOutImagePath;
    }

    public void setCheckOutImagePath(String checkOutImagePath) {
        this.checkOutImagePath = checkOutImagePath;
    }

    public String getWorkStatus() {
        return workStatus;
    }

    public void setWorkStatus(String workStatus) {
        this.workStatus = workStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
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
        dest.writeLong(recordId);
        dest.writeLong(employeeId);
        dest.writeString(employeeNo);
        dest.writeString(employeeName);
        dest.writeLong(date);
        dest.writeLong(checkInTime);
        dest.writeLong(checkOutTime);
        dest.writeString(checkInStatus);
        dest.writeString(checkOutStatus);
        dest.writeString(checkInImagePath);
        dest.writeString(checkOutImagePath);
        dest.writeString(workStatus);
        dest.writeString(remark);
        dest.writeLong(createTime);
        dest.writeLong(updateTime);
    }
}
