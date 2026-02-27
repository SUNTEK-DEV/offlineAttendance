package com.arcsoft.arcfacedemo.ui.viewmodel;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.arcsoft.arcfacedemo.ArcFaceApplication;
import com.arcsoft.arcfacedemo.R;
import com.arcsoft.arcfacedemo.api.ApiConfig;
import com.arcsoft.arcfacedemo.api.model.PunchRequest;
import com.arcsoft.arcfacedemo.api.model.PunchResponse;
import com.arcsoft.arcfacedemo.api.repo.AttendanceRepository;
import com.arcsoft.arcfacedemo.facedb.FaceDatabase;
import com.arcsoft.arcfacedemo.facedb.dao.FaceDao;
import com.arcsoft.arcfacedemo.facedb.entity.FaceEntity;
import com.arcsoft.arcfacedemo.ui.model.CompareResult;
import com.arcsoft.arcfacedemo.faceserver.FaceServer;
import com.arcsoft.arcfacedemo.ui.callback.OnRegisterFinishedCallback;
import com.arcsoft.arcfacedemo.ui.model.PreviewConfig;
import com.arcsoft.arcfacedemo.util.ConfigUtil;
import com.arcsoft.arcfacedemo.util.FaceRectTransformer;
import com.arcsoft.arcfacedemo.util.AudioPlayer;
import com.arcsoft.arcfacedemo.util.face.FaceHelper;
import com.arcsoft.arcfacedemo.util.face.model.FacePreviewInfo;
import com.arcsoft.arcfacedemo.util.face.constants.LivenessType;
import com.arcsoft.arcfacedemo.util.face.RecognizeCallback;
import com.arcsoft.arcfacedemo.util.face.constants.RecognizeColor;
import com.arcsoft.arcfacedemo.util.face.model.RecognizeConfiguration;
import com.arcsoft.arcfacedemo.util.face.constants.RequestFeatureStatus;
import com.arcsoft.arcfacedemo.widget.FaceRectView;
import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.ImageQualitySimilar;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.LivenessParam;
import com.arcsoft.face.MaskInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class RecognizeViewModel extends ViewModel implements RecognizeCallback {
    /**
     * 人脸识别过程中数据的更新类型
     */
    public enum EventType {
        /**
         * 人脸插入
         */
        INSERTED,
        /**
         * 人脸移除
         */
        REMOVED
    }

    public static class FaceItemEvent {
        private int index;
        private EventType eventType;

        public FaceItemEvent(int index, EventType eventType) {
            this.index = index;
            this.eventType = eventType;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public EventType getEventType() {
            return eventType;
        }

        public void setEventType(EventType eventType) {
            this.eventType = eventType;
        }
    }

    private static final String TAG = "RecognizeViewModel";


    private OnRegisterFinishedCallback onRegisterFinishedCallback;

    /**
     * 注册人脸状态码，准备注册
     */
    public static final int REGISTER_STATUS_READY = 0;
    /**
     * 注册人脸状态码，注册中
     */
    public static final int REGISTER_STATUS_PROCESSING = 1;
    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    public static final int REGISTER_STATUS_DONE = 2;

    /**
     * 人脸识别的状态，预设值为：已结束
     */
    private int registerStatus = REGISTER_STATUS_DONE;

    private static final int MAX_DETECT_NUM = 10;
    /**
     * 相机预览的分辨率
     */
    private Camera.Size previewSize;
    /**
     * 用于头像RecyclerView显示的信息
     */
    private MutableLiveData<List<CompareResult>> compareResultList;

    private MutableLiveData<FaceItemEvent> faceItemEventMutableLiveData = new MutableLiveData<>();

    /**
     * 各个引擎初始化的错误码
     */
    private MutableLiveData<Integer> ftInitCode = new MutableLiveData<>();
    private MutableLiveData<Integer> frInitCode = new MutableLiveData<>();
    private MutableLiveData<Integer> flInitCode = new MutableLiveData<>();

    /**
     * 人脸操作辅助类，推帧即可，内部会进行特征提取、识别
     */
    private FaceHelper faceHelper;
    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪及图像质量检测
     */
    private FaceEngine ftEngine;
    /**
     * VIDEO模式人脸口罩检测引擎，用于预览帧人脸口罩检测
     */
    private FaceEngine maskEngine;
    /**
     * 用于特征提取的引擎
     */
    private FaceEngine frEngine;
    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    private FaceEngine flEngine;

    private PreviewConfig previewConfig;

    private MutableLiveData<RecognizeConfiguration> recognizeConfiguration = new MutableLiveData<>();

    private MutableLiveData<String> recognizeNotice = new MutableLiveData<>();

    private MutableLiveData<String> drawRectInfoText = new MutableLiveData<>();

    /**
     * 当前活体检测的检测类型
     */
    private LivenessType livenessType;

    /**
     * IR活体数据
     */
    private byte[] irNV21 = null;

    /**
     * 人脸库数据加载完成
     */
    private boolean loadFaceList;

    private Disposable registerNv21Disposable;

    /**
     * 打卡 API 仓库
     */
    private AttendanceRepository attendanceRepository;

    /**
     * 人脸数据库 DAO
     */
    private FaceDao faceDao;

    /**
     * 上次打卡时间（防止重复打卡）
     */
    private long lastPunchTime = 0;
    private static final long PUNCH_COOLDOWN_MS = 3000; // 3秒内只打一次卡

    /**
     * 今天每个员工上班打卡成功记录（早上第一次）
     * key: employeeId, value: 今天的日期字符串(yyyyMMdd)
     */
    private Map<String, String> todayMorningPunchMap = new HashMap<>();

    /**
     * 今天每个员工下班打卡成功记录（下午最后一次）
     * key: employeeId, value: 今天的日期字符串(yyyyMMdd)
     */
    private Map<String, String> todayEveningPunchMap = new HashMap<>();

    /**
     * 打卡结果通知
     */
    private MutableLiveData<String> punchResult = new MutableLiveData<>();

    public void refreshIrPreviewData(byte[] irPreviewData) {
        irNV21 = irPreviewData;
    }

    /**
     * 设置当前活体检测的检测类型
     *
     * @param liveType 活体检测的检测类型
     */
    public void setLiveType(LivenessType liveType) {
        this.livenessType = liveType;
    }

    public void setRgbFaceRectTransformer(FaceRectTransformer rgbFaceRectTransformer) {
        faceHelper.setRgbFaceRectTransformer(rgbFaceRectTransformer);
    }

    public void setIrFaceRectTransformer(FaceRectTransformer irFaceRectTransformer) {
        faceHelper.setIrFaceRectTransformer(irFaceRectTransformer);
    }


    /**
     * 注册实时NV21数据
     *
     * @param nv21            实时相机预览的NV21数据
     * @param facePreviewInfo 人脸信息
     */
    private void registerFace(final byte[] nv21, FacePreviewInfo facePreviewInfo) {
        updateRegisterStatus(REGISTER_STATUS_PROCESSING);
        registerNv21Disposable = Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            FaceEngine registerEngine = new FaceEngine();
            int res = registerEngine.init(ArcFaceApplication.getApplication(), DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                    1, FaceEngine.ASF_FACE_RECOGNITION|FaceEngine.ASF_IMAGEQUALITY);
            if (res == ErrorInfo.MOK) {
                // 判断no mask fq质量  过低过高则注册失败
                ImageQualitySimilar imageQualitySimilar = new ImageQualitySimilar();
                int qualityCode = registerEngine.imageQualityDetect(nv21.clone(), previewSize.width,
                        previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfo.getFaceInfoRgb(),
                        facePreviewInfo.getMask(), imageQualitySimilar);
                if (qualityCode != ErrorInfo.MOK) {
                    Log.e(TAG,"imageQualityDetect failed! code is " + qualityCode);
                    registerEngine.unInit();
                    emitter.onNext(false);
                }
                float quality = imageQualitySimilar.getScore();
                float destQuality = ConfigUtil.getImageQualityNoMaskRegisterThreshold(ArcFaceApplication.getApplication());
                if(quality < destQuality){
                    Log.e(TAG,"image quality invalid");
                    registerEngine.unInit();
                    emitter.onNext(false);
                }
                boolean success = FaceServer.getInstance().registerNv21(ArcFaceApplication.getApplication(), nv21.clone(), previewSize.width,
                        previewSize.height, facePreviewInfo, "registered_" + faceHelper.getTrackedFaceCount(), frEngine, registerEngine);
                registerEngine.unInit();
                emitter.onNext(success);
            } else {
                emitter.onNext(false);
            }
            emitter.onComplete();
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Boolean>() {

                    @Override
                    public void onNext(Boolean success) {
                        if (onRegisterFinishedCallback != null) {
                            onRegisterFinishedCallback.onRegisterFinished(facePreviewInfo, success);
                        }
                        updateRegisterStatus(REGISTER_STATUS_DONE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        if (onRegisterFinishedCallback != null) {
                            onRegisterFinishedCallback.onRegisterFinished(facePreviewInfo, false);
                        }
                        updateRegisterStatus(REGISTER_STATUS_DONE);
                    }

                    @Override
                    public void onComplete() {
                    }
                });

    }

    public MutableLiveData<List<CompareResult>> getCompareResultList() {
        if (compareResultList == null) {
            compareResultList = new MutableLiveData<>();
            compareResultList.setValue(new ArrayList<>());
        }
        return compareResultList;
    }

    /**
     * 初始化引擎
     */
    public void init() {
        Context context = ArcFaceApplication.getApplication();

        // 初始化faceDao
        faceDao = FaceDatabase.getInstance(context).faceDao();

        // 针对当前设备：前置摄像头是彩色RGB，后置摄像头是红外
        // 如果设备不同，可能需要调整这里的配置
        previewConfig = new PreviewConfig(
                Camera.CameraInfo.CAMERA_FACING_FRONT, // RGB使用前置摄像头（彩色）
                Camera.CameraInfo.CAMERA_FACING_BACK,   // IR使用后置摄像头（红外）
                Integer.parseInt(ConfigUtil.getRgbCameraAdditionalRotation(context)),
                Integer.parseInt(ConfigUtil.getIrCameraAdditionalRotation(context))
        );

        // 填入在设置界面设置好的配置信息
        boolean enableLive = !ConfigUtil.getLivenessDetectType(context).equals(context.getString(R.string.value_liveness_type_disable));
        boolean enableFaceQualityDetect = ConfigUtil.isEnableImageQualityDetect(context);
        boolean enableFaceMoveLimit = ConfigUtil.isEnableFaceMoveLimit(context);
        boolean enableFaceSizeLimit = ConfigUtil.isEnableFaceSizeLimit(context);
        RecognizeConfiguration configuration = new RecognizeConfiguration.Builder()
                .enableFaceMoveLimit(enableFaceMoveLimit)
                .enableFaceSizeLimit(enableFaceSizeLimit)
                .faceSizeLimit(ConfigUtil.getFaceSizeLimit(context))
                .faceMoveLimit(ConfigUtil.getFaceMoveLimit(context))
                .enableLiveness(enableLive)
                .enableImageQuality(enableFaceQualityDetect)
                .maxDetectFaces(ConfigUtil.getRecognizeMaxDetectFaceNum(context))
                .keepMaxFace(ConfigUtil.isKeepMaxFace(context))
                .similarThreshold(ConfigUtil.getRecognizeThreshold(context))
                .imageQualityNoMaskRecognizeThreshold(ConfigUtil.getImageQualityNoMaskRecognizeThreshold(context))
                .imageQualityMaskRecognizeThreshold(ConfigUtil.getImageQualityMaskRecognizeThreshold(context))
                .livenessParam(new LivenessParam(ConfigUtil.getRgbLivenessThreshold(context), ConfigUtil.getIrLivenessThreshold(context)))
                .build();

        ftEngine = new FaceEngine();
        ftInitCode.postValue(ftEngine.init(context, DetectMode.ASF_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(context),
                ConfigUtil.getRecognizeMaxDetectFaceNum(context), FaceEngine.ASF_FACE_DETECT));
        maskEngine = new FaceEngine();
        ftInitCode.postValue(maskEngine.init(context, DetectMode.ASF_DETECT_MODE_IMAGE, ConfigUtil.getFtOrient(context),
                ConfigUtil.getRecognizeMaxDetectFaceNum(context), FaceEngine.ASF_MASK_DETECT));

        frEngine = new FaceEngine();
        int frEngineMask = FaceEngine.ASF_FACE_RECOGNITION;
        if (enableFaceQualityDetect) {
            frEngineMask |= FaceEngine.ASF_IMAGEQUALITY;
        }
        frInitCode.postValue(frEngine.init(context, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                10, frEngineMask));
        FaceServer.getInstance().initFaceList(context, frEngine, faceCount -> loadFaceList = true, true);

        //启用活体检测时，才初始化活体引擎
        if (enableLive) {
            flEngine = new FaceEngine();
            int flEngineMask = (livenessType == LivenessType.RGB ? FaceEngine.ASF_LIVENESS : (FaceEngine.ASF_IR_LIVENESS | FaceEngine.ASF_FACE_DETECT));
            flInitCode.postValue(flEngine.init(context, DetectMode.ASF_DETECT_MODE_IMAGE,
                    DetectFaceOrientPriority.ASF_OP_ALL_OUT, 10, flEngineMask));
            LivenessParam livenessParam = new LivenessParam(ConfigUtil.getRgbLivenessThreshold(context), ConfigUtil.getIrLivenessThreshold(context));
            flEngine.setLivenessParam(livenessParam);
        }

        recognizeConfiguration.setValue(configuration);
    }

    /**
     * 销毁引擎，faceHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    private void unInit() {
        if (ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                Log.i(TAG, "unInitEngine: " + ftUnInitCode);
            }
        }
        if (frEngine != null) {
            synchronized (frEngine) {
                int frUnInitCode = frEngine.unInit();
                Log.i(TAG, "unInitEngine: " + frUnInitCode);
            }
        }
        if (flEngine != null) {
            synchronized (flEngine) {
                int flUnInitCode = flEngine.unInit();
                Log.i(TAG, "unInitEngine: " + flUnInitCode);
            }
        }
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    public void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        List<CompareResult> compareResults = compareResultList.getValue();
        if (compareResults != null) {
            for (int i = compareResults.size() - 1; i >= 0; i--) {
                boolean contains = false;
                for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                    if (facePreviewInfo.getTrackId() == compareResults.get(i).getTrackId()) {
                        contains = true;
                        break;
                    }
                }
                if (!contains) {
                    compareResults.remove(i);
                    getFaceItemEventMutableLiveData().postValue(new FaceItemEvent(i, EventType.REMOVED));
                }
            }
        }
    }

    /**
     * 释放操作
     */
    public void destroy() {
        unInit();
        if (faceHelper != null) {
            ConfigUtil.setTrackedFaceCount(ArcFaceApplication.getApplication().getApplicationContext(), faceHelper.getTrackedFaceCount());
            faceHelper.release();
            faceHelper = null;
        }
        FaceServer.getInstance().release();
        if (registerNv21Disposable != null) {
            registerNv21Disposable.dispose();
            registerNv21Disposable = null;
        }
    }

    /**
     * 当相机打开时由activity调用，进行一些初始化操作
     *
     * @param camera 相机实例
     */
    public void onRgbCameraOpened(Camera camera) {
        Camera.Size lastPreviewSize = previewSize;
        previewSize = camera.getParameters().getPreviewSize();
        // 切换相机的时候可能会导致预览尺寸发生变化
        initFaceHelper(lastPreviewSize);
    }

    /**
     * 当相机打开时由activity调用，进行一些初始化操作
     *
     * @param camera 相机实例
     */
    public void onIrCameraOpened(Camera camera) {
        Camera.Size lastPreviewSize = previewSize;
        previewSize = camera.getParameters().getPreviewSize();
        // 切换相机的时候可能会导致预览尺寸发生变化
        initFaceHelper(lastPreviewSize);
    }

    private void initFaceHelper(Camera.Size lastPreviewSize) {
        if (faceHelper == null || lastPreviewSize == null ||
                lastPreviewSize.width != previewSize.width || lastPreviewSize.height != previewSize.height) {
            Integer trackedFaceCount = null;
            // 记录切换时的人脸序号
            if (faceHelper != null) {
                trackedFaceCount = faceHelper.getTrackedFaceCount();
                faceHelper.release();
            }
            Context context = ArcFaceApplication.getApplication().getApplicationContext();
            int horizontalOffset = ConfigUtil.getDualCameraHorizontalOffset(context);
            int verticalOffset = ConfigUtil.getDualCameraVerticalOffset(context);
            int maxDetectFaceNum = ConfigUtil.getRecognizeMaxDetectFaceNum(context);
            faceHelper = new FaceHelper.Builder()
                    .ftEngine(ftEngine)
                    .maskEngine(maskEngine)
                    .frEngine(frEngine)
                    .flEngine(flEngine)
                    .frQueueSize(maxDetectFaceNum)
                    .flQueueSize(maxDetectFaceNum)
                    .previewSize(previewSize)
                    .recognizeCallback(this)
                    .recognizeConfiguration(recognizeConfiguration.getValue())
                    .trackedFaceCount(trackedFaceCount == null ? ConfigUtil.getTrackedFaceCount(context) : trackedFaceCount)
                    .dualCameraFaceInfoTransformer(faceInfo -> {
                        FaceInfo irFaceInfo = new FaceInfo(faceInfo);
                        irFaceInfo.getRect().offset(horizontalOffset, verticalOffset);
                        return irFaceInfo;
                    })
                    .build();
        }
    }

    @Override
    public void onRecognized(CompareResult compareResult, Integer live, boolean similarPass) {
        Disposable disposable = Observable.just(true).observeOn(AndroidSchedulers.mainThread()).subscribe(aBoolean -> {
            if (similarPass) {
                // 识别到已知人脸
                boolean isAdded = false;
                List<CompareResult> compareResults = compareResultList.getValue();
                if (compareResults != null && !compareResults.isEmpty()) {
                    for (CompareResult compareResult1 : compareResults) {
                        if (compareResult1.getTrackId() == compareResult.getTrackId()) {
                            isAdded = true;
                            break;
                        }
                    }
                }
                if (!isAdded) {
                    //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                    if (compareResults != null && compareResults.size() >= MAX_DETECT_NUM) {
                        compareResults.remove(0);
                        getFaceItemEventMutableLiveData().postValue(new FaceItemEvent(0, EventType.REMOVED));
                    }
                    if (compareResults != null) {
                        compareResults.add(compareResult);
                        getFaceItemEventMutableLiveData().postValue(new FaceItemEvent(compareResults.size() - 1, EventType.INSERTED));
                    }

                    // 新人脸识别成功，提交打卡
                    submitPunch(compareResult);
                }
            } else {
                // 识别到陌生人，播放欢迎语
                Log.i(TAG, "Stranger detected, playing welcome message");

                // 防止频繁播放欢迎语（3秒内只播放一次）
                long now = System.currentTimeMillis();
                if (now - lastPunchTime >= PUNCH_COOLDOWN_MS) {
                    lastPunchTime = now;

                    // 播放欢迎语（需要在 AudioPlayer 中添加欢迎语播放方法）
                    // AudioPlayer.getInstance(ArcFaceApplication.getApplication()).playWelcome();

                    String resultMsg = "识别到陌生人";
                    punchResult.postValue(resultMsg);
                }
            }
        });
    }

    @Override
    public void onNoticeChanged(String notice) {
        if (recognizeNotice != null) {
            recognizeNotice.postValue(notice);
        }
    }

    @Override
    public void onAttendanceCheck(CompareResult compareResult) {
        if (compareResult == null || compareResult.getFaceEntity() == null) {
            Log.w(TAG, "Invalid compare result for attendance check");
            return;
        }

        // 防止频繁打卡（3秒内只打卡一次）
        long now = System.currentTimeMillis();
        if (now - lastPunchTime < PUNCH_COOLDOWN_MS) {
            Log.d(TAG, "Attendance punch cooldown, skip");
            return;
        }
        lastPunchTime = now;

        Log.i(TAG, "Processing attendance check for: " + compareResult.getFaceEntity().getUserName());

        // 调用本地考勤集成服务
        com.arcsoft.arcfacedemo.integration.LocalAttendanceIntegration.getInstance(ArcFaceApplication.getApplication())
                .processFaceRecognition(compareResult, new com.arcsoft.arcfacedemo.integration.LocalAttendanceIntegration.ProcessCallback() {
                    @Override
                    public void onAttendanceResult(com.arcsoft.arcfacedemo.attendance.model.CheckResult result) {
                        // 打卡成功，播放提示音
                        AudioPlayer.getInstance(ArcFaceApplication.getApplication()).playPunchSuccess();

                        // 构建打卡结果消息
                        String resultMsg = buildPunchResultMessage(result);
                        punchResult.postValue(resultMsg);

                        Log.i(TAG, "Attendance success: " + resultMsg);
                    }

                    @Override
                    public void onError(String error) {
                        // 打卡失败
                        Log.e(TAG, "Attendance error: " + error);
                        punchResult.postValue("打卡失败: " + error);

                        // 播放失败提示音（使用已打卡提示音代替）
                        AudioPlayer.getInstance(ArcFaceApplication.getApplication()).playAlreadyPunched();
                    }
                });
    }

    /**
     * 构建打卡结果消息
     */
    private String buildPunchResultMessage(com.arcsoft.arcfacedemo.attendance.model.CheckResult result) {
        StringBuilder sb = new StringBuilder();

        // 从message中提取员工姓名
        String message = result.getMessage();
        if (message != null && message.contains(" ")) {
            String[] parts = message.split(" ", 2);
            if (parts.length > 0) {
                sb.append(parts[0]).append(" ");
            }
        }

        // 判断打卡类型
        if ("CHECK_IN".equals(result.getCheckType())) {
            sb.append("上班打卡");
            if ("LATE".equals(result.getStatus())) {
                sb.append("（迟到）");
            } else {
                sb.append("（正常）");
            }
        } else if ("CHECK_OUT".equals(result.getCheckType())) {
            sb.append("下班打卡");
            if ("EARLY".equals(result.getStatus())) {
                sb.append("（早退）");
            } else {
                sb.append("（正常）");
            }
        } else {
            sb.append("打卡成功");
        }

        // 添加时间
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
        sb.append(" ").append(sdf.format(new java.util.Date(result.getCheckTime())));

        return sb.toString();
    }

    /**
     * 提交打卡
     */
    private void submitPunch(CompareResult compareResult) {
        // 防止重复打卡
        long now = System.currentTimeMillis();
        if (now - lastPunchTime < PUNCH_COOLDOWN_MS) {
            Log.d(TAG, "Punch cooldown, skip");
            return;
        }
        lastPunchTime = now;

        if (attendanceRepository == null) {
            Log.w(TAG, "AttendanceRepository not initialized");
            return;
        }

        // 通过faceId获取员工信息
        long faceId = compareResult.getFaceEntity().getFaceId();
        Log.i(TAG, "Looking up employee by faceId: " + faceId);

        com.arcsoft.arcfacedemo.employee.service.EmployeeService employeeService =
                com.arcsoft.arcfacedemo.employee.service.EmployeeService.getInstance(ArcFaceApplication.getApplication());

        employeeService.getEmployeeByFaceId(faceId)
                .subscribe(new io.reactivex.SingleObserver<com.arcsoft.arcfacedemo.employee.model.EmployeeEntity>() {
                    @Override
                    public void onSubscribe(io.reactivex.disposables.Disposable d) {}

                    @Override
                    public void onSuccess(com.arcsoft.arcfacedemo.employee.model.EmployeeEntity employee) {
                        if (employee != null) {
                            // 找到员工，使用员工信息打卡
                            submitPunchWithEmployee(employee, compareResult.getSimilar(), now);
                        } else {
                            // 未找到员工信息，使用faceId作为userId打卡
                            Log.w(TAG, "Employee not found for faceId: " + faceId + ", using faceId as userId");
                            submitPunchWithoutEmployee(faceId, compareResult.getSimilar(), now);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Error getting employee by faceId", e);
                        // 出错时使用faceId作为userId打卡
                        submitPunchWithoutEmployee(faceId, compareResult.getSimilar(), now);
                    }
                });
    }

    /**
     * 使用员工信息打卡
     */
    private void submitPunchWithEmployee(com.arcsoft.arcfacedemo.employee.model.EmployeeEntity employee, float similar, long now) {
        String employeeNo = employee.getEmployeeNo();
        String userName = employee.getName();

        Log.i(TAG, "Found employee: " + employeeNo + " - " + userName);

        // 获取当前时间（小时）
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);
        int currentMinutes = hour * 60 + minute;

        // 定义打卡时间段（分钟）
        final int MORNING_START = 8 * 60;
        final int MORNING_END = 9 * 60;
        final int EVENING_START = 18 * 60;

        // 今天的日期字符串
        String todayDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        Integer attendanceType = null;
        boolean shouldPunch = false;

        if (currentMinutes >= MORNING_START && currentMinutes < MORNING_END) {
            attendanceType = 1;
            String lastMorningPunchDate = todayMorningPunchMap.get(employeeNo);
            if (todayDate.equals(lastMorningPunchDate)) {
                Log.i(TAG, "Employee " + employeeNo + " already punched for work this morning");
                return;
            }
            shouldPunch = true;
        } else if (currentMinutes >= EVENING_START) {
            attendanceType = 2;
            shouldPunch = true;
        } else {
            Log.d(TAG, "Current time " + hour + ":" + String.format("%02d", minute) + " is not punch time");
            return;
        }

        if (!shouldPunch) {
            return;
        }

        Log.i(TAG, "Submitting punch for: " + employeeNo + " - " + userName +
                ", similarity: " + similar + ", type: " + (attendanceType == 1 ? "上班" : "下班"));

        final int finalAttendanceType = attendanceType;
        final String punchTypeStr = finalAttendanceType == 1 ? "上班" : "下班";

        // 创建打卡请求
        PunchRequest request = new PunchRequest.Builder()
                .userId(employeeNo)
                .username(userName)
                .phone(employee.getPhone() != null ? employee.getPhone() : "")
                .timestamp(now / 1000)
                .latitude(0.0)
                .longitude(0.0)
                .address("欧雅典")
                .attendanceType(finalAttendanceType)
                .remark(finalAttendanceType == 1 ? "人脸识别上班打卡" : "人脸识别下班打卡")
                .build();

        doSubmitPunch(request, employeeNo, punchTypeStr, finalAttendanceType, todayDate);
    }

    /**
     * 不使用员工信息打卡（兼容旧的faceId）
     */
    private void submitPunchWithoutEmployee(long faceId, float similar, long now) {
        // 从faceId获取FaceEntity
        FaceEntity faceEntity = faceDao.queryByFaceId((int) faceId);
        String userName = faceEntity != null ? faceEntity.getUserName() : "Unknown";

        Log.i(TAG, "Submitting punch for faceId: " + faceId + ", userName: " + userName);

        // 简化逻辑，直接使用faceId作为userId
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);
        int currentMinutes = hour * 60 + minute;

        final int MORNING_START = 8 * 60;
        final int MORNING_END = 9 * 60;
        final int EVENING_START = 18 * 60;

        String todayDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        Integer attendanceType = null;
        boolean shouldPunch = false;

        if (currentMinutes >= MORNING_START && currentMinutes < MORNING_END) {
            attendanceType = 1;
            String lastMorningPunchDate = todayMorningPunchMap.get(userName);
            if (todayDate.equals(lastMorningPunchDate)) {
                return;
            }
            shouldPunch = true;
        } else if (currentMinutes >= EVENING_START) {
            attendanceType = 2;
            shouldPunch = true;
        } else {
            return;
        }

        if (!shouldPunch) {
            return;
        }

        final int finalAttendanceType = attendanceType;
        final String punchTypeStr = finalAttendanceType == 1 ? "上班" : "下班";

        PunchRequest request = new PunchRequest.Builder()
                .userId(String.valueOf(faceId))
                .username(userName)
                .phone("")
                .timestamp(now / 1000)
                .latitude(0.0)
                .longitude(0.0)
                .address("欧雅典")
                .attendanceType(finalAttendanceType)
                .remark(punchTypeStr + "人脸识别打卡")
                .build();

        doSubmitPunch(request, userName, punchTypeStr, finalAttendanceType, todayDate);
    }

    /**
     * 执行打卡API调用
     */
    private void doSubmitPunch(PunchRequest request, String userKey, String punchTypeStr,
                               int attendanceType, String todayDate) {
        // 调用打卡 API
        attendanceRepository.submitPunch(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            if (response.isSuccess()) {
                                if (attendanceType == 1) {
                                    todayMorningPunchMap.put(userKey, todayDate);
                                } else {
                                    todayEveningPunchMap.put(userKey, todayDate);
                                }

                                Log.i(TAG, "Punch success - user: " + request.getUsername()
                                        + ", type: " + punchTypeStr
                                        + ", code: " + response.getCode()
                                        + ", message: " + response.getMessage()
                                        + ", recordId: " + response.getRecordId());

                                AudioPlayer.getInstance(ArcFaceApplication.getApplication())
                                        .playPunchSuccess();

                                String resultMsg = punchTypeStr + "打卡成功: " + request.getUsername();
                                punchResult.postValue(resultMsg);
                            } else {
                                Log.w(TAG, "Punch failed - user: " + request.getUsername()
                                        + ", type: " + punchTypeStr
                                        + ", code: " + response.getCode()
                                        + ", message: " + response.getMessage());
                                String resultMsg = "打卡失败: " + response.getMessage();
                                punchResult.postValue(resultMsg);
                            }
                        },
                        error -> {
                            String errorType = error.getClass().getSimpleName();
                            String errorMessage = error.getMessage();
                            String errorDetails = "";

                            if (error instanceof retrofit2.adapter.rxjava2.HttpException) {
                                retrofit2.adapter.rxjava2.HttpException httpException =
                                        (retrofit2.adapter.rxjava2.HttpException) error;
                                errorDetails = "HTTP " + httpException.code() + " - " + httpException.message();
                            }

                            Log.e(TAG, "Punch error - user: " + request.getUsername()
                                    + ", type: " + punchTypeStr
                                    + ", error: " + errorType
                                    + ", message: " + errorMessage
                                    + ", details: " + errorDetails, error);

                            String resultMsg = "打卡请求失败: " + errorMessage;
                            punchResult.postValue(resultMsg);
                        }
                );
    }

    public void setDrawRectInfoTextValue(boolean openDrawRect) {
        String stringDrawText = openDrawRect ? "关闭绘制" : "开启绘制";
        if (drawRectInfoText != null) {
            drawRectInfoText.postValue(stringDrawText);
        }
    }

    /**
     * 设置实时注册的结果回调
     *
     * @param onRegisterFinishedCallback 实时注册的结果回调
     */
    public void setOnRegisterFinishedCallback(OnRegisterFinishedCallback onRegisterFinishedCallback) {
        this.onRegisterFinishedCallback = onRegisterFinishedCallback;
    }

    public MutableLiveData<Integer> getFtInitCode() {
        return ftInitCode;
    }

    public MutableLiveData<Integer> getFrInitCode() {
        return frInitCode;
    }

    public MutableLiveData<Integer> getFlInitCode() {
        return flInitCode;
    }

    public MutableLiveData<String> getRecognizeNotice() {
        return recognizeNotice;
    }

    public MutableLiveData<String> getDrawRectInfoText() {
        return drawRectInfoText;
    }

    public MutableLiveData<FaceItemEvent> getFaceItemEventMutableLiveData() {
        return faceItemEventMutableLiveData;
    }

    public MutableLiveData<String> getPunchResult() {
        return punchResult;
    }

    /**
     * 初始化打卡仓库
     */
    public void initAttendanceRepository(Context context) {
        if (attendanceRepository == null) {
            attendanceRepository = AttendanceRepository.getInstance(context);
            Log.i(TAG, "AttendanceRepository initialized");
        }
    }

    /**
     * 准备注册，将注册的状态值修改为待注册
     */
    public void prepareRegister() {
        if (registerStatus == REGISTER_STATUS_DONE) {
            updateRegisterStatus(REGISTER_STATUS_READY);
        }
    }

    private void updateRegisterStatus(int status) {
        registerStatus = status;
    }

    /**
     * 根据预览信息生成绘制信息
     *
     * @param facePreviewInfoList 预览信息
     * @return 绘制信息
     */
    public List<FaceRectView.DrawInfo> getDrawInfo(List<FacePreviewInfo> facePreviewInfoList, LivenessType livenessType, boolean drawRectInfo) {
        List<FaceRectView.DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            int trackId = facePreviewInfoList.get(i).getTrackId();
            String name = faceHelper.getName(trackId);
            Integer liveness = faceHelper.getLiveness(trackId);
            Integer recognizeStatus = faceHelper.getRecognizeStatus(trackId);

            // 根据识别结果和活体结果设置颜色
            int color = RecognizeColor.COLOR_UNKNOWN;
            if (recognizeStatus != null) {
                if (recognizeStatus == RequestFeatureStatus.FAILED) {
                    color = RecognizeColor.COLOR_FAILED;
                }
                if (recognizeStatus == RequestFeatureStatus.SUCCEED) {
                    color = RecognizeColor.COLOR_SUCCESS;
                }
            }
            if (liveness != null && liveness == LivenessInfo.NOT_ALIVE) {
                color = RecognizeColor.COLOR_FAILED;
            }

            drawInfoList.add(new FaceRectView.DrawInfo(
                    livenessType == LivenessType.RGB ? facePreviewInfoList.get(i).getRgbTransformedRect() : facePreviewInfoList.get(i).getIrTransformedRect(),
                    GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, liveness == null ? LivenessInfo.UNKNOWN : liveness, color,
                    name == null ? "" : name, facePreviewInfoList.get(i).getFaceInfoRgb().getIsWithinBoundary(),
                    facePreviewInfoList.get(i).getForeRect(), facePreviewInfoList.get(i).getFaceInfoRgb().getFaceAttributeInfo(), drawRectInfo,
                    livenessType == LivenessType.RGB));
        }
        return drawInfoList;
    }


    /**
     * 传入可见光相机预览数据
     *
     * @param nv21        可见光相机预览数据
     * @param doRecognize 是否进行识别
     * @return 当前帧的检测结果信息
     */
    public List<FacePreviewInfo> onPreviewFrame(byte[] nv21, boolean doRecognize) {
        if (faceHelper != null) {
            if (!loadFaceList) {
                return null;
            }
            if (livenessType == LivenessType.IR && irNV21 == null) {
                return null;
            }
            List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21, irNV21, doRecognize);
            if (registerStatus == REGISTER_STATUS_READY && !facePreviewInfoList.isEmpty()) {
                FacePreviewInfo facePreviewInfo = facePreviewInfoList.get(0);
                if (facePreviewInfo.getMask() != MaskInfo.WORN) {
                    registerFace(nv21, facePreviewInfoList.get(0));
                } else {
                    Toast.makeText(ArcFaceApplication.getApplication(), "注册照要求不戴口罩", Toast.LENGTH_SHORT).show();
                    updateRegisterStatus(REGISTER_STATUS_DONE);
                }
            }
            return facePreviewInfoList;
        }
        return null;
    }

    /**
     * 设置可识别区域（相对于View）
     *
     * @param recognizeArea 可识别区域
     */
    public void setRecognizeArea(Rect recognizeArea) {
        if (faceHelper != null) {
            faceHelper.setRecognizeArea(recognizeArea);
        }
    }

    public MutableLiveData<RecognizeConfiguration> getRecognizeConfiguration() {
        return recognizeConfiguration;
    }

    public PreviewConfig getPreviewConfig() {
        return previewConfig;
    }

    public Point loadPreviewSize() {
        String[] size = ConfigUtil.getPreviewSize(ArcFaceApplication.getApplication()).split("x");
        return new Point(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
    }
}
