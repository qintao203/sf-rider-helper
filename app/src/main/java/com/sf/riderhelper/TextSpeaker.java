package com.sf.riderhelper;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

/**
 * 语音播报引擎 (TTS)
 * 抢单成功时语音播报订单信息，方便骑行中获取订单概况
 */
public class TextSpeaker {

    private static final String TAG = "TextSpeaker";
    private TextToSpeech tts;
    private boolean ready = false;
    private final Context ctx;
    private boolean enabled = true;

    public TextSpeaker(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    /** 初始化TTS引擎 */
    public void init() {
        if (tts != null) return;
        tts = new TextToSpeech(ctx, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.CHINESE);
                tts.setSpeechRate(0.9f);  // 略慢语速，更清晰
                tts.setPitch(1.0f);
                ready = true;
                Log.d(TAG, "TTS initialized");
            } else {
                ready = false;
                Log.w(TAG, "TTS init failed: " + status);
            }
        });
    }

    public void setEnabled(boolean e) { this.enabled = e; }
    public boolean isEnabled() { return enabled; }
    public boolean isReady() { return ready; }

    /** 播报抢单信息 */
    public void speakGrab(float price, String direction, float distance, String strategy) {
        if (!enabled || !ready || tts == null) return;

        String text = strategy + "订单，" +
                String.format("%.0f", price) + "元，" +
                (direction.isEmpty() ? "" : direction + "方向，") +
                (distance > 0 ? String.format("%.1f", distance) + "公里" : "");

        speak(text);
    }

    /** 播报自定义文本 */
    public void speak(String text) {
        if (!enabled || !ready || tts == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "grab_" + System.currentTimeMillis());
            } else {
                HashMap<String, String> map = new HashMap<>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "grab");
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
            }
            Log.d(TAG, "TTS: " + text);
        } catch (Exception e) {
            Log.e(TAG, "TTS speak failed", e);
        }
    }

    /** 释放资源 */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            ready = false;
        }
    }
}
