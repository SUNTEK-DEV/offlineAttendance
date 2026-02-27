package com.arcsoft.arcfacedemo.dooraccess.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.Index;

/**
 * 门禁记录实体
 */
@Entity(
        tableName = "door_access_record",
        indices = {
                @Index("employee_id"),
                @Index("access_time")
        }
)
public class DoorAccessRecordEntity implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private long recordId;

    /**
     * 员工ID (0表示访客)
     */
    @ColumnInfo(name = "employee_id")
    private long employeeId;

    /**
     * 工号
     */
    @ColumnInfo(name = "employee_no")
    private String employeeNo;

    /**
     * 姓名
     */
    @ColumnInfo(name = "employee_name")
    private String employeeName;

    /**
     * 类型: FACE(人脸), CARD(卡), PASSWORD(密码), VISITOR(访客)
     */
    @ColumnInfo(name = "access_type")
    private String accessType;

    /**
     * 结果: SUCCESS(成功), FAILED(失败), DENIED(拒绝)
     */
    @ColumnInfo(name = "access_result")
    private String accessResult;

    /**
     * 失败原因
     */
    @ColumnInfo(name = "fail_reason")
    private String failReason;

    /**
     * 抓拍图片路径
     */
    @ColumnInfo(name = "image_path")
    private String imagePath;

    /**
     * 访问时间
     */
    @ColumnInfo(name = "access_time")
    private long accessTime;

    /**
     * 设备信息
     */
    @ColumnInfo(name = "device_info")
    private String deviceInfo;

    public DoorAccessRecordEntity() {
        this.accessTime = System.currentTimeMillis();
    }

    @Ignore
    public DoorAccessRecordEntity(long employeeId, String employeeNo, String employeeName,
                                   String accessType, String accessResult, String imagePath) {
        this.employeeId = employeeId;
        this.employeeNo = employeeNo;
        this.employeeName = employeeName;
        this.accessType = accessType;
        this.accessResult = accessResult;
        this.imagePath = imagePath;
        this.accessTime = System.currentTimeMillis();
    }

    @Ignore
    protected DoorAccessRecordEntity(Parcel in) {
        recordId = in.readLong();
        employeeId = in.readLong();
        employeeNo = in.readString();
        employeeName = in.readString();
        accessType = in.readString();
        accessResult = in.readString();
        failReason = in.readString();
        imagePath = in.readString();
        accessTime = in.readLong();
        deviceInfo = in.readString();
    }

    public static final Creator<DoorAccessRecordEntity> CREATOR = new Creator<DoorAccessRecordEntity>() {
        @Override
        public DoorAccessRecordEntity createFromParcel(Parcel in) {
            return new DoorAccessRecordEntity(in);
        }

        @Override
        public DoorAccessRecordEntity[] newArray(int size) {
            return new DoorAccessRecordEntity[size];
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

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getAccessResult() {
        return accessResult;
    }

    public void setAccessResult(String accessResult) {
        this.accessResult = accessResult;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
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
        dest.writeString(accessType);
        dest.writeString(accessResult);
        dest.writeString(failReason);
        dest.writeString(imagePath);
        dest.writeLong(accessTime);
        dest.writeString(deviceInfo);
    }
}
