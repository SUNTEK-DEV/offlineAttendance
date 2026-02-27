package com.arcsoft.arcfacedemo.employee.service;

import android.content.Context;
import android.util.Log;

import com.arcsoft.arcfacedemo.employee.model.DepartmentEntity;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.model.PositionEntity;
import com.arcsoft.arcfacedemo.employee.repository.EmployeeRepository;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * 员工服务
 */
public class EmployeeService {

    private static final String TAG = "EmployeeService";

    private static EmployeeService instance;

    private EmployeeRepository employeeRepository;

    private EmployeeService(Context context) {
        this.employeeRepository = EmployeeRepository.getInstance(context);
    }

    public static synchronized EmployeeService getInstance(Context context) {
        if (instance == null) {
            instance = new EmployeeService(context.getApplicationContext());
        }
        return instance;
    }

    // ==================== 员工操作 ====================

    /**
     * 获取所有员工
     */
    public Single<List<EmployeeEntity>> getAllEmployees() {
        return employeeRepository.getAllEmployees();
    }

    /**
     * 根据ID获取员工
     */
    public Single<EmployeeEntity> getEmployeeById(long employeeId) {
        return employeeRepository.getEmployeeById(employeeId);
    }

    /**
     * 根据工号获取员工
     */
    public Single<EmployeeEntity> getEmployeeByNo(String employeeNo) {
        return employeeRepository.getEmployeeByNo(employeeNo);
    }

    /**
     * 根据人脸ID获取员工
     * 这是人脸识别打卡的关键方法
     */
    public Single<EmployeeEntity> getEmployeeByFaceId(long faceId) {
        return employeeRepository.getEmployeeByFaceId(faceId);
    }

    /**
     * 根据部门获取员工
     */
    public Single<List<EmployeeEntity>> getEmployeesByDepartment(long departmentId) {
        return employeeRepository.getEmployeesByDepartment(departmentId);
    }

    /**
     * 搜索员工
     */
    public Single<List<EmployeeEntity>> searchEmployees(String keyword) {
        return employeeRepository.searchEmployees(keyword);
    }

    /**
     * 分页获取员工
     */
    public Single<List<EmployeeEntity>> getEmployees(int limit, int offset) {
        return employeeRepository.getEmployees(limit, offset);
    }

    /**
     * 添加员工
     */
    public Single<Long> addEmployee(EmployeeEntity employee) {
        return Single.fromCallable(() -> {
            Log.d(TAG, "========== addEmployee 开始 ==========");
            Log.d(TAG, "输入员工信息:");
            Log.d(TAG, "  - 工号: " + employee.getEmployeeNo());
            Log.d(TAG, "  - 姓名: " + employee.getName());
            Log.d(TAG, "  - 人脸ID: " + employee.getFaceId());
            Log.d(TAG, "  - 部门ID: " + employee.getDepartmentId());
            Log.d(TAG, "  - 岗位ID: " + employee.getPositionId());

            // 验证工号唯一性 - 使用 Maybe 来处理可能为null的情况
            Log.d(TAG, "步骤1: 检查工号唯一性...");
            try {
                EmployeeEntity existing = employeeRepository.getEmployeeByNoBlocking(employee.getEmployeeNo());
                if (existing != null) {
                    Log.e(TAG, "工号已存在: " + employee.getEmployeeNo());
                    throw new IllegalArgumentException("工号已存在: " + employee.getEmployeeNo());
                }
                Log.d(TAG, "工号唯一性检查通过 - 工号不存在");
            } catch (Exception e) {
                Log.e(TAG, "检查工号时出错", e);
                // 如果是 IllegalArgumentException 则继续抛出
                if (e instanceof IllegalArgumentException) {
                    throw e;
                }
                // 其他错误也抛出
                throw new RuntimeException("检查工号失败: " + e.getMessage(), e);
            }

            // 设置默认值
            Log.d(TAG, "步骤2: 设置默认值...");
            if (employee.getStatus() == null) {
                employee.setStatus("ACTIVE");
                Log.d(TAG, "  - 设置status为ACTIVE");
            }
            if (employee.getHireDate() == 0) {
                employee.setHireDate(System.currentTimeMillis());
                Log.d(TAG, "  - 设置hireDate为当前时间");
            }
            long now = System.currentTimeMillis();
            employee.setCreateTime(now);
            employee.setUpdateTime(now);
            Log.d(TAG, "  - 创建时间: " + now);
            Log.d(TAG, "  - 更新时间: " + now);

            // 插入数据库
            Log.d(TAG, "步骤3: 调用Repository插入数据库...");
            Long result = employeeRepository.insertEmployee(employee).blockingGet();

            if (result == null) {
                Log.e(TAG, "插入数据库返回null!");
                throw new RuntimeException("插入数据库失败：返回值为null");
            }

            Log.i(TAG, "========== addEmployee 成功 ==========");
            Log.i(TAG, "新员工ID: " + result);
            return result;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 更新员工
     */
    public Single<Integer> updateEmployee(EmployeeEntity employee) {
        return employeeRepository.updateEmployee(employee);
    }

    /**
     * 删除员工（软删除）
     */
    public Single<Integer> deleteEmployee(long employeeId) {
        return employeeRepository.deleteEmployee(employeeId);
    }

    /**
     * 批量导入员工
     */
    public Single<Integer> importEmployees(List<EmployeeEntity> employees) {
        return Single.fromCallable(() -> {
            int successCount = 0;
            for (EmployeeEntity employee : employees) {
                try {
                    // 检查工号是否存在（使用允许null的方法）
                    EmployeeEntity existing = employeeRepository.getEmployeeByNoBlocking(employee.getEmployeeNo());
                    if (existing == null) {
                        // 新增
                        employee.setStatus("ACTIVE");
                        employee.setCreateTime(System.currentTimeMillis());
                        employee.setUpdateTime(System.currentTimeMillis());
                        employeeRepository.insertEmployee(employee).blockingGet();
                        successCount++;
                        Log.d(TAG, "导入新员工成功: " + employee.getEmployeeNo());
                    } else {
                        // 更新
                        employee.setEmployeeId(existing.getEmployeeId());
                        employee.setUpdateTime(System.currentTimeMillis());
                        employeeRepository.updateEmployee(employee).blockingGet();
                        successCount++;
                        Log.d(TAG, "更新员工成功: " + employee.getEmployeeNo());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Import employee failed: " + employee.getEmployeeNo(), e);
                }
            }
            Log.i(TAG, "批量导入完成，成功: " + successCount + "/" + employees.size());
            return successCount;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 获取员工数量
     */
    public Single<Integer> getEmployeeCount() {
        return employeeRepository.getEmployeeCount();
    }

    /**
     * 根据部门获取员工数量
     */
    public Single<Integer> getEmployeeCountByDepartment(long departmentId) {
        return employeeRepository.getEmployeeCountByDepartment(departmentId);
    }

    // ==================== 部门操作 ====================

    /**
     * 获取所有部门
     */
    public Single<List<DepartmentEntity>> getAllDepartments() {
        return employeeRepository.getAllDepartments();
    }

    /**
     * 根据ID获取部门
     */
    public Single<DepartmentEntity> getDepartmentById(long departmentId) {
        return employeeRepository.getDepartmentById(departmentId);
    }

    /**
     * 搜索部门
     */
    public Single<List<DepartmentEntity>> searchDepartments(String keyword) {
        return employeeRepository.searchDepartments(keyword);
    }

    /**
     * 添加部门
     */
    public Single<Long> addDepartment(DepartmentEntity department) {
        return employeeRepository.insertDepartment(department);
    }

    /**
     * 更新部门
     */
    public Single<Integer> updateDepartment(DepartmentEntity department) {
        return employeeRepository.updateDepartment(department);
    }

    /**
     * 删除部门
     */
    public Single<Integer> deleteDepartment(DepartmentEntity department) {
        return employeeRepository.deleteDepartment(department);
    }

    /**
     * 获取部门数量
     */
    public Single<Integer> getDepartmentCount() {
        return employeeRepository.getDepartmentCount();
    }

    // ==================== 岗位操作 ====================

    /**
     * 获取所有岗位
     */
    public Single<List<PositionEntity>> getAllPositions() {
        return employeeRepository.getAllPositions();
    }

    /**
     * 根据ID获取岗位
     */
    public Single<PositionEntity> getPositionById(long positionId) {
        return employeeRepository.getPositionById(positionId);
    }

    /**
     * 搜索岗位
     */
    public Single<List<PositionEntity>> searchPositions(String keyword) {
        return employeeRepository.searchPositions(keyword);
    }

    /**
     * 添加岗位
     */
    public Single<Long> addPosition(PositionEntity position) {
        return employeeRepository.insertPosition(position);
    }

    /**
     * 更新岗位
     */
    public Single<Integer> updatePosition(PositionEntity position) {
        return employeeRepository.updatePosition(position);
    }

    /**
     * 删除岗位
     */
    public Single<Integer> deletePosition(PositionEntity position) {
        return employeeRepository.deletePosition(position);
    }

    /**
     * 获取岗位数量
     */
    public Single<Integer> getPositionCount() {
        return employeeRepository.getPositionCount();
    }
}
