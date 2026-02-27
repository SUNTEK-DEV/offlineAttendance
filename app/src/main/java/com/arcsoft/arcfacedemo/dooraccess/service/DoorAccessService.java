package com.arcsoft.arcfacedemo.dooraccess.service;

import android.content.Context;
import android.util.Log;

import com.arcsoft.arcfacedemo.dooraccess.model.AccessResult;
import com.arcsoft.arcfacedemo.dooraccess.model.DoorAccessRecordEntity;
import com.arcsoft.arcfacedemo.dooraccess.repository.DoorAccessRepository;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.repository.EmployeeRepository;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * 门禁服务
 */
public class DoorAccessService {

    private static final String TAG = "DoorAccessService";

    private static DoorAccessService instance;

    private DoorAccessRepository doorAccessRepository;
    private EmployeeRepository employeeRepository;

    private DoorAccessService(Context context) {
        this.doorAccessRepository = DoorAccessRepository.getInstance(context);
        this.employeeRepository = EmployeeRepository.getInstance(context);
    }

    public static synchronized DoorAccessService getInstance(Context context) {
        if (instance == null) {
            instance = new DoorAccessService(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 通过人脸ID验证门禁权限
     *
     * @param faceId    人脸ID
     * @param imagePath 抓拍图片路径
     * @return 验证结果
     */
    public Single<AccessResult> verifyAccessByFace(long faceId, String imagePath) {
        return Single.create((SingleOnSubscribe<AccessResult>) emitter -> {
            try {
                // 1. 根据人脸ID查找员工
                EmployeeEntity employee = employeeRepository.getEmployeeByFaceId(faceId).blockingGet();

                if (employee == null) {
                    // 未找到员工
                    AccessResult result = AccessResult.failed("NO_EMPLOYEE", "未识别到有效员工信息");
                    recordAccessResult(result, 0, null, null, "FACE", imagePath);
                    emitter.onSuccess(result);
                    return;
                }

                // 2. 检查员工状态
                if (!"ACTIVE".equals(employee.getStatus())) {
                    // 员工状态异常
                    AccessResult result = AccessResult.denied("员工状态异常: " + employee.getStatus());
                    recordAccessResult(result, employee.getEmployeeId(), employee.getEmployeeNo(),
                            employee.getName(), "FACE", imagePath);
                    emitter.onSuccess(result);
                    return;
                }

                // 3. 验证通过
                AccessResult result = AccessResult.success(
                        employee.getEmployeeId(),
                        employee.getEmployeeNo(),
                        employee.getName()
                );
                result.setAccessType("FACE");

                // 4. 记录访问日志
                long recordId = recordAccessResult(result, employee.getEmployeeId(),
                        employee.getEmployeeNo(), employee.getName(), "FACE", imagePath).blockingGet();
                result.setRecordId(recordId);

                // 5. 控制开门（这里可以根据实际硬件实现）
                openDoor();

                emitter.onSuccess(result);

            } catch (Exception e) {
                Log.e(TAG, "Verify access failed", e);
                AccessResult result = AccessResult.failed("SYSTEM_ERROR", "系统错误: " + e.getMessage());
                emitter.onSuccess(result);
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 通过工号验证门禁权限
     *
     * @param employeeNo 工号
     * @param imagePath  抓拍图片路径
     * @return 验证结果
     */
    public Single<AccessResult> verifyAccessByEmployeeNo(String employeeNo, String imagePath) {
        return Single.create((SingleOnSubscribe<AccessResult>) emitter -> {
            try {
                // 1. 根据工号查找员工
                EmployeeEntity employee = employeeRepository.getEmployeeByNo(employeeNo).blockingGet();

                if (employee == null) {
                    AccessResult result = AccessResult.failed("NO_EMPLOYEE", "未找到工号为 " + employeeNo + " 的员工");
                    recordAccessResult(result, 0, employeeNo, null, "PASSWORD", imagePath);
                    emitter.onSuccess(result);
                    return;
                }

                // 2. 检查员工状态
                if (!"ACTIVE".equals(employee.getStatus())) {
                    AccessResult result = AccessResult.denied("员工状态异常: " + employee.getStatus());
                    recordAccessResult(result, employee.getEmployeeId(), employee.getEmployeeNo(),
                            employee.getName(), "PASSWORD", imagePath);
                    emitter.onSuccess(result);
                    return;
                }

                // 3. 验证通过
                AccessResult result = AccessResult.success(
                        employee.getEmployeeId(),
                        employee.getEmployeeNo(),
                        employee.getName()
                );
                result.setAccessType("PASSWORD");

                // 4. 记录访问日志
                long recordId = recordAccessResult(result, employee.getEmployeeId(),
                        employee.getEmployeeNo(), employee.getName(), "PASSWORD", imagePath).blockingGet();
                result.setRecordId(recordId);

                // 5. 控制开门
                openDoor();

                emitter.onSuccess(result);

            } catch (Exception e) {
                Log.e(TAG, "Verify access by employee no failed", e);
                AccessResult result = AccessResult.failed("SYSTEM_ERROR", "系统错误: " + e.getMessage());
                emitter.onSuccess(result);
            }
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 记录访问结果
     */
    private Single<Long> recordAccessResult(AccessResult result, long employeeId,
                                             String employeeNo, String employeeName,
                                             String accessType, String imagePath) {
        return Single.fromCallable(() -> {
            DoorAccessRecordEntity record = new DoorAccessRecordEntity();
            record.setEmployeeId(employeeId);
            record.setEmployeeNo(employeeNo);
            record.setEmployeeName(employeeName);
            record.setAccessType(accessType);
            record.setAccessResult(result.getResult());
            record.setFailReason(result.getFailReason());
            record.setImagePath(imagePath);
            record.setAccessTime(result.getAccessTime());
            record.setDeviceInfo(getDeviceInfo());

            return doorAccessRepository.insertRecord(record).blockingGet();
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 获取访问历史记录
     */
    public Single<java.util.List<DoorAccessRecordEntity>> getAccessHistory(int limit, int offset) {
        return doorAccessRepository.getRecords(limit, offset);
    }

    /**
     * 获取最近的访问记录
     */
    public Single<java.util.List<DoorAccessRecordEntity>> getRecentAccess(int limit) {
        return doorAccessRepository.getRecentRecords(limit);
    }

    /**
     * 获取成功的访问记录
     */
    public Single<java.util.List<DoorAccessRecordEntity>> getSuccessAccess(int limit, int offset) {
        return doorAccessRepository.getSuccessRecords(limit, offset);
    }

    /**
     * 获取失败的访问记录
     */
    public Single<java.util.List<DoorAccessRecordEntity>> getFailedAccess(int limit, int offset) {
        return doorAccessRepository.getFailedRecords(limit, offset);
    }

    /**
     * 获取访问统计
     */
    public Single<AccessStatistics> getStatistics() {
        return Single.create((SingleOnSubscribe<AccessStatistics>) emitter -> {
            int totalCount = doorAccessRepository.getRecordCount().blockingGet();
            int successCount = doorAccessRepository.getSuccessCount().blockingGet();
            int failedCount = doorAccessRepository.getFailedCount().blockingGet();

            AccessStatistics stats = new AccessStatistics();
            stats.totalCount = totalCount;
            stats.successCount = successCount;
            stats.failedCount = failedCount;
            stats.successRate = totalCount > 0 ? (successCount * 100.0 / totalCount) : 0;

            emitter.onSuccess(stats);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * 控制开门
     * TODO: 根据实际硬件实现
     * 可能通过：
     * 1. 串口控制
     * 2. GPIO控制
     * 3. 网络控制
     */
    private void openDoor() {
        Log.i(TAG, "Opening door...");
        // 这里实现实际的开门控制逻辑
        // 例如：通过串口发送开门指令
        // SerialPortManager.sendOpenCommand();
    }

    /**
     * 获取设备信息
     */
    private String getDeviceInfo() {
        return android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
    }

    /**
     * 访问统计数据
     */
    public static class AccessStatistics {
        public int totalCount;
        public int successCount;
        public int failedCount;
        public double successRate; // 成功率（百分比）
    }
}
