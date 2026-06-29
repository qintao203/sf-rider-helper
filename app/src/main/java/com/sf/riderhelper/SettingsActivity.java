package com.sf.riderhelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
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
        root.setBackgroundColor(0xFF0A0A14);

        // 顶部导航
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(12), dp(16), dp(12));
        header.setBackgroundColor(0xFF12121E);

        Button back = new Button(this);
        back.setText("← 返回");
        back.setTextColor(0xFF9E9EB8);
        back.setTextSize(14);
        back.setBackground(null);
        back.setOnClickListener(v -> finish());
        header.addView(back);

        TextView title = new TextView(this);
        title.setText("设置");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(17);
        title.setTypeface(null, 1);
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(title);

        // 占位让标题居中
        TextView spacer = new TextView(this);
        spacer.setText("    ");
        spacer.setTextSize(14);
        header.addView(spacer);

        root.addView(header);

        // 滚动内容
        ScrollView sv = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(24));

        content.addView(section("金额过滤"));
        etMin = addInputRow(content, "最低金额", "10.00", "元");
        etMax = addInputRow(content, "最高金额", "200.00", "元");

        content.addView(section("距离限制"));
        etDist = addInputRow(content, "最大距离", "5.0", "km");

        content.addView(section("方向偏好"));
        etPref = addTextRow(content, "优先方向", "科技园,福田,南山");
        etExcl = addTextRow(content, "排除方向", "龙岗,坪山");

        content.addView(section("抢单行为"));
        etScan = addInputRow(content, "扫描间隔", "2000", "ms");
        etDelay = addInputRow(content, "点击延迟", "100", "ms");
        etCool = addInputRow(content, "失败冷却", "30", "秒");

        content.addView(section("提醒设置"));
        cbVib = addToggleRow(content, "抢单震动", true);
        cbNot = addToggleRow(content, "抢单通知", true);
        cbKeep = addToggleRow(content, "屏幕常亮", true);

        // 保存按钮
        Button save = new Button(this);
        save.setText("保存配置");
        save.setTextColor(0xFF0A0A14);
        save.setTextSize(15);
        save.setTypeface(null, 1);
        save.setBackground(btnBg(0xFFFFD700, 10));
        save.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(46)));
        ((LinearLayout.LayoutParams)save.getLayoutParams()).setMargins(0, dp(20), 0, 0);
        save.setOnClickListener(v -> doSave());
        content.addView(save);

        sv.addView(content);
        root.addView(sv);

        setContentView(root);
        loadValues();
    }

    private View section(String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFF555570);
        tv.setTextSize(11);
        tv.setPadding(dp(4), dp(18), dp(4), dp(6));
        return tv;
    }

    private EditText addInputRow(LinearLayout parent, String label, String def, String unit) {
        LinearLayout row = row();
        row.addView(makeLabel(label));
        EditText et = makeEdit(def);
        row.addView(et);
        TextView unitLabel = new TextView(this);
        unitLabel.setText(unit);
        unitLabel.setTextColor(0xFF6B6B80);
        unitLabel.setTextSize(12);
        unitLabel.setPadding(dp(8), 0, 0, 0);
        unitLabel.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(unitLabel);
        parent.addView(row);
        return et;
    }

    private EditText addTextRow(LinearLayout parent, String label, String hint) {
        LinearLayout row = row();
        row.addView(makeLabel(label));
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextColor(0xFFFFFFFF);
        et.setHintTextColor(0xFF3A3A55);
        et.setTextSize(13);
        et.setBackground(card(0xFF151525));
        et.setPadding(dp(10), dp(10), dp(10), dp(10));
        et.setLayoutParams(new LinearLayout.LayoutParams(0, dp(38), 1));
        ((LinearLayout.LayoutParams)et.getLayoutParams()).leftMargin = dp(12);
        row.addView(et);
        parent.addView(row);
        return et;
    }

    private CheckBox addToggleRow(LinearLayout parent, String label, boolean def) {
        LinearLayout row = row();
        row.addView(makeLabel(label));

        CheckBox cb = new CheckBox(this);
        cb.setChecked(def);
        cb.setButtonDrawable(null);
        cb.setBackground(dot(def ? 0xFF2E7D32 : 0xFF33334D, 22));
        cb.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(28)));
        ((LinearLayout.LayoutParams)cb.getLayoutParams()).leftMargin = dp(12);
        cb.setOnCheckedChangeListener((button, checked) ->
            button.setBackgroundColor(checked ? 0xFF2E7D32 : 0xFF33334D));
        parent.addView(row);
        return cb;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(card(0xFF151525));
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(rlp);
        return row;
    }

    private TextView makeLabel(String text) {
        TextView lbl = new TextView(this);
        lbl.setText(text);
        lbl.setTextColor(0xFFCCCCDD);
        lbl.setTextSize(14);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        return lbl;
    }

    private EditText makeEdit(String def) {
        EditText et = new EditText(this);
        et.setText(def);
        et.setTextColor(0xFFFFFFFF);
        et.setTextSize(14);
        et.setTypeface(null, 1);
        et.setGravity(Gravity.CENTER);
        et.setBackground(card(0xFF1E1E32));
        et.setLayoutParams(new LinearLayout.LayoutParams(dp(72), dp(36)));
        return et;
    }

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setNavigationBarColor(0xFF0A0A14);
        getWindow().setStatusBarColor(0xFF0A0A14);
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
        } catch (Exception e) { Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show(); }
    }

    private String fmt(float v) { return v == (int)v ? String.valueOf((int)v) : String.format("%.1f", v); }
    private float pf(EditText et, float d) {
        String s = et.getText().toString().trim(); return s.isEmpty() ? d : Float.parseFloat(s);
    }
    private int pi(EditText et, int d) {
        String s = et.getText().toString().trim(); return s.isEmpty() ? d : Integer.parseInt(s);
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable card(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(8)); return g;
    }
    private GradientDrawable btnBg(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r)); return g;
    }
    private GradientDrawable dot(int color, int r) {
        GradientDrawable g = new GradientDrawable(); g.setShape(GradientDrawable.OVAL);
        g.setColor(color); return g;
    }
}
