package com.sf.riderhelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单历史数据库 (SQLite)
 * 记录每笔抢单的详细信息
 */
public class OrderDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "sf_orders.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "orders";

    private static final String COL_ID = "_id";
    private static final String COL_TIME = "ts";
    private static final String COL_PRICE = "price";
    private static final String COL_DISTANCE = "distance";
    private static final String COL_DIRECTION = "direction";
    private static final String COL_STORE = "store";
    private static final String COL_RESULT = "result"; // success/fail/skip
    private static final String COL_STRATEGY = "strategy"; // 高优/中优/保底
    private static final String COL_SCORE = "score";
    private static final String COL_REASON = "reason";

    private static OrderDatabase instance;

    private OrderDatabase(Context ctx) {
        super(ctx.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    public static synchronized OrderDatabase getInstance(Context ctx) {
        if (instance == null) instance = new OrderDatabase(ctx);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_TIME + " INTEGER," +
                COL_PRICE + " REAL," +
                COL_DISTANCE + " REAL," +
                COL_DIRECTION + " TEXT," +
                COL_STORE + " TEXT," +
                COL_RESULT + " TEXT," +
                COL_STRATEGY + " TEXT," +
                COL_SCORE + " INTEGER," +
                COL_REASON + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    /** 插入一条订单记录 */
    public long insertOrder(long ts, float price, float distance, String direction,
                            String store, String result, String strategy, int score, String reason) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TIME, ts);
        cv.put(COL_PRICE, price);
        cv.put(COL_DISTANCE, distance);
        cv.put(COL_DIRECTION, direction);
        cv.put(COL_STORE, store);
        cv.put(COL_RESULT, result);
        cv.put(COL_STRATEGY, strategy);
        cv.put(COL_SCORE, score);
        cv.put(COL_REASON, reason);
        return db.insert(TABLE, null, cv);
    }

    /** 获取最近N条记录 */
    public List<OrderRecord> getRecent(int limit) {
        List<OrderRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE, null, null, null, null, null,
                COL_ID + " DESC", String.valueOf(limit));
        while (c.moveToNext()) {
            OrderRecord r = new OrderRecord();
            r.id = c.getLong(0);
            r.timestamp = c.getLong(1);
            r.price = c.getFloat(2);
            r.distance = c.getFloat(3);
            r.direction = c.getString(4);
            r.store = c.getString(5);
            r.result = c.getString(6);
            r.strategy = c.getString(7);
            r.score = c.getInt(8);
            r.reason = c.getString(9);
            list.add(r);
        }
        c.close();
        return list;
    }

    /** 获取统计数据 */
    public Stats getStats() {
        SQLiteDatabase db = getReadableDatabase();
        Stats s = new Stats();

        Cursor c = db.rawQuery("SELECT COUNT(*), SUM(price) FROM " + TABLE +
                " WHERE result='success'", null);
        if (c.moveToFirst()) { s.totalGrabbed = c.getInt(0); s.totalIncome = c.getFloat(1); }
        c.close();

        c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE + " WHERE result='fail'", null);
        if (c.moveToFirst()) s.totalFailed = c.getInt(0);
        c.close();

        c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE + " WHERE result='skip'", null);
        if (c.moveToFirst()) s.totalSkipped = c.getInt(0);
        c.close();

        c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE, null);
        if (c.moveToFirst()) s.total = c.getInt(0);
        c.close();

        return s;
    }

    /** 清除所有数据 */
    public void clearAll() {
        getWritableDatabase().execSQL("DELETE FROM " + TABLE);
    }

    /** 订单记录模型 */
    public static class OrderRecord {
        public long id;
        public long timestamp;
        public float price;
        public float distance;
        public String direction = "";
        public String store = "";
        public String result = "";
        public String strategy = "";
        public int score;
        public String reason = "";

        public boolean isSuccess() { return "success".equals(result); }

        public String formatTime() {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss",
                    java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(timestamp));
        }
    }

    /** 统计数据 */
    public static class Stats {
        public int total;
        public int totalGrabbed;
        public int totalFailed;
        public int totalSkipped;
        public float totalIncome;
    }
}
