package com.arcsoft.arcfacedemo.ui.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Camera;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.arcsoft.arcfacedemo.R;
import com.arcsoft.arcfacedemo.databinding.ActivityRegisterAndRecognizeBinding;
import com.arcsoft.arcfacedemo.ui.model.PreviewConfig;
import com.arcsoft.arcfacedemo.ui.viewmodel.RecognizeViewModel;
import com.arcsoft.arcfacedemo.util.ConfigUtil;
import com.arcsoft.arcfacedemo.util.ErrorCodeUtil;
import com.arcsoft.arcfacedemo.util.AudioPlayer;
import com.arcsoft.arcfacedemo.util.FaceRectTransformer;
import com.arcsoft.arcfacedemo.util.camera.CameraListener;
import com.arcsoft.arcfacedemo.util.camera.DualCameraHelper;
import com.arcsoft.arcfacedemo.util.face.constants.LivenessType;
import com.arcsoft.arcfacedemo.util.face.model.FacePreviewInfo;
import com.arcsoft.arcfacedemo.widget.FaceRectView;
import com.arcsoft.arcfacedemo.widget.RecognizeAreaView;
import com.arcsoft.face.ErrorInfo;

import java.util.List;

public class RegisterAndRecognizeActivity extends BaseActivity implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "RegisterAndRecognize";

    // 打卡方式枚举
    private enum CheckInMode {
        FACE_RECOGNITION,  // 人脸识别
        NFC,              // NFC 打卡
        QR_CODE           // 二维码打卡
    }

    private CheckInMode currentCheckInMode = CheckInMode.FACE_RECOGNITION;
    private static final String ADMIN_PASSWORD = "123456";  // 管理员密码
    private static final int ADMIN_CLICK_COUNT = 5;  // 管理员验证点击次数
    private int adminClickCount = 0;  // 当前点击次数
    private long lastClickTime = 0;   // 上次点击时间

    private Button btnSwitchCheckInMode;

    // NFC 相关
    private NfcAdapter nfcAdapter;
    private static final int REQUEST_NFC_PERMISSION = 0x002;

    // NFC 键盘模拟输入相关（USB NFC 读卡器模拟键盘输出）
    private String nfcKeyContent = "";  // NFC 读卡器输出内容
    private boolean nfcGetFlag = true;  // 读取标志位

    // 二维码输入相关（虚拟键盘）
    private EditText qrCodeInputEditText;

    private DualCameraHelper rgbCameraHelper;
    private DualCameraHelper irCameraHelper;
    private FaceRectTransformer rgbFaceRectTransformer;
    private FaceRectTransformer irFaceRectTransformer;

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;

    int actionAfterFinish = 0;
    private static final int NAVIGATE_TO_RECOGNIZE_SETTINGS_ACTIVITY = 1;
    private static final int NAVIGATE_TO_RECOGNIZE_DEBUG_ACTIVITY = 2;

    /**
     * 所需的所有权限信息
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
    };
    private ActivityRegisterAndRecognizeBinding binding;
    private RecognizeViewModel recognizeViewModel;
    private LivenessType livenessType;
    private boolean enableLivenessDetect = false;
    private RecognizeAreaView recognizeAreaView;
    private TextView textViewRgb;
    private TextView textViewIr;
    private boolean openRectInfoDraw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register_and_recognize);
        Log.i(TAG,"进入到了人脸识别中-----");
        //保持亮屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().setAttributes(attributes);
        }

        // Activity启动后就锁定为启动时的方向
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        initData();//初始化数据
        initViewModel();

        // 初始化音频播放器
        Log.i(TAG, "Initializing AudioPlayer...");
        AudioPlayer.getInstance(this);

        initView();
        initNfc();
        updateCheckInModeButton();
        initQrCodeInput();
        openRectInfoDraw = true;
        recognizeViewModel.setDrawRectInfoTextValue(true);

        // 启动科技感扫描线动画
        startScanLineAnimation();
    }

    /**
     * 启动扫描线动画
     */
    private void startScanLineAnimation() {
        View scanLine = binding.getRoot().findViewById(R.id.scan_line);
        if (scanLine != null) {
            scanLine.setVisibility(View.VISIBLE);
            android.view.animation.Animation animation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scan_line_animation);
            scanLine.startAnimation(animation);
        }
    }

    private void initData() {
        String livenessTypeStr = ConfigUtil.getLivenessDetectType(this);

        if (livenessTypeStr.equals((getString(R.string.value_liveness_type_rgb)))) {
            livenessType = LivenessType.RGB;
        } else if (livenessTypeStr.equals(getString(R.string.value_liveness_type_ir))) {
            livenessType = LivenessType.IR;
        } else {
            livenessType = null;
        }
        enableLivenessDetect = !ConfigUtil.getLivenessDetectType(this).equals(getString(R.string.value_liveness_type_disable));
    }

    private void initViewModel() {
        recognizeViewModel = new ViewModelProvider(
                getViewModelStore(),
                new ViewModelProvider.AndroidViewModelFactory(getApplication())
        )
                .get(RecognizeViewModel.class);

        recognizeViewModel.setLiveType(livenessType);

        recognizeViewModel.getFtInitCode().observe(this, ftInitCode -> {
            if (ftInitCode != ErrorInfo.MOK) {
                String error = getString(R.string.specific_engine_init_failed, "ftEngine",
                        ftInitCode, ErrorCodeUtil.arcFaceErrorCodeToFieldName(ftInitCode));
                Log.i(TAG, "initEngine: " + error);
                showToast(error);
            }
        });
        recognizeViewModel.getFrInitCode().observe(this, frInitCode -> {
            if (frInitCode != ErrorInfo.MOK) {
                String error = getString(R.string.specific_engine_init_failed, "frEngine",
                        frInitCode, ErrorCodeUtil.arcFaceErrorCodeToFieldName(frInitCode));
                Log.i(TAG, "initEngine: " + error);
                showToast(error);
            }
        });
        recognizeViewModel.getFlInitCode().observe(this, flInitCode -> {
            if (flInitCode != ErrorInfo.MOK) {
                String error = getString(R.string.specific_engine_init_failed, "flEngine",
                        flInitCode, ErrorCodeUtil.arcFaceErrorCodeToFieldName(flInitCode));
                Log.i(TAG, "initEngine: " + error);
                showToast(error);
            }
        });
        recognizeViewModel.getFaceItemEventMutableLiveData().observe(this, faceItemEvent -> {
            RecyclerView.Adapter adapter = binding.dualCameraRecyclerViewPerson.getAdapter();
            switch (faceItemEvent.getEventType()) {
                case REMOVED:
                    if (adapter != null) {
                        adapter.notifyItemRemoved(faceItemEvent.getIndex());
                    }
                    break;
                case INSERTED:
                    if (adapter != null) {
                        adapter.notifyItemInserted(faceItemEvent.getIndex());
                    }
                    break;
                default:
                    break;
            }
        });

        recognizeViewModel.getRecognizeConfiguration().observe(this, recognizeConfiguration -> {
            Log.i(TAG, "initViewModel recognizeConfiguration: " + recognizeConfiguration.toString());
        });

        recognizeViewModel.setOnRegisterFinishedCallback((facePreviewInfo, success) -> showToast(success ? "register success" : "register failed"));

        recognizeViewModel.getRecognizeNotice().observe(this, notice -> binding.setRecognizeNotice(notice));

        recognizeViewModel.getDrawRectInfoText().observe(this, drawRectInfoText -> {
            binding.setDrawRectInfoText(drawRectInfoText);
        });

        // 初始化打卡仓库
        recognizeViewModel.initAttendanceRepository(this);

        // 观察打卡结果
        recognizeViewModel.getPunchResult().observe(this, punchResult -> {
            showToast(punchResult);
            Log.i(TAG, "Punch result: " + punchResult);
        });
    }

    private void initView() {
        if (!DualCameraHelper.hasDualCamera() || livenessType != LivenessType.IR) {
            binding.flRecognizeIr.setVisibility(View.GONE);
        }
        //在布局结束后才做初始化操作
        binding.dualCameraTexturePreviewRgb.getViewTreeObserver().addOnGlobalLayoutListener(this);
        binding.setCompareResultList(recognizeViewModel.getCompareResultList().getValue());
    }

    @Override
    protected void onDestroy() {
        // 释放音频播放器资源
        AudioPlayer.getInstance(this).release();

        if (irCameraHelper != null) {
            irCameraHelper.release();
            irCameraHelper = null;
        }

        if (rgbCameraHelper != null) {
            rgbCameraHelper.release();
            rgbCameraHelper = null;
        }

        recognizeViewModel.destroy();
        switch (actionAfterFinish) {
            case NAVIGATE_TO_RECOGNIZE_DEBUG_ACTIVITY:
                navigateToNewPage(RecognizeDebugActivity.class);
                break;
            case NAVIGATE_TO_RECOGNIZE_SETTINGS_ACTIVITY:
                navigateToNewPage(RecognizeSettingsActivity.class);
                break;
            default:
                break;
        }
        super.onDestroy();
    }


    /**
     * 调整View的宽高，使2个预览同时显示
     *
     * @param previewView        显示预览数据的view
     * @param faceRectView       画框的view
     * @param previewSize        预览大小
     * @param displayOrientation 相机旋转角度
     * @return 调整后的LayoutParams
     */
    /**
     * 调整View的宽高，使2个预览同时显示（限制不超过屏幕尺寸）
     */
    private ViewGroup.LayoutParams adjustPreviewViewSize(View rgbPreview, View previewView, FaceRectView faceRectView, Camera.Size previewSize, int displayOrientation, float scale) {
        ViewGroup.LayoutParams layoutParams = previewView.getLayoutParams();
        int measuredWidth = previewView.getMeasuredWidth();
        int measuredHeight = previewView.getMeasuredHeight();
        float ratio = ((float) previewSize.height) / (float) previewSize.width;
        if (ratio > 1) {
            ratio = 1 / ratio;
        }
        if (displayOrientation % 180 == 0) {
            layoutParams.width = measuredWidth;
            layoutParams.height = (int) (measuredWidth * ratio);
        } else {
            layoutParams.height = measuredHeight;
            layoutParams.width = (int) (measuredHeight * ratio);
        }
        if (scale < 1f) {
            ViewGroup.LayoutParams rgbParam = rgbPreview.getLayoutParams();
            layoutParams.width = (int) (rgbParam.width * scale);
            layoutParams.height = (int) (rgbParam.height * scale);
        } else {
            layoutParams.width *= scale;
            layoutParams.height *= scale;
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if (layoutParams.width >= metrics.widthPixels) {
            float viewRatio = layoutParams.width / ((float) metrics.widthPixels);
            layoutParams.width /= viewRatio;
            layoutParams.height /= viewRatio;
        }
        if (layoutParams.height >= metrics.heightPixels) {
            float viewRatio = layoutParams.height / ((float) metrics.heightPixels);
            layoutParams.width /= viewRatio;
            layoutParams.height /= viewRatio;
        }

        previewView.setLayoutParams(layoutParams);
        faceRectView.setLayoutParams(layoutParams);
        return layoutParams;
    }

    /**
     * 调整View的宽高，不限制屏幕尺寸，用于放大画面
     */
    private ViewGroup.LayoutParams adjustPreviewViewSizeNoLimit(View rgbPreview, View previewView, FaceRectView faceRectView, Camera.Size previewSize, int displayOrientation, float scale) {
        ViewGroup.LayoutParams layoutParams = previewView.getLayoutParams();
        int measuredWidth = previewView.getMeasuredWidth();
        int measuredHeight = previewView.getMeasuredHeight();
        float ratio = ((float) previewSize.height) / (float) previewSize.width;
        if (ratio > 1) {
            ratio = 1 / ratio;
        }
        if (displayOrientation % 180 == 0) {
            layoutParams.width = measuredWidth;
            layoutParams.height = (int) (measuredWidth * ratio);
        } else {
            layoutParams.height = measuredHeight;
            layoutParams.width = (int) (measuredHeight * ratio);
        }

        // 应用缩放比例（不限制屏幕尺寸）
        layoutParams.width = (int) (layoutParams.width * scale);
        layoutParams.height = (int) (layoutParams.height * scale);

        previewView.setLayoutParams(layoutParams);
        faceRectView.setLayoutParams(layoutParams);
        return layoutParams;
    }

    private void initRgbCamera() {
        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                runOnUiThread(() -> {
                    Camera.Size previewSizeRgb = camera.getParameters().getPreviewSize();

                    // 获取屏幕尺寸
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                    // 计算目标尺寸：宽度充满屏幕，高度为屏幕的2/3
                    int targetWidth = displayMetrics.widthPixels;
                    int targetHeight = (int) (displayMetrics.heightPixels * 2.0 / 3.0);

                    FrameLayout parentView = ((FrameLayout) binding.dualCameraTexturePreviewRgb.getParent());

                    // 设置父View的尺寸和重力，使内容居中
                    ViewGroup.LayoutParams parentLayoutParams = parentView.getLayoutParams();
                    parentLayoutParams.width = targetWidth;
                    parentLayoutParams.height = targetHeight;
                    parentView.setLayoutParams(parentLayoutParams);

                    // 计算保持宽高比的尺寸（类似CENTER_CROP）
                    float previewRatio = (float) previewSizeRgb.width / (float) previewSizeRgb.height;
                    float targetRatio = (float) targetWidth / (float) targetHeight;

                    int finalWidth, finalHeight;
                    if (previewRatio > targetRatio) {
                        // 相机预览更宽，以高度为基准
                        finalHeight = targetHeight;
                        finalWidth = (int) (finalHeight * previewRatio);
                    } else {
                        // 相机预览更高，以宽度为基准
                        finalWidth = targetWidth;
                        finalHeight = (int) (finalWidth / previewRatio);
                    }

                    // 设置TextureView和FaceRectView为相同尺寸
                    ViewGroup.LayoutParams textureParams = binding.dualCameraTexturePreviewRgb.getLayoutParams();
                    textureParams.width = finalWidth;
                    textureParams.height = finalHeight;
                    binding.dualCameraTexturePreviewRgb.setLayoutParams(textureParams);

                    // 设置LayoutGravity使TextureView居中
                    FrameLayout.LayoutParams textureLayoutParams = (FrameLayout.LayoutParams) binding.dualCameraTexturePreviewRgb.getLayoutParams();
                    textureLayoutParams.gravity = android.view.Gravity.CENTER;
                    binding.dualCameraTexturePreviewRgb.setLayoutParams(textureLayoutParams);

                    // FaceRectView使用相同尺寸和位置
                    ViewGroup.LayoutParams faceRectParams = binding.dualCameraFaceRectView.getLayoutParams();
                    faceRectParams.width = finalWidth;
                    faceRectParams.height = finalHeight;
                    binding.dualCameraFaceRectView.setLayoutParams(faceRectParams);

                    FrameLayout.LayoutParams faceRectLayoutParams = (FrameLayout.LayoutParams) binding.dualCameraFaceRectView.getLayoutParams();
                    faceRectLayoutParams.gravity = android.view.Gravity.CENTER;
                    binding.dualCameraFaceRectView.setLayoutParams(faceRectLayoutParams);

                    if (textViewRgb == null) {
                        textViewRgb = new TextView(RegisterAndRecognizeActivity.this, null);
                    } else {
                        parentView.removeView(textViewRgb);
                    }
                    textViewRgb.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    textViewRgb.setText(getString(R.string.camera_rgb_preview_size, previewSizeRgb.width, previewSizeRgb.height));
                    textViewRgb.setTextColor(Color.WHITE);
                    textViewRgb.setBackgroundColor(getResources().getColor(R.color.color_bg_notification));
                    parentView.addView(textViewRgb);

                    // 创建FaceRectTransformer，使用TextureView的实际显示尺寸
                    rgbFaceRectTransformer = new FaceRectTransformer(previewSizeRgb.width, previewSizeRgb.height,
                            finalWidth, finalHeight, displayOrientation, cameraId, isMirror,
                            ConfigUtil.isDrawRgbRectHorizontalMirror(RegisterAndRecognizeActivity.this),
                            ConfigUtil.isDrawRgbRectVerticalMirror(RegisterAndRecognizeActivity.this));

                    // 添加recognizeAreaView，在识别区域发生变更时，更新数据给FaceHelper
                    if (ConfigUtil.isRecognizeAreaLimited(RegisterAndRecognizeActivity.this)) {
                        if (recognizeAreaView == null) {
                            recognizeAreaView = new RecognizeAreaView(RegisterAndRecognizeActivity.this);
                            recognizeAreaView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        } else {
                            parentView.removeView(recognizeAreaView);
                        }
                        recognizeAreaView.setOnRecognizeAreaChangedListener(recognizeArea -> recognizeViewModel.setRecognizeArea(recognizeArea));
                        parentView.addView(recognizeAreaView);
                    }

                    recognizeViewModel.onRgbCameraOpened(camera);
                    recognizeViewModel.setRgbFaceRectTransformer(rgbFaceRectTransformer);
                });
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                binding.dualCameraFaceRectView.clearFaceInfo();
                List<FacePreviewInfo> facePreviewInfoList = recognizeViewModel.onPreviewFrame(nv21, true);
                if (facePreviewInfoList != null && rgbFaceRectTransformer != null) {
                    drawPreviewInfo(facePreviewInfoList);
                }
                recognizeViewModel.clearLeftFace(facePreviewInfoList);
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                Log.i(TAG, "onCameraConfigurationChanged:" + Thread.currentThread().getName());
                if (rgbFaceRectTransformer != null) {
                    rgbFaceRectTransformer.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        PreviewConfig previewConfig = recognizeViewModel.getPreviewConfig();
        rgbCameraHelper = new DualCameraHelper.Builder()
                .previewViewSize(new Point(binding.dualCameraTexturePreviewRgb.getMeasuredWidth(), binding.dualCameraTexturePreviewRgb.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .additionalRotation(previewConfig.getRgbAdditionalDisplayOrientation())
                .previewSize(recognizeViewModel.loadPreviewSize())
                .specificCameraId(previewConfig.getRgbCameraId())
                .isMirror(true)  // 前置摄像头需要镜像显示，才符合用户直觉
                .previewOn(binding.dualCameraTexturePreviewRgb)
                .cameraListener(cameraListener)
                .build();
        rgbCameraHelper.init();
        rgbCameraHelper.start();
    }

    /**
     * 初始化红外相机，若活体检测类型是可见光活体检测或不启用活体，则不需要启用
     */
    private void initIrCamera() {
        if (livenessType == LivenessType.RGB || !enableLivenessDetect) {
            return;
        }
        CameraListener irCameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Camera.Size previewSizeIr = camera.getParameters().getPreviewSize();
                ViewGroup.LayoutParams layoutParams = adjustPreviewViewSize(binding.dualCameraTexturePreviewRgb,
                        binding.dualCameraTexturePreviewIr, binding.dualCameraFaceRectViewIr,
                        previewSizeIr, displayOrientation, 0.25f);

                irFaceRectTransformer = new FaceRectTransformer(previewSizeIr.width, previewSizeIr.height,
                        layoutParams.width, layoutParams.height, displayOrientation, cameraId, isMirror,
                        ConfigUtil.isDrawIrRectHorizontalMirror(RegisterAndRecognizeActivity.this),
                        ConfigUtil.isDrawIrRectVerticalMirror(RegisterAndRecognizeActivity.this));

                FrameLayout parentView = ((FrameLayout) binding.dualCameraTexturePreviewIr.getParent());
                if (textViewIr == null) {
                    textViewIr = new TextView(RegisterAndRecognizeActivity.this, null);
                } else {
                    parentView.removeView(textViewIr);
                }
                textViewIr.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                textViewIr.setText(getString(R.string.camera_ir_preview_size, previewSizeIr.width, previewSizeIr.height));
                textViewIr.setTextColor(Color.WHITE);
                textViewIr.setBackgroundColor(getResources().getColor(R.color.color_bg_notification));
                parentView.addView(textViewIr);

                recognizeViewModel.onIrCameraOpened(camera);
                recognizeViewModel.setIrFaceRectTransformer(irFaceRectTransformer);
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                recognizeViewModel.refreshIrPreviewData(nv21);
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (irFaceRectTransformer != null) {
                    irFaceRectTransformer.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        PreviewConfig previewConfig = recognizeViewModel.getPreviewConfig();
        irCameraHelper = new DualCameraHelper.Builder()
                .previewViewSize(new Point(binding.dualCameraTexturePreviewIr.getMeasuredWidth(), binding.dualCameraTexturePreviewIr.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(previewConfig.getIrCameraId())
                .previewOn(binding.dualCameraTexturePreviewIr)
                .cameraListener(irCameraListener)
                .isMirror(ConfigUtil.isDrawIrPreviewHorizontalMirror(this))
                .previewSize(recognizeViewModel.loadPreviewSize()) //相机预览大小设置，RGB与IR需使用相同大小
                .additionalRotation(previewConfig.getIrAdditionalDisplayOrientation()) //额外旋转角度
                .build();
        irCameraHelper.init();
        try {
            irCameraHelper.start();
        } catch (RuntimeException e) {
            showToast(e.getMessage() + getString(R.string.camera_error_notice));
        }
    }


    /**
     * 绘制RGB、IR画面的实时人脸信息
     *
     * @param facePreviewInfoList RGB画面的实时人脸信息
     */
    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        Log.w(TAG,"人脸信息在这里实时绘制66666666666666666");
        if (rgbFaceRectTransformer != null) {
            List<FaceRectView.DrawInfo> rgbDrawInfoList = recognizeViewModel.getDrawInfo(facePreviewInfoList, LivenessType.RGB, openRectInfoDraw);
            binding.dualCameraFaceRectView.drawRealtimeFaceInfo(rgbDrawInfoList);
        }
        if (irFaceRectTransformer != null) {
            List<FaceRectView.DrawInfo> irDrawInfoList = recognizeViewModel.getDrawInfo(facePreviewInfoList, LivenessType.IR, openRectInfoDraw);
            binding.dualCameraFaceRectViewIr.drawRealtimeFaceInfo(irDrawInfoList);
        }
    }

    @Override
    protected void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                recognizeViewModel.init();
                initRgbCamera();
                if (DualCameraHelper.hasDualCamera() && livenessType == LivenessType.IR) {
                    initIrCamera();
                }
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    public void openRectInfoDraw(View view) {
        openRectInfoDraw = !openRectInfoDraw;
        recognizeViewModel.setDrawRectInfoTextValue(openRectInfoDraw);
    }

    /**
     * 将准备注册的状态置为待注册
     *
     * @param view 注册按钮
     */
    public void register(View view) {
        recognizeViewModel.prepareRegister();
    }

    /**
     * 参数配置
     *
     * @param view
     */
    public void setting(View view) {
        this.actionAfterFinish = NAVIGATE_TO_RECOGNIZE_SETTINGS_ACTIVITY;
        showLongToast(getString(R.string.please_wait));
        finish();
    }

    /**
     * 识别分析界面
     *
     * @param view 注册按钮
     */
    public void recognizeDebug(View view) {
        this.actionAfterFinish = NAVIGATE_TO_RECOGNIZE_DEBUG_ACTIVITY;
        showLongToast(getString(R.string.please_wait));
        finish();
    }

    /**
     * 管理员登录按钮点击事件（左下角隐藏区域，连续点击 5 次触发）
     *
     * @param view 管理员登录按钮
     */
    public void onAdminClick(View view) {
        long currentTime = System.currentTimeMillis();
        // 如果在 2 秒内连续点击
        if (currentTime - lastClickTime < 2000) {
            adminClickCount++;
            if (adminClickCount >= ADMIN_CLICK_COUNT) {
                // 达到 5 次，显示管理员密码验证对话框
                adminClickCount = 0;
                showAdminPasswordDialog();
                return;
            }
        } else {
            // 重置点击计数
            adminClickCount = 1;
        }
        lastClickTime = currentTime;
    }

    /**
     * 切换打卡方式
     *
     * @param view 切换按钮
     */
    public void switchCheckInMode(View view) {
        // 切换打卡方式
        switch (currentCheckInMode) {
            case FACE_RECOGNITION:
                currentCheckInMode = CheckInMode.NFC;
                break;
            case NFC:
                currentCheckInMode = CheckInMode.QR_CODE;
                break;
            case QR_CODE:
                currentCheckInMode = CheckInMode.FACE_RECOGNITION;
                break;
        }
        updateCheckInModeButton();
        handleCheckInModeChange();
    }

    /**
     * 更新打卡方式按钮显示
     */
    private void updateCheckInModeButton() {
        if (btnSwitchCheckInMode == null) {
            btnSwitchCheckInMode = findViewById(R.id.btn_switch_check_in_mode);
        }
        if (btnSwitchCheckInMode != null) {
            String modeText = "";
            switch (currentCheckInMode) {
                case FACE_RECOGNITION:
                    modeText = getString(R.string.face_recognition);
                    break;
                case NFC:
                    modeText = getString(R.string.nfc_check_in);
                    break;
                case QR_CODE:
                    modeText = getString(R.string.qr_code_check_in);
                    break;
            }
            btnSwitchCheckInMode.setText(modeText);
        }
    }

    /**
     * 处理打卡方式切换
     */
    private void handleCheckInModeChange() {
        switch (currentCheckInMode) {
            case FACE_RECOGNITION:
                // 恢复相机预览
                resumeCamera();
                break;
            case NFC:
                // 检查 NFC 权限
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.NFC}, REQUEST_NFC_PERMISSION);
                } else {
                    showNfcCheckInHint();
                }
                break;
            case QR_CODE:
                // 切换到二维码模式，等待扫描枪输入
                showQrCodeCheckInHint();
                break;
        }
    }

    /**
     * 显示 NFC 打卡提示
     */
    private void showNfcCheckInHint() {
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            // 使用 USB NFC 读卡器模式（模拟键盘输出），不需要原生 NFC
            showToast(getString(R.string.waiting_nfc_card_input));
        } else {
            showToast(getString(R.string.waiting_nfc_card_input));
        }
    }

    /**
     * 显示二维码打卡提示
     */
    private void showQrCodeCheckInHint() {
        // 暂停相机以节省资源
        pauseCamera();
        showToast(getString(R.string.waiting_qr_code_input));
        // 请求焦点到隐藏的 EditText，准备接收扫描枪输入
        if (qrCodeInputEditText != null) {
            qrCodeInputEditText.requestFocus();
            qrCodeInputEditText.requestFocusFromTouch();
        }
    }

    /**
     * 显示管理员密码验证对话框
     */
    private void showAdminPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.admin_password_title));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(getString(R.string.admin_password_hint));
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
            String password = input.getText().toString();
            if (ADMIN_PASSWORD.equals(password)) {
                showToast(getString(R.string.admin_password_success));
                // 跳转到主界面（管理菜单）
                navigateToNewPage(HomeActivity.class);
            } else {
                showToast(getString(R.string.admin_password_wrong));
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.cancel();
        });

        builder.show();
    }

    /**
     * 在{@link ActivityRegisterAndRecognizeBinding#dualCameraTexturePreviewRgb}第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    @Override
    public void onGlobalLayout() {
        binding.dualCameraTexturePreviewRgb.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            recognizeViewModel.init();
            initRgbCamera();
            if (DualCameraHelper.hasDualCamera() && livenessType == LivenessType.IR) {
                initIrCamera();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeCamera();
    }

    private void resumeCamera() {
        if (rgbCameraHelper != null) {
            rgbCameraHelper.start();
        }
        if (irCameraHelper != null) {
            irCameraHelper.start();
        }
    }

    @Override
    protected void onPause() {
        pauseCamera();
        super.onPause();
    }

    private void pauseCamera() {
        if (rgbCameraHelper != null) {
            rgbCameraHelper.stop();
        }
        if (irCameraHelper != null) {
            irCameraHelper.stop();
        }
    }

    /**
     * 初始化 NFC
     */
    private void initNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Log.w(TAG, "NFC 不可用");
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            Log.w(TAG, "NFC 未启用");
        }
    }

    /**
     * 初始化二维码输入（隐藏的 EditText 用于接收虚拟键盘输入）
     */
    private void initQrCodeInput() {
        // 创建一个隐藏的 EditText 来接收扫描枪输入
        qrCodeInputEditText = new EditText(this);
        qrCodeInputEditText.setVisibility(View.GONE);
        qrCodeInputEditText.setInputType(EditorInfo.TYPE_NULL);  // 接收任何输入

        // 监听输入完成
        qrCodeInputEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String qrCodeContent = qrCodeInputEditText.getText().toString();
                qrCodeInputEditText.setText("");  // 清空输入
                if (currentCheckInMode == CheckInMode.QR_CODE && qrCodeContent != null && !qrCodeContent.isEmpty()) {
                    handleQrCodeCheckIn(qrCodeContent);
                }
                return true;
            }
            return false;
        });

        // 将 EditText 添加到 FrameLayout 根布局
        binding.dualCameraLlParent.addView(qrCodeInputEditText);
    }

    /**
     * 处理二维码打卡（使用虚拟键盘输入）
     */
    private void handleQrCodeCheckIn(String qrCodeContent) {
        if (qrCodeContent != null && !qrCodeContent.isEmpty()) {
            Log.i(TAG, "QR Code content: " + qrCodeContent);
            showToast(getString(R.string.qr_code_check_in_success) + ": " + qrCodeContent);
        } else {
            showToast(getString(R.string.qr_code_invalid));
        }
        // 打卡完成后切换回人脸识别
        currentCheckInMode = CheckInMode.FACE_RECOGNITION;
        updateCheckInModeButton();
        resumeCamera();
    }

    /**
     * 处理 NFC 打卡（通过键盘模拟输出）
     */
    private void handleNfcCheckInByContent(String content) {
        if (content != null && !content.isEmpty()) {
            Log.i(TAG, "NFC Check-in content: " + content);
            showToast(getString(R.string.nfc_check_in_success) + ": " + content);
            // 这里可以添加实际的打卡逻辑
            // 例如：recognizeViewModel.recordNfcCheckIn(content);
        } else {
            showToast(getString(R.string.nfc_check_in_failed));
        }
        // 打卡完成后切换回人脸识别
        currentCheckInMode = CheckInMode.FACE_RECOGNITION;
        updateCheckInModeButton();
        resumeCamera();
    }

    /**
     * 处理 NFC 打卡（原生 NFC 标签）
     */
    private void handleNfcCheckIn(Tag tag) {
        if (tag != null) {
            String tagId = bytesToHex(tag.getId());
            Log.i(TAG, "NFC Tag ID: " + tagId);
            showToast(getString(R.string.nfc_check_in_success) + ": " + tagId);
        } else {
            showToast(getString(R.string.nfc_check_in_failed));
        }
        // 打卡完成后切换回人脸识别
        currentCheckInMode = CheckInMode.FACE_RECOGNITION;
        updateCheckInModeButton();
        resumeCamera();
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    /**
     * 将键值转成对应的 ASCII 码（用于 NFC 读卡器模拟键盘输出）
     */
    public char getCharByKeyCode(int keyCode) {
        char outChar = 0;

        if (keyCode > 6 && keyCode < 17) // 数字
            outChar = (char) ((keyCode - 7) + 0x30);

        if (keyCode > 28 && keyCode < 55) // 字母
            outChar = (char) ((keyCode - 29) + 0x41);

        return outChar;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 处理 NFC 标签（原生 NFC）
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            handleNfcCheckIn(tag);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // 处理 NFC 读卡器模拟键盘输出（USB NFC 读卡器）
        if (currentCheckInMode == CheckInMode.NFC) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (nfcKeyContent.length() > 0) {
                    // 显示读取到的内容
                    Log.i(TAG, "NFC content: " + nfcKeyContent);
                    showToast(nfcKeyContent);
                    handleNfcCheckInByContent(nfcKeyContent);
                    nfcKeyContent = "";
                }
            } else {
                int keyCode = event.getKeyCode();
                char keyChar = getCharByKeyCode(keyCode);

                if (keyChar != 0) { // 确保是有效的字符
                    if (nfcGetFlag) {
                        nfcKeyContent += keyChar;
                        nfcGetFlag = false;
                    } else {
                        nfcGetFlag = true;
                    }
                }
            }
            return true;  // 消费掉这个事件
        }

        // 处理扫描枪的硬键盘事件（二维码模式）
        if (currentCheckInMode == CheckInMode.QR_CODE && qrCodeInputEditText != null) {
            // 将事件转发给隐藏的 EditText
            return qrCodeInputEditText.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NFC_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showNfcCheckInHint();
            } else {
                showToast(getString(R.string.permission_denied));
                currentCheckInMode = CheckInMode.FACE_RECOGNITION;
                updateCheckInModeButton();
            }
        }
    }
}
