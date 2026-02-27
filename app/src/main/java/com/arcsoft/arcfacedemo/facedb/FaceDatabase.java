package com.arcsoft.arcfacedemo.facedb;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.arcsoft.arcfacedemo.attendance.dao.AttendanceRecordDao;
import com.arcsoft.arcfacedemo.attendance.dao.AttendanceRuleDao;
import com.arcsoft.arcfacedemo.attendance.dao.HolidayDao;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceRecordEntity;
import com.arcsoft.arcfacedemo.attendance.model.AttendanceRuleEntity;
import com.arcsoft.arcfacedemo.attendance.model.HolidayEntity;
import com.arcsoft.arcfacedemo.dooraccess.dao.DoorAccessRecordDao;
import com.arcsoft.arcfacedemo.dooraccess.model.DoorAccessRecordEntity;
import com.arcsoft.arcfacedemo.employee.dao.DepartmentDao;
import com.arcsoft.arcfacedemo.employee.dao.EmployeeDao;
import com.arcsoft.arcfacedemo.employee.dao.PositionDao;
import com.arcsoft.arcfacedemo.employee.model.DepartmentEntity;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.model.PositionEntity;
import com.arcsoft.arcfacedemo.facedb.dao.FaceDao;
import com.arcsoft.arcfacedemo.facedb.entity.FaceEntity;

import java.io.File;

@Database(
        entities = {
                FaceEntity.class,
                DepartmentEntity.class,
                PositionEntity.class,
                EmployeeEntity.class,
                AttendanceRecordEntity.class,
                AttendanceRuleEntity.class,
                HolidayEntity.class,
                DoorAccessRecordEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class FaceDatabase extends RoomDatabase {

    // 现有DAO
    public abstract FaceDao faceDao();

    // 新增DAO
    public abstract DepartmentDao departmentDao();
    public abstract PositionDao positionDao();
    public abstract EmployeeDao employeeDao();
    public abstract AttendanceRecordDao attendanceRecordDao();
    public abstract AttendanceRuleDao attendanceRuleDao();
    public abstract HolidayDao holidayDao();
    public abstract DoorAccessRecordDao doorAccessRecordDao();

    private static volatile FaceDatabase faceDatabase = null;

    /**
     * 数据库迁移：从版本1升级到版本2
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 检查旧表是否存在并需要迁移
            boolean oldDepartmentExists = tableExists(database, "department");
            boolean oldPositionExists = tableExists(database, "position");

            // 处理部门表
            if (oldDepartmentExists) {
                // 重命名列
                renameColumnIfExists(database, "department", "departmentId", "department_id");
                renameColumnIfExists(database, "department", "departmentName", "department_name");
                renameColumnIfExists(database, "department", "sortOrder", "sort_order");
                renameColumnIfExists(database, "department", "createTime", "create_time");
            } else {
                // 创建新表
                database.execSQL(
                        "CREATE TABLE IF NOT EXISTS department (" +
                                "department_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "department_name TEXT, " +
                                "description TEXT, " +
                                "sort_order INTEGER NOT NULL, " +
                                "create_time INTEGER NOT NULL)"
                );
            }

            // 处理岗位表
            if (oldPositionExists) {
                renameColumnIfExists(database, "position", "positionId", "position_id");
                renameColumnIfExists(database, "position", "positionName", "position_name");
                renameColumnIfExists(database, "position", "sortOrder", "sort_order");
                renameColumnIfExists(database, "position", "createTime", "create_time");
            } else {
                database.execSQL(
                        "CREATE TABLE IF NOT EXISTS position (" +
                                "position_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "position_name TEXT, " +
                                "description TEXT, " +
                                "sort_order INTEGER NOT NULL, " +
                                "create_time INTEGER NOT NULL)"
                );
            }

            // 创建员工表（新表）
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS employee (" +
                            "employee_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "employee_no TEXT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "face_id INTEGER NOT NULL, " +
                            "department_id INTEGER NOT NULL, " +
                            "position_id INTEGER NOT NULL, " +
                            "phone TEXT, " +
                            "status TEXT NOT NULL, " +
                            "hire_date INTEGER NOT NULL, " +
                            "create_time INTEGER NOT NULL, " +
                            "update_time INTEGER NOT NULL, " +
                            "FOREIGN KEY(department_id) REFERENCES department(department_id) ON DELETE RESTRICT, " +
                            "FOREIGN KEY(position_id) REFERENCES position(position_id) ON DELETE RESTRICT)"
            );

            // 创建员工表索引
            database.execSQL("CREATE INDEX IF NOT EXISTS index_employee_employee_no ON employee(employee_no)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_employee_department_id ON employee(department_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_employee_position_id ON employee(position_id)");

            // 创建考勤记录表
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS attendance_record (" +
                            "recordId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "employee_id INTEGER NOT NULL, " +
                            "employee_no TEXT, " +
                            "employee_name TEXT, " +
                            "date INTEGER NOT NULL, " +
                            "check_in_time INTEGER, " +
                            "check_out_time INTEGER, " +
                            "check_in_status TEXT, " +
                            "check_out_status TEXT, " +
                            "check_in_image_path TEXT, " +
                            "check_out_image_path TEXT, " +
                            "work_status TEXT NOT NULL, " +
                            "remark TEXT, " +
                            "create_time INTEGER NOT NULL, " +
                            "update_time INTEGER NOT NULL, " +
                            "FOREIGN KEY(employee_id) REFERENCES employee(employee_id) ON DELETE CASCADE)"
            );

            // 创建考勤记录表索引
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_attendance_record_employee_id_date ON attendance_record(employee_id, date)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_record_date ON attendance_record(date)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_record_check_in_time ON attendance_record(check_in_time)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_record_check_out_time ON attendance_record(check_out_time)");

            // 创建考勤规则表
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS attendance_rule (" +
                            "id INTEGER NOT NULL PRIMARY KEY, " +
                            "work_mode TEXT, " +
                            "work_days INTEGER NOT NULL, " +
                            "morning_start_time INTEGER, " +
                            "morning_end_time INTEGER, " +
                            "afternoon_start_time INTEGER, " +
                            "afternoon_end_time INTEGER, " +
                            "late_tolerance INTEGER NOT NULL, " +
                            "early_leave_tolerance INTEGER NOT NULL, " +
                            "overtime_threshold INTEGER NOT NULL, " +
                            "require_photo INTEGER NOT NULL, " +
                            "require_liveness INTEGER NOT NULL, " +
                            "update_time INTEGER NOT NULL)"
            );

            // 创建节假日表
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS holiday (" +
                            "holiday_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "date INTEGER NOT NULL, " +
                            "holiday_type TEXT, " +
                            "name TEXT, " +
                            "remark TEXT)"
            );

            // 创建门禁记录表
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS door_access_record (" +
                            "recordId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "employee_id INTEGER NOT NULL, " +
                            "employee_no TEXT, " +
                            "employee_name TEXT, " +
                            "access_type TEXT, " +
                            "access_result TEXT, " +
                            "fail_reason TEXT, " +
                            "image_path TEXT, " +
                            "access_time INTEGER NOT NULL, " +
                            "device_info TEXT)"
            );

            // 创建门禁记录表索引
            database.execSQL("CREATE INDEX IF NOT EXISTS index_door_access_record_employee_id ON door_access_record(employee_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_door_access_record_access_time ON door_access_record(access_time)");

            // 初始化默认考勤规则
            database.execSQL(
                    "INSERT OR REPLACE INTO attendance_rule (" +
                            "id, work_mode, work_days, " +
                            "morning_start_time, morning_end_time, " +
                            "afternoon_start_time, afternoon_end_time, " +
                            "late_tolerance, early_leave_tolerance, overtime_threshold, " +
                            "require_photo, require_liveness, update_time) VALUES (" +
                            "1, 'FIXED', 31, " +  // 周一到周五
                            "30600000, 43200000, " +  // 8:30, 12:00
                            "48600000, 64800000, " +  // 13:30, 18:00
                            "5, 5, 60, " +
                            "1, 1, " + System.currentTimeMillis() + ")"
            );
        }

        /**
         * 检查表是否存在
         */
        private boolean tableExists(SupportSQLiteDatabase db, String tableName) {
            try {
                android.database.Cursor cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'");
                boolean exists = cursor != null && cursor.getCount() > 0;
                if (cursor != null) {
                    cursor.close();
                }
                return exists;
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * 重命名列（如果存在）
         */
        private void renameColumnIfExists(SupportSQLiteDatabase db, String tableName, String oldName, String newName) {
            try {
                // 检查列是否存在
                android.database.Cursor cursor = db.query("PRAGMA table_info(" + tableName + ")");
                if (cursor != null) {
                    boolean columnExists = false;
                    while (cursor.moveToNext()) {
                        String columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                        if (oldName.equals(columnName)) {
                            columnExists = true;
                            break;
                        }
                    }
                    cursor.close();

                    if (columnExists && !oldName.equals(newName)) {
                        db.execSQL("ALTER TABLE " + tableName + " RENAME COLUMN " + oldName + " TO " + newName);
                    }
                }
            } catch (Exception e) {
                // 列不存在或其他错误，忽略
            }
        }
    };

    public static FaceDatabase getInstance(Context context) {
        if (faceDatabase == null) {
            synchronized (FaceDatabase.class) {
                if (faceDatabase == null) {
                    faceDatabase = Room.databaseBuilder(context, FaceDatabase.class,
                                    context.getExternalFilesDir("database") + File.separator + "faceDB.db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return faceDatabase;
    }
}
