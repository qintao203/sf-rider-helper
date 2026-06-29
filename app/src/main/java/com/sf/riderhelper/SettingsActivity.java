package com.sf.riderhelper;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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
        config = ConfigManager.getInstance(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0F0F1A);
        root.setPadding(12, 12, 12, 12);

        // 标题 + 返回
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);

        Button back = new Button(this);
        back.setText("< 返回");
        back.setTextColor(0xFFCCCCCC);
        back.setTextSize(14);
        back.setBackgroundColor(0xFF333355);
        back.setOnClickListener(v -> finish());
        header.addView(back);

        TextView title = new TextView(this);
        title.setText("设置");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(null, 1);
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(title);

        root.addView(header);

        // 滚动区域
        ScrollView sv = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        content.addView(makeSectionLabel("订单过滤"));
        etMin = addSetting(content, "最低金额", "10.00");
        etMax = addSetting(content, "最高金额", "200.00");
        etDist = addSetting(content, "最大距离", "5.0");

        content.addView(makeSectionLabel("方向偏好"));
        etPref = addInput(content, "优先方向", "科技园,福田,南山");
        etExcl = addInput(content, "排除方向", "龙岗,坪山");

        content.addView(makeSectionLabel("抢单行为"));
        etScan = addSetting(content, "扫描间隔", "2000");
        etDelay = addSetting(content, "点击延迟", "100");
        etCool = addSetting(content, "失败冷却", "30");

        content.addView(makeSectionLabel("提醒设置"));
        cbVib = addToggle(content, "抢单震动");
        cbNot = addToggle(content, "抢单通知");
        cbKeep = addToggle(content, "屏幕常亮");

        // 保存按钮
        Button save = new Button(this);
        save.setText("保存配置");
        save.setTextColor(0xFF1A1A2E);
        save.setTextSize(16);
        save.setTypeface(null, 1);
        save.setBackgroundColor(0xFFFFD700);
        save.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 48));
        ((LinearLayout.LayoutParams)save.getLayoutParams()).setMargins(0, 16, 0, 0);
        save.setOnClickListener(v -> save());

        content.addView(save);

        sv.addView(content);
        root.addView(sv);

        setContentView(root);
        load();
    }

    private TextView makeSectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF666666);
        tv.setTextSize(12);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams)tv.getLayoutParams()).setMargins(0, 16, 0, 4);
        return tv;
    }

    private EditText addSetting(LinearLayout parent, String label, String def) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(0xFF16213E);
        row.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, 2, 0, 2);
        row.setLayoutParams(rlp);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFFCCCCCC);
        lbl.setTextSize(14);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(lbl);

        EditText et = new EditText(this);
        et.setText(def);
        et.setTextColor(0xFFFFFFFF);
        et.setTextSize(14);
        et.setTypeface(null, 1);
        et.setGravity(Gravity.CENTER);
        et.setBackgroundColor(0xFF0F3460);
        et.setLayoutParams(new LinearLayout.LayoutParams(80, 36));
        row.addView(et);

        parent.addView(row);
        return et;
    }

    private EditText addInput(LinearLayout parent, String label, String hint) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(0xFF16213E);
        row.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, 2, 0, 2);
        row.setLayoutParams(rlp);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFFCCCCCC);
        lbl.setTextSize(14);
        row.addView(lbl);

        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextColor(0xFFFFFFFF);
        et.setHintTextColor(0xFF666666);
        et.setTextSize(13);
        et.setBackgroundColor(0xFF0F3460);
        et.setPadding(8, 0, 8, 0);
        et.setLayoutParams(new LinearLayout.LayoutParams(0, 36, 1));
        ((LinearLayout.LayoutParams)et.getLayoutParams()).leftMargin = 12;
        row.addView(et);

        parent.addView(row);
        return et;
    }

    private CheckBox addToggle(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(0xFF16213E);
        row.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, 2, 0, 2);
        row.setLayoutParams(rlp);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFFCCCCCC);
        lbl.setTextSize(14);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(lbl);

        CheckBox cb = new CheckBox(this);
        cb.setChecked(true);
        cb.setButtonDrawable(null);
        cb.setBackgroundColor(0xFF2E7D32);
        cb.setOnCheckedChangeListener((button, checked) ->
            button.setBackgroundColor(checked ? 0xFF2E7D32 : 0xFF333333));
        row.addView(cb);

        parent.addView(row);
        return cb;
    }

    private void load() {
        etMin.setText(String.valueOf(config.getMinPrice()));
        etMax.setText(String.valueOf(config.getMaxPrice()));
        etDist.setText(String.valueOf(config.getMaxDistance()));
        etScan.setText(String.valueOf(config.getScanInterval()));
        etDelay.setText(String.valueOf(config.getGrabDelay()));
        etCool.setText(String.valueOf(config.getCooldownSeconds()));
        etPref.setText(TextUtils.join(",", config.getPreferredDirections()));
        etExcl.setText(TextUtils.join(",", config.getExcludedDirections()));
        cbVib.setChecked(config.isVibrateOnGrab());
        cbNot.setChecked(config.isNotifyOnGrab());
        cbKeep.setChecked(config.isKeepScreenOn());
    }

    private void save() {
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
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show(); }
    }

    private float pf(EditText et, float d) {
        String s = et.getText().toString().trim(); return s.isEmpty() ? d : Float.parseFloat(s);
    }
    private int pi(EditText et, int d) {
        String s = et.getText().toString().trim(); return s.isEmpty() ? d : Integer.parseInt(s);
    }
}
