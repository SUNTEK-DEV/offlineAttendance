package com.arcsoft.arcfacedemo.employee.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 部门实体
 */
@Entity(tableName = "department")
public class DepartmentEntity implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "department_id")
    private long departmentId;

    /**
     * 部门名称
     */
    @ColumnInfo(name = "department_name")
    private String departmentName;

    /**
     * 描述
     */
    @ColumnInfo(name = "description")
    private String description;

    /**
     * 排序
     */
    @ColumnInfo(name = "sort_order")
    private int sortOrder;

    /**
     * 创建时间
     */
    @ColumnInfo(name = "create_time")
    private long createTime;

    public DepartmentEntity() {
        this.createTime = System.currentTimeMillis();
        this.sortOrder = 0;
    }

    @Ignore
    public DepartmentEntity(String departmentName, String description) {
        this.departmentName = departmentName;
        this.description = description;
        this.createTime = System.currentTimeMillis();
        this.sortOrder = 0;
    }

    protected DepartmentEntity(Parcel in) {
        departmentId = in.readLong();
        departmentName = in.readString();
        description = in.readString();
        sortOrder = in.readInt();
        createTime = in.readLong();
    }

    public static final Creator<DepartmentEntity> CREATOR = new Creator<DepartmentEntity>() {
        @Override
        public DepartmentEntity createFromParcel(Parcel in) {
            return new DepartmentEntity(in);
        }

        @Override
        public DepartmentEntity[] newArray(int size) {
            return new DepartmentEntity[size];
        }
    };

    public long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(departmentId);
        dest.writeString(departmentName);
        dest.writeString(description);
        dest.writeInt(sortOrder);
        dest.writeLong(createTime);
    }
}
