package com.arcsoft.arcfacedemo.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.arcsoft.arcfacedemo.R;
import com.arcsoft.arcfacedemo.attendance.dao.AttendanceRecordDao;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceRecordEntity;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceStatistics;
import com.arcsoft.arcfacedemo.attendance.service.AttendanceStatisticsService;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.service.EmployeeService;
import com.arcsoft.arcfacedemo.facedb.FaceDatabase;
import com.arcsoft.arcfacedemo.widget.TechClockView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 考勤查询Activity
 * 功能：员工列表查询、考勤记录查询
 */
public class AttendanceQueryActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceQuery";

    private EmployeeService employeeService;
    private AttendanceRecordDao attendanceRecordDao;
    private AttendanceStatisticsService statisticsService;

    private TextView tvResult;
    private TechClockView techClockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_query);

        // 初始化服务
        employeeService = EmployeeService.getInstance(this);
        attendanceRecordDao = FaceDatabase.getInstance(this).attendanceRecordDao();
        statisticsService = AttendanceStatisticsService.getInstance(this);

        initViews();
    }

    private void initViews() {
        techClockView = findViewById(R.id.tech_clock);
        tvResult = findViewById(R.id.tv_result);

        // 绑定按钮事件
        findViewById(R.id.btn_list_employees).setOnClickListener(this::onListEmployees);
        findViewById(R.id.btn_query_records).setOnClickListener(this::onQueryRecords);
        findViewById(R.id.btn_generate_report).setOnClickListener(this::onGenerateReport);
    }

    /**
     * 查询员工列表
     */
    private void onListEmployees(View view) {
        Log.d(TAG, "========== 查询员工列表 ==========");

        employeeService.getAllEmployees()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<EmployeeEntity>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        tvResult.setText("正在加载员工列表...");
                    }

                    @Override
                    public void onSuccess(List<EmployeeEntity> employees) {
                        Log.i(TAG, "员工数量: " + employees.size());
                        displayEmployeeList(employees);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "查询员工列表失败", e);
                        tvResult.setText("查询失败: " + e.getMessage());
                    }
                });
    }

    /**
     * 显示员工列表
     */
    private void displayEmployeeList(List<EmployeeEntity> employees) {
        if (employees == null || employees.isEmpty()) {
            tvResult.setText("暂无员工数据\n\n请在\"员工管理\"中添加员工");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("========== 员工列表 ==========\n");
        sb.append(String.format("共 %d 名员工\n\n", employees.size()));

        for (EmployeeEntity employee : employees) {
            sb.append("━━━━━━━━━━━━━━━━━━━━\n");
            sb.append(String.format("工号: %s\n", employee.getEmployeeNo()));
            sb.append(String.format("姓名: %s\n", employee.getName()));
            sb.append(String.format("部门ID: %d\n", employee.getDepartmentId()));
            sb.append(String.format("岗位ID: %d\n", employee.getPositionId()));
            sb.append(String.format("人脸ID: %d\n", employee.getFaceId()));
            sb.append(String.format("状态: %s\n", employee.getStatus()));
            if (employee.getPhone() != null && !employee.getPhone().isEmpty()) {
                sb.append(String.format("电话: %s\n", employee.getPhone()));
            }
            sb.append(String.format("入职时间: %s\n",
                    formatDate(employee.getHireDate(), "yyyy-MM-dd")));
            sb.append("\n");
        }

        tvResult.setText(sb.toString());
    }

    /**
     * 查询考勤记录
     */
    private void onQueryRecords(View view) {
        Log.d(TAG, "========== 查询考勤记录 ==========");

        // 弹出对话框选择查询类型
        new AlertDialog.Builder(this)
                .setTitle("选择查询类型")
                .setItems(new String[]{"全部记录", "今日记录", "本周记录"},
                        (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    queryAllRecords();
                                    break;
                                case 1:
                                    queryTodayRecords();
                                    break;
                                case 2:
                                    queryWeekRecords();
                                    break;
                            }
                        })
                .show();
    }

    /**
     * 查询全部考勤记录
     */
    private void queryAllRecords() {
        new Thread(() -> {
            // 获取最近100条记录
            List<AttendanceRecordEntity> records = attendanceRecordDao.getRecords(100, 0);
            runOnUiThread(() -> {
                displayAttendanceRecords(records, "最近100条考勤记录");
            });
        }).start();
    }

    /**
     * 查询今日考勤记录
     */
    private void queryTodayRecords() {
        new Thread(() -> {
            long todayStart = getTodayStart();
            long todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1;
            List<AttendanceRecordEntity> records = attendanceRecordDao.getRecordsByDateRange(todayStart, todayEnd);
            runOnUiThread(() -> {
                displayAttendanceRecords(records, "今日考勤记录");
            });
        }).start();
    }

    /**
     * 查询本周考勤记录
     */
    private void queryWeekRecords() {
        new Thread(() -> {
            long weekStart = getWeekStart();
            long weekEnd = System.currentTimeMillis();
            List<AttendanceRecordEntity> records = attendanceRecordDao.getRecordsByDateRange(weekStart, weekEnd);
            runOnUiThread(() -> {
                displayAttendanceRecords(records, "本周考勤记录");
            });
        }).start();
    }

    /**
     * 显示考勤记录
     */
    private void displayAttendanceRecords(List<AttendanceRecordEntity> records, String title) {
        if (records == null || records.isEmpty()) {
            tvResult.setText(title + "\n暂无数据");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("========== ").append(title).append(" ==========\n");
        sb.append(String.format("共 %d 条记录\n\n", records.size()));

        for (AttendanceRecordEntity record : records) {
            sb.append("━━━━━━━━━━━━━━━━━━━━\n");
            sb.append(String.format("工号: %s\n", record.getEmployeeNo()));
            sb.append(String.format("姓名: %s\n", record.getEmployeeName()));
            sb.append(String.format("日期: %s\n",
                    formatDate(record.getDate(), "yyyy-MM-dd")));
            if (record.getCheckInTime() > 0) {
                sb.append(String.format("上班: %s (%s)\n",
                        formatTime(record.getCheckInTime()),
                        record.getCheckInStatus()));
            }
            if (record.getCheckOutTime() > 0) {
                sb.append(String.format("下班: %s (%s)\n",
                        formatTime(record.getCheckOutTime()),
                        record.getCheckOutStatus()));
            }
            sb.append(String.format("工作状态: %s\n", record.getWorkStatus()));
            sb.append("\n");
        }

        tvResult.setText(sb.toString());
    }

    /**
     * 生成考勤报表
     */
    private void onGenerateReport(View view) {
        Log.d(TAG, "========== 生成考勤报表 ==========");

        // 弹出对话框选择报表类型
        new AlertDialog.Builder(this)
                .setTitle("选择报表类型")
                .setItems(new String[]{"本月月报", "上月月报"},
                        (dialog, which) -> {
                            Calendar calendar = Calendar.getInstance();
                            int year, month;
                            if (which == 0) {
                                // 本月
                                year = calendar.get(Calendar.YEAR);
                                month = calendar.get(Calendar.MONTH) + 1;
                            } else {
                                // 上月
                                calendar.add(Calendar.MONTH, -1);
                                year = calendar.get(Calendar.YEAR);
                                month = calendar.get(Calendar.MONTH) + 1;
                            }
                            generateMonthlyReport(year, month);
                        })
                .show();
    }

    /**
     * 生成月度报表
     */
    private void generateMonthlyReport(int year, int month) {
        tvResult.setText("正在生成报表...");

        statisticsService.generateAllEmployeesMonthlyReport(year, month)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<AttendanceStatistics>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "开始生成报表...");
                    }

                    @Override
                    public void onSuccess(List<AttendanceStatistics> statsList) {
                        Log.i(TAG, "报表生成成功，共 " + statsList.size() + " 名员工");
                        displayStatisticsReport(statsList, year, month);
                        Toast.makeText(AttendanceQueryActivity.this,
                                "报表生成成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "报表生成失败", e);
                        tvResult.setText("报表生成失败\n\n错误: " + e.getMessage());
                        Toast.makeText(AttendanceQueryActivity.this,
                                "报表生成失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 显示统计报表
     */
    private void displayStatisticsReport(List<AttendanceStatistics> statsList, int year, int month) {
        if (statsList == null || statsList.isEmpty()) {
            tvResult.setText(String.format("%d年%d月 考勤报表\n\n暂无数据", year, month));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("========== %d年%d月 考勤报表 ==========\n\n", year, month));
        sb.append(String.format("统计员工数: %d 人\n\n", statsList.size()));

        for (AttendanceStatistics stats : statsList) {
            sb.append("━━━━━━━━━━━━━━━━━━━━\n");
            sb.append(String.format("工号: %s\n", stats.getEmployeeNo()));
            sb.append(String.format("姓名: %s\n", stats.getEmployeeName()));
            sb.append(String.format("应出勤: %d 天\n", stats.getShouldAttendDays()));
            sb.append(String.format("实出勤: %d 天\n", stats.getActualAttendDays()));
            sb.append(String.format("出勤率: %.1f%%\n", stats.getAttendanceRate()));
            sb.append(String.format("正常: %d 次\n", stats.getNormalCount()));
            sb.append(String.format("迟到: %d 次\n", stats.getLateCount()));
            sb.append(String.format("早退: %d 次\n", stats.getEarlyLeaveCount()));
            sb.append(String.format("缺勤: %d 次\n", stats.getAbsentCount()));
            if (stats.getOvertimeCount() > 0) {
                sb.append(String.format("加班: %d 次\n", stats.getOvertimeCount()));
            }
            sb.append("\n");
        }

        tvResult.setText(sb.toString());
    }

    // 辅助方法

    private long getTodayStart() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getWeekStart() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getMonthStart() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String formatDate(long timestamp, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
