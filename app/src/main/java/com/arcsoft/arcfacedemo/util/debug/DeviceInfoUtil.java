package com.arcsoft.arcfacedemo.util.debug;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class DeviceInfoUtil {

    /**
     * 获取CPU型号，即/proc/cpuinfo中的Hardware字段
     *
     * @return CPU型号
     */
    public static String getCpuModel() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("/proc/cpuinfo");
            Properties properties = new Properties();
            properties.load(fis);
            String cpu = (String) properties.get("Hardware");
            return cpu == null ? "unknown" : cpu;
        } catch (IOException e) {
            return "unknown";
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取CPU核心数
     *
     * @return CPU核心数
     */
    public static int getCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 获取CPU硬件信息（芯片）
     *
     * @return 芯片信息
     */
    public static String getHardware() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/cpuinfo")));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Hardware")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        reader.close();
                        return parts[1].trim();
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Build.HARDWARE;
    }

    /**
     * 获取芯片组信息
     *
     * @return 芯片组信息
     */
    public static String getChipset() {
        String board = Build.BOARD;
        String hardware = Build.HARDWARE;
        return board + "/" + hardware;
    }

    /**
     * 获取总内存大小
     *
     * @param context 上下文
     * @return 总内存大小（格式化字符串）
     */
    public static String getTotalMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        return Formatter.formatFileSize(context, memoryInfo.totalMem);
    }

    /**
     * 获取可用内存大小
     *
     * @param context 上下文
     * @return 可用内存大小（格式化字符串）
     */
    public static String getAvailableMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        return Formatter.formatFileSize(context, memoryInfo.availMem);
    }

    /**
     * 获取总存储空间
     *
     * @param context 上下文
     * @return 总存储空间（格式化字符串）
     */
    public static String getTotalStorage(Context context) {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long totalBytes = stat.getBlockCountLong() * stat.getBlockSizeLong();
            return Formatter.formatFileSize(context, totalBytes);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取可用存储空间
     *
     * @param context 上下文
     * @return 可用存储空间（格式化字符串）
     */
    public static String getAvailableStorage(Context context) {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long availableBytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            return Formatter.formatFileSize(context, availableBytes);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取GPU供应商
     *
     * @return GPU供应商
     */
    public static String getGlVendor() {
        // 从系统属性获取GPU信息
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/cpuinfo")));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Hardware")) {
                    reader.close();
                    // 从硬件信息推断GPU
                    if (line.toLowerCase().contains("qualcomm")) {
                        return "Qualcomm Adreno";
                    } else if (line.toLowerCase().contains("mediatek") || line.toLowerCase().contains("mtk")) {
                        return "ARM Mali";
                    } else if (line.toLowerCase().contains("exynos")) {
                        return "ARM Mali";
                    } else if (line.toLowerCase().contains("kirin")) {
                        return "ARM Mali";
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            // 忽略异常
        }

        // 默认返回制造商+GPU
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer.equalsIgnoreCase("Xiaomi") || manufacturer.equalsIgnoreCase("Qualcomm")) {
            return "Qualcomm Adreno";
        } else if (manufacturer.equalsIgnoreCase("MediaTek")) {
            return "ARM Mali";
        } else if (manufacturer.equalsIgnoreCase("Samsung")) {
            return "ARM Mali";
        }
        return manufacturer + " GPU";
    }

    /**
     * 获取GPU渲染器信息
     *
     * @return GPU信息
     */
    public static String getGlRenderer() {
        return getGlVendor();
    }

    /**
     * 获取NDK版本
     *
     * @return NDK版本
     */
    public static String getNdkVersion() {
        try {
            return "NDK " + android.os.Build.VERSION.RELEASE;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取Android版本
     *
     * @return Android版本
     */
    public static String getAndroidVersion() {
        return "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
    }

    /**
     * 获取设备制造商和型号
     *
     * @return 设备信息
     */
    public static String getDeviceInfo() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    /**
     * 获取完整的CPU信息
     *
     * @return CPU信息字符串
     */
    public static String getCpuInfo() {
        return getCpuModel() + " (" + getCpuCores() + " cores)";
    }

    /**
     * 获取完整的GPU信息
     *
     * @return GPU信息字符串
     */
    public static String getGpuInfo() {
        String vendor = getGlVendor();
        if (!"unknown".equals(vendor) && !vendor.contains("unknown")) {
            return vendor;
        }
        return getGlRenderer();
    }
}
