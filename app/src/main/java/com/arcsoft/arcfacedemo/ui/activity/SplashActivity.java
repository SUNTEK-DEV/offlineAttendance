package com.arcsoft.arcfacedemo.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.arcsoft.arcfacedemo.R;
import com.arcsoft.arcfacedemo.attendance.repository.AttendanceRepository;
import com.arcsoft.arcfacedemo.attendance.service.AttendanceRuleService;
import com.arcsoft.arcfacedemo.employee.repository.EmployeeRepository;
import com.arcsoft.arcfacedemo.facedb.FaceDatabase;

/**
 * 启动页 Splash Screen
 * 显示初始化全局图 (open.gif)，应用初始化完成后跳转到主界面
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY_MS = 2000; // 最少显示 2 秒

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mainHandler = new Handler(Looper.getMainLooper());

        // 显示版本号
        showVersion();

        // 初始化应用数据
        initializeApp();
    }

    /**
     * 显示应用版本号
     */
    private void showVersion() {
        try {
            String packageName = getPackageName();
            String versionName = getPackageManager()
                    .getPackageInfo(packageName, 0).versionName;
            int versionCode = getPackageManager()
                    .getPackageInfo(packageName, 0).versionCode;


        } catch (Exception e) {
            Log.e(TAG, "Get version info failed", e);
        }
    }

    /**
     * 初始化应用数据
     */
    private void initializeApp() {
        new Thread(() -> {
            try {
                Log.i(TAG, "Initializing app data...");

                // 初始化数据库
                FaceDatabase.getInstance(this);
                Log.i(TAG, "FaceDatabase initialized");

                // 初始化仓库
                AttendanceRepository.getInstance(this);
                Log.i(TAG, "AttendanceRepository initialized");

                // 初始化服务
                AttendanceRuleService.getInstance(this);
                Log.i(TAG, "AttendanceRuleService initialized");

                // 初始化员工仓库
                EmployeeRepository.getInstance(this);
                Log.i(TAG, "EmployeeRepository initialized");

                Log.i(TAG, "App initialization completed");

                // 延迟跳转到主界面
                mainHandler.postDelayed(this::navigateToHome, SPLASH_DELAY_MS);

            } catch (Exception e) {
                Log.e(TAG, "App initialization error", e);
                // 即使出错也继续跳转
                mainHandler.postDelayed(this::navigateToHome, SPLASH_DELAY_MS);
            }
        }).start();
    }

    /**
     * 跳转到主界面
     */
    private void navigateToHome() {
        try {
            Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Navigate to home failed", e);
        }
    }

    @Override
    protected void onDestroy() {
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }
}
