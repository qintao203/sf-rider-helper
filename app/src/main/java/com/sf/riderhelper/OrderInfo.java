package com.sf.riderhelper;

public class OrderInfo {
    public String id;
    public String title;
    public float price;
    public float distance;
    public String direction;
    public long timestamp;

    public OrderInfo() {}

    public OrderInfo(String id, String title, float price, float distance, String direction) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.distance = distance;
        this.direction = direction;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isValid() {
        return id != null && !id.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("¥%.2f %s %s", price, direction, title);
    }
}
