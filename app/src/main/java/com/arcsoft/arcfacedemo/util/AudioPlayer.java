package com.arcsoft.arcfacedemo.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 音频播放工具
 * 使用本地音频文件播放
 */
public class AudioPlayer {
    private static final String TAG = "AudioPlayer";
    private static AudioPlayer instance;
    private Context context;
    private Map<String, String> soundUriMap = new HashMap<>();
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    private AudioPlayer(Context context) {
        this.context = context.getApplicationContext();
        initSounds();
    }

    /**
     * 获取单例实例
     */
    public static synchronized AudioPlayer getInstance(Context context) {
        if (instance == null) {
            instance = new AudioPlayer(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 初始化音频资源映射
     * 使用 Uri 直接引用资源文件，注意：不要包含文件扩展名
     */
    private void initSounds() {
        String packageName = context.getPackageName();
        Log.i(TAG, "Package name: " + packageName);

        // 使用 android.resource:// 协议直接引用资源
        // 注意：资源路径不要包含文件扩展名 (.mp3)
        soundUriMap.put("punch_success", "android.resource://" + packageName + "/raw/punch_success");
        soundUriMap.put("already_punched", "android.resource://" + packageName + "/raw/already_punched");

        Log.i(TAG, "Audio player initialized with " + soundUriMap.size() + " sounds");
        Log.d(TAG, "punch_success URI: " + soundUriMap.get("punch_success"));
        Log.d(TAG, "already_punched URI: " + soundUriMap.get("already_punched"));
    }

    /**
     * 播放音频
     *
     * @param soundName 音频名称
     */
    public void play(String soundName) {
        String soundUri = soundUriMap.get(soundName);
        if (soundUri == null) {
            Log.w(TAG, "Sound not found: " + soundName);
            return;
        }

        playSoundUri(soundUri);
    }

    /**
     * 播放打卡成功音频
     */
    public void playPunchSuccess() {
        play("punch_success");
    }

    /**
     * 播放"已打过卡"提示音
     */
    public void playAlreadyPunched() {
        play("already_punched");
    }

    /**
     * 播放系统提示音作为备选方案
     */
    private void playSystemSound() {
        try {
            android.net.Uri notificationUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
            android.media.Ringtone rt = android.media.RingtoneManager.getRingtone(context, notificationUri);

            if (rt != null) {
                rt.play();
                Log.i(TAG, "Playing system notification sound");
                // 2秒后停止
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        rt.stop();
                    }
                }, 2000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to play system sound", e);
        }
    }

    /**
     * 使用 Uri 播放音频
     */
    private void playSoundUri(final String uriString) {
        try {
            Log.d(TAG, "playSoundUri called with uri: " + uriString);

            // 如果正在播放，先停止
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            // 请求音频焦点
            final android.media.AudioManager audioManager =
                    (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int result = audioManager.requestAudioFocus(
                        new android.media.AudioManager.OnAudioFocusChangeListener() {
                            @Override
                            public void onAudioFocusChange(int focusChange) {
                                if (focusChange == android.media.AudioManager.AUDIOFOCUS_LOSS) {
                                    stop();
                                }
                            }
                        },
                        android.media.AudioManager.STREAM_MUSIC,
                        android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                );
                Log.d(TAG, "Audio focus request result: " + result);
            }

            // 创建 MediaPlayer 并设置数据源
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);

            Uri uri = Uri.parse(uriString);
            Log.d(TAG, "Parsed Uri: " + uri);

            mediaPlayer.setDataSource(context, uri);
            mediaPlayer.prepareAsync();

            // 设置准备完成的监听
            mediaPlayer.setOnPreparedListener(player -> {
                isPlaying = true;

                // 设置音量
                mediaPlayer.setVolume(1.0f, 1.0f);

                // 设置完成监听
                mediaPlayer.setOnCompletionListener(player1 -> {
                    Log.i(TAG, "Sound playback completed: " + uriString);
                    isPlaying = false;
                    mediaPlayer.release();
                    mediaPlayer = null;

                    // 释放音频焦点
                    if (audioManager != null) {
                        audioManager.abandonAudioFocus(null);
                    }
                });

                // 设置错误监听
                mediaPlayer.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                        Log.e(TAG, "MediaPlayer error - what: " + what + ", extra: " + extra);
                        isPlaying = false;
                        mp.release();
                        mediaPlayer = null;
                        if (audioManager != null) {
                            audioManager.abandonAudioFocus(null);
                        }
                        return true;
                    }
                });

                // 获取音频时长
                int duration = mediaPlayer.getDuration();
                Log.d(TAG, "Audio duration: " + duration + "ms");

                // 开始播放
                mediaPlayer.start();
                Log.i(TAG, "▶️  Playing sound: " + uriString + " (duration: " + duration + "ms)");
            });

            // 设置准备错误监听
            mediaPlayer.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                    Log.e(TAG, "MediaPlayer prepare error - what: " + what + ", extra: " + extra);
                    isPlaying = false;
                    mp.release();
                    mediaPlayer = null;
                    if (audioManager != null) {
                        audioManager.abandonAudioFocus(null);
                    }
                    return true;
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception in playSoundUri: " + e.getMessage(), e);
            isPlaying = false;

            // 尝试使用备用方法
            Log.i(TAG, "Trying fallback method using resource ID");
            tryFallbackMethod(uriString);
        }
    }

    /**
     * 备用方法：使用资源 ID 播放音频
     */
    private void tryFallbackMethod(String uriString) {
        try {
            // 从 Uri 中提取资源名称
            String resourceName = null;
            if (uriString.contains("punch_success")) {
                resourceName = "punch_success";
            } else if (uriString.contains("already_punched")) {
                resourceName = "already_punched";
            }

            if (resourceName == null) {
                Log.e(TAG, "Cannot extract resource name from URI");
                return;
            }

            // 动态获取资源 ID
            int resId = context.getResources().getIdentifier(
                    resourceName,
                    "raw",
                    context.getPackageName()
            );

            if (resId == 0) {
                Log.e(TAG, "Resource not found: " + resourceName);
                return;
            }

            Log.i(TAG, "Found resource ID: " + resId + " for " + resourceName);

            // 请求音频焦点
            final android.media.AudioManager audioManager =
                    (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.requestAudioFocus(
                        new android.media.AudioManager.OnAudioFocusChangeListener() {
                            @Override
                            public void onAudioFocusChange(int focusChange) {
                                if (focusChange == android.media.AudioManager.AUDIOFOCUS_LOSS) {
                                    stop();
                                }
                            }
                        },
                        android.media.AudioManager.STREAM_MUSIC,
                        android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                );
            }

            // 创建 MediaPlayer 并使用资源 ID
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = MediaPlayer.create(context, resId);
            if (mediaPlayer == null) {
                Log.e(TAG, "Failed to create MediaPlayer from resource ID");
                return;
            }

            mediaPlayer.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(android.media.MediaPlayer mp) {
                    Log.i(TAG, "Sound playback completed (fallback method)");
                    isPlaying = false;
                    mp.release();
                    mediaPlayer = null;
                    if (audioManager != null) {
                        audioManager.abandonAudioFocus(null);
                    }
                }
            });

            mediaPlayer.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                    Log.e(TAG, "MediaPlayer error (fallback) - what: " + what + ", extra: " + extra);
                    isPlaying = false;
                    mp.release();
                    mediaPlayer = null;
                    if (audioManager != null) {
                        audioManager.abandonAudioFocus(null);
                    }
                    return true;
                }
            });

            isPlaying = true;
            mediaPlayer.start();
            Log.i(TAG, "▶️  Playing sound using fallback method (resource ID: " + resId + ")");

        } catch (Exception e) {
            Log.e(TAG, "Exception in fallback method", e);
            isPlaying = false;
        }
    }

    /**
     * 播放短促的提示音（使用 ToneGenerator）
     *
     * @param durationMs 持续时间（毫秒）
     */
    public void playBeep(int durationMs) {
        try {
            // 使用 ToneGenerator 播放提示音
            android.media.AudioManager audioManager =
                    (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager != null) {
                int streamType = android.media.AudioManager.STREAM_MUSIC;
                int volume = (int) (audioManager.getStreamVolume(streamType) * 0.8); // 80% 音量

                android.media.ToneGenerator toneGenerator =
                        new android.media.ToneGenerator(streamType, volume);

                // 播放提示音：类型0为标准提示音
                toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, durationMs);

                Log.i(TAG, "Playing beep sound for " + durationMs + "ms");

                // 震动提示
                android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(durationMs);
                    }
                }

                // 稍后释放资源
                new android.os.Handler().postDelayed(() -> {
                    toneGenerator.release();
                    Log.d(TAG, "ToneGenerator released");
                }, durationMs + 50);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to play beep", e);
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
                Log.i(TAG, "Sound stopped");
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop sound", e);
            } finally {
                mediaPlayer = null;
                isPlaying = false;

                // 释放音频焦点
                android.media.AudioManager audioManager =
                        (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    audioManager.abandonAudioFocus(null);
                }
            }
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        stop();
        instance = null;
        Log.i(TAG, "Audio player released");
    }

    /**
     * 检查是否正在播放
     */
    public boolean isPlaying() {
        return isPlaying && mediaPlayer != null && mediaPlayer.isPlaying();
    }
}
