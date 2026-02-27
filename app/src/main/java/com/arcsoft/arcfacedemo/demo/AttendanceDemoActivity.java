package com.arcsoft.arcfacedemo.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arcsoft.arcfacedemo.R;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceRecordEntity;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceStatistics;
import com.arcsoft.arcfacedemo.attendance.model.CheckResult;
import com.arcsoft.arcfacedemo.attendance.repository.AttendanceRepository;
import com.arcsoft.arcfacedemo.attendance.service.AttendanceService;
import com.arcsoft.arcfacedemo.attendance.service.AttendanceStatisticsService;
import com.arcsoft.arcfacedemo.dooraccess.model.AccessResult;
import com.arcsoft.arcfacedemo.dooraccess.service.DoorAccessService;
import com.arcsoft.arcfacedemo.employee.model.DepartmentEntity;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.model.PositionEntity;
import com.arcsoft.arcfacedemo.employee.service.EmployeeService;
import com.arcsoft.arcfacedemo.facedb.FaceDatabase;
import com.arcsoft.arcfacedemo.facedb.entity.FaceEntity;
import com.arcsoft.arcfacedemo.facedb.dao.FaceDao;
import com.arcsoft.arcfacedemo.integration.LocalAttendanceIntegration;
import com.arcsoft.arcfacedemo.widget.TechClockView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

/**
 * 考勤门禁系统演示Activity
 * 展示如何使用新的考勤和门禁功能
 */
public class AttendanceDemoActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceDemo";

    private EmployeeService employeeService;
    private AttendanceService attendanceService;
    private DoorAccessService doorAccessService;
    private AttendanceStatisticsService statisticsService;
    private LocalAttendanceIntegration integration;
    private FaceDao faceDao;

    private EditText etEmployeeNo;
    private EditText etName;
    private EditText etFaceId;
    private TextView tvResult;
    private TechClockView techClockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_demo);

        // 初始化服务
        employeeService = EmployeeService.getInstance(this);
        attendanceService = AttendanceService.getInstance(this);
        doorAccessService = DoorAccessService.getInstance(this);
        statisticsService = AttendanceStatisticsService.getInstance(this);
        integration = LocalAttendanceIntegration.getInstance(this);

        // 初始化数据库
        FaceDatabase database = FaceDatabase.getInstance(this);
        faceDao = database.faceDao();

        initViews();
        initData();
    }

    private void initViews() {
        techClockView = findViewById(R.id.tech_clock);
        etEmployeeNo = findViewById(R.id.et_employee_no);
        etName = findViewById(R.id.et_name);
        etFaceId = findViewById(R.id.et_face_id);
        tvResult = findViewById(R.id.tv_result);

        // 绑定按钮事件
        findViewById(R.id.btn_add_employee).setOnClickListener(this::onAddEmployee);
        findViewById(R.id.btn_check_in).setOnClickListener(this::onCheckIn);
        findViewById(R.id.btn_check_out).setOnClickListener(this::onCheckOut);
        findViewById(R.id.btn_door_access).setOnClickListener(this::onDoorAccess);
        findViewById(R.id.btn_query_records).setOnClickListener(this::onQueryRecords);
        findViewById(R.id.btn_generate_report).setOnClickListener(this::onGenerateReport);
        findViewById(R.id.btn_list_employees).setOnClickListener(this::onListEmployees);
    }

    private void initData() {
        // 检查是否需要创建演示数据
        checkAndCreateDemoData();
    }

    private void checkAndCreateDemoData() {
        Log.d(TAG, "========== 检查并创建演示数据 ==========");
        employeeService.getDepartmentCount()
                .subscribe(new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "开始检查部门数量...");
                    }

                    @Override
                    public void onSuccess(Integer count) {
                        Log.d(TAG, "当前部门数量: " + count);
                        if (count == 0) {
                            Log.d(TAG, "部门数为0，开始创建演示数据...");
                            createDemoDepartments();
                        } else {
                            Log.d(TAG, "部门已存在，跳过创建");
                            // 检查岗位
                            checkAndCreateDemoPositions();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Check demo data failed", e);
                    }
                });
    }

    private void checkAndCreateDemoPositions() {
        employeeService.getPositionCount()
                .subscribe(new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "开始检查岗位数量...");
                    }

                    @Override
                    public void onSuccess(Integer count) {
                        Log.d(TAG, "当前岗位数量: " + count);
                        if (count == 0) {
                            Log.d(TAG, "岗位数为0，开始创建演示岗位...");
                            createDemoPositions();
                        } else {
                            Log.d(TAG, "岗位已存在，跳过创建");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Check position count failed", e);
                    }
                });
    }

    private void createDemoDepartments() {
        Log.d(TAG, "========== 创建演示部门 ==========");
        DepartmentEntity dept1 = new DepartmentEntity("研发部", "负责产品研发");
        DepartmentEntity dept2 = new DepartmentEntity("市场部", "负责市场营销");
        DepartmentEntity dept3 = new DepartmentEntity("行政部", "负责行政管理");

        Log.d(TAG, "准备创建部门:");
        Log.d(TAG, "  1. " + dept1.getDepartmentName());
        Log.d(TAG, "  2. " + dept2.getDepartmentName());
        Log.d(TAG, "  3. " + dept3.getDepartmentName());

        employeeService.addDepartment(dept1)
                .subscribe(new SingleObserver<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onSuccess(Long deptId) {
                        Log.i(TAG, "部门1创建成功! ID: " + deptId);
                        // 继续创建部门2
                        employeeService.addDepartment(dept2)
                                .subscribe(new SingleObserver<Long>() {
                                    @Override
                                    public void onSubscribe(Disposable d) {}

                                    @Override
                                    public void onSuccess(Long deptId) {
                                        Log.i(TAG, "部门2创建成功! ID: " + deptId);
                                        // 继续创建部门3
                                        employeeService.addDepartment(dept3)
                                                .subscribe(new SingleObserver<Long>() {
                                                    @Override
                                                    public void onSubscribe(Disposable d) {}

                                                    @Override
                                                    public void onSuccess(Long deptId) {
                                                        Log.i(TAG, "部门3创建成功! ID: " + deptId);
                                                        // 创建完部门后，创建岗位
                                                        createDemoPositions();
                                                    }

                                                    @Override
                                                    public void onError(Throwable e) {
                                                        Log.e(TAG, "Create demo department 3 failed", e);
                                                        // 即使失败也尝试创建岗位
                                                        createDemoPositions();
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        Log.e(TAG, "Create demo department 2 failed", e);
                                        // 继续创建部门3
                                        employeeService.addDepartment(dept3)
                                                .subscribe(new SingleObserver<Long>() {
                                                    @Override
                                                    public void onSubscribe(Disposable d) {}

                                                    @Override
                                                    public void onSuccess(Long deptId) {
                                                        Log.i(TAG, "部门3创建成功! ID: " + deptId);
                                                        createDemoPositions();
                                                    }

                                                    @Override
                                                    public void onError(Throwable e) {
                                                        Log.e(TAG, "Create demo department 3 failed", e);
                                                        createDemoPositions();
                                                    }
                                                });
                                    }
                                });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Create demo department 1 failed", e);
                    }
                });
    }

    private void createDemoPositions() {
        Log.d(TAG, "========== 创建演示岗位 ==========");
        PositionEntity pos1 = new PositionEntity("软件工程师", "负责软件开发");
        PositionEntity pos2 = new PositionEntity("产品经理", "负责产品设计");
        PositionEntity pos3 = new PositionEntity("销售经理", "负责销售业务");

        Log.d(TAG, "准备创建岗位:");
        Log.d(TAG, "  1. " + pos1.getPositionName());
        Log.d(TAG, "  2. " + pos2.getPositionName());
        Log.d(TAG, "  3. " + pos3.getPositionName());

        employeeService.addPosition(pos1)
                .subscribe(new SingleObserver<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onSuccess(Long posId) {
                        Log.i(TAG, "岗位1创建成功! ID: " + posId);
                        // 继续创建岗位2
                        employeeService.addPosition(pos2)
                                .subscribe(new SingleObserver<Long>() {
                                    @Override
                                    public void onSubscribe(Disposable d) {}

                                    @Override
                                    public void onSuccess(Long posId) {
                                        Log.i(TAG, "岗位2创建成功! ID: " + posId);
                                        // 继续创建岗位3
                                        employeeService.addPosition(pos3)
                                                .subscribe(new SingleObserver<Long>() {
                                                    @Override
                                                    public void onSubscribe(Disposable d) {}

                                                    @Override
                                                    public void onSuccess(Long posId) {
                                                        Log.i(TAG, "岗位3创建成功! ID: " + posId);
                                                        Log.i(TAG, "========== 演示数据创建完成 ==========");
                                                    }

                                                    @Override
                                                    public void onError(Throwable e) {
                                                        Log.e(TAG, "Create demo position 3 failed", e);
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        Log.e(TAG, "Create demo position 2 failed", e);
                                    }
                                });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Create demo position 1 failed", e);
                    }
                });
    }

    private void onAddEmployee(View view) {
        String employeeNo = etEmployeeNo.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String faceIdStr = etFaceId.getText().toString().trim();

        Log.d(TAG, "========== 开始添加员工 ==========");
        Log.d(TAG, "工号: [" + employeeNo + "]");
        Log.d(TAG, "姓名: [" + name + "]");
        Log.d(TAG, "人脸ID: [" + faceIdStr + "]");

        if (employeeNo.isEmpty() || name.isEmpty()) {
            Log.w(TAG, "验证失败: 工号或姓名为空");
            Toast.makeText(this, "请输入工号和姓名", Toast.LENGTH_SHORT).show();
            return;
        }

        long faceId;
        try {
            faceId = faceIdStr.isEmpty() ? 0 : Long.parseLong(faceIdStr);
            Log.d(TAG, "解析后的faceId: " + faceId);
        } catch (NumberFormatException e) {
            Log.e(TAG, "人脸ID格式错误: " + faceIdStr, e);
            Toast.makeText(this, "人脸ID格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeNo(employeeNo);
        employee.setName(name);
        employee.setFaceId(faceId);
        employee.setDepartmentId(1); // 默认部门
        employee.setPositionId(1);   // 默认岗位
        employee.setPhone("13800138000");
        employee.setStatus("ACTIVE");

        Log.d(TAG, "员工对象创建完成:");
        Log.d(TAG, "  - employeeNo: " + employee.getEmployeeNo());
        Log.d(TAG, "  - name: " + employee.getName());
        Log.d(TAG, "  - faceId: " + employee.getFaceId());
        Log.d(TAG, "  - departmentId: " + employee.getDepartmentId());
        Log.d(TAG, "  - positionId: " + employee.getPositionId());
        Log.d(TAG, "  - phone: " + employee.getPhone());
        Log.d(TAG, "  - status: " + employee.getStatus());
        Log.d(TAG, "调用 employeeService.addEmployee()...");

        employeeService.addEmployee(employee)
                .subscribe(new SingleObserver<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: 订阅成功");
                    }

                    @Override
                    public void onSuccess(Long employeeId) {
                        Log.i(TAG, "========== 员工添加成功 ==========");
                        Log.i(TAG, "新员工ID: " + employeeId);
                        runOnUiThread(() -> {
                            Toast.makeText(AttendanceDemoActivity.this,
                                    "员工添加成功！ID: " + employeeId, Toast.LENGTH_SHORT).show();
                            tvResult.setText("员工添加成功\n工号: " + employeeNo + "\n姓名: " + name);
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "========== 员工添加失败 ==========");
                        Log.e(TAG, "错误类型: " + e.getClass().getName());
                        Log.e(TAG, "错误消息: " + e.getMessage(), e);
                        runOnUiThread(() -> {
                            Toast.makeText(AttendanceDemoActivity.this,
                                    "添加失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void onCheckIn(View view) {
        String faceIdStr = etFaceId.getText().toString().trim();
        if (faceIdStr.isEmpty()) {
            Toast.makeText(this, "请输入人脸ID", Toast.LENGTH_SHORT).show();
            return;
        }

        long faceId = Long.parseLong(faceIdStr);
        String imagePath = "/data/data/com.arcsoft.arcfacedemo/files/checkin/test.jpg";

        attendanceService.checkIn(faceId, imagePath)
                .subscribe(new SingleObserver<CheckResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onSuccess(CheckResult result) {
                        runOnUiThread(() -> {
                            if (result.isSuccess()) {
                                String msg = "上班打卡成功\n" +
                                        "状态: " + result.getStatus() + "\n" +
                                        "时间: " + new java.util.Date(result.getCheckTime());
                                tvResult.setText(msg);
                                Toast.makeText(AttendanceDemoActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                            } else {
                                tvResult.setText("打卡失败: " + result.getError());
                                Toast.makeText(AttendanceDemoActivity.this, result.getError(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        runOnUiThread(() -> {
                            Toast.makeText(AttendanceDemoActivity.this,
                                    "打卡异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void onCheckOut(View view) {
        String faceIdStr = etFaceId.getText().toString().trim();
        if (faceIdStr.isEmpty()) {
            Toast.makeText(this, "请输入人脸ID", Toast.LENGTH_SHORT).show();
            return;
        }

        long faceId = Long.parseLong(faceIdStr);
        String imagePath = "/data/data/com.arcsoft.arcfacedemo/files/checkin/test.jpg";

        attendanceService.checkOut(faceId, imagePath)
                .subscribe(new SingleObserver<CheckResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onSuccess(CheckResult result) {
                        runOnUiThread(() -> {
                            if (result.isSuccess()) {
                                String msg = "下班打卡成功\n" +
                                        "状态: " + result.getStatus() + "\n" +
                                        "时间: " + new java.util.Date(result.getCheckTime());
                                tvResult.setText(msg);
                                Toast.makeText(AttendanceDemoActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                            } else {
                                tvResult.setText("打卡失败: " + result.getError());
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        runOnUiThread(() -> {
                            Toast.makeText(AttendanceDemoActivity.this,
                                    "打卡异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void onDoorAccess(View view) {
        String faceIdStr = etFaceId.getText().toString().trim();
        if (faceIdStr.isEmpty()) {
            Toast.makeText(this, "请输入人脸ID", Toast.LENGTH_SHORT).show();
            return;
        }

        long faceId = Long.parseLong(faceIdStr);
        String imagePath = "/data/data/com.arcsoft.arcfacedemo/files/dooraccess/test.jpg";

        doorAccessService.verifyAccessByFace(faceId, imagePath)
                .subscribe(new SingleObserver<AccessResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onSuccess(AccessResult result) {
                        runOnUiThread(() -> {
                            if (result.isSuccess()) {
                                String msg = "门禁验证通过\n" +
                                        "员工: " + result.getEmployeeName() + "\n" +
                                        "工号: " + result.getEmployeeNo();
                                tvResult.setText(msg);
                                Toast.makeText(AttendanceDemoActivity.this, "欢迎 " + result.getEmployeeName(), Toast.LENGTH_SHORT).show();
                            } else {
                                String msg = "门禁验证失败\n" + result.getMessage();
                                tvResult.setText(msg);
                                Toast.makeText(AttendanceDemoActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        runOnUiThread(() -> {
                            Toast.makeText(AttendanceDemoActivity.this,
                                    "验证异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void onQueryRecords(View view) {
        AttendanceRepository repository = AttendanceRepository.getInstance(this);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        long startDate = calendar.getTimeInMillis();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        long endDate = calendar.getTimeInMillis();

        repository.getRecordsByDateRange(startDate, endDate)
                .subscribe(new SingleObserver<List<AttendanceRecordEntity>>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onSuccess(List<AttendanceRecordEntity> records) {
                        runOnUiThread(() -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append("本月打卡记录\n");
                            sb.append("共 ").append(records.size()).append(" 条记录\n\n");

                            for (AttendanceRecordEntity record : records) {
                                sb.append("员工: ").append(record.getEmployeeName())
                                  .append("\n上班: ").append(formatTime(record.getCheckInTime()))
                                  .append(" (").append(record.getCheckInStatus()).append(")")
                                  .append("\n下班: ").append(formatTime(record.getCheckOutTime()))
                                  .append(" (").append(record.getCheckOutStatus()).append(")")
                                  .append("\n\n");
                            }

                            tvResult.setText(sb.toString());
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        runOnUiThread(() -> {
                            Toast.makeText(AttendanceDemoActivity.this,
                                    "查询失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void onGenerateReport(View view) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;

        statisticsService.generateAllEmployeesMonthlyReport(year, month)
                .subscribe(new SingleObserver<List<AttendanceStatistics>>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onSuccess(List<AttendanceStatistics> statsList) {
                        runOnUiThread(() -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append(year).append("年").append(month).append("月 考勤报表\n\n");

                            for (AttendanceStatistics stats : statsList) {
                                sb.append("员工: ").append(stats.getEmployeeName())
                                  .append("\n应出勤: ").append(stats.getShouldAttendDays()).append(" 天")
                                  .append("\n实出勤: ").append(stats.getActualAttendDays()).append(" 天")
                                  .append("\n出勤率: ").append(String.format(Locale.getDefault(), "%.1f%%", stats.getAttendanceRate()))
                                  .append("\n正常: ").append(stats.getNormalCount())
                                  .append(" 迟到: ").append(stats.getLateCount())
                                  .append(" 早退: ").append(stats.getEarlyLeaveCount())
                                  .append("\n\n");
                            }

                            tvResult.setText(sb.toString());
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        runOnUiThread(() -> {
                            Toast.makeText(AttendanceDemoActivity.this,
                                    "生成报表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void onListEmployees(View view) {
        Log.d(TAG, "========== 查询员工列表 ==========");

        // 先查询部门和岗位
        employeeService.getAllDepartments()
                .subscribe(new SingleObserver<List<DepartmentEntity>>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onSuccess(List<DepartmentEntity> departments) {
                        Log.d(TAG, "部门列表:");
                        for (DepartmentEntity dept : departments) {
                            Log.d(TAG, "  ID=" + dept.getDepartmentId() + ", 名称=" + dept.getDepartmentName());
                        }

                        employeeService.getAllPositions()
                                .subscribe(new SingleObserver<List<PositionEntity>>() {
                                    @Override
                                    public void onSubscribe(Disposable d) {}

                                    @Override
                                    public void onSuccess(List<PositionEntity> positions) {
                                        Log.d(TAG, "岗位列表:");
                                        for (PositionEntity pos : positions) {
                                            Log.d(TAG, "  ID=" + pos.getPositionId() + ", 名称=" + pos.getPositionName());
                                        }

                                        // 然后查询员工
                                        employeeService.getAllEmployees()
                                                .subscribe(new SingleObserver<List<EmployeeEntity>>() {
                                                    @Override
                                                    public void onSubscribe(Disposable d) {}

                                                    @Override
                                                    public void onSuccess(List<EmployeeEntity> employees) {
                                                        Log.d(TAG, "员工列表，共 " + employees.size() + " 人");
                                                        for (EmployeeEntity emp : employees) {
                                                            Log.d(TAG, "  ID=" + emp.getEmployeeId() +
                                                                    ", 工号=" + emp.getEmployeeNo() +
                                                                    ", 姓名=" + emp.getName() +
                                                                    ", 部门ID=" + emp.getDepartmentId() +
                                                                    ", 岗位ID=" + emp.getPositionId());
                                                        }

                                                        runOnUiThread(() -> {
                                                            StringBuilder sb = new StringBuilder();
                                                            sb.append("=== 数据库状态 ===\n\n");
                                                            sb.append("部门数: ").append(departments.size()).append("\n");
                                                            sb.append("岗位数: ").append(positions.size()).append("\n");
                                                            sb.append("员工数: ").append(employees.size()).append("\n\n");

                                                            sb.append("=== 部门列表 ===\n");
                                                            for (DepartmentEntity dept : departments) {
                                                                sb.append("ID: ").append(dept.getDepartmentId())
                                                                  .append(" - ").append(dept.getDepartmentName()).append("\n");
                                                            }
                                                            sb.append("\n");

                                                            sb.append("=== 岗位列表 ===\n");
                                                            for (PositionEntity pos : positions) {
                                                                sb.append("ID: ").append(pos.getPositionId())
                                                                  .append(" - ").append(pos.getPositionName()).append("\n");
                                                            }
                                                            sb.append("\n");

                                                            sb.append("=== 员工列表 ===\n");
                                                            for (EmployeeEntity emp : employees) {
                                                                sb.append("ID: ").append(emp.getEmployeeId())
                                                                  .append("\n工号: ").append(emp.getEmployeeNo())
                                                                  .append("\n姓名: ").append(emp.getName())
                                                                  .append("\n人脸ID: ").append(emp.getFaceId())
                                                                  .append("\n部门ID: ").append(emp.getDepartmentId())
                                                                  .append("\n岗位ID: ").append(emp.getPositionId())
                                                                  .append("\n状态: ").append(emp.getStatus())
                                                                  .append("\n\n");
                                                            }

                                                            tvResult.setText(sb.toString());
                                                        });
                                                    }

                                                    @Override
                                                    public void onError(Throwable e) {
                                                        Log.e(TAG, "查询员工列表失败", e);
                                                        runOnUiThread(() -> {
                                                            Toast.makeText(AttendanceDemoActivity.this,
                                                                    "查询失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        });
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        Log.e(TAG, "查询岗位列表失败", e);
                                    }
                                });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "查询部门列表失败", e);
                    }
                });
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "未打卡";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
}
