package com.arcsoft.arcfacedemo.util;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC 认证工具类
 * 用于生成 API 请求所需的签名头
 */
public class HmacAuthUtil {
    private static final String TAG = "HmacAuthUtil";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private String deviceId;
    private byte[] secretKeyHash;

    /**
     * 构造函数
     *
     * @param deviceId     设备 ID
     * @param deviceSecret 设备密钥（原始密钥或 SHA256 哈希值）
     */
    public HmacAuthUtil(String deviceId, String deviceSecret) {
        this.deviceId = deviceId;
        this.secretKeyHash = hashDeviceSecret(deviceSecret);
        Log.d(TAG, "HMAC Auth initialized for device: " + deviceId);
    }

    /**
     * 处理设备密钥
     * 如果已经是 64 字符的十六进制 SHA256 哈希值，直接使用
     * 否则计算 SHA256
     */
    private byte[] hashDeviceSecret(String secret) {
        try {
            // 检测是否已经是 SHA256 哈希值（64字符十六进制）
            if (secret.length() == 64 && isHexString(secret)) {
                Log.d(TAG, "Device secret is already SHA256 hash");
                return hexStringToBytes(secret);
            } else {
                // 计算原始密钥的 SHA256
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
                Log.d(TAG, "Device secret hashed to SHA256");
                return hash;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hash device secret", e);
            throw new RuntimeException("Failed to hash device secret", e);
        }
    }

    /**
     * 检测是否为十六进制字符串
     */
    private boolean isHexString(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isDigit(c) && (Character.toLowerCase(c) < 'a' || Character.toLowerCase(c) > 'f')) {
                return false;
            }
        }
        return true;
    }

    /**
     * 十六进制字符串转字节数组
     */
    private byte[] hexStringToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format(Locale.US, "%02x", b));
        }
        return result.toString();
    }

    /**
     * 生成 HMAC 认证所需的请求头
     *
     * @param bodyJson 请求 body（JSON 字符串）
     * @return 包含认证头部的 Map
     */
    public Map<String, String> generateHeaders(String bodyJson) {
        try {
            // 生成时间戳
            long timestamp = System.currentTimeMillis() / 1000;

            // 生成随机 nonce
            String nonce = generateNonce();

            // 计算 Body SHA256
            String bodySha256 = "";
            if (bodyJson != null && !bodyJson.isEmpty()) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(bodyJson.getBytes(StandardCharsets.UTF_8));
                bodySha256 = bytesToHex(hash);
            }

            // 构造签名消息: timestamp:nonce:body_sha256
            String message = timestamp + ":" + nonce + ":" + bodySha256;

            // 计算 HMAC-SHA256 签名
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyHash, HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String signature = bytesToHex(signatureBytes);

            // 详细调试日志
            Log.d(TAG, "=== HMAC Debug Info ===");
            Log.d(TAG, "Device ID: " + deviceId);
            Log.d(TAG, "Secret Hash (hex): " + bytesToHex(secretKeyHash).substring(0, 16) + "...");
            Log.d(TAG, "Timestamp: " + timestamp);
            Log.d(TAG, "Nonce: " + nonce);
            Log.d(TAG, "Body SHA256: " + bodySha256);
            Log.d(TAG, "Message to sign: " + message);
            Log.d(TAG, "Signature: " + signature);
            Log.d(TAG, "=====================");

            // 构造认证头部
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Device-Id", deviceId);
            headers.put("X-Timestamp", String.valueOf(timestamp));
            headers.put("X-Nonce", nonce);
            headers.put("X-Body-SHA256", bodySha256);
            headers.put("X-Signature", signature);

            Log.d(TAG, "Generated HMAC headers for device: " + deviceId);
            return headers;

        } catch (Exception e) {
            Log.e(TAG, "Failed to generate HMAC headers", e);
            throw new RuntimeException("Failed to generate HMAC headers", e);
        }
    }

    /**
     * 生成随机 nonce（32字符十六进制字符串）
     */
    private String generateNonce() {
        try {
            SecureRandom random = new SecureRandom();
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);
            return bytesToHex(bytes);
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate nonce", e);
            // 降级方案：使用时间戳
            return String.valueOf(System.currentTimeMillis());
        }
    }
}
