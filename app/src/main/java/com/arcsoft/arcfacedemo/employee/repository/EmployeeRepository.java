package com.arcsoft.arcfacedemo.employee.repository;

import android.content.Context;
import android.util.Log;

import com.arcsoft.arcfacedemo.employee.dao.DepartmentDao;
import com.arcsoft.arcfacedemo.employee.dao.EmployeeDao;
import com.arcsoft.arcfacedemo.employee.dao.PositionDao;
import com.arcsoft.arcfacedemo.employee.model.DepartmentEntity;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.model.PositionEntity;
import com.arcsoft.arcfacedemo.facedb.FaceDatabase;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * 员工仓库
 */
public class EmployeeRepository {

    private static EmployeeRepository instance;

    private EmployeeDao employeeDao;
    private DepartmentDao departmentDao;
    private PositionDao positionDao;

    private EmployeeRepository(Context context) {
        FaceDatabase database = FaceDatabase.getInstance(context);
        this.employeeDao = database.employeeDao();
        this.departmentDao = database.departmentDao();
        this.positionDao = database.positionDao();
    }

    public static synchronized EmployeeRepository getInstance(Context context) {
        if (instance == null) {
            instance = new EmployeeRepository(context.getApplicationContext());
        }
        return instance;
    }

    // ==================== 员工操作 ====================

    /**
     * 获取所有员工
     */
    public Single<List<EmployeeEntity>> getAllEmployees() {
        return Single.fromCallable(employeeDao::getAllEmployees)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 根据ID获取员工
     */
    public Single<EmployeeEntity> getEmployeeById(long employeeId) {
        return Single.fromCallable(() -> {
            EmployeeEntity entity = employeeDao.getEmployeeById(employeeId);
            if (entity == null) {
                throw new Exception("Employee not found with id: " + employeeId);
            }
            return entity;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 根据工号获取员工
     * 注意：当找不到员工时，使用 Maybe 或返回 null 前需要特殊处理
     * 这里改为返回 Optional 或者允许 null 通过 Single.defer()
     */
    public Single<EmployeeEntity> getEmployeeByNo(String employeeNo) {
        return Single.fromCallable(() -> {
            EmployeeEntity entity = employeeDao.getEmployeeByNo(employeeNo);
            Log.d("EmployeeRepository", "getEmployeeByNo(" + employeeNo + ") 返回: " + (entity != null ? entity.getName() : "null"));
            if (entity == null) {
                // 抛出异常而不是返回null，因为Single不允许null
                throw new Exception("Employee not found");
            }
            return entity;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 根据工号获取员工（阻塞版本，允许返回null）
     * 用于内部检查工号是否存在
     */
    public EmployeeEntity getEmployeeByNoBlocking(String employeeNo) {
        try {
            EmployeeEntity entity = employeeDao.getEmployeeByNo(employeeNo);
            Log.d("EmployeeRepository", "getEmployeeByNoBlocking(" + employeeNo + ") 返回: " + (entity != null ? entity.getName() : "null"));
            return entity; // 可以返回null
        } catch (Exception e) {
            Log.e("EmployeeRepository", "getEmployeeByNoBlocking 出错", e);
            return null;
        }
    }

    /**
     * 根据人脸ID获取员工
     */
    public Single<EmployeeEntity> getEmployeeByFaceId(long faceId) {
        return Single.fromCallable(() -> {
            EmployeeEntity entity = employeeDao.getEmployeeByFaceId(faceId);
            if (entity == null) {
                throw new Exception("Employee not found with faceId: " + faceId);
            }
            return entity;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 根据部门获取员工
     */
    public Single<List<EmployeeEntity>> getEmployeesByDepartment(long departmentId) {
        return Single.fromCallable(() -> employeeDao.getEmployeesByDepartment(departmentId))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 搜索员工
     */
    public Single<List<EmployeeEntity>> searchEmployees(String keyword) {
        return Single.fromCallable(() -> employeeDao.searchEmployees(keyword))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 分页获取员工
     */
    public Single<List<EmployeeEntity>> getEmployees(int limit, int offset) {
        return Single.fromCallable(() -> employeeDao.getEmployees(limit, offset))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 添加员工
     */
    public Single<Long> insertEmployee(EmployeeEntity employee) {
        return Single.fromCallable(() -> {
            Log.d("EmployeeRepository", "========== insertEmployee 开始 ==========");
            Log.d("EmployeeRepository", "准备插入的员工数据:");
            Log.d("EmployeeRepository", "  - employeeNo: " + employee.getEmployeeNo());
            Log.d("EmployeeRepository", "  - name: " + employee.getName());
            Log.d("EmployeeRepository", "  - faceId: " + employee.getFaceId());
            Log.d("EmployeeRepository", "  - departmentId: " + employee.getDepartmentId());
            Log.d("EmployeeRepository", "  - positionId: " + employee.getPositionId());
            Log.d("EmployeeRepository", "  - phone: " + employee.getPhone());
            Log.d("EmployeeRepository", "  - status: " + employee.getStatus());
            Log.d("EmployeeRepository", "  - hireDate: " + employee.getHireDate());
            Log.d("EmployeeRepository", "  - createTime: " + employee.getCreateTime());
            Log.d("EmployeeRepository", "  - updateTime: " + employee.getUpdateTime());

            // 验证外键是否存在
            Log.d("EmployeeRepository", "验证外键约束...");

            if (employeeDao != null) {
                Log.d("EmployeeRepository", "employeeDao 不为 null");
            } else {
                Log.e("EmployeeRepository", "employeeDao 为 null!");
                throw new RuntimeException("employeeDao未初始化");
            }

            // 检查部门是否存在
            Log.d("EmployeeRepository", "检查部门 ID=" + employee.getDepartmentId() + " 是否存在...");
            DepartmentDao deptDao = departmentDao;
            if (deptDao == null) {
                Log.e("EmployeeRepository", "departmentDao 为 null!");
            } else {
                try {
                    DepartmentEntity dept = deptDao.getDepartmentById(employee.getDepartmentId());
                    if (dept == null) {
                        Log.e("EmployeeRepository", "部门不存在! departmentId=" + employee.getDepartmentId());
                        Log.e("EmployeeRepository", "请先创建部门");
                    } else {
                        Log.d("EmployeeRepository", "部门存在: " + dept.getDepartmentName());
                    }
                } catch (Exception e) {
                    Log.e("EmployeeRepository", "检查部门时出错", e);
                }
            }

            // 检查岗位是否存在
            Log.d("EmployeeRepository", "检查岗位 ID=" + employee.getPositionId() + " 是否存在...");
            PositionDao posDao = positionDao;
            if (posDao == null) {
                Log.e("EmployeeRepository", "positionDao 为 null!");
            } else {
                try {
                    PositionEntity pos = posDao.getPositionById(employee.getPositionId());
                    if (pos == null) {
                        Log.e("EmployeeRepository", "岗位不存在! positionId=" + employee.getPositionId());
                        Log.e("EmployeeRepository", "请先创建岗位");
                    } else {
                        Log.d("EmployeeRepository", "岗位存在: " + pos.getPositionName());
                    }
                } catch (Exception e) {
                    Log.e("EmployeeRepository", "检查岗位时出错", e);
                }
            }

            // 执行插入
            Log.d("EmployeeRepository", "调用 employeeDao.insert()...");
            long result = employeeDao.insert(employee);
            Log.d("EmployeeRepository", "插入结果: " + result);

            if (result <= 0) {
                Log.e("EmployeeRepository", "插入失败，返回值: " + result);
                throw new RuntimeException("插入失败，返回值: " + result);
            }

            Log.i("EmployeeRepository", "========== insertEmployee 成功 ==========");
            Log.i("EmployeeRepository", "新员工ID: " + result);
            return result;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 更新员工
     */
    public Single<Integer> updateEmployee(EmployeeEntity employee) {
        return Single.fromCallable(() -> {
            employee.setUpdateTime(System.currentTimeMillis());
            return employeeDao.update(employee);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 删除员工（软删除）
     */
    public Single<Integer> deleteEmployee(long employeeId) {
        return Single.fromCallable(() ->
                employeeDao.softDelete(employeeId, System.currentTimeMillis())
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 获取员工数量
     */
    public Single<Integer> getEmployeeCount() {
        return Single.fromCallable(employeeDao::getEmployeeCount)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 根据部门获取员工数量
     */
    public Single<Integer> getEmployeeCountByDepartment(long departmentId) {
        return Single.fromCallable(() -> employeeDao.getEmployeeCountByDepartment(departmentId))
                .subscribeOn(Schedulers.io());
    }

    // ==================== 部门操作 ====================

    /**
     * 获取所有部门
     */
    public Single<List<DepartmentEntity>> getAllDepartments() {
        return Single.fromCallable(departmentDao::getAllDepartments)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 根据ID获取部门
     */
    public Single<DepartmentEntity> getDepartmentById(long departmentId) {
        return Single.fromCallable(() -> {
            DepartmentEntity entity = departmentDao.getDepartmentById(departmentId);
            if (entity == null) {
                throw new Exception("Department not found with id: " + departmentId);
            }
            return entity;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 搜索部门
     */
    public Single<List<DepartmentEntity>> searchDepartments(String keyword) {
        return Single.fromCallable(() -> departmentDao.searchDepartments(keyword))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 添加部门
     */
    public Single<Long> insertDepartment(DepartmentEntity department) {
        return Single.fromCallable(() -> departmentDao.insert(department))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 更新部门
     */
    public Single<Integer> updateDepartment(DepartmentEntity department) {
        return Single.fromCallable(() -> departmentDao.update(department))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 删除部门
     */
    public Single<Integer> deleteDepartment(DepartmentEntity department) {
        return Single.fromCallable(() -> departmentDao.delete(department))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 获取部门数量
     */
    public Single<Integer> getDepartmentCount() {
        return Single.fromCallable(departmentDao::getDepartmentCount)
                .subscribeOn(Schedulers.io());
    }

    // ==================== 岗位操作 ====================

    /**
     * 获取所有岗位
     */
    public Single<List<PositionEntity>> getAllPositions() {
        return Single.fromCallable(positionDao::getAllPositions)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 根据ID获取岗位
     */
    public Single<PositionEntity> getPositionById(long positionId) {
        return Single.fromCallable(() -> {
            PositionEntity entity = positionDao.getPositionById(positionId);
            if (entity == null) {
                throw new Exception("Position not found with id: " + positionId);
            }
            return entity;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 搜索岗位
     */
    public Single<List<PositionEntity>> searchPositions(String keyword) {
        return Single.fromCallable(() -> positionDao.searchPositions(keyword))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 添加岗位
     */
    public Single<Long> insertPosition(PositionEntity position) {
        return Single.fromCallable(() -> positionDao.insert(position))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 更新岗位
     */
    public Single<Integer> updatePosition(PositionEntity position) {
        return Single.fromCallable(() -> positionDao.update(position))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 删除岗位
     */
    public Single<Integer> deletePosition(PositionEntity position) {
        return Single.fromCallable(() -> positionDao.delete(position))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 获取岗位数量
     */
    public Single<Integer> getPositionCount() {
        return Single.fromCallable(positionDao::getPositionCount)
                .subscribeOn(Schedulers.io());
    }
}
