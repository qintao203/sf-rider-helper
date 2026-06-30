# 顺丰抢单助手APP 代码审计与专业测试报告

**审计日期**: 2026-06-30  
**项目版本**: v6.0 (Manifest versionCode=55, versionName=5.5.0)  
**包名**: com.sf.riderhelper  
**Min/Target SDK**: 26 / 31  
**源码路径**: ~/sf-rider-app/  
**文件总数**: 22 Java文件, 1 AndroidManifest, 3 资源文件  
**分析深度**: 逐行审查

---

## 目录
1. [严重问题 (CRITICAL)](#1-严重问题-critical)
2. [警告 (WARNING)](#2-警告-warning)
3. [建议 (SUGGESTION)](#3-建议-suggestion)
4. [总结统计](#4-总结统计)

---

## 1. 严重问题 (CRITICAL)

### C-01 [GrabAccessibilityService.java:182] 主线程阻塞 — Thread.sleep 导致 ANR

**位置**: `GrabAccessibilityService.java` 第182行  
**严重性**: 🔴 严重 — 生产环境必现ANR

```java
// 第182行 — 在 handler (主线程Looper) 中调用 Thread.sleep()
try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
```

**问题描述**:  
`grabLoop` Runnable 通过 `handler.post(grabLoop)` 投递。Handler 通过 `new Handler()` 创建（第17行），绑定到**主线程 Looper**。`Thread.sleep(delay)` 直接阻塞主线程：
- 优先抢单: 0-50ms ✓ 安全  
- 中优抢单: 100-300ms ⚠️
- 保底抢单: 500-1500ms 🔴
- 扫描间隔: 800-1600ms 🔴

连续两次保底抢单可使主线程被阻塞长达 **1500+1600=3100ms**。若加上页面解析、数据库写入、TTS播报，综合耗时可超过 **5秒ANR阈值**。

**修复方案**:  
```java
// 方案A：使用 Handler.postDelayed 替代 Thread.sleep
handler.postDelayed(() -> {
    boolean ok = interactor.clickNode(grabBtn);
    if (grabBtn != null) grabBtn.recycle();
    // ...后续处理
}, delay);

// 方案B：在独立线程中运行抢单循环，Handler只做UI交互
```

---

### C-02 [ScreenInteractor.java:98] AccessibilityNodeInfo 内存泄漏

**位置**: `ScreenInteractor.java` 第98行  
**严重性**: 🔴 严重 — 每次搜索泄漏中间节点

```java
// findInSubtree 方法中，找到目标后不回收中间节点
for (int i = 0; i < node.getChildCount(); i++) {
    AccessibilityNodeInfo child = node.getChild(i);
    AccessibilityNodeInfo found = findInSubtree(child, keyword);
    if (found != null) return found;  // ← child 未被回收！
    if (child != null) child.recycle();
}
```

**问题描述**:  
当递归搜索在深度 N 找到目标节点时，路径上的 N-1 个中间父节点全部被跳过回收（因 `return found` 提前退出）。每个未被回收的 `AccessibilityNodeInfo` 对象占用系统资源，服务长时间运行后将耗尽 Binder 事务缓冲区（通常限制在 1MB），导致系统级故障。

**修复方案**:  
```java
private AccessibilityNodeInfo findInSubtree(AccessibilityNodeInfo node, String keyword) {
    if (node == null) return null;
    CharSequence t = node.getText();
    if (t != null && t.toString().contains(keyword)) return node;
    for (int i = 0; i < node.getChildCount(); i++) {
        AccessibilityNodeInfo child = node.getChild(i);
        AccessibilityNodeInfo found = findInSubtree(child, keyword);
        if (found != null) {
            child.recycle();  // ← 关键修复：回收当前子节点
            return found;
        }
        if (child != null) child.recycle();
    }
    return null;
}
```
**注意**: 必须保证返回的节点**不**被回收，留待调用方通过 `grabBtn.recycle()` 统一回收。

---

### C-03 [ScreenInteractor.java:59] `|| true` 掩码导致点击失败无感知

**位置**: `ScreenInteractor.java` 第59行  
**严重性**: 🔴 严重 — 抢单点击失败被静默掩盖

```java
public boolean clickByText(String text) {
    AccessibilityNodeInfo n = findNodeByText(text);
    return n != null && (n.performAction(AccessibilityNodeInfo.ACTION_CLICK) || true);
    //                                                                    ^^^^^^^^
}
```

**问题描述**:  
`|| true` 使返回值**永远为 true**（只要 n 非空），即使 `performAction` 实际返回 false。在 `GrabAccessibilityService.executeSmartGrabCycle()` 中，点击成功与否决定了是否更新统计和数据（第188-227行）。这个 bug 导致所有"表面找到按钮但实际无法点击"的失败都被统计为成功。

**修复方案**:  
```java
public boolean clickByText(String text) {
    AccessibilityNodeInfo n = findNodeByText(text);
    if (n == null) return false;
    boolean result = n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    n.recycle();  // 别忘了回收！
    return result;
}
```

---

### C-04 [GuardService.java:30] 线程泄漏 — 每次 START_STICKY 重启未清理旧线程

**位置**: `GuardService.java` 第30行  
**严重性**: 🔴 严重 — 服务重启导致线程堆积

```java
private Thread monitorThread = new Thread(() -> {
    while (true) {  // ← 无限循环，无中断检查
        try { Thread.sleep(10000); } catch (InterruptedException e) { break; }
        // ...
    }
});
```

**问题描述**:  
1. `monitorThread` 是字段初始化器，在 `GuardService` 构造时创建并立即启动（第26行 `monitorThread.start()` 实则在 `onStartCommand` 中）。
2. 系统通过 `START_STICKY` 重启服务时，新的 GuardService 实例创建**新线程**，**旧线程继续运行**（因为没有中断机制）。
3. 守护线程无法被 GC 回收（无限循环持有强引用），多次重启后线程数无限增长。

**修复方案**:  
```java
private Thread monitorThread;
private volatile boolean running = false;

@Override
public int onStartCommand(Intent intent, int flags, int startId) {
    if (monitorThread == null || !monitorThread.isAlive()) {
        running = true;
        monitorThread = new Thread(() -> {
            int failCount = 0;
            while (running) {
                try { Thread.sleep(10000); } catch (InterruptedException e) { break; }
                // ...
            }
        });
        monitorThread.start();
    }
    return START_STICKY;
}

@Override
public void onDestroy() {
    running = false;
    if (monitorThread != null) monitorThread.interrupt();
    instance = null;
    super.onDestroy();
}
```

---

### C-05 [GrabAccessibilityService.java:114] 前台服务启动无运行时权限检查

**位置**: `GrabAccessibilityService.java` 第114行  
**严重性**: 🔴 严重 — Android 10+ 可能崩溃

```java
// FloatingWindowManager.show() — 无 SYSTEM_ALERT_WINDOW 运行时检查
FloatingWindowManager fwm = new FloatingWindowManager(this);
fwm.show();  // ← 如果用户未授予悬浮窗权限，addView 抛出 BadTokenException
```

虽然外层有 `try-catch`，但**GrabForegroundService 的启动**存在类似问题：Android 12+ 对 `startForegroundService()` 有严格限制，若调用后5秒内未 `startForeground()` 系统会抛 `ForegroundServiceDidNotStartInTimeException`。

此外，`POST_NOTIFICATIONS` (Android 13+) 未做运行时权限检查，通知将被静默丢弃。

**修复方案**:  
```java
// 在展示悬浮窗前检查
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
    // 跳转权限设置页
    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName()));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    return;
}
```

---

## 2. 警告 (WARNING)

### W-01 [AccessibilityService] onAccessibilityEvent 和 onInterrupt 均为空实现

**位置**: `GrabAccessibilityService.java` 第36-39行  
**严重性**: 🟡 警告

无障碍服务声明 `typeAllMask` 但完全不处理事件，完全依赖主动轮询。服务被系统中断时不做任何处理（可能错过状态恢复窗口）。最佳实践应至少处理 `TYPE_WINDOW_STATE_CHANGED` 和 `TYPE_WINDOW_CONTENT_CHANGED` 来触发页面扫描，替代低效轮询。

---

### W-02 [GrabFilterEngine.java:112] `shouldGrab` 字段未与实际抢单逻辑同步

**位置**: `GrabFilterEngine.java` 第111-114行  
**严重性**: 🟡 警告

```java
boolean shouldGrab = score >= SCORE_LOW;
return new FilterResult(Math.round(score), ..., shouldGrab);
```

`FilterResult.shouldGrab` 为 true 时，`GrabStrategy.decideTier()` 仍可能返回 `REJECT`（因 fallback 跳过逻辑）。`shouldGrab` 字段实际上**不可信**，容易误导后续开发。

---

### W-03 [GrabStrategy.java:53] Fallback 跳数逻辑硬编码

**位置**: `GrabStrategy.java` 第53行  
**严重性**: 🟡 警告

```java
if (fallbackSkipCount % 3 == 0) {  // 每3个保底单抢1个
```

跳数阈值硬编码为3，应抽取为可配置参数，让用户控制保底单抢单频率。

---

### W-04 [MainActivity.java:25] Handler 内存泄漏风险

**位置**: `MainActivity.java` 第25、286行  
**严重性**: 🟡 警告

```java
private Handler handler = new Handler();  // 匿名内部类，隐式持有Activity引用
```

`onDestroy()` 虽然调用 `handler.removeCallbacksAndMessages(null)`（第538行），但 Android 不保证 `onDestroy()` 一定会被调用（如进程被杀）。但此问题影响较小（进程中持有该引用的唯一场景）。建议使用静态内部类 + WeakReference。

---

### W-05 [OrderDatabase.java] 数据库操作无异常处理

**位置**: `OrderDatabase.java` 全文件  
**严重性**: 🟡 警告

- `insertOrder()`: SQLiteDatabase.insert() 可返回 -1，未检查
- `getRecent()`: 无 try-catch，数据库损坏时崩溃
- `getStats()`: `rawQuery` 无异常保护

---

### W-06 [SplashActivity.java:129] 加载文字动画在跳转后仍运行

**位置**: `SplashActivity.java` 第129-138行  
**严重性**: 🟡 警告

```java
loading.postDelayed(new Runnable() {
    // 每400ms轮换文字
    loading.postDelayed(this, 400);
}, 400);
```

2500ms 后跳转到 MainActivity 并 `finish()`，但 Runnable 继续在 Handler 队列中存在，直到 `onDestroy()` 被系统调用。在低端设备上可能造成短时内存残留。

---

### W-07 [NotificationHelper.java:37/52] 使用系统默认小图标，无自定义图标

**位置**: `NotificationHelper.java` 第40、55行  
**严重性**: 🟡 警告

```java
.setSmallIcon(android.R.drawable.ic_dialog_info)
```

使用系统默认图标导致通知外观没有辨识度。应为应用提供自定义 notification icon 资源。

---

### W-08 [FloatingWindowManager.java] 悬浮窗无权限运行时检查

**位置**: `FloatingWindowManager.java` 第126行  
**严重性**: 🟡 警告

虽外层有 try-catch 防崩溃，但在 Android 10+ 上若用户未开启悬浮窗权限，`wm.addView()` 抛出的异常被静默捕获，用户无任何反馈。

---

### W-09 [OrderParser.java:63] 距离正则可能误匹配金额

**位置**: `OrderParser.java` 第63行  
**严重性**: 🟡 警告

```java
Pattern distFull = Pattern.compile("(\\d+\\.?\\d*)\\s*(公里|km|KM|千米)");
```

当 `distFull` 未匹配时，降级到通用数字正则 `DISTANCE_PATTERN`（第12行），其中 `< 100` 的过滤阈值（第71行）可能误将金额（如 ¥25.00）识别为距离。

---

### W-10 [GuardService.java:36] 守护线程使用 `getInstance()` 跨进程判断

**位置**: `GuardService.java` 第35行  
**严重性**: 🟡 警告

```java
GrabAccessibilityService grabService = GrabAccessibilityService.getInstance();
if (grabService == null || !grabService.isActive()) {
```

判断主服务是否存活的逻辑：`getInstance()` 返回静态引用，如果服务进程未死但 service 对象被重建（不常见），判断不可靠。应结合 `bindService` 返回值或检查 service 的 `onDestroy` 标志更准确。

---

## 3. 建议 (SUGGESTION)

### S-01 [ThemeEngine.java:100] `dp()` 静态方法接受 View 参数不合理

静态方法 `dp(float n, View v)` 需要 View 实例才能获取 density，造成不必要的依赖。建议改为接受 Context。

---

### S-02 [ChartView.java:97-113] onDraw 中重复创建 RectF 对象

`onDraw()` 每帧创建多个 `RectF`、`Path` 对象，应在构造时预分配或使用对象池。

---

### S-03 [StatsActivity.java:9] 未使用的 import

```java
import android.view.ViewGroup;  // 未使用
```

---

### S-04 [ThemeEngine.java:4] 未使用的 import

```java
import android.animation.Animator;  // 未使用
```

---

### S-05 [MainActivity.java:509] `makeFullScreen()` 缩进不一致

第509行方法声明缩进与项目其他方法不一致（多一个 tab），虽不影响编译但降低代码可读性。

---

### S-06 [SplashActivity.java:201] `dp()` 方法缩进不一致

与方法内其他代码缩进不同。

---

### S-07 [OrderDatabase.java:111-127] SQL 字符串拼接

```java
db.rawQuery("SELECT COUNT(*) FROM " + TABLE + " WHERE result='success'", null);
```

虽然 TABLE 是常量无注入风险，但最佳实践仍应使用参数化查询。

---

### S-08 [ConfigManager.java:34-44] StringSet 含空字符串风险

`setPreferredDirections()` 和 `setExcludedDirections()` 用 `split(",")` 分割 CSV，若输入为 `"科技园,,福田"`，空字符串会被加入 Set。建议过滤空串：

```java
public void setPreferredDirections(String csv) {
    Set<String> set = new HashSet<>();
    for (String s : csv.split(",")) {
        if (!s.trim().isEmpty()) set.add(s.trim());
    }
    sp.edit().putStringSet("pref_dirs", set).apply();
}
```

---

### S-09 [GrabAccessibilityService.java:67] `setPaused()` 非线程安全

```java
public void setPaused(boolean p) { paused = p; }  // 非 volatile
```

`paused` 变量被 `grabLoop` 线程（Handler 主线程）读取，和外部调用线程写入。应声明为 `volatile` 或使用原子类。

---

### S-10 [OrderInfo.java] 数据模型未在代码中实际使用

`OrderInfo.java` 定义了 `OrderInfo` 类但未被任何其他文件引用。项目中实际使用的数据模型为 `OrderParser.ParsedOrder` 和 `OrderDatabase.OrderRecord`。`OrderInfo` 可能是旧版遗留代码。

---

### S-11 [AndroidManifest.xml] 版本号不一致

`android:versionCode="55"` + `android:versionName="5.5.0"`，但 SplashActivity 和 MainActivity 的 UI 中显示 "v6.0 · 霓虹版"，两个版本号不一致。

---

### S-12 [GrabForegroundService.java:23] 前台服务类型声明

`android:foregroundServiceType="dataSync"` 但前台服务主要作用是维持进程存活，没有实际的数据同步功能。更适合使用 `specialUse` 或 `shortService`（Android 12+）。但当前声明能通过编译。

---

## 4. 总结统计

| 等级 | 数量 | 关键风险 |
|------|------|----------|
| 🔴 严重 (Critical) | 5 | 主线程ANR、AccessibilityNodeInfo泄漏、点击掩码、线程泄漏、悬浮窗权限 |
| 🟡 警告 (Warning) | 10 | Handler泄漏、事件空实现、数据库异常、逻辑硬编码、SQL拼接等 |
| 💡 建议 (Suggestion) | 12 | 代码风格、未使用导入、变量可见性、版本号不一致等 |
| **总计** | **27项** | |

### 风险评级

| 风险维度 | 评级 | 说明 |
|----------|------|------|
| **运行时稳定性** | ⚠️ 高 | C-01 ANR 为必现问题，C-02 长期运行后必导致系统异常 |
| **功能正确性** | ⚠️ 高 | C-03 点击失败被掩盖，抢单成功率统计完全不可信 |
| **资源管理** | ⚠️ 高 | C-02/C-04 分别导致 Binder 泄漏和线程泄漏 |
| **数据完整性** | 🟡 中 | DB异常无保护，统计数据可能不准确 |
| **UI/UX** | 🟢 低 | 主要是优化建议，无可视化崩溃 |
| **安全合规** | 🟢 低 | 本地辅助工具，仅有 SQL 拼接风险（可控） |

### 按文件统计问题分布

| 文件 | 严重 | 警告 | 建议 | 总分 |
|------|------|------|------|------|
| ScreenInteractor.java | 2 | 0 | 0 | ⚠️⚠️ |
| GrabAccessibilityService.java | 1 | 1 | 1 | ⚠️⚠️ |
| GuardService.java | 1 | 1 | 0 | ⚠️⚠️ |
| FloatingWindowManager.java | 1 | 1 | 0 | ⚠️⚠️ |
| MainActivity.java | 0 | 1 | 1 | 🟡 |
| GrabFilterEngine.java | 0 | 1 | 0 | 🟡 |
| GrabStrategy.java | 0 | 1 | 0 | 🟡 |
| OrderDatabase.java | 0 | 1 | 1 | 🟡 |
| SplashActivity.java | 0 | 1 | 1 | 🟡 |
| NotificationHelper.java | 0 | 1 | 0 | 🟡 |
| OrderParser.java | 0 | 1 | 0 | 🟡 |
| ConfigManager.java | 0 | 0 | 1 | 💡 |
| ChartView.java | 0 | 0 | 1 | 💡 |
| ThemeEngine.java | 0 | 0 | 2 | 💡 |
| StatsActivity.java | 0 | 0 | 1 | 💡 |
| OrderInfo.java | 0 | 0 | 1 | 💡 |
| AndroidManifest.xml | 0 | 0 | 1 | 💡 |
| GrabForegroundService.java | 0 | 0 | 1 | 💡 |

---

*审计完成于 2026-06-30。所有问题均基于最小 SDK 26、Target SDK 31 的环境评估。建议修复C-01至C-05后发版测试。*
