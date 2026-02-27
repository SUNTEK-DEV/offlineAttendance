package com.arcsoft.arcfacedemo.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * 打卡响应
 */
public class PunchResponse {
    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private PunchData data;

    public PunchResponse() {
    }

    public PunchResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public PunchData getData() {
        return data;
    }

    /**
     * 判断打卡是否成功
     */
    public boolean isSuccess() {
        return code == 200;
    }

    /**
     * 获取记录ID
     */
    public String getRecordId() {
        return data != null ? data.getRecordId() : null;
    }

    /**
     * 打卡数据
     */
    public static class PunchData {
        @SerializedName("record_id")
        private String recordId;

        public PunchData() {
        }

        public String getRecordId() {
            return recordId;
        }

        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }
    }
}
