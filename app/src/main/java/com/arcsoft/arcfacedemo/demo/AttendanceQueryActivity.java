package com.arcsoft.arcfacedemo.demo;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.arcsoft.arcfacedemo.R;
import com.arcsoft.arcfacedemo.attendance.dao.AttendanceRecordDao;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceRecordEntity;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceStatistics;
import com.arcsoft.arcfacedemo.attendance.service.AttendanceStatisticsService;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.service.EmployeeService;
import com.arcsoft.arcfacedemo.facedb.FaceDatabase;
import com.arcsoft.arcfacedemo.widget.TechClockView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AttendanceQueryActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceQuery";
    private static final int REQUEST_WRITE_STORAGE = 2001;

    private EmployeeService employeeService;
    private AttendanceRecordDao attendanceRecordDao;
    private AttendanceStatisticsService statisticsService;

    private TextView tvResult;
    private Button btnExportExcel;

    private List<AttendanceStatistics> lastGeneratedReport = new ArrayList<>();
    private int lastReportYear;
    private int lastReportMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_query);

        employeeService = EmployeeService.getInstance(this);
        attendanceRecordDao = FaceDatabase.getInstance(this).attendanceRecordDao();
        statisticsService = AttendanceStatisticsService.getInstance(this);

        initViews();
    }

    private void initViews() {
        TechClockView techClockView = findViewById(R.id.tech_clock);
        tvResult = findViewById(R.id.tv_result);
        btnExportExcel = findViewById(R.id.btn_export_excel);
        btnExportExcel.setEnabled(false);
        btnExportExcel.setAlpha(0.6f);

        findViewById(R.id.btn_list_employees).setOnClickListener(this::onListEmployees);
        findViewById(R.id.btn_query_records).setOnClickListener(this::onQueryRecords);
        findViewById(R.id.btn_generate_report).setOnClickListener(this::onGenerateReport);
        btnExportExcel.setOnClickListener(this::onExportExcel);
    }

    private void onListEmployees(View view) {
        Log.d(TAG, "========== query employees ==========");
        employeeService.getAllEmployees()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<EmployeeEntity>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        tvResult.setText("Loading employee list...");
                    }

                    @Override
                    public void onSuccess(List<EmployeeEntity> employees) {
                        displayEmployeeList(employees);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Query employee list failed", e);
                        tvResult.setText("Query failed: " + e.getMessage());
                    }
                });
    }

    private void displayEmployeeList(List<EmployeeEntity> employees) {
        if (employees == null || employees.isEmpty()) {
            tvResult.setText("No employee data");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("========== Employee List ==========\n");
        sb.append(String.format(Locale.getDefault(), "Total: %d employees\n\n", employees.size()));
        for (EmployeeEntity employee : employees) {
            sb.append("--------------------------------\n");
            sb.append("Employee No: ").append(employee.getEmployeeNo()).append('\n');
            sb.append("Name: ").append(employee.getName()).append('\n');
            sb.append("Department ID: ").append(employee.getDepartmentId()).append('\n');
            sb.append("Position ID: ").append(employee.getPositionId()).append('\n');
            sb.append("Face ID: ").append(employee.getFaceId()).append('\n');
            sb.append("Status: ").append(employee.getStatus()).append('\n');
            if (employee.getPhone() != null && !employee.getPhone().isEmpty()) {
                sb.append("Phone: ").append(employee.getPhone()).append('\n');
            }
            sb.append("Hire Date: ").append(formatDate(employee.getHireDate(), "yyyy-MM-dd")).append("\n\n");
        }
        tvResult.setText(sb.toString());
    }

    private void onQueryRecords(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Select query type")
                .setItems(new String[]{"Latest 100 records", "Today", "This week"}, (dialog, which) -> {
                    if (which == 0) {
                        queryAllRecords();
                    } else if (which == 1) {
                        queryTodayRecords();
                    } else {
                        queryWeekRecords();
                    }
                })
                .show();
    }

    private void queryAllRecords() {
        new Thread(() -> {
            List<AttendanceRecordEntity> records = attendanceRecordDao.getRecords(100, 0);
            runOnUiThread(() -> displayAttendanceRecords(records, "Latest 100 attendance records"));
        }).start();
    }

    private void queryTodayRecords() {
        new Thread(() -> {
            long todayStart = getTodayStart();
            long todayEnd = todayStart + 24L * 60L * 60L * 1000L - 1L;
            List<AttendanceRecordEntity> records = attendanceRecordDao.getRecordsByDateRange(todayStart, todayEnd);
            runOnUiThread(() -> displayAttendanceRecords(records, "Today attendance records"));
        }).start();
    }

    private void queryWeekRecords() {
        new Thread(() -> {
            long weekStart = getWeekStart();
            long weekEnd = System.currentTimeMillis();
            List<AttendanceRecordEntity> records = attendanceRecordDao.getRecordsByDateRange(weekStart, weekEnd);
            runOnUiThread(() -> displayAttendanceRecords(records, "This week attendance records"));
        }).start();
    }

    private void displayAttendanceRecords(List<AttendanceRecordEntity> records, String title) {
        if (records == null || records.isEmpty()) {
            tvResult.setText(title + "\nNo data");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("========== ").append(title).append(" ==========\n");
        sb.append(String.format(Locale.getDefault(), "Total: %d records\n\n", records.size()));
        for (AttendanceRecordEntity record : records) {
            sb.append("--------------------------------\n");
            sb.append("Employee No: ").append(record.getEmployeeNo()).append('\n');
            sb.append("Name: ").append(record.getEmployeeName()).append('\n');
            sb.append("Date: ").append(formatDate(record.getDate(), "yyyy-MM-dd")).append('\n');
            if (record.getCheckInTime() > 0) {
                sb.append("Check-in: ").append(formatTime(record.getCheckInTime()))
                        .append(" (").append(record.getCheckInStatus()).append(")\n");
            }
            if (record.getCheckOutTime() > 0) {
                sb.append("Check-out: ").append(formatTime(record.getCheckOutTime()))
                        .append(" (").append(record.getCheckOutStatus()).append(")\n");
            }
            sb.append("Work Status: ").append(record.getWorkStatus()).append('\n');
            if (record.getRemark() != null && !record.getRemark().isEmpty()) {
                sb.append("Remark: ").append(record.getRemark()).append('\n');
            }
            sb.append('\n');
        }
        tvResult.setText(sb.toString());
    }

    private void onGenerateReport(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Select report type")
                .setItems(new String[]{"Current month", "Previous month"}, (dialog, which) -> {
                    Calendar calendar = Calendar.getInstance();
                    int year;
                    int month;
                    if (which == 0) {
                        year = calendar.get(Calendar.YEAR);
                        month = calendar.get(Calendar.MONTH) + 1;
                    } else {
                        calendar.add(Calendar.MONTH, -1);
                        year = calendar.get(Calendar.YEAR);
                        month = calendar.get(Calendar.MONTH) + 1;
                    }
                    generateMonthlyReport(year, month);
                })
                .show();
    }

    private void generateMonthlyReport(int year, int month) {
        tvResult.setText("Generating report...");
        statisticsService.generateAllEmployeesMonthlyReport(year, month)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<AttendanceStatistics>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onSuccess(List<AttendanceStatistics> statsList) {
                        lastGeneratedReport = statsList == null ? new ArrayList<>() : new ArrayList<>(statsList);
                        lastReportYear = year;
                        lastReportMonth = month;
                        btnExportExcel.setEnabled(statsList != null && !statsList.isEmpty());
                        btnExportExcel.setAlpha(btnExportExcel.isEnabled() ? 1f : 0.6f);
                        displayStatisticsReport(statsList, year, month);
                        Toast.makeText(AttendanceQueryActivity.this, "Report generated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Generate report failed", e);
                        tvResult.setText("Generate report failed\n\nError: " + e.getMessage());
                        btnExportExcel.setEnabled(false);
                        btnExportExcel.setAlpha(0.6f);
                        Toast.makeText(AttendanceQueryActivity.this, "Generate report failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayStatisticsReport(List<AttendanceStatistics> statsList, int year, int month) {
        if (statsList == null || statsList.isEmpty()) {
            tvResult.setText(String.format(Locale.getDefault(), "%d-%02d attendance report\n\nNo data", year, month));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.getDefault(), "========== %d-%02d attendance report ==========\n\n", year, month));
        sb.append(String.format(Locale.getDefault(), "Employees: %d\n", statsList.size()));
        sb.append("You can now click [Export Report to Excel] to save the report to Downloads/AttendanceReports.\n\n");

        for (AttendanceStatistics stats : statsList) {
            sb.append("--------------------------------\n");
            sb.append("Employee No: ").append(stats.getEmployeeNo()).append('\n');
            sb.append("Name: ").append(stats.getEmployeeName()).append('\n');
            sb.append("Should Attend: ").append(stats.getShouldAttendDays()).append(" days\n");
            sb.append("Actual Attend: ").append(stats.getActualAttendDays()).append(" days\n");
            sb.append(String.format(Locale.getDefault(), "Attendance Rate: %.1f%%\n", stats.getAttendanceRate()));
            sb.append("Normal: ").append(stats.getNormalCount()).append('\n');
            sb.append("Late: ").append(stats.getLateCount()).append('\n');
            sb.append("Early Leave: ").append(stats.getEarlyLeaveCount()).append('\n');
            sb.append("Absent: ").append(stats.getAbsentCount()).append('\n');
            if (stats.getOvertimeCount() > 0) {
                sb.append("Overtime: ").append(stats.getOvertimeCount()).append('\n');
            }
            sb.append('\n');
        }

        tvResult.setText(sb.toString());
    }

    private void onExportExcel(View view) {
        if (lastGeneratedReport == null || lastGeneratedReport.isEmpty()) {
            Toast.makeText(this, "Please generate a report first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
            return;
        }

        exportReportToExcel();
    }

    private void exportReportToExcel() {
        btnExportExcel.setEnabled(false);
        btnExportExcel.setAlpha(0.6f);
        Toast.makeText(this, "Exporting Excel...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            OutputStream outputStream = null;
            Uri fileUri = null;
            String fileName = String.format(Locale.getDefault(),
                    "attendance_report_%04d_%02d_%s.xls",
                    lastReportYear,
                    lastReportMonth,
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()));
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.ms-excel");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AttendanceReports");
                    values.put(MediaStore.Downloads.IS_PENDING, 1);

                    ContentResolver resolver = getContentResolver();
                    fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (fileUri == null) {
                        throw new IllegalStateException("Failed to create export file");
                    }
                    outputStream = resolver.openOutputStream(fileUri);
                    writeExcelXml(outputStream, lastGeneratedReport, lastReportYear, lastReportMonth);
                    values.clear();
                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                    resolver.update(fileUri, values, null, null);
                } else {
                    File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AttendanceReports");
                    if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                        throw new IllegalStateException("Failed to create export directory");
                    }
                    File outFile = new File(downloadDir, fileName);
                    outputStream = new FileOutputStream(outFile);
                    writeExcelXml(outputStream, lastGeneratedReport, lastReportYear, lastReportMonth);
                    fileUri = Uri.fromFile(outFile);
                }

                Uri finalFileUri = fileUri;
                runOnUiThread(() -> {
                    btnExportExcel.setEnabled(true);
                    btnExportExcel.setAlpha(1f);
                    String pathInfo = finalFileUri == null ? "Downloads/AttendanceReports" : finalFileUri.toString();
                    Toast.makeText(this, "Excel exported: " + pathInfo, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Export Excel failed", e);
                runOnUiThread(() -> {
                    btnExportExcel.setEnabled(true);
                    btnExportExcel.setAlpha(1f);
                    Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Exception ignore) {
                    }
                }
            }
        }).start();
    }

    private void writeExcelXml(OutputStream outputStream,
                               List<AttendanceStatistics> statsList,
                               int year,
                               int month) throws Exception {
        String xml = buildExcelXml(statsList, year, month);
        outputStream.write(xml.getBytes("UTF-8"));
        outputStream.flush();
    }

    private String buildExcelXml(List<AttendanceStatistics> statsList, int year, int month) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<?mso-application progid=\"Excel.Sheet\"?>");
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"")
                .append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"")
                .append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"")
                .append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
        sb.append("<Styles>")
                .append("<Style ss:ID=\"Header\"><Font ss:Bold=\"1\"/><Interior ss:Color=\"#D9EAF7\" ss:Pattern=\"Solid\"/></Style>")
                .append("<Style ss:ID=\"Title\"><Font ss:Bold=\"1\" ss:Size=\"14\"/></Style>")
                .append("</Styles>");
        sb.append("<Worksheet ss:Name=\"Attendance Report\"><Table>");
        sb.append(row(cell("String", String.format(Locale.getDefault(), "%d-%02d Attendance Report", year, month), "Title")));
        sb.append(row(cell("String", "Export Time") + cell("String", formatDate(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss"))));
        sb.append(row(cell("String", "Employee No", "Header")
                + cell("String", "Name", "Header")
                + cell("String", "Should Attend", "Header")
                + cell("String", "Actual Attend", "Header")
                + cell("String", "Attendance Rate", "Header")
                + cell("String", "Normal", "Header")
                + cell("String", "Late", "Header")
                + cell("String", "Early Leave", "Header")
                + cell("String", "Absent", "Header")
                + cell("String", "Overtime", "Header")));

        for (AttendanceStatistics stats : statsList) {
            sb.append(row(cell("String", safe(stats.getEmployeeNo()))
                    + cell("String", safe(stats.getEmployeeName()))
                    + cell("Number", String.valueOf(stats.getShouldAttendDays()))
                    + cell("Number", String.valueOf(stats.getActualAttendDays()))
                    + cell("String", String.format(Locale.getDefault(), "%.1f%%", stats.getAttendanceRate()))
                    + cell("Number", String.valueOf(stats.getNormalCount()))
                    + cell("Number", String.valueOf(stats.getLateCount()))
                    + cell("Number", String.valueOf(stats.getEarlyLeaveCount()))
                    + cell("Number", String.valueOf(stats.getAbsentCount()))
                    + cell("Number", String.valueOf(stats.getOvertimeCount()))));
        }

        sb.append("</Table></Worksheet></Workbook>");
        return sb.toString();
    }

    private String row(String cells) {
        return "<Row>" + cells + "</Row>";
    }

    private String cell(String type, String value) {
        return cell(type, value, null);
    }

    private String cell(String type, String value, String styleId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Cell");
        if (styleId != null) {
            sb.append(" ss:StyleID=\"").append(styleId).append("\"");
        }
        sb.append("><Data ss:Type=\"").append(type).append("\">")
                .append(escapeXml(value))
                .append("</Data></Cell>");
        return sb.toString();
    }

    private String escapeXml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private long getTodayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getWeekStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String formatDate(long timestamp, String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(timestamp));
    }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(timestamp));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportReportToExcel();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}