package com.arcsoft.arcfacedemo.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * 打卡请求
 */
public class PunchRequest {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("username")
    private String username;

    @SerializedName("phone")
    private String phone;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("address")
    private String address;

    @SerializedName("attendance_type")
    private int attendanceType;

    @SerializedName("remark")
    private String remark;

    private PunchRequest(Builder builder) {
        this.userId = builder.userId;
        this.username = builder.username;
        this.phone = builder.phone;
        this.timestamp = builder.timestamp;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.address = builder.address;
        this.attendanceType = builder.attendanceType;
        this.remark = builder.remark;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPhone() {
        return phone;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }

    public int getAttendanceType() {
        return attendanceType;
    }

    public String getRemark() {
        return remark;
    }

    /**
     * Builder 模式创建 PunchRequest
     */
    public static class Builder {
        private String userId;
        private String username;
        private String phone;
        private long timestamp = System.currentTimeMillis() / 1000; // 默认当前时间戳（秒）
        private double latitude;
        private double longitude;
        private String address;
        private int attendanceType = 1; // 默认上班打卡
        private String remark = "人脸识别打卡"; // 默认备注

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder latitude(double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder attendanceType(int attendanceType) {
            this.attendanceType = attendanceType;
            return this;
        }

        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public PunchRequest build() {
            return new PunchRequest(this);
        }
    }
}
