package com.arcsoft.arcfacedemo.attendance.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.arcsoft.arcfacedemo.attendance.model.HolidayEntity;

import java.util.List;

/**
 * 节假日DAO
 */
@Dao
public interface HolidayDao {

    /**
     * 获取所有节假日
     */
    @Query("SELECT * FROM holiday ORDER BY date ASC")
    List<HolidayEntity> getAllHolidays();

    /**
     * 根据日期范围获取节假日
     */
    @Query("SELECT * FROM holiday WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    List<HolidayEntity> getHolidaysByDateRange(long startDate, long endDate);

    /**
     * 判断指定日期是否为节假日
     */
    @Query("SELECT * FROM holiday WHERE date = :date AND holiday_type = 'HOLIDAY' LIMIT 1")
    HolidayEntity isHoliday(long date);

    /**
     * 判断指定日期是否为调休工作日
     */
    @Query("SELECT * FROM holiday WHERE date = :date AND holiday_type = 'WORKDAY' LIMIT 1")
    HolidayEntity isWorkday(long date);

    /**
     * 插入节假日
     */
    @Insert
    long insert(HolidayEntity holiday);

    /**
     * 批量插入节假日
     */
    @Insert
    void insertAll(List<HolidayEntity> holidays);

    /**
     * 更新节假日
     */
    @Update
    int update(HolidayEntity holiday);

    /**
     * 删除节假日
     */
    @Delete
    int delete(HolidayEntity holiday);

    /**
     * 删除指定日期之前的节假日
     */
    @Query("DELETE FROM holiday WHERE date < :beforeDate")
    int deleteHolidaysBefore(long beforeDate);
}
