package com.arcsoft.arcfacedemo.employee.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * 员工实体
 */
@Entity(
        tableName = "employee",
        foreignKeys = {
                @ForeignKey(
                        entity = DepartmentEntity.class,
                        parentColumns = "department_id",
                        childColumns = "department_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = PositionEntity.class,
                        parentColumns = "position_id",
                        childColumns = "position_id",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index("employee_no"),
                @Index("department_id"),
                @Index("position_id")
        }
)
public class EmployeeEntity implements Parcelable {

    @PrimaryKey(autoGenerate = true)
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
    @ColumnInfo(name = "name")
    private String name;

    /**
     * 关联人脸ID (外键 -> face.faceId)
     */
    @ColumnInfo(name = "face_id")
    private long faceId;

    /**
     * 部门ID
     */
    @ColumnInfo(name = "department_id")
    private long departmentId;

    /**
     * 岗位ID
     */
    @ColumnInfo(name = "position_id")
    private long positionId;

    /**
     * 联系电话
     */
    @ColumnInfo(name = "phone")
    private String phone;

    /**
     * 状态: ACTIVE, INACTIVE, DELETED
     */
    @ColumnInfo(name = "status")
    private String status;

    /**
     * 入职日期
     */
    @ColumnInfo(name = "hire_date")
    private long hireDate;

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

    public EmployeeEntity() {
        this.status = "ACTIVE";
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.hireDate = System.currentTimeMillis();
    }

    @Ignore
    protected EmployeeEntity(Parcel in) {
        employeeId = in.readLong();
        employeeNo = in.readString();
        name = in.readString();
        faceId = in.readLong();
        departmentId = in.readLong();
        positionId = in.readLong();
        phone = in.readString();
        status = in.readString();
        hireDate = in.readLong();
        createTime = in.readLong();
        updateTime = in.readLong();
    }

    public static final Creator<EmployeeEntity> CREATOR = new Creator<EmployeeEntity>() {
        @Override
        public EmployeeEntity createFromParcel(Parcel in) {
            return new EmployeeEntity(in);
        }

        @Override
        public EmployeeEntity[] newArray(int size) {
            return new EmployeeEntity[size];
        }
    };

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFaceId() {
        return faceId;
    }

    public void setFaceId(long faceId) {
        this.faceId = faceId;
    }

    public long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(long departmentId) {
        this.departmentId = departmentId;
    }

    public long getPositionId() {
        return positionId;
    }

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getHireDate() {
        return hireDate;
    }

    public void setHireDate(long hireDate) {
        this.hireDate = hireDate;
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
        dest.writeLong(employeeId);
        dest.writeString(employeeNo);
        dest.writeString(name);
        dest.writeLong(faceId);
        dest.writeLong(departmentId);
        dest.writeLong(positionId);
        dest.writeString(phone);
        dest.writeString(status);
        dest.writeLong(hireDate);
        dest.writeLong(createTime);
        dest.writeLong(updateTime);
    }
}
