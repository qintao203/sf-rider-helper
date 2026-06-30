package com.sf.riderhelper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import java.util.List;

/**
 * 顺丰同城骑士APP 深度集成桥接
 * 闭环联动：启动+检测+同屏+状态感知
 */
public class SFRiderBridge {

    private static final String TAG = "SFRiderBridge";

    // 顺丰同城骑士可能的包名（按可能性排序）
    private static final String[] SF_PACKAGES = {
        "com.sf.sfexpress",          // 顺丰速运
        "com.sf.rider",              // 顺丰同城骑士
        "com.sf.ridertc",            // 顺丰同城
        "com.sf.st",                 // 顺丰科技
        "com.sf.express",
        "com.sf.city",
        "com.sf.sf Rider",
    };

    private static String cachedPackage = null;

    /** 检测顺丰同城骑士APP包名（带缓存） */
    public static String findSFApp(Context ctx) {
        if (cachedPackage != null) return cachedPackage;
        PackageManager pm = ctx.getPackageManager();
        for (String pkg : SF_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                cachedPackage = pkg;
                Log.d(TAG, "Found SF app: " + pkg);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return null;
    }

    /** 是否已安装 */
    public static boolean isInstalled(Context ctx) {
        return findSFApp(ctx) != null;
    }

    /** 获取APP名称 */
    public static String getAppName(Context ctx) {
        String pkg = findSFApp(ctx);
        if (pkg == null) return "顺丰同城骑士";
        try {
            PackageManager pm = ctx.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) { return "顺丰同城骑士"; }
    }

    /** 一键启动顺丰同城骑士 */
    public static boolean launchSFApp(Context ctx) {
        String pkg = findSFApp(ctx);
        if (pkg == null) return false;
        try {
            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                ctx.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Launch failed", e);
        }
        return false;
    }

    /** 检测顺丰同城是否正在前台运行 */
    public static boolean isInForeground(Context ctx) {
        String pkg = findSFApp(ctx);
        if (pkg == null) return false;
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return false;
            List<ActivityManager.AppTask> tasks = am.getAppTasks();
            // 简化检测：检查最近任务
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                List<ActivityManager.AppTask> taskList = am.getAppTasks();
                for (ActivityManager.AppTask task : taskList) {
                    Intent baseIntent = task.getTaskInfo().baseIntent;
                    if (baseIntent != null && pkg.equals(baseIntent.getPackage())) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /** 前往应用商店/官网下载 */
    public static boolean openMarket(Context ctx) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.sf.sfexpress"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
            return true;
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.sf-cityrush.com.cn"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                return true;
            } catch (Exception e2) { return false; }
        }
    }

    /** 清空包名缓存（安装/卸载后调用） */
    public static void clearCache() { cachedPackage = null; }
}
