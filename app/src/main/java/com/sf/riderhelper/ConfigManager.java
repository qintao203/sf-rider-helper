package com.sf.riderhelper;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ConfigManager {
    private static ConfigManager instance;
    private SharedPreferences sp;
    private static final String PREFS_NAME = "sf_rider_config";

    private ConfigManager(Context c) {
        sp = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ConfigManager getInstance(Context c) {
        if (instance == null) instance = new ConfigManager(c.getApplicationContext());
        return instance;
    }

    // --- 金额 ---
    public float getMinPrice() { return sp.getFloat("min_price", 10); }
    public void setMinPrice(float v) { sp.edit().putFloat("min_price", v).apply(); }
    public float getMaxPrice() { return sp.getFloat("max_price", 200); }
    public void setMaxPrice(float v) { sp.edit().putFloat("max_price", v).apply(); }

    // --- 距离 ---
    public float getMaxDistance() { return sp.getFloat("max_dist", 5); }
    public void setMaxDistance(float v) { sp.edit().putFloat("max_dist", v).apply(); }

    // --- 方向 ---
    public Set<String> getPreferredDirections() {
        return sp.getStringSet("pref_dirs", new HashSet<>(Arrays.asList("科技园","福田","南山")));
    }
    public void setPreferredDirections(String csv) {
        sp.edit().putStringSet("pref_dirs", new HashSet<>(Arrays.asList(csv.split(",")))).apply();
    }
    public Set<String> getExcludedDirections() {
        return sp.getStringSet("excl_dirs", new HashSet<>(Arrays.asList("龙岗","坪山")));
    }
    public void setExcludedDirections(String csv) {
        sp.edit().putStringSet("excl_dirs", new HashSet<>(Arrays.asList(csv.split(",")))).apply();
    }

    // --- 行为 ---
    public int getScanInterval() { return sp.getInt("scan_interval", 2000); }
    public void setScanInterval(int v) { sp.edit().putInt("scan_interval", v).apply(); }
    public int getGrabDelay() { return sp.getInt("grab_delay", 100); }
    public void setGrabDelay(int v) { sp.edit().putInt("grab_delay", v).apply(); }
    public int getCooldownSeconds() { return sp.getInt("cooldown", 30); }
    public void setCooldownSeconds(int v) { sp.edit().putInt("cooldown", v).apply(); }

    // --- 提醒 ---
    public boolean isVibrateOnGrab() { return sp.getBoolean("vibrate", true); }
    public void setVibrateOnGrab(boolean v) { sp.edit().putBoolean("vibrate", v).apply(); }
    public boolean isNotifyOnGrab() { return sp.getBoolean("notify", true); }
    public void setNotifyOnGrab(boolean v) { sp.edit().putBoolean("notify", v).apply(); }
    public boolean isKeepScreenOn() { return sp.getBoolean("keep_screen", true); }
    public void setKeepScreenOn(boolean v) { sp.edit().putBoolean("keep_screen", v).apply(); }
    public boolean isTtsEnabled() { return sp.getBoolean("tts", true); }
    public void setTtsEnabled(boolean v) { sp.edit().putBoolean("tts", v).apply(); }

    // --- 省电 ---
    public boolean isPowerSaving() { return sp.getBoolean("power_saving", false); }
    public void setPowerSaving(boolean v) { sp.edit().putBoolean("power_saving", v).apply(); }

    // --- 勿扰时段 ---
    public boolean isDndEnabled() { return sp.getBoolean("dnd_enabled", false); }
    public void setDndEnabled(boolean v) { sp.edit().putBoolean("dnd_enabled", v).apply(); }
    public int getDndStartHour() { return sp.getInt("dnd_start_hour", 23); }
    public void setDndStartHour(int v) { sp.edit().putInt("dnd_start_hour", v).apply(); }
    public int getDndEndHour() { return sp.getInt("dnd_end_hour", 6); }
    public void setDndEndHour(int v) { sp.edit().putInt("dnd_end_hour", v).apply(); }

    // --- 悬浮球 ---
    public boolean isFloatingBall() { return sp.getBoolean("float_ball", false); }
    public void setFloatingBall(boolean v) { sp.edit().putBoolean("float_ball", v).apply(); }

    // --- 多配置场景 ---
    public String getSceneProfile() { return sp.getString("scene_profile", "默认"); }
    public void setSceneProfile(String v) { sp.edit().putString("scene_profile", v).apply(); }

    // --- 策略 ---
    public String getStrategyMode() { return sp.getString("strategy_mode", "中优"); }
    public void setStrategyMode(String v) { sp.edit().putString("strategy_mode", v).apply(); }
    public int getMinScore() { return sp.getInt("min_score", 20); }
    public void setMinScore(int v) { sp.edit().putInt("min_score", v).apply(); }

    // --- 统计 ---
    public int getStatGrabbed() { return sp.getInt("stat_grabbed", 0); }
    public void setStatGrabbed(int v) { sp.edit().putInt("stat_grabbed", v).apply(); }
    public int getStatFailed() { return sp.getInt("stat_failed", 0); }
    public void setStatFailed(int v) { sp.edit().putInt("stat_failed", v).apply(); }
    public int getStatSkipped() { return sp.getInt("stat_skipped", 0); }
    public void setStatSkipped(int v) { sp.edit().putInt("stat_skipped", v).apply(); }
    public void resetStats() {
        sp.edit().putInt("stat_grabbed", 0).putInt("stat_failed", 0)
                .putInt("stat_skipped", 0).apply();
    }
}
