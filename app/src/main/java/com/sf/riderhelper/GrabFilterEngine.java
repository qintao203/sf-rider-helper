package com.sf.riderhelper;

import java.util.Set;

/**
 * 多维过滤评分引擎：对订单进行综合评分，决定是否抢单
 */
public class GrabFilterEngine {

    public static final int SCORE_PERFECT = 100;
    public static final int SCORE_HIGH = 80;
    public static final int SCORE_MEDIUM = 50;
    public static final int SCORE_LOW = 20;
    public static final int SCORE_REJECT = 0;

    private final ConfigManager config;

    public GrabFilterEngine(ConfigManager config) {
        this.config = config;
    }

    /**
     * 对订单评分（0-100），同时给出是否应该抢的建议
     */
    public FilterResult evaluate(OrderParser.ParsedOrder order) {
        if (order == null || !order.isValid()) {
            return new FilterResult(SCORE_REJECT, "无效订单", false);
        }

        float score = 100;
        StringBuilder reasons = new StringBuilder();

        // 1. 金额检查 (权重40%)
        float minPrice = config.getMinPrice();
        float maxPrice = config.getMaxPrice();
        if (order.price < minPrice) {
            score -= 60;
            reasons.append("低于最低金额¥").append((int)minPrice).append(" ");
        } else if (order.price > maxPrice) {
            score -= 20; // 太高反而可能有问题
            reasons.append("高于最高金额¥").append((int)maxPrice).append(" ");
        } else if (order.price >= 30) {
            score += 10; // 优质单加分
        } else if (order.price >= 20) {
            score += 5;
        }

        // 2. 距离检查 (权重25%)
        float maxDist = config.getMaxDistance();
        if (order.distance > 0 && order.distance > maxDist) {
            score -= 35;
            reasons.append("超过最大距离").append(maxDist).append("km ");
        } else if (order.distance > 0 && order.distance <= 1) {
            score += 8; // 近距离加分
        }

        // 3. 方向检查 (权重20%)
        String direction = order.direction;
        if (!direction.isEmpty()) {
            Set<String> preferred = config.getPreferredDirections();
            Set<String> excluded = config.getExcludedDirections();

            boolean isPreferred = false;
            for (String pref : preferred) {
                if (direction.contains(pref) || pref.contains(direction)) {
                    isPreferred = true;
                    break;
                }
            }
            boolean isExcluded = false;
            for (String excl : excluded) {
                if (direction.contains(excl) || excl.contains(direction)) {
                    isExcluded = true;
                    break;
                }
            }

            if (isExcluded) {
                score -= 30;
                reasons.append("排除方向 ");
            } else if (isPreferred) {
                score += 15;
                reasons.append("优先方向+ ");
            }
        }

        // 4. 重量/超重惩罚 (权重10%)
        if (order.hasOverweightFee) {
            score -= 15;
            reasons.append("含超重费 ");
        }
        if (order.weight > 10) {
            score -= 10;
            reasons.append("超重");
        }

        // 5. 排除低价+远距离组合
        if (order.price < 15 && order.distance > 3) {
            score -= 20;
            reasons.append("低价远距 ");
        }

        // 6. 保底：如果能赚到钱就至少给基础分
        if (order.price >= config.getMinPrice() && score < SCORE_LOW) {
            score = SCORE_LOW;
        }

        // 确保范围
        score = Math.max(0, Math.min(100, score));

        boolean shouldGrab = score >= SCORE_LOW;
        String level = getLevelText(score);

        return new FilterResult(Math.round(score), level + " " + reasons.toString().trim(), shouldGrab);
    }

    /** 获取三级策略对应的最低分 */
    public static int getThresholdForStrategy(String strategy) {
        switch (strategy) {
            case "高优": return SCORE_HIGH;
            case "中优": return SCORE_MEDIUM;
            case "保底": return SCORE_LOW;
            default: return SCORE_MEDIUM;
        }
    }

    private String getLevelText(float score) {
        if (score >= SCORE_HIGH) return "[高优]";
        if (score >= SCORE_MEDIUM) return "[中优]";
        if (score >= SCORE_LOW) return "[保底]";
        return "";
    }

    /** 过滤结果 */
    public static class FilterResult {
        public final int score;
        public final String reason;
        public final boolean shouldGrab;

        public FilterResult(int score, String reason, boolean shouldGrab) {
            this.score = score;
            this.reason = reason;
            this.shouldGrab = shouldGrab;
        }

        public String getStrategyName() {
            if (score >= SCORE_HIGH) return "高优";
            if (score >= SCORE_MEDIUM) return "中优";
            if (score >= SCORE_LOW) return "保底";
            return "拒绝";
        }

        @Override
        public String toString() {
            return String.format("[%s] %d分 %s", getStrategyName(), score, reason);
        }
    }
}
