package com.sf.riderhelper;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/**
 * 完整骑手数据层
 * 管理订单、用户、收入等核心业务数据
 */
public class RiderDataManager {

    private static RiderDataManager instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<OrderData> orders = new ArrayList<>();
    private RiderProfile profile;
    private DataListener listener;

    // ========== 单例 ==========
    public static synchronized RiderDataManager getInstance() {
        if (instance == null) instance = new RiderDataManager();
        return instance;
    }

    private RiderDataManager() {
        profile = new RiderProfile();
        loadMockData(); // TODO: 替换为真实API数据
    }

    // ========== 数据模型 ==========

    /** 完整订单模型 */
    public static class OrderData {
        public String id;
        public String orderNo;           // 订单号
        public long createTime;
        public long acceptTime;
        public long pickupTime;
        public long deliverTime;
        public int status;               // 0:待抢 1:已抢 2:已取货 3:配送中 4:已完成 5:已取消
        public String storeName;         // 商家名
        public String storeAddress;      // 商家地址
        public String storePhone;        // 商家电话
        public double storeLat;          // 商家纬度
        public double storeLng;          // 商家经度
        public String customerName;      // 顾客名
        public String customerAddress;   // 顾客地址
        public String customerPhone;     // 顾客电话
        public double customerLat;       // 顾客纬度
        public double customerLng;       // 顾客经度
        public float price;              // 配送费
        public float distance;           // 配送距离(km)
        public float weight;             // 重量(kg)
        public String category;          // 品类: 餐饮/商超/文件等
        public String note;              // 备注
        public String strategy;          // 抢单策略
        public int score;                // 评分
        public String grabResult;        // 抢单结果

        public String getStatusText() {
            switch (status) {
                case 0: return "待抢单";
                case 1: return "已接单";
                case 2: return "已取货";
                case 3: return "配送中";
                case 4: return "已完成";
                case 5: return "已取消";
                default: return "未知";
            }
        }

        public String getTimeAgo() {
            long diff = System.currentTimeMillis() - createTime;
            long min = diff / 60000;
            if (min < 1) return "刚刚";
            if (min < 60) return min + "分钟前";
            long hour = min / 60;
            if (hour < 24) return hour + "小时前";
            return (hour / 24) + "天前";
        }
    }

    /** 骑手资料 */
    public static class RiderProfile {
        public String name = "骑手";
        public String phone = "138****0000";
        public String avatar = "";
        public int level = 1;                // 等级
        public float rating = 4.8f;          // 评分
        public int completedOrders = 0;      // 完成单数
        public float todayIncome = 0;        // 今日收入
        public float weekIncome = 0;         // 本周收入
        public float monthIncome = 0;        // 本月收入
        public int onlineHours = 0;          // 在线时长(小时)
        public float totalDistance = 0;      // 总里程(km)
    }

    // ========== 数据管理 ==========

    public interface DataListener {
        void onOrdersUpdated(List<OrderData> orders);
        void onProfileUpdated(RiderProfile profile);
        void onNewOrder(OrderData order);
        void onOrderStatusChanged(String orderId, int newStatus);
    }

    public void setListener(DataListener l) { this.listener = l; }

    /** 获取订单列表 */
    public List<OrderData> getOrders() { return new ArrayList<>(orders); }

    /** 获取订单（按状态过滤） */
    public List<OrderData> getOrdersByStatus(int status) {
        List<OrderData> result = new ArrayList<>();
        for (OrderData o : orders) if (o.status == status) result.add(o);
        return result;
    }

    /** 获取骑手资料 */
    public RiderProfile getProfile() { return profile; }

    /** 添加新订单 */
    public void addOrder(OrderData order) {
        orders.add(0, order);
        notifyOrders();
        if (listener != null) listener.onNewOrder(order);
    }

    /** 更新订单状态 */
    public void updateOrderStatus(String orderId, int newStatus) {
        for (OrderData o : orders) {
            if (o.id.equals(orderId)) {
                o.status = newStatus;
                notifyOrders();
                if (listener != null) listener.onOrderStatusChanged(orderId, newStatus);
                return;
            }
        }
    }

    /** 获取统计数据 */
    public Object[] getStats() {
        int total = orders.size();
        int completed = 0;
        int cancelled = 0;
        float income = 0;
        for (OrderData o : orders) {
            if (o.status == 4) { completed++; income += o.price; }
            if (o.status == 5) cancelled++;
        }
        return new Object[]{total, completed, cancelled, income};
    }

    /** 通知监听器 */
    private void notifyOrders() {
        if (listener != null) {
            mainHandler.post(() -> listener.onOrdersUpdated(getOrders()));
        }
    }

    // ========== 模拟数据 ==========

    private void loadMockData() {
        String[][] mockOrders = {
            {"ORD001", "老娘舅", "科技园A座1楼", "科技园B座8楼", "25.50", "1.2", "0.8", "餐饮", "不要辣"},
            {"ORD002", "麦当劳", "深南大道100号", "科技园C座3楼", "18.00", "0.8", "1.2", "餐饮", "尽快"},
            {"ORD003", "华润万家", "科技路88号", "科苑路15号", "32.00", "2.5", "5.0", "商超", "重物"},
            {"ORD004", "星巴克", "高新南一道2号", "科技中一路6号", "15.00", "0.5", "0.3", "餐饮", ""},
            {"ORD005", "必胜客", "深南大道200号", "科技园D栋12楼", "38.00", "1.8", "1.5", "餐饮", "电话联系"},
            {"ORD006", "文件快送", "科技园E座", "福田中心区", "45.00", "5.0", "0.5", "文件", "重要文件"},
            {"ORD007", "永辉超市", "科技中二路", "科技园南区", "28.00", "1.5", "3.0", "商超", ""},
            {"ORD008", "KFC", "科技路200号", "科技园F座", "22.00", "0.6", "0.6", "餐饮", "不要饮料"},
            {"ORD009", "水果先生", "科苑路8号", "高新南一道", "20.00", "1.0", "2.0", "商超", ""},
            {"ORD010", "绝味鸭脖", "科技园商业街", "科技园宿舍区", "16.00", "0.3", "0.4", "餐饮", ""},
        };

        for (int i = 0; i < mockOrders.length; i++) {
            String[] m = mockOrders[i];
            OrderData o = new OrderData();
            o.id = "order_" + (i + 1);
            o.orderNo = m[0];
            o.storeName = m[1];
            o.storeAddress = m[2];
            o.customerAddress = m[3];
            o.price = Float.parseFloat(m[4]);
            o.distance = Float.parseFloat(m[5]);
            o.weight = Float.parseFloat(m[6]);
            o.category = m[7];
            o.note = m[8];
            o.createTime = System.currentTimeMillis() - (i * 120000); // 每2分钟一个
            o.status = i < 3 ? 0 : (i < 6 ? 1 : (i < 8 ? 2 : (i < 9 ? 3 : 4))); // 各种状态
            o.strategy = i % 3 == 0 ? "高优" : (i % 3 == 1 ? "中优" : "保底");
            o.score = (int)(60 + Math.random() * 40);
            o.grabResult = o.status > 0 ? "已抢" : "";
            orders.add(o);
        }

        // 骑手资料
        profile.completedOrders = 156;
        profile.todayIncome = 186.5f;
        profile.weekIncome = 1250.0f;
        profile.monthIncome = 5280.0f;
        profile.onlineHours = 8;
        profile.totalDistance = 45.5f;
        profile.rating = 4.9f;
        profile.level = 3;
    }
}
