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
import com.arcsoft.arcfacedemo.attendance.model.CheckResult;
import com.arcsoft.arcfacedemo.attendance.service.AttendanceService;
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

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class RegisterAndRecognizeActivity extends BaseActivity implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "RegisterAndRecognize";

    // Check-in mode enumeration
    private enum CheckInMode {
        FACE_RECOGNITION,  // Face Recognition
        NFC,              // NFC Check-in
        QR_CODE           // QR Code Check-in
    }

    private CheckInMode currentCheckInMode = CheckInMode.FACE_RECOGNITION;
    private static final String ADMIN_PASSWORD = "123456";  // Admin password
    private static final int ADMIN_CLICK_COUNT = 5;  // Admin verification click count
    private int adminClickCount = 0;  // Current click count
    private long lastClickTime = 0;   // Last click time

    private Button btnSwitchCheckInMode;

    // NFC related
    private NfcAdapter nfcAdapter;
    private static final int REQUEST_NFC_PERMISSION = 0x002;

    // NFC keyboard simulation input (USB NFC reader simulates keyboard output)
    private String nfcKeyContent = "";  // NFC reader output content
    private boolean nfcGetFlag = true;  // Read flag

    // QR code input related (virtual keyboard)
    private EditText qrCodeInputEditText;
    private AttendanceService attendanceService;

    private DualCameraHelper rgbCameraHelper;
    private DualCameraHelper irCameraHelper;
    private FaceRectTransformer rgbFaceRectTransformer;
    private FaceRectTransformer irFaceRectTransformer;

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;

    int actionAfterFinish = 0;
    private static final int NAVIGATE_TO_RECOGNIZE_SETTINGS_ACTIVITY = 1;
    private static final int NAVIGATE_TO_RECOGNIZE_DEBUG_ACTIVITY = 2;

    /**
     * All required permissions
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
        attendanceService = AttendanceService.getInstance(this);
        Log.i(TAG,"杩涘叆鍒颁簡浜鸿劯璇嗗埆涓?----");
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().setAttributes(attributes);
        }

        // Activity鍚姩鍚庡氨閿佸畾涓哄惎鍔ㄦ椂鐨勬柟鍚?
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        initData();// Initialize data
        initViewModel();

        // Initialize audio player
        Log.i(TAG, "Initializing AudioPlayer...");
        AudioPlayer.getInstance(this);

        initView();
        initNfc();
        updateCheckInModeButton();
        initQrCodeInput();
        openRectInfoDraw = true;
        recognizeViewModel.setDrawRectInfoTextValue(true);

        // Start tech scan line animation
        startScanLineAnimation();
    }

    /**
     * Start scan line animation
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

        // 鍒濆鍖栨墦鍗′粨搴?
        recognizeViewModel.initAttendanceRepository(this);

        // 瑙傚療鎵撳崱缁撴灉
        recognizeViewModel.getPunchResult().observe(this, punchResult -> {
            showToast(punchResult);
            Log.i(TAG, "Punch result: " + punchResult);
        });
    }

    private void initView() {
        if (!DualCameraHelper.hasDualCamera() || livenessType != LivenessType.IR) {
            binding.flRecognizeIr.setVisibility(View.GONE);
        }
        //鍦ㄥ竷灞€缁撴潫鍚庢墠鍋氬垵濮嬪寲鎿嶄綔
        binding.dualCameraTexturePreviewRgb.getViewTreeObserver().addOnGlobalLayoutListener(this);
        binding.setCompareResultList(recognizeViewModel.getCompareResultList().getValue());
    }

    @Override
    protected void onDestroy() {
        // 閲婃斁闊抽鎾斁鍣ㄨ祫婧?
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
     * 璋冩暣View鐨勫楂橈紝浣?涓瑙堝悓鏃舵樉绀?
     *
     * @param previewView        鏄剧ず棰勮鏁版嵁鐨剉iew
     * @param faceRectView       鐢绘鐨剉iew
     * @param previewSize        棰勮澶у皬
     * @param displayOrientation 鐩告満鏃嬭浆瑙掑害
     * @return 璋冩暣鍚庣殑LayoutParams
     */
    /**
     * 璋冩暣View鐨勫楂橈紝浣?涓瑙堝悓鏃舵樉绀猴紙闄愬埗涓嶈秴杩囧睆骞曞昂瀵革級
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
     * 璋冩暣View鐨勫楂橈紝涓嶉檺鍒跺睆骞曞昂瀵革紝鐢ㄤ簬鏀惧ぇ鐢婚潰
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

        // 搴旂敤缂╂斁姣斾緥锛堜笉闄愬埗灞忓箷灏哄锛?
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

                    // Get screen dimensions
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                    // 璁＄畻鐩爣灏哄锛氬搴﹀厖婊″睆骞曪紝楂樺害涓哄睆骞曠殑2/3
                    int targetWidth = displayMetrics.widthPixels;
                    //int targetHeight = (int) (displayMetrics.heightPixels * 2.0 / 3.0);
                    int targetHeight = displayMetrics.heightPixels ;

                    FrameLayout parentView = ((FrameLayout) binding.dualCameraTexturePreviewRgb.getParent());

                    // 璁剧疆鐖禫iew鐨勫昂瀵稿拰閲嶅姏锛屼娇鍐呭灞呬腑
                    ViewGroup.LayoutParams parentLayoutParams = parentView.getLayoutParams();
                    parentLayoutParams.width = targetWidth;
                    parentLayoutParams.height = targetHeight;
                    parentView.setLayoutParams(parentLayoutParams);

                    // 璁＄畻淇濇寔瀹介珮姣旂殑灏哄锛堢被浼糃ENTER_CROP锛?
                    float previewRatio = (float) previewSizeRgb.width / (float) previewSizeRgb.height;
                    float targetRatio = (float) targetWidth / (float) targetHeight;

                    int finalWidth, finalHeight;
                    if (previewRatio > targetRatio) {
                        // Camera preview is wider, base on height
                        finalHeight = targetHeight;
                        finalWidth = (int) (finalHeight * previewRatio);
                    } else {
                        // Camera preview is taller, base on width
                        finalWidth = targetWidth;
                        finalHeight = (int) (finalWidth / previewRatio);
                    }

                    // 璁剧疆TextureView鍜孎aceRectView涓虹浉鍚屽昂瀵?
                    ViewGroup.LayoutParams textureParams = binding.dualCameraTexturePreviewRgb.getLayoutParams();
                    textureParams.width = finalWidth;
                    textureParams.height = finalHeight;
                    binding.dualCameraTexturePreviewRgb.setLayoutParams(textureParams);

                    // 璁剧疆LayoutGravity浣縏extureView灞呬腑
                    FrameLayout.LayoutParams textureLayoutParams = (FrameLayout.LayoutParams) binding.dualCameraTexturePreviewRgb.getLayoutParams();
                    textureLayoutParams.gravity = android.view.Gravity.CENTER;
                    binding.dualCameraTexturePreviewRgb.setLayoutParams(textureLayoutParams);

                    // FaceRectView浣跨敤鐩稿悓灏哄鍜屼綅缃?
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

                    // 鍒涘缓FaceRectTransformer锛屼娇鐢═extureView鐨勫疄闄呮樉绀哄昂瀵?
                    rgbFaceRectTransformer = new FaceRectTransformer(previewSizeRgb.width, previewSizeRgb.height,
                            finalWidth, finalHeight, displayOrientation, cameraId, isMirror,
                            ConfigUtil.isDrawRgbRectHorizontalMirror(RegisterAndRecognizeActivity.this),
                            ConfigUtil.isDrawRgbRectVerticalMirror(RegisterAndRecognizeActivity.this));

                    // 娣诲姞recognizeAreaView锛屽湪璇嗗埆鍖哄煙鍙戠敓鍙樻洿鏃讹紝鏇存柊鏁版嵁缁橣aceHelper
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
                .isMirror(true)  // Front camera needs mirror display for user intuition
                .previewOn(binding.dualCameraTexturePreviewRgb)
                .cameraListener(cameraListener)
                .build();
        rgbCameraHelper.init();
        rgbCameraHelper.start();
    }

    /**
     * 鍒濆鍖栫孩澶栫浉鏈猴紝鑻ユ椿浣撴娴嬬被鍨嬫槸鍙鍏夋椿浣撴娴嬫垨涓嶅惎鐢ㄦ椿浣擄紝鍒欎笉闇€瑕佸惎鐢?
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
                .previewSize(recognizeViewModel.loadPreviewSize()) //鐩告満棰勮澶у皬璁剧疆锛孯GB涓嶪R闇€浣跨敤鐩稿悓澶у皬
                .additionalRotation(previewConfig.getIrAdditionalDisplayOrientation()) //棰濆鏃嬭浆瑙掑害
                .build();
        irCameraHelper.init();
        try {
            irCameraHelper.start();
        } catch (RuntimeException e) {
            showToast(e.getMessage() + getString(R.string.camera_error_notice));
        }
    }


    /**
     * 缁樺埗RGB銆両R鐢婚潰鐨勫疄鏃朵汉鑴镐俊鎭?
     *
     * @param facePreviewInfoList RGB鐢婚潰鐨勫疄鏃朵汉鑴镐俊鎭?
     */
    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        Log.w(TAG,"浜鸿劯淇℃伅鍦ㄨ繖閲屽疄鏃剁粯鍒?6666666666666666");
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
     * 灏嗗噯澶囨敞鍐岀殑鐘舵€佺疆涓哄緟娉ㄥ唽
     *
     * @param view 娉ㄥ唽鎸夐挳
     */
    public void register(View view) {
        recognizeViewModel.prepareRegister();
    }

    /**
     * 鍙傛暟閰嶇疆
     *
     * @param view
     */
    public void setting(View view) {
        this.actionAfterFinish = NAVIGATE_TO_RECOGNIZE_SETTINGS_ACTIVITY;
        showLongToast(getString(R.string.please_wait));
        finish();
    }

    /**
     * 璇嗗埆鍒嗘瀽鐣岄潰
     *
     * @param view 娉ㄥ唽鎸夐挳
     */
    public void recognizeDebug(View view) {
        this.actionAfterFinish = NAVIGATE_TO_RECOGNIZE_DEBUG_ACTIVITY;
        showLongToast(getString(R.string.please_wait));
        finish();
    }

    /**
     * 绠＄悊鍛樼櫥褰曟寜閽偣鍑讳簨浠讹紙宸︿笅瑙掗殣钘忓尯鍩燂紝杩炵画鐐瑰嚮 5 娆¤Е鍙戯級
     *
     * @param view 绠＄悊鍛樼櫥褰曟寜閽?
     */
    public void onAdminClick(View view) {
        long currentTime = System.currentTimeMillis();
        // If clicked continuously within 2 seconds
        if (currentTime - lastClickTime < 2000) {
            adminClickCount++;
            if (adminClickCount >= ADMIN_CLICK_COUNT) {
                // After 5 clicks, show admin password dialog
                adminClickCount = 0;
                showAdminPasswordDialog();
                return;
            }
        } else {
            // Reset click count
            adminClickCount = 1;
        }
        lastClickTime = currentTime;
    }

    /**
     * 鍒囨崲鎵撳崱鏂瑰紡
     *
     * @param view 鍒囨崲鎸夐挳
     */
    public void switchCheckInMode(View view) {
        // Switch check-in mode
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
     * 鏇存柊鎵撳崱鏂瑰紡鎸夐挳鏄剧ず
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
     * 澶勭悊鎵撳崱鏂瑰紡鍒囨崲
     */
    private void handleCheckInModeChange() {
        switch (currentCheckInMode) {
            case FACE_RECOGNITION:
                // Resume camera preview
                resumeCamera();
                break;
            case NFC:
                // Check NFC permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.NFC}, REQUEST_NFC_PERMISSION);
                } else {
                    showNfcCheckInHint();
                }
                break;
            case QR_CODE:
                // Switch to QR code mode, wait for scanner input
                showQrCodeCheckInHint();
                break;
        }
    }

    /**
     * 鏄剧ず NFC 鎵撳崱鎻愮ず
     */
    private void showNfcCheckInHint() {
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            // 浣跨敤 USB NFC 璇诲崱鍣ㄦā寮忥紙妯℃嫙閿洏杈撳嚭锛夛紝涓嶉渶瑕佸師鐢?NFC
            showToast(getString(R.string.waiting_nfc_card_input));
        } else {
            showToast(getString(R.string.waiting_nfc_card_input));
        }
    }

    /**
     * 鏄剧ず浜岀淮鐮佹墦鍗℃彁绀?
     */
    private void showQrCodeCheckInHint() {
        // Pause camera to save resources
        pauseCamera();
        showToast(getString(R.string.waiting_qr_code_input));
        // Request focus to hidden EditText, ready to receive scanner input
        if (qrCodeInputEditText != null) {
            qrCodeInputEditText.requestFocus();
            qrCodeInputEditText.requestFocusFromTouch();
        }
    }

    /**
     * 鏄剧ず绠＄悊鍛樺瘑鐮侀獙璇佸璇濇
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
                // Navigate to main interface (management menu)
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
     * 鍦▄@link ActivityRegisterAndRecognizeBinding#dualCameraTexturePreviewRgb}绗竴娆″竷灞€瀹屾垚鍚庯紝鍘婚櫎璇ョ洃鍚紝骞朵笖杩涜寮曟搸鍜岀浉鏈虹殑鍒濆鍖?
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
     * 鍒濆鍖?NFC
     */
    private void initNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Log.w(TAG, "NFC disabled");
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            Log.w(TAG, "NFC disabled");
        }
    }

    /**
     * 鍒濆鍖栦簩缁寸爜杈撳叆锛堥殣钘忕殑 EditText 鐢ㄤ簬鎺ユ敹铏氭嫙閿洏杈撳叆锛?
     */
    private void initQrCodeInput() {
        // Create a hidden EditText to receive scanner input
        qrCodeInputEditText = new EditText(this);
        qrCodeInputEditText.setVisibility(View.GONE);
        qrCodeInputEditText.setInputType(EditorInfo.TYPE_NULL);  // 鎺ユ敹浠讳綍杈撳叆

        // Listen for input completion
        qrCodeInputEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String qrCodeContent = qrCodeInputEditText.getText().toString();
                qrCodeInputEditText.setText("");  // Clear input
                if (currentCheckInMode == CheckInMode.QR_CODE && qrCodeContent != null && !qrCodeContent.isEmpty()) {
                    handleQrCodeCheckIn(qrCodeContent);
                }
                return true;
            }
            return false;
        });

        // Add EditText to FrameLayout root layout
        binding.dualCameraLlParent.addView(qrCodeInputEditText);
    }

    /**
     * 澶勭悊浜岀淮鐮佹墦鍗★紙浣跨敤铏氭嫙閿洏杈撳叆锛?
     */
    private void handleQrCodeCheckIn(String qrCodeContent) {
        if (qrCodeContent != null && !qrCodeContent.trim().isEmpty()) {
            Log.i(TAG, "QR Code content: " + qrCodeContent);
            submitTextCheckIn(qrCodeContent.trim(), "QR_CODE");
        } else {
            showToast(getString(R.string.qr_code_invalid));
            restoreFaceRecognitionMode();
        }
    }

    /**
     * 澶勭悊 NFC 鎵撳崱锛堥€氳繃閿洏妯℃嫙杈撳嚭锛?
     */
    private void handleNfcCheckInByContent(String content) {
        if (content != null && !content.trim().isEmpty()) {
            Log.i(TAG, "NFC Check-in content: " + content);
            submitTextCheckIn(content.trim(), "NFC");
        } else {
            showToast(getString(R.string.nfc_check_in_failed));
            restoreFaceRecognitionMode();
        }
    }

    /**
     * 澶勭悊 NFC 鎵撳崱锛堝師鐢?NFC 鏍囩锛?
     */
    private void handleNfcCheckIn(Tag tag) {
        if (tag != null) {
            String tagId = bytesToHex(tag.getId());
            Log.i(TAG, "NFC Tag ID: " + tagId);
            submitTextCheckIn(tagId, "NFC");
        } else {
            showToast(getString(R.string.nfc_check_in_failed));
            restoreFaceRecognitionMode();
        }
    }

    private void submitTextCheckIn(String employeeNo, String source) {
        if (attendanceService == null) {
            attendanceService = AttendanceService.getInstance(this);
        }

        attendanceService.checkByEmployeeNo(employeeNo, null, source)
                .subscribe(new SingleObserver<CheckResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onSuccess(CheckResult result) {
                        runOnUiThread(() -> {
                            if (result != null && result.isSuccess()) {
                                AudioPlayer.getInstance(RegisterAndRecognizeActivity.this).playPunchSuccess();
                                showToast(result.getMessage());
                            } else if (result != null) {
                                AudioPlayer.getInstance(RegisterAndRecognizeActivity.this).playAlreadyPunched();
                                showToast(result.getError());
                            } else {
                                showToast(getString(R.string.qr_code_invalid));
                            }
                            restoreFaceRecognitionMode();
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Text check-in failed", e);
                        runOnUiThread(() -> {
                            AudioPlayer.getInstance(RegisterAndRecognizeActivity.this).playAlreadyPunched();
                            showToast("打卡失败: " + e.getMessage());
                            restoreFaceRecognitionMode();
                        });
                    }
                });
    }

    private void restoreFaceRecognitionMode() {
        currentCheckInMode = CheckInMode.FACE_RECOGNITION;
        updateCheckInModeButton();
        resumeCamera();
    }

    /**
     * 瀛楄妭鏁扮粍杞崄鍏繘鍒跺瓧绗︿覆
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    /**
     * 灏嗛敭鍊艰浆鎴愬搴旂殑 ASCII 鐮侊紙鐢ㄤ簬 NFC 璇诲崱鍣ㄦā鎷熼敭鐩樿緭鍑猴級
     */
    public char getCharByKeyCode(int keyCode) {
        char outChar = 0;

        if (keyCode > 6 && keyCode < 17) // 鏁板瓧
            outChar = (char) ((keyCode - 7) + 0x30);

        if (keyCode > 28 && keyCode < 55) // 瀛楁瘝
            outChar = (char) ((keyCode - 29) + 0x41);

        return outChar;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle NFC tag (native NFC)
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            handleNfcCheckIn(tag);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Handle NFC reader keyboard simulation (USB NFC reader)
        if (currentCheckInMode == CheckInMode.NFC) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (nfcKeyContent.length() > 0) {
                    // Display read content
                    Log.i(TAG, "NFC content: " + nfcKeyContent);
                    showToast(nfcKeyContent);
                    handleNfcCheckInByContent(nfcKeyContent);
                    nfcKeyContent = "";
                }
            } else {
                int keyCode = event.getKeyCode();
                char keyChar = getCharByKeyCode(keyCode);

                if (keyChar != 0) { // Ensure valid character
                    if (nfcGetFlag) {
                        nfcKeyContent += keyChar;
                        nfcGetFlag = false;
                    } else {
                        nfcGetFlag = true;
                    }
                }
            }
            return true;  // Consume the event
        }

        // Handle scanner hardware keyboard events (QR code mode)
        if (currentCheckInMode == CheckInMode.QR_CODE && qrCodeInputEditText != null) {
            // Forward event to hidden EditText
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

