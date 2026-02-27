package com.arcsoft.arcfacedemo.attendance.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.arcsoft.arcfacedemo.attendance.model.AttendanceRecordEntity;

import java.util.List;

/**
 * 考勤记录DAO
 */
@Dao
public interface AttendanceRecordDao {

    /**
     * 根据日期范围获取记录
     */
    @Query("SELECT * FROM attendance_record WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, check_in_time DESC")
    List<AttendanceRecordEntity> getRecordsByDateRange(long startDate, long endDate);

    /**
     * 根据员工ID和日期范围获取记录
     */
    @Query("SELECT * FROM attendance_record WHERE employee_id = :employeeId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<AttendanceRecordEntity> getRecordsByEmployee(long employeeId, long startDate, long endDate);

    /**
     * 获取员工某一天的考勤记录
     */
    @Query("SELECT * FROM attendance_record WHERE employee_id = :employeeId AND date = :date LIMIT 1")
    AttendanceRecordEntity getTodayRecord(long employeeId, long date);

    /**
     * 获取某一天的所有考勤记录
     */
    @Query("SELECT * FROM attendance_record WHERE date = :date ORDER BY check_in_time ASC")
    List<AttendanceRecordEntity> getRecordsByDate(long date);

    /**
     * 插入或更新记录（使用CONFLICT_REPLACE）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrUpdate(AttendanceRecordEntity record);

    /**
     * 更新记录
     */
    @Update
    int update(AttendanceRecordEntity record);

    /**
     * 获取指定日期范围内的迟到记录
     */
    @Query("SELECT * FROM attendance_record WHERE date BETWEEN :startDate AND :endDate AND check_in_status = 'LATE' ORDER BY date DESC")
    List<AttendanceRecordEntity> getLateRecords(long startDate, long endDate);

    /**
     * 获取指定日期范围内的早退记录
     */
    @Query("SELECT * FROM attendance_record WHERE date BETWEEN :startDate AND :endDate AND check_out_status = 'EARLY' ORDER BY date DESC")
    List<AttendanceRecordEntity> getEarlyLeaveRecords(long startDate, long endDate);

    /**
     * 获取指定日期范围内的缺勤记录
     */
    @Query("SELECT * FROM attendance_record WHERE date BETWEEN :startDate AND :endDate AND (check_in_status = 'ABSENT' OR check_out_status = 'ABSENT') ORDER BY date DESC")
    List<AttendanceRecordEntity> getAbsentRecords(long startDate, long endDate);

    /**
     * 分页获取记录
     */
    @Query("SELECT * FROM attendance_record ORDER BY date DESC, check_in_time DESC LIMIT :limit OFFSET :offset")
    List<AttendanceRecordEntity> getRecords(int limit, int offset);

    /**
     * 获取记录总数
     */
    @Query("SELECT COUNT(1) FROM attendance_record")
    int getRecordCount();

    /**
     * 获取员工在指定日期范围内的记录数
     */
    @Query("SELECT COUNT(1) FROM attendance_record WHERE employee_id = :employeeId AND date BETWEEN :startDate AND :endDate")
    int getRecordCountByEmployee(long employeeId, long startDate, long endDate);

    /**
     * 删除指定日期之前的记录（用于数据清理）
     */
    @Query("DELETE FROM attendance_record WHERE date < :beforeDate")
    int deleteRecordsBefore(long beforeDate);
}
