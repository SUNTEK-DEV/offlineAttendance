package com.arcsoft.arcfacedemo.employee.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Transaction;

import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;

import java.util.List;

/**
 * 员工DAO
 */
@Dao
public interface EmployeeDao {

    /**
     * 获取所有员工
     */
    @Query("SELECT * FROM employee WHERE status != 'DELETED' ORDER BY employee_id DESC")
    List<EmployeeEntity> getAllEmployees();

    /**
     * 根据ID获取员工
     */
    @Query("SELECT * FROM employee WHERE employee_id = :employeeId LIMIT 1")
    EmployeeEntity getEmployeeById(long employeeId);

    /**
     * 根据工号获取员工
     */
    @Query("SELECT * FROM employee WHERE employee_no = :employeeNo AND status != 'DELETED' LIMIT 1")
    EmployeeEntity getEmployeeByNo(String employeeNo);

    /**
     * 根据人脸ID获取员工
     */
    @Query("SELECT * FROM employee WHERE face_id = :faceId AND status != 'DELETED' LIMIT 1")
    EmployeeEntity getEmployeeByFaceId(long faceId);

    /**
     * 根据部门ID获取员工列表
     */
    @Query("SELECT * FROM employee WHERE department_id = :departmentId AND status != 'DELETED' ORDER BY employee_id DESC")
    List<EmployeeEntity> getEmployeesByDepartment(long departmentId);

    /**
     * 根据姓名或工号搜索员工
     */
    @Query("SELECT * FROM employee WHERE (name LIKE '%' || :keyword || '%' OR employee_no LIKE '%' || :keyword || '%') AND status != 'DELETED' ORDER BY employee_id DESC")
    List<EmployeeEntity> searchEmployees(String keyword);

    /**
     * 分页获取员工
     */
    @Query("SELECT * FROM employee WHERE status != 'DELETED' ORDER BY employee_id DESC LIMIT :limit OFFSET :offset")
    List<EmployeeEntity> getEmployees(int limit, int offset);

    /**
     * 插入员工
     */
    @Insert
    long insert(EmployeeEntity employee);

    /**
     * 更新员工
     */
    @Update
    int update(EmployeeEntity employee);

    /**
     * 删除员工（软删除）
     */
    @Query("UPDATE employee SET status = 'DELETED', update_time = :updateTime WHERE employee_id = :employeeId")
    int softDelete(long employeeId, long updateTime);

    /**
     * 物理删除员工
     */
    @Delete
    int delete(EmployeeEntity employee);

    /**
     * 获取员工数量
     */
    @Query("SELECT COUNT(1) FROM employee WHERE status != 'DELETED'")
    int getEmployeeCount();

    /**
     * 根据部门获取员工数量
     */
    @Query("SELECT COUNT(1) FROM employee WHERE department_id = :departmentId AND status != 'DELETED'")
    int getEmployeeCountByDepartment(long departmentId);
}
