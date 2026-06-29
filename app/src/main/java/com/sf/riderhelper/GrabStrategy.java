package com.sf.riderhelper;

import java.util.Random;

/**
 * 三级抢单策略引擎
 * 高优(Priority) → 优质单，秒抢不犹豫
 * 中优(Medium)  → 正常单，评估后抢
 * 保底(Fallback) → 普通单，空闲时再抢
 */
public class GrabStrategy {

    public enum Tier {
        PRIORITY("高优", 0),    // 秒抢
        MEDIUM("中优", 1),      // 正常抢
        FALLBACK("保底", 2),    // 空闲抢
        REJECT("拒绝", -1);     // 不抢

        public final String label;
        public final int level;
        Tier(String label, int level) { this.label = label; this.level = level; }
    }

    private final ConfigManager config;
    private final Random rng = new Random();
    private long lastGrabTime = 0;
    private long lastFallbackGrabTime = 0;
    private int fallbackSkipCount = 0;

    /** 连续拒绝次数（用于退让策略） */
    private int consecutiveRejects = 0;

    public GrabStrategy(ConfigManager config) {
        this.config = config;
    }

    /**
     * 根据评分决定按哪个策略执行抢单
     * @return 如果是REJECT就不抢，其他按对应策略执行
     */
    public Tier decideTier(int score) {
        if (score >= GrabFilterEngine.SCORE_HIGH) {
            consecutiveRejects = 0;
            return Tier.PRIORITY;
        }
        if (score >= GrabFilterEngine.SCORE_MEDIUM) {
            consecutiveRejects = 0;
            return Tier.MEDIUM;
        }
        if (score >= GrabFilterEngine.SCORE_LOW) {
            // 保底策略：连续保底单跳过一些，避免抢低价单太积极
            fallbackSkipCount++;
            if (fallbackSkipCount % 3 == 0) { // 每3个保底单抢1个
                return Tier.FALLBACK;
            }
            consecutiveRejects++;
            return Tier.REJECT;
        }
        consecutiveRejects++;
        return Tier.REJECT;
    }

    /**
     * 计算执行抢单前的延迟（毫秒）
     * 高优：极短延迟 0-50ms
     * 中优：短延迟 100-300ms
     * 保底：长延迟 500-1500ms
     */
    public int getDelay(Tier tier) {
        switch (tier) {
            case PRIORITY:
                return rng.nextInt(50);          // 秒级响应
            case MEDIUM:
                return 100 + rng.nextInt(200);   // 正常响应
            case FALLBACK:
                return 500 + rng.nextInt(1000);  // 保守响应
            default:
                return 0;
        }
    }

    /** 抢单后的冷却时间（毫秒）：高优先单冷却短 */
    public int getCooldown(Tier tier) {
        switch (tier) {
            case PRIORITY: return config.getCooldownSeconds() * 500;  // 一半冷却
            case MEDIUM:   return config.getCooldownSeconds() * 1000; // 正常冷却
            case FALLBACK: return config.getCooldownSeconds() * 1500; // 更长冷却
            default:       return config.getCooldownSeconds() * 1000;
        }
    }

    /** 记录抢单时间 */
    public void onGrab(Tier tier) {
        long now = System.currentTimeMillis();
        lastGrabTime = now;
        if (tier == Tier.FALLBACK) lastFallbackGrabTime = now;
        consecutiveRejects = 0;
        fallbackSkipCount = 0;
    }

    /** 获取当前策略状态描述 */
    public String getStatus() {
        long now = System.currentTimeMillis();
        long sinceLastGrab = now - lastGrabTime;
        if (lastGrabTime == 0) return "等待接单";
        if (sinceLastGrab < 60000) return "刚刚抢单";
        return String.format("%d分钟前", sinceLastGrab / 60000);
    }

    public int getConsecutiveRejects() { return consecutiveRejects; }
}
