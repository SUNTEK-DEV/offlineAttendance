package com.arcsoft.arcfacedemo.dooraccess.repository;

import android.content.Context;

import com.arcsoft.arcfacedemo.dooraccess.dao.DoorAccessRecordDao;
import com.arcsoft.arcfacedemo.dooraccess.model.DoorAccessRecordEntity;
import com.arcsoft.arcfacedemo.facedb.FaceDatabase;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * 门禁仓库
 */
public class DoorAccessRepository {

    private static DoorAccessRepository instance;

    private DoorAccessRecordDao doorAccessRecordDao;

    private DoorAccessRepository(Context context) {
        FaceDatabase database = FaceDatabase.getInstance(context);
        this.doorAccessRecordDao = database.doorAccessRecordDao();
    }

    public static synchronized DoorAccessRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DoorAccessRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 插入访问记录
     */
    public Single<Long> insertRecord(DoorAccessRecordEntity record) {
        return Single.fromCallable(() -> doorAccessRecordDao.insert(record))
                .subscribeOn(Schedulers.io());
    }

    /**
     * 根据日期范围获取记录
     */
    public Single<List<DoorAccessRecordEntity>> getRecordsByDateRange(long startTime, long endTime) {
        return Single.fromCallable(() ->
                doorAccessRecordDao.getRecordsByDateRange(startTime, endTime)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 根据员工ID获取记录
     */
    public Single<List<DoorAccessRecordEntity>> getRecordsByEmployee(long employeeId, int limit) {
        return Single.fromCallable(() ->
                doorAccessRecordDao.getRecordsByEmployee(employeeId, limit)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 获取成功的记录
     */
    public Single<List<DoorAccessRecordEntity>> getSuccessRecords(int limit, int offset) {
        return Single.fromCallable(() ->
                doorAccessRecordDao.getSuccessRecords(limit, offset)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 获取失败的记录
     */
    public Single<List<DoorAccessRecordEntity>> getFailedRecords(int limit, int offset) {
        return Single.fromCallable(() ->
                doorAccessRecordDao.getFailedRecords(limit, offset)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 分页获取所有记录
     */
    public Single<List<DoorAccessRecordEntity>> getRecords(int limit, int offset) {
        return Single.fromCallable(() ->
                doorAccessRecordDao.getRecords(limit, offset)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 获取记录总数
     */
    public Single<Integer> getRecordCount() {
        return Single.fromCallable(doorAccessRecordDao::getRecordCount)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 获取成功记录数量
     */
    public Single<Integer> getSuccessCount() {
        return Single.fromCallable(doorAccessRecordDao::getSuccessCount)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 获取失败记录数量
     */
    public Single<Integer> getFailedCount() {
        return Single.fromCallable(doorAccessRecordDao::getFailedCount)
                .subscribeOn(Schedulers.io());
    }

    /**
     * 获取最近的访问记录
     */
    public Single<List<DoorAccessRecordEntity>> getRecentRecords(int limit) {
        return Single.fromCallable(() ->
                doorAccessRecordDao.getRecentRecords(limit)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * 删除指定日期之前的记录
     */
    public Single<Integer> deleteRecordsBefore(long beforeTime) {
        return Single.fromCallable(() ->
                doorAccessRecordDao.deleteRecordsBefore(beforeTime)
        ).subscribeOn(Schedulers.io());
    }
}
