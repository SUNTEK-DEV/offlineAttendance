package com.arcsoft.arcfacedemo.attendance.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.arcsoft.arcfacedemo.attendance.model.AttendanceRuleEntity;

/**
 * 考勤规则DAO
 */
@Dao
public interface AttendanceRuleDao {

    /**
     * 获取考勤规则（单例）
     */
    @Query("SELECT * FROM attendance_rule WHERE id = 1 LIMIT 1")
    AttendanceRuleEntity getRule();

    /**
     * 插入或更新规则
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrUpdate(AttendanceRuleEntity rule);

    /**
     * 更新规则
     */
    @Update
    int update(AttendanceRuleEntity rule);
}
