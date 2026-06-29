package com.sf.riderhelper;

import android.animation.ValueAnimator;
import android.app.Activity;
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
    private EditText etMin, etMax, etDist, etPref, etExcl, etScan, etDelay, etCool;
    private CheckBox cbVib, cbNot, cbKeep;

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

        content.addView(makeSection("🔔 提醒设置"));
        cbVib = makeToggleRow(content, "抢单震动", true);
        cbNot = makeToggleRow(content, "抢单通知", true);
        cbKeep = makeToggleRow(content, "屏幕常亮", true);

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

    private GradientDrawable toggleBg(boolean on) {
        return ThemeEngine.roundedBg(on ? 0xFF00E5FF : 0xFF33334D, dp(14));
    }

    // ==================== 逻辑 ====================

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setNavigationBarColor(ThemeEngine.BG_DARK);
        getWindow().setStatusBarColor(ThemeEngine.BG_DARK);
    }

    private void loadValues() {
        etMin.setText(fmt(config.getMinPrice()));
        etMax.setText(fmt(config.getMaxPrice()));
        etDist.setText(fmt(config.getMaxDistance()));
        etScan.setText(String.valueOf(config.getScanInterval()));
        etDelay.setText(String.valueOf(config.getGrabDelay()));
        etCool.setText(String.valueOf(config.getCooldownSeconds()));
        etPref.setText(TextUtils.join(",", config.getPreferredDirections()));
        etExcl.setText(TextUtils.join(",", config.getExcludedDirections()));
        cbVib.setChecked(config.isVibrateOnGrab());
        cbNot.setChecked(config.isNotifyOnGrab());
        cbKeep.setChecked(config.isKeepScreenOn());
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
            config.setKeepScreenOn(cbKeep.isChecked());
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
    private int dp(int n) { return ThemeEngine.dp(n, findViewById(android.R.id.content)); }
}
