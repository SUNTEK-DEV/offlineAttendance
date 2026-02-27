package com.arcsoft.arcfacedemo.attendance.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 节假日实体
 */
@Entity(tableName = "holiday")
public class HolidayEntity implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private long holidayId;

    /**
     * 日期 (时间戳，0点)
     */
    @ColumnInfo(name = "date")
    private long date;

    /**
     * 类型: HOLIDAY(节假日), WORKDAY(调休工作日)
     */
    @ColumnInfo(name = "holiday_type")
    private String holidayType;

    /**
     * 名称: "春节", "国庆" 等
     */
    @ColumnInfo(name = "name")
    private String name;

    /**
     * 备注
     */
    @ColumnInfo(name = "remark")
    private String remark;

    public HolidayEntity() {
    }

    @Ignore
    public HolidayEntity(long date, String holidayType, String name) {
        this.date = date;
        this.holidayType = holidayType;
        this.name = name;
    }

    @Ignore
    protected HolidayEntity(Parcel in) {
        holidayId = in.readLong();
        date = in.readLong();
        holidayType = in.readString();
        name = in.readString();
        remark = in.readString();
    }

    public static final Creator<HolidayEntity> CREATOR = new Creator<HolidayEntity>() {
        @Override
        public HolidayEntity createFromParcel(Parcel in) {
            return new HolidayEntity(in);
        }

        @Override
        public HolidayEntity[] newArray(int size) {
            return new HolidayEntity[size];
        }
    };

    public long getHolidayId() {
        return holidayId;
    }

    public void setHolidayId(long holidayId) {
        this.holidayId = holidayId;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(String holidayType) {
        this.holidayType = holidayType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(holidayId);
        dest.writeLong(date);
        dest.writeString(holidayType);
        dest.writeString(name);
        dest.writeString(remark);
    }
}
