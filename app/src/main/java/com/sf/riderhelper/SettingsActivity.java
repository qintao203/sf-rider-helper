package com.sf.riderhelper;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private ConfigManager config;
    private EditText etMin, etMax, etDist, etPref, etExcl, etScan, etDelay, etCool, etScore, etDndStart, etDndEnd;
    private CheckBox cbVib, cbNot, cbTts, cbKeep, cbPower, cbFloat, cbDnd;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        makeFullScreen();
        config = ConfigManager.getInstance(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeEngine.BG_DARK);

        // ========== 顶部导航 ==========
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(12), dp(16), dp(12));
        header.setBackgroundColor(ThemeEngine.BG_TOPBAR);

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(ThemeEngine.TEXT_SECONDARY);
        back.setTextSize(18);
        back.setBackground(null);
        back.setPadding(dp(12), dp(4), dp(12), dp(4));
        back.setOnClickListener(v -> finish());
        header.addView(back);

        // 霓虹装饰点
        View accent = new View(this);
        accent.setBackground(ThemeEngine.roundedBg(ThemeEngine.NEON_CYAN, dp(4)));
        accent.setLayoutParams(new LinearLayout.LayoutParams(dp(4), dp(4)));
        ((LinearLayout.LayoutParams)accent.getLayoutParams()).setMargins(dp(4), 0, dp(8), 0);
        header.addView(accent);

        TextView title = new TextView(this);
        title.setText("设置");
        title.setTextColor(ThemeEngine.TEXT_PRIMARY);
        title.setTextSize(17);
        title.setTypeface(null, 1);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(title);

        root.addView(header);

        // ========== 内容区 ==========
        ScrollView sv = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(24));

        content.addView(makeSection("💰 金额过滤"));
        etMin = makeSettingRow(content, "最低金额", "10.00", "元");
        etMax = makeSettingRow(content, "最高金额", "200.00", "元");

        content.addView(makeSection("📍 距离限制"));
        etDist = makeSettingRow(content, "最大距离", "5.0", "km");

        content.addView(makeSection("🧭 方向偏好"));
        etPref = makeTextRow(content, "优先方向", "科技园,福田,南山");
        etExcl = makeTextRow(content, "排除方向", "龙岗,坪山");

        content.addView(makeSection("⚡ 抢单行为"));
        etScan = makeSettingRow(content, "扫描间隔", "2000", "ms");
        etDelay = makeSettingRow(content, "点击延迟", "100", "ms");
        etCool = makeSettingRow(content, "失败冷却", "30", "秒");

        content.addView(makeSection("🎯 抢单策略"));
        content.addView(makeStrategySelector(content));
        etScore = makeSettingRow(content, "最低评分", "20", "分");

        content.addView(makeSection("🔔 提醒设置"));
        cbVib = makeToggleRow(content, "抢单震动", true);
        cbNot = makeToggleRow(content, "抢单通知", true);
        cbTts = makeToggleRow(content, "语音播报", true);
        cbKeep = makeToggleRow(content, "屏幕常亮", true);

        content.addView(makeSection("🔋 省电与后台"));
        cbPower = makeToggleRow(content, "省电模式", false);
        cbFloat = makeToggleRow(content, "悬浮球", false);

        content.addView(makeSection("🌙 勿扰时段"));
        cbDnd = makeToggleRow(content, "启用勿扰", false);
        etDndStart = makeSettingRow(content, "开始时间", "23", "时");
        etDndEnd = makeSettingRow(content, "结束时间", "6", "时");

        content.addView(makeSection("📋 配置场景"));
        content.addView(makeSceneSelector(content));

        // ========== 保存按钮 ==========
        Button save = new Button(this);
        save.setText("💾 保存配置");
        save.setTextColor(0xFF0A0A14);
        save.setTextSize(15);
        save.setTypeface(null, 1);
        save.setBackground(ThemeEngine.diagonalGradient(
                new int[]{ThemeEngine.NEON_CYAN, 0xFF00B8D4}, ThemeEngine.RADIUS_LARGE));
        save.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(48)));
        ((LinearLayout.LayoutParams)save.getLayoutParams()).setMargins(0, dp(24), 0, 0);
        save.setElevation(dp(3));
        save.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
            else if (event.getAction() == MotionEvent.ACTION_UP ||
                     event.getAction() == MotionEvent.ACTION_CANCEL)
                v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
            return false;
        });
        save.setOnClickListener(v -> doSave());
        content.addView(save);

        sv.addView(content);
        root.addView(sv);

        setContentView(root);
        loadValues();
    }

    // ==================== UI组件工厂 ====================

    private TextView makeSection(String label) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setGravity(Gravity.CENTER_VERTICAL);
        wrap.setPadding(dp(4), dp(20), dp(4), dp(6));

        View dot = new View(this);
        dot.setBackground(ThemeEngine.roundedBg(ThemeEngine.NEON_CYAN, dp(3)));
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(6), dp(16)));
        ((LinearLayout.LayoutParams)dot.getLayoutParams()).setMargins(0, 0, dp(8), 0);
        wrap.addView(dot);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(ThemeEngine.NEON_CYAN);
        tv.setTextSize(12);
        tv.setLetterSpacing(0.05f);
        tv.setTypeface(null, 1);
        wrap.addView(tv);

        return tv;
    }

    private EditText makeSettingRow(LinearLayout parent, String label, String def, String unit) {
        LinearLayout row = glassRow();

        TextView lbl = makeRowLabel(label);
        row.addView(lbl);

        EditText et = makeRowEdit(def);
        row.addView(et);

        TextView unitLbl = new TextView(this);
        unitLbl.setText(unit);
        unitLbl.setTextColor(ThemeEngine.TEXT_DISABLED);
        unitLbl.setTextSize(12);
        unitLbl.setPadding(dp(8), 0, 0, 0);
        unitLbl.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(unitLbl);

        parent.addView(row);
        return et;
    }

    private EditText makeTextRow(LinearLayout parent, String label, String hint) {
        LinearLayout row = glassRow();

        TextView lbl = makeRowLabel(label);
        row.addView(lbl);

        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextColor(ThemeEngine.TEXT_PRIMARY);
        et.setHintTextColor(ThemeEngine.TEXT_MUTED);
        et.setTextSize(13);
        et.setBackground(ThemeEngine.roundedBg(ThemeEngine.BG_CARD_DARK, ThemeEngine.RADIUS_SMALL));
        et.setPadding(dp(10), dp(10), dp(10), dp(10));
        et.setLayoutParams(new LinearLayout.LayoutParams(0, dp(38), 1));
        ((LinearLayout.LayoutParams)et.getLayoutParams()).leftMargin = dp(12);
        row.addView(et);

        parent.addView(row);
        return et;
    }

    private CheckBox makeToggleRow(LinearLayout parent, String label, boolean def) {
        LinearLayout row = glassRow();

        TextView lbl = makeRowLabel(label);
        row.addView(lbl);

        CheckBox cb = new CheckBox(this);
        cb.setChecked(def);
        cb.setButtonDrawable(null);
        cb.setBackground(toggleBg(def));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(46), dp(28));
        clp.leftMargin = dp(12);
        cb.setLayoutParams(clp);
        cb.setOnCheckedChangeListener((button, checked) ->
                button.setBackground(toggleBg(checked)));
        parent.addView(row);
        return cb;
    }

    private LinearLayout glassRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(ThemeEngine.glassCard(
                ThemeEngine.BG_CARD, ThemeEngine.RADIUS_SMALL, ThemeEngine.BORDER_CARD));
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.setMargins(0, dp(3), 0, dp(3));
        row.setLayoutParams(rlp);
        return row;
    }

    private TextView makeRowLabel(String text) {
        TextView lbl = new TextView(this);
        lbl.setText(text);
        lbl.setTextColor(ThemeEngine.TEXT_SECONDARY);
        lbl.setTextSize(14);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        return lbl;
    }

    private EditText makeRowEdit(String def) {
        EditText et = new EditText(this);
        et.setText(def);
        et.setTextColor(ThemeEngine.TEXT_PRIMARY);
        et.setTextSize(14);
        et.setTypeface(null, 1);
        et.setGravity(Gravity.CENTER);
        et.setBackground(ThemeEngine.roundedBg(ThemeEngine.BG_CARD_DARK, ThemeEngine.RADIUS_SMALL));
        et.setLayoutParams(new LinearLayout.LayoutParams(dp(72), dp(36)));
        return et;
    }

    private CheckBox[] stratBtns;

    private LinearLayout makeStrategySelector(LinearLayout parent) {
        LinearLayout row = glassRow();
        row.setPadding(dp(14), dp(6), dp(14), dp(6));

        String[] modes = {"高优", "中优", "保底"};
        String[] descs = {"秒抢优质单", "智能均衡", "保底接单"};
        int[] colors = {ThemeEngine.NEON_GREEN, ThemeEngine.NEON_CYAN, ThemeEngine.NEON_ORANGE};
        int currentMode = 1; // default: 中优
        String saved = config.getStrategyMode();
        for (int i = 0; i < modes.length; i++) {
            if (saved.equals(modes[i])) currentMode = i;
        }

        stratBtns = new CheckBox[3];
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            LinearLayout btn = new LinearLayout(this);
            btn.setOrientation(LinearLayout.VERTICAL);
            btn.setGravity(Gravity.CENTER);
            btn.setBackground(ThemeEngine.glassCard(
                    i == currentMode ? 0x2200E5FF : ThemeEngine.BG_CARD_DARK,
                    ThemeEngine.RADIUS_SMALL,
                    i == currentMode ? 0x4400E5FF : ThemeEngine.BORDER_CARD));
            btn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1));
            ((LinearLayout.LayoutParams)btn.getLayoutParams()).setMargins(dp(2), 0, dp(2), 0);
            btn.setClickable(true);
            btn.setTag(i);

            // 模拟Checkbox选中状态
            CheckBox cb = new CheckBox(this);
            cb.setChecked(i == currentMode);
            cb.setButtonDrawable(null);
            cb.setBackground(ThemeEngine.dot(colors[i], 6, cb));
            cb.setLayoutParams(new LinearLayout.LayoutParams(dp(6), dp(6)));
            btn.addView(cb);
            stratBtns[i] = cb;

            TextView tv = new TextView(this);
            tv.setText(modes[i]);
            tv.setTextColor(i == currentMode ? colors[i] : ThemeEngine.TEXT_DISABLED);
            tv.setTextSize(10);
            tv.setTypeface(null, 1);
            btn.addView(tv);

            btn.setOnClickListener(v -> selectStrategy(idx, modes, descs, colors));

            row.addView(btn);
        }

        return row;
    }

    private void selectStrategy(int idx, String[] modes, String[] descs, int[] colors) {
        for (int i = 0; i < 3; i++) {
            stratBtns[i].setChecked(i == idx);
            LinearLayout p = (LinearLayout) stratBtns[i].getParent();
            p.setBackground(ThemeEngine.glassCard(
                    i == idx ? 0x2200E5FF : ThemeEngine.BG_CARD_DARK,
                    ThemeEngine.RADIUS_SMALL,
                    i == idx ? 0x4400E5FF : ThemeEngine.BORDER_CARD));
            ((TextView) p.getChildAt(1)).setTextColor(
                    i == idx ? colors[i] : ThemeEngine.TEXT_DISABLED);
        }
        // 保存
        config.setStrategyMode(modes[idx]);
    }

    /** 多配置场景选择器 */
    private String[] scenes = {"默认", "午高峰", "晚高峰", "周末"};
    private LinearLayout makeSceneSelector(LinearLayout parent) {
        LinearLayout row = glassRow();
        row.setPadding(dp(10), dp(6), dp(10), dp(6));
        row.setOrientation(LinearLayout.VERTICAL);

        // 场景按钮行
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        String current = config.getSceneProfile();
        for (int i = 0; i < scenes.length; i++) {
            final String sceneName = scenes[i];
            boolean active = sceneName.equals(current);

            Button btn = new Button(this);
            btn.setText(sceneName);
            btn.setTextSize(11);
            btn.setTypeface(null, active ? 1 : 0);
            btn.setTextColor(active ? 0xFF0A0A14 : ThemeEngine.TEXT_SECONDARY);
            btn.setBackground(ThemeEngine.roundedBg(
                    active ? ThemeEngine.NEON_CYAN : ThemeEngine.BG_CARD_DARK,
                    ThemeEngine.RADIUS_SMALL));
            btn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(32), 1));
            ((LinearLayout.LayoutParams)btn.getLayoutParams()).setMargins(dp(2), 0, dp(2), 0);

            btn.setOnClickListener(v -> {
                loadScene(sceneName);
                // 刷新所有btn高亮
                for (int j = 0; j < btnRow.getChildCount(); j++) {
                    View b = btnRow.getChildAt(j);
                    if (b instanceof Button) {
                        boolean a = ((Button) b).getText().toString().equals(sceneName);
                        ((Button) b).setTextColor(a ? 0xFF0A0A14 : ThemeEngine.TEXT_SECONDARY);
                        ((Button) b).setTypeface(null, a ? 1 : 0);
                        b.setBackground(ThemeEngine.roundedBg(
                                a ? ThemeEngine.NEON_CYAN : ThemeEngine.BG_CARD_DARK,
                                ThemeEngine.RADIUS_SMALL));
                    }
                }
            });

            btnRow.addView(btn);
        }
        row.addView(btnRow);

        // 场景说明
        TextView hint = new TextView(this);
        hint.setText("点击切换配置场景（含金额/距离/方向/策略）");
        hint.setTextColor(ThemeEngine.TEXT_MUTED);
        hint.setTextSize(9);
        hint.setPadding(dp(4), dp(6), 0, 0);
        row.addView(hint);

        return row;
    }

    private void loadScene(String scene) {
        config.setSceneProfile(scene);
        switch (scene) {
            case "午高峰":
                config.setMinPrice(15); config.setMaxPrice(50);
                config.setMaxDistance(3); config.setStrategyMode("高优");
                break;
            case "晚高峰":
                config.setMinPrice(18); config.setMaxPrice(60);
                config.setMaxDistance(4); config.setStrategyMode("高优");
                break;
            case "周末":
                config.setMinPrice(12); config.setMaxPrice(30);
                config.setMaxDistance(5); config.setStrategyMode("保底");
                break;
            default: // 默认
                config.setMinPrice(10); config.setMaxPrice(200);
                config.setMaxDistance(5); config.setStrategyMode("中优");
                break;
        }
        loadValues();
        Toast.makeText(this, "已切换: " + scene + "模式", Toast.LENGTH_SHORT).show();
    }

    private GradientDrawable toggleBg(boolean on) {
        return ThemeEngine.roundedBg(on ? 0xFF00E5FF : 0xFF33334D, dp(14));
    }

    // ==================== 逻辑 ====================

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } catch (Exception ignored) {}
        try {
            getWindow().setNavigationBarColor(ThemeEngine.BG_DARK);
            getWindow().setStatusBarColor(ThemeEngine.BG_DARK);
        } catch (Exception ignored) {}
    }

    private void loadValues() {
        etMin.setText(fmt(config.getMinPrice()));
        etMax.setText(fmt(config.getMaxPrice()));
        etDist.setText(fmt(config.getMaxDistance()));
        etScan.setText(String.valueOf(config.getScanInterval()));
        etDelay.setText(String.valueOf(config.getGrabDelay()));
        etCool.setText(String.valueOf(config.getCooldownSeconds()));
        etScore.setText(String.valueOf(config.getMinScore()));
        etPref.setText(TextUtils.join(",", config.getPreferredDirections()));
        etExcl.setText(TextUtils.join(",", config.getExcludedDirections()));
        cbVib.setChecked(config.isVibrateOnGrab());
        cbNot.setChecked(config.isNotifyOnGrab());
        cbTts.setChecked(config.isTtsEnabled());
        cbKeep.setChecked(config.isKeepScreenOn());
        cbPower.setChecked(config.isPowerSaving());
        cbFloat.setChecked(config.isFloatingBall());
        cbDnd.setChecked(config.isDndEnabled());
        etDndStart.setText(String.valueOf(config.getDndStartHour()));
        etDndEnd.setText(String.valueOf(config.getDndEndHour()));
    }

    private void doSave() {
        try {
            config.setMinPrice(pf(etMin, 10));
            config.setMaxPrice(pf(etMax, 200));
            config.setMaxDistance(pf(etDist, 5));
            config.setScanInterval(pi(etScan, 2000));
            config.setGrabDelay(pi(etDelay, 100));
            config.setCooldownSeconds(pi(etCool, 30));
            config.setPreferredDirections(etPref.getText().toString().trim());
            config.setExcludedDirections(etExcl.getText().toString().trim());
            config.setVibrateOnGrab(cbVib.isChecked());
            config.setNotifyOnGrab(cbNot.isChecked());
            config.setTtsEnabled(cbTts.isChecked());
            config.setKeepScreenOn(cbKeep.isChecked());
            config.setPowerSaving(cbPower.isChecked());
            config.setFloatingBall(cbFloat.isChecked());
            config.setDndEnabled(cbDnd.isChecked());
            config.setDndStartHour(pi(etDndStart, 23));
            config.setDndEndHour(pi(etDndEnd, 6));
            config.setMinScore(pi(etScore, 20));
            Toast.makeText(this, "✓ 已保存", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "✗ 保存失败", Toast.LENGTH_SHORT).show(); }
    }

    private String fmt(float v) { return v == (int)v ? String.valueOf((int)v) : String.format("%.1f", v); }
    private float pf(EditText et, float d) {
        String s = et.getText().toString().trim(); return s.isEmpty() ? d : Float.parseFloat(s);
    }
    private int pi(EditText et, int d) {
        String s = et.getText().toString().trim(); return s.isEmpty() ? d : Integer.parseInt(s);
    }
        private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
}
