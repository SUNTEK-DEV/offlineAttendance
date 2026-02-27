package com.arcsoft.arcfacedemo.util;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * TTS 语音播报工具
 */
public class TtsHelper {
    private static final String TAG = "TtsHelper";
    private static TtsHelper instance;
    private TextToSpeech tts;
    private boolean initialized = false;

    private TtsHelper(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.CHINA);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Chinese TTS not supported");
                    initialized = false;
                } else {
                    initialized = true;
                    Log.i(TAG, "TTS initialized successfully");
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
                initialized = false;
            }
        });
    }

    /**
     * 获取单例实例
     */
    public static synchronized TtsHelper getInstance(Context context) {
        if (instance == null) {
            instance = new TtsHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 播放语音
     *
     * @param text 要播放的文本
     */
    public void speak(String text) {
        speak(text, TextToSpeech.QUEUE_FLUSH);
    }

    /**
     * 播放语音
     *
     * @param text  要播放的文本
     * @param mode  播放模式
     */
    public void speak(String text, int mode) {
        Log.d(TAG, "speak called - initialized: " + initialized + ", tts: " + (tts != null) + ", text: " + text);

        if (!initialized || tts == null) {
            Log.w(TAG, "TTS not initialized, skipping speech");
            return;
        }

        // 检查语音是否正在播放
        if (tts.isSpeaking()) {
            Log.d(TAG, "TTS is speaking, stopping first");
            tts.stop();
        }

        int result = tts.speak(text, mode, null, "punch_tts_" + System.currentTimeMillis());
        Log.i(TAG, "TTS speak result: " + result + ", text: " + text);
    }

    /**
     * 停止播放
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            initialized = false;
            Log.i(TAG, "TTS released");
        }
    }

    /**
     * 设置语速
     *
     * @param speed 语速 (0.5-2.0，1.0为正常)
     */
    public void setSpeechRate(float speed) {
        if (tts != null) {
            tts.setSpeechRate(speed);
        }
    }

    /**
     * 设置音调
     *
     * @param pitch 音调 (0.5-2.0，1.0为正常)
     */
    public void setPitch(float pitch) {
        if (tts != null) {
            tts.setPitch(pitch);
        }
    }
}
