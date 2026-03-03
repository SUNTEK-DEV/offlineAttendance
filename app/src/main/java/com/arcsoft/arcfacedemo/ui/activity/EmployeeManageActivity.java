package com.arcsoft.arcfacedemo.ui.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arcsoft.arcfacedemo.R;
import com.arcsoft.arcfacedemo.databinding.ActivityEmployeeManageBinding;
import com.arcsoft.arcfacedemo.databinding.ItemEmployeeBinding;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.service.EmployeeService;
import com.arcsoft.arcfacedemo.facedb.entity.FaceEntity;
import com.arcsoft.arcfacedemo.ui.viewmodel.EmployeeViewModel;
import com.arcsoft.arcfacedemo.util.ImageUtil;
import com.arcsoft.arcfacedemo.widget.FaceCountNotificationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Employee Management Activity
 * Features: Add, edit, delete employees, support face registration
 */
public class EmployeeManageActivity extends BaseActivity implements BaseActivity.OnGetImageFromAlbumCallback {
    private static final String TAG = "EmployeeManageActivity";
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private ActivityEmployeeManageBinding binding;
    private EmployeeViewModel employeeViewModel;
    private EmployeeAdapter employeeAdapter;
    private EmployeeService employeeService;
    private ColorStateList originBackgroundTintList;

    // 临时存储正在添加的员工信息
    private String tempName;
    private String tempEmployeeNo;
    private String tempDepartment;
    private String tempPosition;
    private String tempPhone;
    private Long tempDepartmentId = 1L;  // Default department ID
    private Long tempPositionId = 1L;    // Default position ID
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_employee_manage);
        initServices();
        initView();
        initViewModel();
        initData();
    }

    private void initServices() {
        Context context = getApplicationContext();
        employeeService = EmployeeService.getInstance(context);
    }

    private void initData() {
        binding.setHasEmployee(true);
        runOnSubThread(() -> {
            employeeViewModel.init();
            employeeViewModel.loadEmployees();
        });
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        employeeViewModel.release();
        employeeViewModel = null;
        super.onDestroy();
    }

    private void initViewModel() {
        employeeViewModel = new ViewModelProvider(
                getViewModelStore(),
                new ViewModelProvider.Factory() {
                    @NonNull
                    @Override
                    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                        if (modelClass == EmployeeViewModel.class) {
                            return (T) new EmployeeViewModel();
                        }
                        throw new IllegalArgumentException(modelClass.getName() + " is not " + EmployeeViewModel.class.getName());
                    }
                }
        ).get(EmployeeViewModel.class);

        // 观察员工列表数据
        employeeViewModel.getEmployeeList().observe(this, employees -> {
            if (employees != null) {
                employeeAdapter.submitList(new LinkedList<>(employees));
                binding.setHasEmployee(employees.size() > 0);
            } else {
                employeeAdapter.submitList(null);
                binding.setHasEmployee(false);
            }
        });

        // 观察员工总数
        employeeViewModel.getTotalEmployeeCount().observe(this, count -> {
            binding.employeeCountNotificationView.refreshTotalFaceCount(count);
        });

        // 观察初始化完成状态
        employeeViewModel.getInitFinished().observe(this, aBoolean -> {
            binding.fabAdd.setClickable(true);
            if (originBackgroundTintList != null) {
                binding.fabAdd.setBackgroundTintList(originBackgroundTintList);
            }
        });
    }

    private void initView() {
        // 设置Toolbar
        setSupportActionBar(binding.toolbar);
        enableBackIfActionBarExists();

        // 设置RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        binding.rvEmployee.setLayoutManager(layoutManager);
        binding.rvEmployee.setItemAnimator(new DefaultItemAnimator());

        employeeAdapter = new EmployeeAdapter();
        binding.rvEmployee.setAdapter(employeeAdapter);

        // 滚动加载
        binding.rvEmployee.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                binding.employeeCountNotificationView.refreshCurrentFaceCount(lastVisibleItem + 1);
                runOnSubThread(() -> employeeViewModel.listScrolled(lastVisibleItem, totalItemCount));
            }
        });

        // 添加按钮点击事件
        binding.fabAdd.setOnClickListener(v -> showAddEmployeeDialog());

        // 初始状态不可点击
        binding.fabAdd.setClickable(false);
        originBackgroundTintList = binding.fabAdd.getBackgroundTintList();
        ColorStateList grayColorStateList = new ColorStateList(
                new int[][]{{android.graphics.Color.GRAY}},
                new int[]{android.graphics.Color.GRAY}
        );
        binding.fabAdd.setBackgroundTintList(grayColorStateList);
    }

    /**
     * 显示添加员工对话框
     */
    private void showAddEmployeeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_employee, null);

        EditText etName = dialogView.findViewById(R.id.et_name);
        EditText etEmployeeNo = dialogView.findViewById(R.id.et_employee_no);
        EditText etDepartment = dialogView.findViewById(R.id.et_department);
        EditText etPosition = dialogView.findViewById(R.id.et_position);
        EditText etPhone = dialogView.findViewById(R.id.et_phone);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnRegisterFace = dialogView.findViewById(R.id.btn_register_face);
        //Button btnSave = dialogView.findViewById(R.id.btn_save);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Register face button
        btnRegisterFace.setOnClickListener(v -> {
            tempName = etName.getText().toString().trim();
            tempEmployeeNo = etEmployeeNo.getText().toString().trim();
            tempDepartment = etDepartment.getText().toString().trim();
            tempPosition = etPosition.getText().toString().trim();
            tempPhone = etPhone.getText().toString().trim();

            // 验证必填字段
            if (TextUtils.isEmpty(tempName)) {
                showToast("Please enter name");
                return;
            }
            if (TextUtils.isEmpty(tempEmployeeNo)) {
                showToast("Please enter employee number");
                return;
            }
            if (TextUtils.isEmpty(tempDepartment)) {
                showToast("Please enter department");
                return;
            }
            if (TextUtils.isEmpty(tempPosition)) {
                showToast("Please enter position");
                return;
            }

            dialog.dismiss();
            // Select photo from album
            getImageFromAlbum(EmployeeManageActivity.this);
        });

        // Save button (save without face registration)
//        btnSave.setOnClickListener(v -> {
//            String name = etName.getText().toString().trim();
//            String employeeNo = etEmployeeNo.getText().toString().trim();
//            String department = etDepartment.getText().toString().trim();
//            String position = etPosition.getText().toString().trim();
//            String phone = etPhone.getText().toString().trim();
//
//            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(employeeNo) ||
//                    TextUtils.isEmpty(department) || TextUtils.isEmpty(position)) {
//                showToast("Please fill in all required fields");
//                return;
//            }
//
//            dialog.dismiss();
//            // Save employee info (without face)
//            saveEmployeeWithoutFace(name, employeeNo, department, position, phone);
//        });

        dialog.show();
    }

    /**
     * 保存员工信息（不带人脸）
     */
    private void saveEmployeeWithoutFace(String name, String employeeNo, String department, String position, String phone) {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setName(name);
        employee.setEmployeeNo(employeeNo);
        employee.setDepartmentId(tempDepartmentId);  // Use default ID
        employee.setPositionId(tempPositionId);        // Use default ID
        employee.setPhone(phone);
        employee.setFaceId(0); // No face yet

        employeeService.addEmployee(employee)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        showProgressDialog("Saving...");
                    }

                    @Override
                    public void onSuccess(Long employeeId) {
                        dismissProgressDialog();
                        showToast("Employee added successfully");
                        employeeViewModel.loadEmployees();
                    }

                    @Override
                    public void onError(Throwable e) {
                        dismissProgressDialog();
                        Log.e(TAG, "添加员工失败", e);
                        showToast("Failed to add employee: " + e.getMessage());
                    }
                });
    }

    /**
     * 从相册获取照片成功后的回调
     */
    @Override
    public void onGetImageFromAlbumSuccess(Uri uri) {
        Bitmap bitmap = ImageUtil.uriToScaledBitmap(this, uri, ImageUtil.DEFAULT_MAX_WIDTH, ImageUtil.DEFAULT_MAX_HEIGHT);
        if (bitmap != null) {
            Log.i(TAG, "onGetImageFromAlbumSuccess: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            // Register face and link to employee
            registerFaceAndSaveEmployee(bitmap);
        } else {
            showToast("Failed to get photo");
        }
    }

    @Override
    public void onGetImageFromAlbumFailed() {
        showToast("Failed to get photo");
    }

    /**
     * 注册人脸并保存员工信息
     */
    private void registerFaceAndSaveEmployee(Bitmap bitmap) {
        // Pass employee name to registerFace
        employeeViewModel.registerFace(bitmap, tempName, (facePreviewInfo, success) -> {
            if (success) {
                // Face registration successful, now save employee info and link faceId
                EmployeeEntity employee = new EmployeeEntity();
                employee.setName(tempName);
                employee.setEmployeeNo(tempEmployeeNo);
                employee.setDepartmentId(tempDepartmentId);
                employee.setPositionId(tempPositionId);
                employee.setPhone(tempPhone);
                employee.setFaceId(facePreviewInfo.getTrackId()); // Link face ID

                employeeService.addEmployee(employee)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<Long>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                showProgressDialog("Saving...");
                            }

                            @Override
                            public void onSuccess(Long employeeId) {
                                dismissProgressDialog();
                                showToast("Employee added successfully, face registered");
                                employeeViewModel.loadEmployees();
                            }

                            @Override
                            public void onError(Throwable e) {
                                dismissProgressDialog();
                                Log.e(TAG, "保存员工失败", e);
                                showToast("Failed to save employee: " + e.getMessage());
                            }
                        });
            } else {
                showToast("Face registration failed");
            }
        });
    }

    /**
     * 删除员工
     */
    public void onEmployeeDeleted(EmployeeEntity employee) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Employee")
                .setMessage("Are you sure to delete employee \"" + employee.getName() + "\"?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    runOnSubThread(() -> {
                        employeeViewModel.deleteEmployee(employee);
                        showLongSnackBar(binding.fabAdd, "Employee deleted");
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (!isAllGranted) {
                showLongToast("Permission denied");
            }
        }
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * 员工列表Adapter
     */
    private class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {
        private List<EmployeeEntity> employees = new ArrayList<>();

        EmployeeAdapter() {
            this.employees = new ArrayList<>();
        }

        void submitList(List<EmployeeEntity> list) {
            if (list != null) {
                this.employees = list;
            } else {
                this.employees = new ArrayList<>();
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemEmployeeBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.getContext()),
                    R.layout.item_employee,
                    parent,
                    false
            );
            return new EmployeeViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
            EmployeeEntity employee = employees.get(position);
            holder.bind(employee);
        }

        @Override
        public int getItemCount() {
            return employees.size();
        }

        class EmployeeViewHolder extends RecyclerView.ViewHolder {
            private ItemEmployeeBinding binding;

            EmployeeViewHolder(ItemEmployeeBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(EmployeeEntity employee) {
                binding.setEmployee(employee);
                binding.executePendingBindings();

                // 删除按钮
                binding.btnDelete.setOnClickListener(v -> {
                    onEmployeeDeleted(employee);
                });
            }
        }
    }
}
