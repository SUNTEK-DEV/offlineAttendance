package com.arcsoft.arcfacedemo.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.arcsoft.arcfacedemo.R;
import com.arcsoft.arcfacedemo.databinding.ActivityHomeBinding;
import com.arcsoft.arcfacedemo.ui.viewmodel.HomeViewModel;
import com.arcsoft.arcfacedemo.util.ErrorCodeUtil;
import com.arcsoft.arcfacedemo.widget.NavigateItemView;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.VersionInfo;

/**
 * 首页，包括激活、界面选择等功能
 */
public class HomeActivity extends BaseActivity implements View.OnClickListener {
    private ActivityHomeBinding activityHomeBinding;
    private static final String TAG = "HomeActivity";
    private NavigateItemView activeView;
    private PermissionDegreeDialog permissionDegreeDialog;
    HomeViewModel homeViewModel;

    private static final String[] NEEDED_PERMISSIONS = new String[]{Manifest.permission.READ_PHONE_STATE};

    private static final int REQUEST_ACTIVE_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityHomeBinding = DataBindingUtil.setContentView(this, R.layout.activity_home);
        initViewModel();
        initView();
        permissionDegreeDialog();
    }

    private void initData() {
        homeViewModel.getActivated().postValue(homeViewModel.isActivated(this));
    }

    private void initViewModel() {
        homeViewModel = new ViewModelProvider(
                getViewModelStore(),
                new ViewModelProvider.AndroidViewModelFactory(getApplication())
        )
                .get(HomeViewModel.class);


        // 设置监听，在数据变更时，更新View中内容
        homeViewModel.getActivated().observe(this, activated
                -> activeView.changeTipHint(getString(activated ? R.string.already_activated : R.string.not_activated))
        );

        homeViewModel.getActiveCode().observe(this, activeCode -> {
            String notification;
            switch (activeCode) {
                case ErrorInfo.MOK:
                    notification = getString(R.string.active_success);
                    break;
                case ErrorInfo.MERR_ASF_ALREADY_ACTIVATED:
                    notification = getString(R.string.dont_need_active_anymore);
                    break;
                default:
                    notification = getString(R.string.active_failed, activeCode, ErrorCodeUtil.arcFaceErrorCodeToFieldName(activeCode));
                    break;
            }
            activeView.changeTipHint(notification);
            showToast(notification);
        });
    }

    private void initView() {
        VersionInfo versionInfo = new VersionInfo();
        if (FaceEngine.getVersion(versionInfo) == ErrorInfo.MOK) {
            activityHomeBinding.setSdkVersion("ArcFace SDK Version:" + versionInfo.getVersion());
        }

        activityHomeBinding.llRootView.addView(new NavigateItemView(this, R.drawable.ic_face_id_ir, getString(R.string.page_ir_face_recognize), RegisterAndRecognizeActivity.class));
        activityHomeBinding.llRootView.addView(new NavigateItemView(this, R.drawable.ic_liveness_check, getString(R.string.page_liveness_detect), LivenessDetectActivity.class));
        activityHomeBinding.llRootView.addView(new NavigateItemView(this, R.drawable.ic_face_attr, getString(R.string.page_single_image), ImageFaceAttrDetectActivity.class));
        activityHomeBinding.llRootView.addView(new NavigateItemView(this, R.drawable.ic_face_compare, getString(R.string.page_face_compare), FaceCompareActivity.class));
        activityHomeBinding.llRootView.addView(new NavigateItemView(this, R.drawable.ic_face_manage, getString(R.string.page_face_manage), EmployeeManageActivity.class));
        activityHomeBinding.llRootView.addView(new NavigateItemView(this, R.drawable.ic_settings, getString(R.string.page_settings), RecognizeSettingsActivity.class));

        // 添加考勤查询入口（替代原来的考勤门禁管理）
        activityHomeBinding.llRootView.addView(new NavigateItemView(this, android.R.drawable.ic_menu_agenda, "Attendance Query", com.arcsoft.arcfacedemo.demo.AttendanceQueryActivity.class));

        activeView = new NavigateItemView(this, R.drawable.ic_online_active, getString(R.string.active_engine), "", ActivationActivity.class);
        activityHomeBinding.llRootView.addView(activeView);

//        activityHomeBinding.llRootView.addView(new NavigateItemView(this, R.drawable.ic_readme, getString(R.string.page_readme), ReadmeActivity.class));


        // 添加诊断按钮
        activityHomeBinding.btnDiagnose.setOnClickListener(v -> diagnoseData());

        int childCount = activityHomeBinding.llRootView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View itemView = activityHomeBinding.llRootView.getChildAt(i);
            itemView.setOnClickListener(this);
        }
    }

    /**
     * 诊断数据
     */
    private void diagnoseData() {
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("========== Data Diagnostics ==========\n\n");

            try {
                // 1. 查询员工数据
                com.arcsoft.arcfacedemo.employee.service.EmployeeService employeeService =
                        com.arcsoft.arcfacedemo.employee.service.EmployeeService.getInstance(this);

                java.util.List<com.arcsoft.arcfacedemo.employee.model.EmployeeEntity> employees =
                        employeeService.getAllEmployees().blockingGet();

                sb.append("[Employee Data]\n");
                sb.append("Employee Count: ").append(employees.size()).append("\n\n");

                for (com.arcsoft.arcfacedemo.employee.model.EmployeeEntity emp : employees) {
                    sb.append("━━━━━━━━━━━━━━━━━━━━\n");
                    sb.append("Employee ID: ").append(emp.getEmployeeId()).append("\n");
                    sb.append("Employee No: ").append(emp.getEmployeeNo()).append("\n");
                    sb.append("Name: ").append(emp.getName()).append("\n");
                    sb.append("Face ID: ").append(emp.getFaceId()).append("\n");
                    sb.append("\n");
                }

                // 2. 查询人脸数据
                com.arcsoft.arcfacedemo.facedb.dao.FaceDao faceDao =
                        com.arcsoft.arcfacedemo.facedb.FaceDatabase.getInstance(this).faceDao();

                java.util.List<com.arcsoft.arcfacedemo.facedb.entity.FaceEntity> faces = faceDao.getAllFaces();

                sb.append("\n[Face Data]\n");
                sb.append("Face Count: ").append(faces.size()).append("\n\n");

                for (com.arcsoft.arcfacedemo.facedb.entity.FaceEntity face : faces) {
                    sb.append("━━━━━━━━━━━━━━━━━━━━\n");
                    sb.append("Face ID: ").append(face.getFaceId()).append("\n");
                    sb.append("Username: ").append(face.getUserName()).append("\n");
                    sb.append("\n");
                }

                // 3. 检查关联
                sb.append("\n[Data Relationship Check]\n");
                for (com.arcsoft.arcfacedemo.employee.model.EmployeeEntity emp : employees) {
                    if (emp.getFaceId() > 0) {
                        boolean faceExists = false;
                        for (com.arcsoft.arcfacedemo.facedb.entity.FaceEntity face : faces) {
                            if (face.getFaceId() == emp.getFaceId()) {
                                faceExists = true;
                                sb.append("✓ ").append(emp.getName()).append(" -> faceId=").append(emp.getFaceId()).append("\n");
                                break;
                            }
                        }
                        if (!faceExists) {
                            sb.append("✗ ").append(emp.getName()).append(" -> faceId=").append(emp.getFaceId()).append(" (Face not exist)\n");
                        }
                    } else {
                        sb.append("! ").append(emp.getName()).append(" -> No face registered\n");
                    }
                }

            } catch (Exception e) {
                sb.append("\nDiagnostics failed: ").append(e.getMessage());
                e.printStackTrace();
            }

            String result = sb.toString();
            runOnUiThread(() -> {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Data Diagnostics Result")
                        .setMessage(result)
                        .setPositiveButton("OK", null)
                        .show();
            });
        }).start();
    }


    @Override
    protected void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (isAllGranted) {
            initData();
        } else {
            showToast(getString(R.string.permission_denied));
        }
    }

    @Override
    public void onClick(View v) {
        if (v instanceof NavigateItemView) {
            switch (((NavigateItemView) v).getImgRes()) {
                case R.drawable.ic_online_active:
                    navigateToNewPageForResult(((Class) ((NavigateItemView) v).getExtraData()), REQUEST_ACTIVE_CODE);
                    break;
                case R.drawable.ic_readme:
                    navigateToNewPage(((Class) ((NavigateItemView) v).getExtraData()));
                    break;
                default:

                    boolean activated = homeViewModel.isActivated(this);
                    if (!activated) {
                        showLongToast(getString(R.string.notice_please_active_before_use));
                        activeView.performClick();
                    } else {
                        navigateToNewPage(((Class) ((NavigateItemView) v).getExtraData()));
                    }
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACTIVE_CODE:
                homeViewModel.getActivated().postValue(homeViewModel.isActivated(this));
                break;
            default:
                break;
        }
    }

    public void permissionDegreeDialog() {
        if (permissionDegreeDialog == null) {
            permissionDegreeDialog = new PermissionDegreeDialog();
            permissionDegreeDialog.setCallback(new PermissionDegreeDialog.Callback() {
                @Override
                public void onRefuse(boolean refuse) {
                    if (refuse) {
                        finish();
                    } else {
                        if (checkPermissions(NEEDED_PERMISSIONS)) {
                            initData();
                        } else {
                            ActivityCompat.requestPermissions(HomeActivity.this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
                        }
                    }
                }
            });
        }
        if (permissionDegreeDialog.isAdded()) {
            permissionDegreeDialog.dismiss();
        }
        permissionDegreeDialog.show(getSupportFragmentManager(), PermissionDegreeDialog.class.getSimpleName());
    }
}
