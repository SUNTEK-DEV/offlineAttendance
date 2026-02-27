package com.arcsoft.arcfacedemo.employee.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 岗位实体
 */
@Entity(tableName = "position")
public class PositionEntity implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "position_id")
    private long positionId;

    /**
     * 岗位名称
     */
    @ColumnInfo(name = "position_name")
    private String positionName;

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

    public PositionEntity() {
        this.createTime = System.currentTimeMillis();
        this.sortOrder = 0;
    }

    @Ignore
    public PositionEntity(String positionName, String description) {
        this.positionName = positionName;
        this.description = description;
        this.createTime = System.currentTimeMillis();
        this.sortOrder = 0;
    }

    protected PositionEntity(Parcel in) {
        positionId = in.readLong();
        positionName = in.readString();
        description = in.readString();
        sortOrder = in.readInt();
        createTime = in.readLong();
    }

    public static final Creator<PositionEntity> CREATOR = new Creator<PositionEntity>() {
        @Override
        public PositionEntity createFromParcel(Parcel in) {
            return new PositionEntity(in);
        }

        @Override
        public PositionEntity[] newArray(int size) {
            return new PositionEntity[size];
        }
    };

    public long getPositionId() {
        return positionId;
    }

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
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
        dest.writeLong(positionId);
        dest.writeString(positionName);
        dest.writeString(description);
        dest.writeInt(sortOrder);
        dest.writeLong(createTime);
    }
}
