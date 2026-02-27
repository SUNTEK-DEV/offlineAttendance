package com.arcsoft.arcfacedemo.employee.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.arcsoft.arcfacedemo.employee.model.DepartmentEntity;

import java.util.List;

/**
 * 部门DAO
 */
@Dao
public interface DepartmentDao {

    /**
     * 获取所有部门
     */
    @Query("SELECT * FROM department ORDER BY sort_order ASC")
    List<DepartmentEntity> getAllDepartments();

    /**
     * 根据ID获取部门
     */
    @Query("SELECT * FROM department WHERE department_id = :departmentId LIMIT 1")
    DepartmentEntity getDepartmentById(long departmentId);

    /**
     * 根据名称搜索部门
     */
    @Query("SELECT * FROM department WHERE department_name LIKE '%' || :keyword || '%' ORDER BY sort_order ASC")
    List<DepartmentEntity> searchDepartments(String keyword);

    /**
     * 插入部门
     */
    @Insert
    long insert(DepartmentEntity department);

    /**
     * 更新部门
     */
    @Update
    int update(DepartmentEntity department);

    /**
     * 删除部门
     */
    @Delete
    int delete(DepartmentEntity department);

    /**
     * 获取部门数量
     */
    @Query("SELECT COUNT(1) FROM department")
    int getDepartmentCount();
}
