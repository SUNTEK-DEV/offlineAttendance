package com.arcsoft.arcfacedemo.dooraccess.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.arcsoft.arcfacedemo.dooraccess.model.DoorAccessRecordEntity;

import java.util.List;

/**
 * 门禁记录DAO
 */
@Dao
public interface DoorAccessRecordDao {

    /**
     * 根据日期范围获取记录
     */
    @Query("SELECT * FROM door_access_record WHERE access_time BETWEEN :startTime AND :endTime ORDER BY access_time DESC")
    List<DoorAccessRecordEntity> getRecordsByDateRange(long startTime, long endTime);

    /**
     * 根据员工ID获取记录
     */
    @Query("SELECT * FROM door_access_record WHERE employee_id = :employeeId ORDER BY access_time DESC LIMIT :limit")
    List<DoorAccessRecordEntity> getRecordsByEmployee(long employeeId, int limit);

    /**
     * 获取成功的记录
     */
    @Query("SELECT * FROM door_access_record WHERE access_result = 'SUCCESS' ORDER BY access_time DESC LIMIT :limit OFFSET :offset")
    List<DoorAccessRecordEntity> getSuccessRecords(int limit, int offset);

    /**
     * 获取失败的记录
     */
    @Query("SELECT * FROM door_access_record WHERE access_result != 'SUCCESS' ORDER BY access_time DESC LIMIT :limit OFFSET :offset")
    List<DoorAccessRecordEntity> getFailedRecords(int limit, int offset);

    /**
     * 插入记录
     */
    @Insert
    long insert(DoorAccessRecordEntity record);

    /**
     * 分页获取所有记录
     */
    @Query("SELECT * FROM door_access_record ORDER BY access_time DESC LIMIT :limit OFFSET :offset")
    List<DoorAccessRecordEntity> getRecords(int limit, int offset);

    /**
     * 获取记录总数
     */
    @Query("SELECT COUNT(1) FROM door_access_record")
    int getRecordCount();

    /**
     * 获取成功记录数量
     */
    @Query("SELECT COUNT(1) FROM door_access_record WHERE access_result = 'SUCCESS'")
    int getSuccessCount();

    /**
     * 获取失败记录数量
     */
    @Query("SELECT COUNT(1) FROM door_access_record WHERE access_result != 'SUCCESS'")
    int getFailedCount();

    /**
     * 删除指定日期之前的记录（用于数据清理）
     */
    @Query("DELETE FROM door_access_record WHERE access_time < :beforeTime")
    int deleteRecordsBefore(long beforeTime);

    /**
     * 获取最近的访问记录
     */
    @Query("SELECT * FROM door_access_record ORDER BY access_time DESC LIMIT :limit")
    List<DoorAccessRecordEntity> getRecentRecords(int limit);
}
