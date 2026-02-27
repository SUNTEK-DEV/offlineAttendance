package com.arcsoft.arcfacedemo.employee.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.arcsoft.arcfacedemo.employee.model.PositionEntity;

import java.util.List;

/**
 * 岗位DAO
 */
@Dao
public interface PositionDao {

    /**
     * 获取所有岗位
     */
    @Query("SELECT * FROM position ORDER BY sort_order ASC")
    List<PositionEntity> getAllPositions();

    /**
     * 根据ID获取岗位
     */
    @Query("SELECT * FROM position WHERE position_id = :positionId LIMIT 1")
    PositionEntity getPositionById(long positionId);

    /**
     * 根据名称搜索岗位
     */
    @Query("SELECT * FROM position WHERE position_name LIKE '%' || :keyword || '%' ORDER BY sort_order ASC")
    List<PositionEntity> searchPositions(String keyword);

    /**
     * 插入岗位
     */
    @Insert
    long insert(PositionEntity position);

    /**
     * 更新岗位
     */
    @Update
    int update(PositionEntity position);

    /**
     * 删除岗位
     */
    @Delete
    int delete(PositionEntity position);

    /**
     * 获取岗位数量
     */
    @Query("SELECT COUNT(1) FROM position")
    int getPositionCount();
}
