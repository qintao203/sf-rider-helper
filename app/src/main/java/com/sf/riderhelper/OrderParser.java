package com.sf.riderhelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能订单解析器：从顺丰页面文本中提取金额/距离/方向等关键信息
 */
public class OrderParser {

    private static final Pattern PRICE_PATTERN = Pattern.compile("[¥￥]?\\s*(\\d+\\.?\\d*)\\s*元?");
    private static final Pattern DISTANCE_PATTERN = Pattern.compile("(\\d+\\.?\\d*)\\s*k?m?");
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d+\\.?\\d*)\\s*(kg|公斤|斤|千克)");

    // 常见方向/区域关键词
    private static final String[][] DIRECTION_MAP = {
        {"科技园", "南山科技园", "高新区"},
        {"福田", "福田区", "华强北"},
        {"南山", "南山区", "蛇口", "前海", "后海"},
        {"罗湖", "罗湖区", "东门"},
        {"宝安", "宝安区", "西乡", "沙井", "福永"},
        {"龙华", "龙华区", "民治"},
        {"龙岗", "龙岗区", "坂田", "布吉", "横岗"},
        {"光明", "光明区"},
        {"坪山", "坪山区"},
        {"盐田", "盐田区"},
    };

    public static class ParsedOrder {
        public float price = 0;
        public float distance = 0;
        public float weight = 0;
        public String direction = "";
        public String storeName = "";
        public String pickupAddr = "";
        public String deliveryAddr = "";
        public String rawText = "";
        public boolean hasOverweightFee = false;

        public boolean isValid() { return price > 0; }

        @Override
        public String toString() {
            return String.format("¥%.1f %.1fkm %s", price, distance, direction);
        }
    }

    /** 解析页面文本，返回结构化订单 */
    public static ParsedOrder parse(String pageText) {
        if (pageText == null || pageText.trim().isEmpty()) return new ParsedOrder();

        ParsedOrder order = new ParsedOrder();
        order.rawText = pageText.length() > 200 ? pageText.substring(0, 200) : pageText;

        // 1. 提取金额：找数字+元 或 ¥+数字
        Matcher pm = PRICE_PATTERN.matcher(pageText);
        if (pm.find()) {
            order.price = Float.parseFloat(pm.group(1));
        }

        // 2. 提取距离
        // 找 "数字km" 或 "数字公里" 或 距离关键词附近的数字
        Pattern distFull = Pattern.compile("(\\d+\\.?\\d*)\\s*(公里|km|KM|千米)");
        Matcher dm = distFull.matcher(pageText);
        if (dm.find()) {
            order.distance = Float.parseFloat(dm.group(1));
        } else {
            Matcher dm2 = DISTANCE_PATTERN.matcher(pageText);
            if (dm2.find()) {
                float val = Float.parseFloat(dm2.group(1));
                if (val < 100) order.distance = val; // 避免把金额误识别为距离
            }
        }

        // 3. 提取重量
        Matcher wm = WEIGHT_PATTERN.matcher(pageText);
        if (wm.find()) {
            order.weight = Float.parseFloat(wm.group(1));
            if (wm.group(2).contains("斤")) order.weight *= 0.5f; // 斤→kg
        }

        // 4. 检测超重费（关键词）
        order.hasOverweightFee = pageText.contains("超重") || 
                                 pageText.contains("大件") || 
                                 pageText.contains("超体积");

        // 5. 提取方向：扫描已知区域
        for (String[] dirGroup : DIRECTION_MAP) {
            for (String area : dirGroup) {
                if (pageText.contains(area)) {
                    order.direction = dirGroup[0]; // 用第一个作为标准名
                    break;
                }
            }
            if (!order.direction.isEmpty()) break;
        }

        // 6. 提取商家名（通常在金额附近的第一行非数字文字）
        String[] lines = pageText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 1 && line.length() < 30 && !line.matches(".*\\d.*")) {
                if (order.storeName.isEmpty() && !line.contains("¥") && !line.contains("元")) {
                    order.storeName = line;
                }
            }
        }

        return order;
    }

    /** 估算配送收入等级 */
    public static String getPriceLevel(float price) {
        if (price >= 30) return "优质";
        if (price >= 20) return "良好";
        if (price >= 12) return "普通";
        return "低价";
    }
}
