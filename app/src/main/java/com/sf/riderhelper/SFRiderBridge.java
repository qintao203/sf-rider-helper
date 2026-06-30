package com.sf.riderhelper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import java.util.List;

/**
 * 顺丰同城骑士APP集成桥接
 * 负责启动/检测/跳转顺丰同城应用
 */
public class SFRiderBridge {

    private static final String TAG = "SFRiderBridge";

    // 顺丰同城骑士可能的包名列表
    private static final String[] SF_PACKAGES = {
        "com.sf.sfexpress",          // 顺丰速运
        "com.sf.rider",              // 顺丰同城骑士
        "com.sf.ridertc",            // 顺丰同城
        "com.sf.sf Rider",
        "com.sf.st",                 // 顺丰科技
        "com.sf.express",
        "com.sf.city",
    };

    /** 检测顺丰同城骑士是否已安装，返回包名 */
    public static String findSFApp(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        for (String pkg : SF_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                Log.d(TAG, "Found SF app: " + pkg);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return null;
    }

    /** 检查是否已安装 */
    public static boolean isInstalled(Context ctx) {
        return findSFApp(ctx) != null;
    }

    /** 启动顺丰同城骑士APP */
    public static boolean launchSFApp(Context ctx) {
        String pkg = findSFApp(ctx);
        if (pkg == null) return false;

        try {
            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                Log.d(TAG, "Launched SF app: " + pkg);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch SF app", e);
        }
        return false;
    }

    /** 启动并建议分屏模式（只对Android 7+有效） */
    public static boolean launchSplitScreen(Context ctx) {
        if (android.os.Build.VERSION.SDK_INT < 24) return false;

        String pkg = findSFApp(ctx);
        if (pkg == null) return false;

        try {
            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // 提示分屏（系统层控制，无法强制分屏）
                ctx.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Split screen launch failed", e);
        }
        return false;
    }

    /** 获取顺丰同城APP名称 */
    public static String getAppName(Context ctx) {
        String pkg = findSFApp(ctx);
        if (pkg == null) return "顺丰同城骑士";

        try {
            PackageManager pm = ctx.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return "顺丰同城骑士";
        }
    }

    /** 前往应用商店下载 */
    public static boolean openMarket(Context ctx) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.sf.sfexpress"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
            return true;
        } catch (Exception e) {
            // 没有应用商店，打开浏览器
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.sf-cityrush.com.cn"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
}
