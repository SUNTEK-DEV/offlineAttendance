package com.arcsoft.arcfacedemo.ui.viewmodel;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.arcsoft.arcfacedemo.ArcFaceApplication;
import com.arcsoft.arcfacedemo.employee.model.EmployeeEntity;
import com.arcsoft.arcfacedemo.employee.service.EmployeeService;
import com.arcsoft.arcfacedemo.facedb.FaceDatabase;
import com.arcsoft.arcfacedemo.facedb.dao.FaceDao;
import com.arcsoft.arcfacedemo.facedb.entity.FaceEntity;
import com.arcsoft.arcfacedemo.faceserver.FaceServer;
import com.arcsoft.arcfacedemo.ui.callback.OnRegisterFinishedCallback;
import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;

import java.util.LinkedList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * 员工管理ViewModel
 */
public class EmployeeViewModel extends ViewModel {
    private static final String TAG = "EmployeeViewModel";

    private MutableLiveData<List<EmployeeEntity>> employeeList = new MutableLiveData<>();
    private MutableLiveData<Integer> totalEmployeeCount = new MutableLiveData<>();
    private MutableLiveData<Boolean> initFinished = new MutableLiveData<>();
    private MutableLiveData<FaceEntity> faceEntityData = new MutableLiveData<>();

    private FaceDao faceDao;
    private EmployeeService employeeService;

    private static final int PAGE_SIZE = 20;
    private static final int VISIBLE_THRESHOLD = 5;

    private int employeeCount = -1;

    public MutableLiveData<List<EmployeeEntity>> getEmployeeList() {
        return employeeList;
    }

    public MutableLiveData<Integer> getTotalEmployeeCount() {
        return totalEmployeeCount;
    }

    public MutableLiveData<Boolean> getInitFinished() {
        return initFinished;
    }

    public MutableLiveData<FaceEntity> getFaceById(long faceId) {
        return faceEntityData;
    }

    public EmployeeViewModel() {
        Context context = ArcFaceApplication.getApplication();
        faceDao = FaceDatabase.getInstance(context).faceDao();
        employeeService = EmployeeService.getInstance(context);
        // 初始化FaceServer
        FaceServer.getInstance().init(context, faceCount -> {
            Log.d(TAG, "FaceServer initialized with face count: " + faceCount);
        });
    }

    /**
     * 初始化
     */
    public void init() {
        initFinished.postValue(true);
    }

    /**
     * 加载员工列表
     */
    public synchronized void loadEmployees() {
        employeeService.getAllEmployees()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<EmployeeEntity>>() {
                    @Override
                    public void accept(List<EmployeeEntity> employees) {
                        employeeList.postValue(employees);
                        totalEmployeeCount.postValue(employees.size());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable e) {
                        Log.e(TAG, "加载员工列表失败", e);
                    }
                });
    }

    /**
     * 删除员工
     */
    public void deleteEmployee(EmployeeEntity employee) {
        // 先在后台查询人脸，然后再删除员工
        if (employee.getFaceId() > 0) {
            Single.fromCallable(() -> faceDao.queryByFaceId((int) employee.getFaceId()))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<FaceEntity>() {
                        @Override
                        public void accept(FaceEntity faceEntity) {
                            if (faceEntity != null) {
                                FaceServer.getInstance().removeOneFace(faceEntity);
                            }
                            // 删除员工记录
                            deleteEmployeeRecord(employee);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable e) {
                            Log.e(TAG, "查询人脸失败", e);
                            // 即使查询失败，也尝试删除员工记录
                            deleteEmployeeRecord(employee);
                        }
                    });
        } else {
            // 没有关联人脸，直接删除员工
            deleteEmployeeRecord(employee);
        }
    }

    /**
     * 删除员工记录
     */
    private void deleteEmployeeRecord(EmployeeEntity employee) {
        employeeService.deleteEmployee(employee.getEmployeeId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer result) {
                        if (result > 0) {
                            loadEmployees();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable e) {
                        Log.e(TAG, "删除员工失败", e);
                    }
                });
    }

    /**
     * 注册人脸
     */
    /**
     * 注册人脸（已弃用，必须使用带姓名的方法）
     */
    public void registerFace(Bitmap bitmap, OnRegisterFinishedCallback callback) {
        Log.e(TAG, "错误：禁止使用不带姓名的 registerFace 方法");
        if (callback != null) {
            callback.onRegisterFinished(null, false);
        }
    }

    /**
     * 注册人脸（带姓名）
     */
    public void registerFace(Bitmap bitmap, String userName, OnRegisterFinishedCallback callback) {
        Log.w(TAG,"注册人脸不许用时间戳！！！");
        Bitmap alignedBitmap = ArcSoftImageUtil.getAlignedBitmap(bitmap, true);
        Observable.create((ObservableOnSubscribe<byte[]>) emitter -> {
            byte[] bgr24Data = ArcSoftImageUtil.createImageData(alignedBitmap.getWidth(), alignedBitmap.getHeight(), ArcSoftImageFormat.BGR24);
            int transformCode = ArcSoftImageUtil.bitmapToImageData(alignedBitmap, bgr24Data, ArcSoftImageFormat.BGR24);
            if (transformCode == ArcSoftImageUtilError.CODE_SUCCESS) {
                emitter.onNext(bgr24Data);
            } else {
                emitter.onError(new Exception("transform failed, code is " + transformCode));
            }
        })
                .flatMap((Function<byte[], ObservableSource<FaceEntity>>) bgr24Data -> {
                    // 禁止使用时间戳，必须提供真实姓名
                    if (userName == null || userName.trim().isEmpty()) {
                        Log.e(TAG, "注册人脸失败：未提供姓名");
                        return Observable.error(new Exception("注册人脸失败：必须提供员工姓名"));
                    }

                    // 使用真实姓名注册人脸
                    FaceEntity faceEntity = FaceServer.getInstance().registerBgr24(
                            ArcFaceApplication.getApplication(), bgr24Data,
                            alignedBitmap.getWidth(), alignedBitmap.getHeight(),
                            userName.trim());
                    loadEmployees();
                    if (faceEntity == null) {
                        return Observable.error(new Exception("人脸注册失败：请确保图片清晰且包含正脸"));
                    }
                    return Observable.just(faceEntity);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<FaceEntity>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onNext(FaceEntity faceEntity) {
                        if (callback != null && faceEntity != null) {
                            // 创建 FacePreviewInfo，faceInfo设为null（因为我们没有人脸框信息）
                            // trackId使用faceEntity的faceId
                            com.arcsoft.arcfacedemo.util.face.model.FacePreviewInfo info =
                                    new com.arcsoft.arcfacedemo.util.face.model.FacePreviewInfo(
                                            null,  // faceInfoRgb设为null
                                            (int) faceEntity.getFaceId()  // trackId使用faceId
                                    );
                            callback.onRegisterFinished(info, true);
                        } else if (callback != null) {
                            callback.onRegisterFinished(null, false);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "注册人脸失败", e);
                        if (callback != null) {
                            callback.onRegisterFinished(null, false);
                        }
                    }

                    @Override
                    public void onComplete() {}
                });
    }

    /**
     * 创建 FacePreviewInfo 对象（已不再使用，保留作为备用）
     */
    private com.arcsoft.arcfacedemo.util.face.model.FacePreviewInfo createFacePreviewInfo(FaceEntity faceEntity) {
        return new com.arcsoft.arcfacedemo.util.face.model.FacePreviewInfo(
                null,  // faceInfoRgb设为null
                (int) faceEntity.getFaceId()  // trackId使用faceId
        );
    }

    /**
     * 获取人脸信息
     */
    public void loadFaceById(long faceId) {
        new Thread(() -> {
            FaceEntity faceEntity = faceDao.queryByFaceId((int) faceId);
            faceEntityData.postValue(faceEntity);
        }).start();
    }

    /**
     * 滚动加载
     */
    public void listScrolled(int lastVisibleItem, int totalItemCount) {
        // 当前版本一次性加载所有员工，后续可改为分页
    }

    /**
     * 释放资源
     */
    public void release() {
        // 清理资源
    }
}
